package com.icebrown.opencsc.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialsAuthorizeResponse {
    @JsonProperty("SAD")
    private String sad;
    private Long expiresIn;
}
