package io.github.leleueri.keyring.bean;

/**
 * Created by eric on 07/10/15.
 */
public class SecretKey {
    private String alias;

    private String algorithm;

    private String format;

    private String b64Key;

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getB64Key() {
        return b64Key;
    }

    public void setB64Key(String b64Key) {
        this.b64Key = b64Key;
    }
}
