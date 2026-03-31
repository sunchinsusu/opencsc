package com.icebrown.opencsc.model.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OAuth2TokenRequest {
    private String grantType;
    private String clientId;
    private String clientSecret;
    private String scope;
}
