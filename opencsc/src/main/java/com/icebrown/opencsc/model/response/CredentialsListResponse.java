package com.icebrown.opencsc.model.response;

import com.icebrown.opencsc.domain.CredentialInfo;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CredentialsListResponse {
    private List<String> credentialIDs;
    private List<CredentialInfo> credentialInfos;
    private Boolean onlyValid;
}
