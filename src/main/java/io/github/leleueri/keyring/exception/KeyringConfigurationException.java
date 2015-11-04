package io.github.leleueri.keyring.exception;

/**
 * Created by eric on 07/10/15.
 */
public class KeyringConfigurationException extends KeyringException {

    public KeyringConfigurationException(String message) {
        super(message);
    }

    public KeyringConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
