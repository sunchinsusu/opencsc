package com.icebrown.opencsc.domain;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialInfo {
    private String credentialID;
    private String description;
    private String signatureQualifier;
    private KeyInfo key;
    private CertInfo cert;
    private AuthInfo auth;
    @JsonProperty("SCAL")
    private String scal;
    private Integer multisign;
    private String lang;
}
