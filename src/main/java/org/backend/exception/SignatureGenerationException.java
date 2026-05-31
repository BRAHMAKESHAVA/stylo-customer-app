package org.backend.exception;

public class SignatureGenerationException extends RuntimeException {
    public SignatureGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
