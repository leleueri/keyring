package io.github.leleueri.keyring.provider;

import io.github.leleueri.keyring.bean.SecretKey;
import io.github.leleueri.keyring.exception.KeyringApplicativeException;
import io.github.leleueri.keyring.exception.KeyringConfigurationException;
import io.vertx.core.Handler;
import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.io.FileInputStream;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.util.*;
import io.vertx.core.Future;

import javax.crypto.spec.SecretKeySpec;

/**
 * Created by eric on 07/10/15.
 */
public class KeystoreProvider {

    private volatile KeyStore ks;

    private String keyPassword;

    private String path;
    private String pwd;
    private String type;

    private void assertNotNull(String value, String field) {
        if (value == null) {
            throw new KeyringConfigurationException(field + " is required");
        }
    }

    public KeystoreProvider(String type, String pwd, String path, String keypwd) {
        loadKeystore(type, pwd, path, keypwd);
    }

    private void loadKeystore(String type, String pwd, String path, String keypwd) {
        assertNotNull(type, "Keystore Type");
        assertNotNull(pwd, "Keystore Password");
        assertNotNull(path, "Keystore Path");
        assertNotNull(keypwd, "Key Password");

        this.keyPassword = keypwd;
        this.path = path;
        this.pwd = pwd;
        this.type = type;

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
        TreeMap<String, SecretKey> result = new TreeMap<>();
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

    public Optional<SecretKey> getSecretKey(String alias) {
        try {
            final Optional<Key> optKey = Optional.of(ks.getKey(alias, keyPassword.toCharArray()));
            return optKey.map(k -> {
                SecretKey sKey = new SecretKey();
                sKey.setAlias(alias);
                sKey.setAlgorithm(k.getAlgorithm());
                sKey.setFormat(k.getFormat());
                sKey.setB64Key(new BASE64Encoder().encode(k.getEncoded()));
                return sKey;
            });
        } catch (KeyStoreException e) {
            throw new KeyringApplicativeException("Unable to read alias '" + alias + "' from the keystore instance", e);
        } catch (NoSuchAlgorithmException|UnrecoverableKeyException e) {
            throw new KeyringApplicativeException("Unable to read key", e);
        }
    }

    public Set<String> listAlias() {
        TreeSet<String> result = new TreeSet<>();
        try {
            Enumeration<String> aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                if (ks.isKeyEntry(alias)) {
                    result.add(alias);
                }
            }
        } catch (KeyStoreException e) {
            throw new KeyringApplicativeException("Unable to list Aliases from the keystore instance", e);
        }
        return result;
    }


    public String addSecretKey(SecretKey sKey) {
        String alias = sKey.getAlias();
        try {
            SecretKeySpec keySpec = new SecretKeySpec(new BASE64Decoder().decodeBuffer(sKey.getB64Key()), sKey.getAlgorithm());
            this.ks.setKeyEntry(alias, keySpec, keyPassword.toCharArray(), null);

            Path newFile = Paths.get(path+".tmp");
            this.ks.store(new FileOutputStream(newFile.toFile()), pwd.toCharArray());
            newFile.toFile().renameTo(Paths.get(path).toFile());

            loadKeystore(type, pwd, path, keyPassword);
            return alias;
        } catch (KeyStoreException|NoSuchAlgorithmException|CertificateException e) {
            e.printStackTrace();
            throw new KeyringApplicativeException("Unable to add alias '" + alias + "' into the keystore instance", e);
        } catch (IOException e) {
            throw new KeyringApplicativeException("Unable to save the key", e);
        }
    }
}
