package com.jpmorgan.quorum.encryption.unbound;

import com.dyadicsec.cryptoki.*;
import com.quorum.tessera.encryption.KeyPair;
import com.quorum.tessera.encryption.PrivateKey;
import com.quorum.tessera.encryption.PublicKey;
import com.quorum.tessera.encryption.SharedKey;
import com.quorum.tessera.encryption.Encryptor;
import com.quorum.tessera.encryption.EncryptorException;
import com.quorum.tessera.encryption.Nonce;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.*;

public class UnboundEncryptor implements Encryptor
{
  static final int AES_GCM_IV_LEN = 16;
  static final int AES_GCM_TAG_LEN = 16;
  static final int AES_KEY_LEN = 16;
  static final byte[] P256_OID = new byte[] {0x06, 0x08, 0x2a, (byte)0x86, 0x48, (byte)0xce, 0x3d, 0x03, 0x01, 0x07};

  private final SecureRandom secureRandom = new SecureRandom();

  public UnboundEncryptor() throws EncryptorException
  {
    try { Library.C_Initialize(); }
    catch (CKR_Exception e)
    {
      if (e.errorCode!=CK.CKR_CRYPTOKI_ALREADY_INITIALIZED) throw new EncryptorException("Unable initialize UnboundEncryptor");
    }
  }

  private static byte[] longToBytes(long x)
  {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.putLong(0, x);
    return buffer.array();
  }

  private static long bytesToLong(byte[] bytes)
  {
    ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
    buffer.put(bytes, 0, bytes.length);
    buffer.flip();//need flip
    return buffer.getLong();
  }

  @Override
  public KeyPair generateNewKeys()
  {
    CK_SESSION_HANDLE hSession = null;
    try
    {
      hSession = Library.C_OpenSession(0, CK.CKF_RW_SESSION | CK.CKF_SERIAL_SESSION);
      int[] keyHandles = Library.C_GenerateKeyPair(hSession, new CK_MECHANISM(CK.CKM_EC_KEY_PAIR_GEN),
      new CK_ATTRIBUTE[]
      {
        new CK_ATTRIBUTE(CK.CKA_TOKEN, false),
        new CK_ATTRIBUTE(CK.CKA_EC_PARAMS, P256_OID),
      },
      new CK_ATTRIBUTE[]
      {
        new CK_ATTRIBUTE(CK.CKA_TOKEN, true),
        new CK_ATTRIBUTE(CK.CKA_DERIVE, true),
        new CK_ATTRIBUTE(CK.CKA_SIGN, false),
      });
      int hPub = keyHandles[0];
      int hPrv = keyHandles[1];

      CK_ATTRIBUTE[] tPub = new CK_ATTRIBUTE[] {new CK_ATTRIBUTE(CK.CKA_EC_POINT)};
      Library.C_GetAttributeValue(hSession, hPub, tPub);
      byte[] pubKeyValue = (byte[])tPub[0].pValue;

      Library.C_DestroyObject(hSession, hPub);

      CK_ATTRIBUTE[] tPrv = new CK_ATTRIBUTE[] {new CK_ATTRIBUTE(CK.DYCKA_UID)};
      Library.C_GetAttributeValue(hSession, hPrv, tPrv);
      long uid = tPrv[0].getLongValue();
      byte[] prvKeyValue = longToBytes(uid);

      return new KeyPair(PublicKey.from(pubKeyValue), PrivateKey.from(prvKeyValue));
    }
    catch (CKR_Exception e)
    {
       throw new EncryptorException("unable to generate key pair");
    }
    finally
    {
      try { if (hSession!=null) Library.C_CloseSession(hSession); }
      catch (CKR_Exception e) { }
    }
  }

  @Override
  public SharedKey computeSharedKey(PublicKey publicKey, PrivateKey privateKey)
  {
    CK_SESSION_HANDLE hSession = null;
    try
    {
      long uid = bytesToLong(privateKey.getKeyBytes());

      hSession = Library.C_OpenSession(0, CK.CKF_RW_SESSION | CK.CKF_SERIAL_SESSION);
      Library.C_FindObjectsInit(hSession,
        new CK_ATTRIBUTE[]{
          new CK_ATTRIBUTE(CK.DYCKA_UID, uid)
        });
      int[] handles = Library.C_FindObjects(hSession, 1);
      Library.C_FindObjectsFinal(hSession);
      int hPrv = handles[0];

      CK_ECDH1_DERIVE_PARAMS params = new CK_ECDH1_DERIVE_PARAMS();
      params.kdf = CK.CKD_NULL;
      params.pPublicData = publicKey.getKeyBytes();
      params.pSharedData = null;

      int hSecret = Library.C_DeriveKey(hSession, new CK_MECHANISM(CK.CKM_ECDH1_DERIVE, params), hPrv, new CK_ATTRIBUTE[]
      {
        new CK_ATTRIBUTE(CK.CKA_TOKEN, false),
        new CK_ATTRIBUTE(CK.CKA_CLASS, CK.CKO_SECRET_KEY),
        new CK_ATTRIBUTE(CK.CKA_KEY_TYPE, CK.CKK_GENERIC_SECRET),
        new CK_ATTRIBUTE(CK.CKA_SENSITIVE, false),
        new CK_ATTRIBUTE(CK.CKA_VALUE_LEN, 32),
      });

      CK_ATTRIBUTE[] v = { new CK_ATTRIBUTE(CK.CKA_VALUE) };
      Library.C_GetAttributeValue(hSession, hSecret, v);
      byte[] secret = (byte[])v[0].pValue;

      Library.C_DestroyObject(hSession, hSecret);

      byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret);
      return SharedKey.from(digest);
    }
    catch (NoSuchAlgorithmException | CKR_Exception e)
    {
      throw new EncryptorException("unable to generate shared secret");
    }
    finally
    {
      try { if (hSession!=null) Library.C_CloseSession(hSession); }
      catch (CKR_Exception e) { }
    }
  }

  @Override
  public byte[] sealAfterPrecomputation(byte[] message, Nonce nonce, SharedKey sharedKey)
  {
    try
    {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
      cipher.init(
        Cipher.ENCRYPT_MODE,
        new SecretKeySpec(sharedKey.getKeyBytes(), "AES"),
        new GCMParameterSpec(AES_GCM_TAG_LEN*8, nonce.getNonceBytes()));
      return cipher.doFinal(message);
    }
    catch (GeneralSecurityException e)
    {
      throw new EncryptorException("unable to perform symmetric encryption");
    }
  }

  @Override
  public byte[] openAfterPrecomputation(byte[] cipherText, Nonce nonce, SharedKey sharedKey)
  {
    try
    {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding", "SunJCE");
      cipher.init(
        Cipher.DECRYPT_MODE,
        new SecretKeySpec(sharedKey.getKeyBytes(), "AES"),
        new GCMParameterSpec(AES_GCM_TAG_LEN*8, nonce.getNonceBytes()));
      return cipher.doFinal(cipherText);
    }
    catch (GeneralSecurityException e)
    {
      throw new EncryptorException("unable to perform symmetric decryption");
    }
  }

  @Override
  public byte[] seal(byte[] message, Nonce nonce, PublicKey publicKey, PrivateKey privateKey)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public byte[] open(byte[] cipherText, Nonce nonce, PublicKey publicKey, PrivateKey privateKey)
  {
    throw new UnsupportedOperationException();
  }

  @Override
  public Nonce randomNonce()
  {
    final byte[] nonceBytes = new byte[AES_GCM_IV_LEN];
    secureRandom.nextBytes(nonceBytes);
    return new Nonce(nonceBytes);
  }

  @Override
  public SharedKey createSingleKey()
  {
    final byte[] keyBytes = new byte[AES_KEY_LEN];
    secureRandom.nextBytes(keyBytes);
    return SharedKey.from(keyBytes);
  }
}
