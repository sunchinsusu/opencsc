package com.icebrown.opencsc.exception;

public class CscAuthorizationException extends CscException {
    public CscAuthorizationException(String message) {
        super(message);
    }

    public CscAuthorizationException(String message, Throwable cause) {
        super(message, cause);
    }
}
