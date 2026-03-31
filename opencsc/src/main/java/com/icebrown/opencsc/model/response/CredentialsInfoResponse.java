package com.icebrown.opencsc.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.icebrown.opencsc.domain.AuthInfo;
import com.icebrown.opencsc.domain.CertInfo;
import com.icebrown.opencsc.domain.KeyInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialsInfoResponse {
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
