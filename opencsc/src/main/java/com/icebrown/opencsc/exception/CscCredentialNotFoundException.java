package com.icebrown.opencsc.exception;

public class CscCredentialNotFoundException extends CscException {
    public CscCredentialNotFoundException(String credentialId) {
        super("Credential not found: " + credentialId);
    }
}
