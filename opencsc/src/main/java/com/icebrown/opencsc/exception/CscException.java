package com.icebrown.opencsc.exception;

public class CscException extends RuntimeException {
    public CscException(String message) {
        super(message);
    }

    public CscException(String message, Throwable cause) {
        super(message, cause);
    }
}
