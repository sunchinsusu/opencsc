package com.icebrown.opencsc.model.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CredentialsListRequest {
    private String userID;
    private Boolean credentialInfo;
    private String certificates;
    private Boolean certInfo;
    private Boolean authInfo;
    private Boolean onlyValid;
    private String lang;
    private String clientData;
}
