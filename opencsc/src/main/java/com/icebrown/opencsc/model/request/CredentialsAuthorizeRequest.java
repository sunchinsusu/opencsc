package com.icebrown.opencsc.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.icebrown.opencsc.domain.AuthObject;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialsAuthorizeRequest {
    @NotBlank
    private String credentialID;
    @NotNull
    private Integer numSignatures;
    private List<String> hashes;
    private String hashAlgorithmOID;
    private List<AuthObject> authData;
    private String description;
    private String clientData;
}
