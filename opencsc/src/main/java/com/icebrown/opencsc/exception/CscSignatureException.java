package com.icebrown.opencsc.exception;

public class CscSignatureException extends CscException {
    public CscSignatureException(String message) {
        super(message);
    }

    public CscSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
