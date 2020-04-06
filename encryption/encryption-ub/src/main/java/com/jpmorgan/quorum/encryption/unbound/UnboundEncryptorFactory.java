package com.jpmorgan.quorum.encryption.unbound;

import com.quorum.tessera.encryption.*;
import java.util.Map;

public class UnboundEncryptorFactory implements EncryptorFactory {

    @Override
    public String getType() {
        return "Unbound";
    }

    @Override
    public Encryptor create(Map<String, String> properties) {
        return new UnboundEncryptor();
    }
}
