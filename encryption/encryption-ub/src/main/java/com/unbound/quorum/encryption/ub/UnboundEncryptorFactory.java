package com.unbound.quorum.encryption.ub;

import com.quorum.tessera.encryption.Encryptor;
import com.quorum.tessera.encryption.EncryptorFactory;

import java.util.Map;

public class UnboundEncryptorFactory implements EncryptorFactory {

    @Override
    public String getType() {
        return "UB";
    }

    @Override
    public Encryptor create(Map<String, String> properties) {
        return new UnboundEncryptor();
    }
}
