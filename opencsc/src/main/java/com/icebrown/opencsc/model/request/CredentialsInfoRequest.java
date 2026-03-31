package com.icebrown.opencsc.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialsInfoRequest {
    @NotBlank
    private String credentialID;
    private String certificates;
    private Boolean certInfo;
    private Boolean authInfo;
    private String lang;
    private String clientData;
}
