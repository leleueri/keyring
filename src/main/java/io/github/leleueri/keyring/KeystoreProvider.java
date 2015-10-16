package io.github.leleueri.keyring;

import io.github.leleueri.keyring.bean.SecretKey;
import io.github.leleueri.keyring.exception.KeyringApplicativeException;
import io.github.leleueri.keyring.exception.KeyringConfigurationException;
import sun.misc.BASE64Encoder;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.*;
import java.io.FileInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.*;

/**
 * Created by eric on 07/10/15.
 */
public class KeystoreProvider {

    private volatile KeyStore ks;

    private String keyPassword;

    private void assertNotNull(String value, String field) {
        if (value == null) {
            throw new KeyringConfigurationException(field + " is required");
        }
    }

    public void loadKeystore(String type, String pwd, String path, String keypwd) {
        assertNotNull(type, "Keystore Type");
        assertNotNull(pwd, "Keystore Password");
        assertNotNull(path, "Keystore Path");
        assertNotNull(keypwd, "Key Password");

        this.keyPassword = keypwd;
        try {
            ks = KeyStore.getInstance(type);
            char[] password = pwd.toCharArray();
            try (FileInputStream fis = new FileInputStream(path);) {
                ks.load(fis, password);
            }
        } catch (KeyStoreException|NoSuchAlgorithmException|CertificateException e) {
            throw new KeyringConfigurationException("Unable to initialize KeyStore", e);
        } catch (FileNotFoundException e) {
            throw new KeyringConfigurationException("Wrong KeyStore path", e);
        } catch (IOException e) {
            throw new KeyringConfigurationException("Unable to read KeyStore file", e);
        }
    }

    public Map<String, SecretKey> listSecretKeys() {
        TreeMap<String, SecretKey> result = new TreeMap<>(); // TODO faire deux method (une qui retourne que l'alias, l'autre les bean)
        try {
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isKeyEntry(alias)) {
                    final Key key = ks.getKey(alias, keyPassword.toCharArray());
                    SecretKey sKey = new SecretKey();
                    sKey.setAlias(alias);
                    sKey.setAlgorithm(key.getAlgorithm());
                    sKey.setFormat(key.getFormat());
                    sKey.setB64Key(new BASE64Encoder().encode(key.getEncoded()));
                    result.put(alias, sKey );
                }
            }
        } catch (KeyStoreException e) {
            throw new KeyringApplicativeException("Unable to list Aliases from the keystore instance", e);
        } catch (NoSuchAlgorithmException|UnrecoverableKeyException e) {
            throw new KeyringApplicativeException("Unable to read key", e);
        }
        return result;
    }
}
