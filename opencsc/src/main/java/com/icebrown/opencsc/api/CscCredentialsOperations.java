package com.icebrown.opencsc.api;

import com.icebrown.opencsc.model.request.*;
import com.icebrown.opencsc.model.response.*;

public interface CscCredentialsOperations {
    CredentialsListResponse listCredentials(CredentialsListRequest request);
    CredentialsInfoResponse getCredentialInfo(CredentialsInfoRequest request);
    CredentialsAuthorizeResponse authorizeCredential(CredentialsAuthorizeRequest request);
    CredentialsGetChallengeResponse getChallenge(CredentialsGetChallengeRequest request);
}
