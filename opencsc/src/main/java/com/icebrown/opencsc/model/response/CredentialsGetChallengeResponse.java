package com.icebrown.opencsc.model.response;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialsGetChallengeResponse {
    private String challenge;
}
