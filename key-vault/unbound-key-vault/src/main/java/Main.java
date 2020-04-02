import com.jpmorgan.quorum.encryption.unbound.UnboundEncryptor;
import com.quorum.tessera.encryption.*;

import java.util.Arrays;

public class Main {
    static final String plainText = "PlainText";
    static final byte[] plainData = plainText.getBytes();

    public static void main(String[] args)
    {
        Encryptor encryptor = new UnboundEncryptor();
        KeyPair keyPair1 = encryptor.generateNewKeys();
        KeyPair keyPair2 = encryptor.generateNewKeys();
        SharedKey shared1 = encryptor.computeSharedKey(keyPair1.getPublicKey(), keyPair2.getPrivateKey());
        SharedKey shared2 = encryptor.computeSharedKey(keyPair2.getPublicKey(), keyPair1.getPrivateKey());
        Nonce nonce = encryptor.randomNonce();

        byte[] encrypted = encryptor.sealAfterPrecomputation(plainData, nonce, shared1);
        byte[] decrypted = encryptor.openAfterPrecomputation(encrypted, nonce, shared2);
        if (!Arrays.equals(decrypted,plainData))
        {
            throw new RuntimeException("Encrypt/decrypt mismatch");
        }
   }
}
