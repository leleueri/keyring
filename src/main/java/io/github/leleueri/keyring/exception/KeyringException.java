package io.github.leleueri.keyring.exception;

/**
 * Created by eric on 04/11/15.
 */
public class KeyringException extends RuntimeException {
    public KeyringException(String message) {
        super(message);
    }

    public KeyringException(String message, Throwable cause) {
        super(message, cause);
    }
}
