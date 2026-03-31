package com.icebrown.opencsc.client;

import com.icebrown.opencsc.api.CscCredentialsOperations;
import com.icebrown.opencsc.api.CscSignatureOperations;
import com.icebrown.opencsc.autoconfigure.CscProperties;
import com.icebrown.opencsc.exception.CscAuthorizationException;
import com.icebrown.opencsc.exception.CscException;
import com.icebrown.opencsc.exception.CscSignatureException;
import com.icebrown.opencsc.model.request.*;
import com.icebrown.opencsc.model.response.*;
import com.icebrown.opencsc.oauth2.OAuth2ClientCredentialsManager;
import com.icebrown.opencsc.spi.AuditPublisher;
import com.icebrown.opencsc.spi.event.CscApiErrorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.util.UUID;

public class CscApiClient implements CscCredentialsOperations, CscSignatureOperations {

    private static final Logger log = LoggerFactory.getLogger(CscApiClient.class);

    private final RestClient restClient;
    private final OAuth2ClientCredentialsManager tokenManager;
    private final CscProperties properties;
    private final AuditPublisher auditPublisher;

    public CscApiClient(RestClient restClient,
                         OAuth2ClientCredentialsManager tokenManager,
                         CscProperties properties,
                         AuditPublisher auditPublisher) {
        this.restClient = restClient;
        this.tokenManager = tokenManager;
        this.properties = properties;
        this.auditPublisher = auditPublisher;
    }

    @Override
    public CredentialsListResponse listCredentials(CredentialsListRequest request) {
        String url = properties.getBaseUrl() + properties.getCredentials().getListPath();
        return post(url, request, CredentialsListResponse.class, "credentials/list");
    }

    @Override
    public CredentialsInfoResponse getCredentialInfo(CredentialsInfoRequest request) {
        String url = properties.getBaseUrl() + properties.getCredentials().getInfoPath();
        return post(url, request, CredentialsInfoResponse.class, "credentials/info");
    }

    @Override
    public CredentialsAuthorizeResponse authorizeCredential(CredentialsAuthorizeRequest request) {
        String url = properties.getBaseUrl() + properties.getCredentials().getAuthorizePath();
        try {
            return post(url, request, CredentialsAuthorizeResponse.class, "credentials/authorize");
        } catch (CscException e) {
            throw new CscAuthorizationException("Failed to authorize credential: " + request.getCredentialID(), e);
        }
    }

    @Override
    public CredentialsGetChallengeResponse getChallenge(CredentialsGetChallengeRequest request) {
        String url = properties.getBaseUrl() + properties.getCredentials().getGetChallengePath();
        return post(url, request, CredentialsGetChallengeResponse.class, "credentials/getChallenge");
    }

    @Override
    public SignHashResponse signHash(SignHashRequest request) {
        String url = properties.getBaseUrl() + properties.getSignatures().getSignHashPath();
        try {
            return post(url, request, SignHashResponse.class, "signatures/signHash");
        } catch (CscException e) {
            throw new CscSignatureException("Failed to sign hash for credential: " + request.getCredentialID(), e);
        }
    }

    private <T> T post(String url, Object request, Class<T> responseType, String endpointName) {
        String token = tokenManager.getValidAccessToken();
        log.debug("POST {} with Bearer token", endpointName);

        try {
            T response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .body(request)
                    .retrieve()
                    .body(responseType);

            if (response == null) {
                throw new CscException("Null response from " + endpointName);
            }
            return response;
        } catch (RestClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("CSC API error on {}: status={}, body={}", endpointName, e.getStatusCode().value(), errorBody);

            auditPublisher.publish(new CscApiErrorEvent(
                    Instant.now(),
                    UUID.randomUUID().toString(),
                    endpointName,
                    e.getStatusCode().value(),
                    errorBody
            ));

            throw new CscException("CSC API error on " + endpointName + ": " + e.getStatusCode(), e);
        } catch (CscException e) {
            throw e;
        } catch (Exception e) {
            throw new CscException("Unexpected error calling " + endpointName, e);
        }
    }
}
