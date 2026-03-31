package com.icebrown.opencsc.oauth2;

import com.icebrown.opencsc.autoconfigure.CscProperties;
import com.icebrown.opencsc.exception.CscTokenException;
import com.icebrown.opencsc.model.response.OAuth2TokenResponse;
import com.icebrown.opencsc.spi.AuditPublisher;
import com.icebrown.opencsc.spi.event.TokenAcquiredEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.UUID;

public class OAuth2ClientCredentialsManager {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ClientCredentialsManager.class);
    private static final String TOKEN_KEY = "default";

    private final CscProperties properties;
    private final TokenStore tokenStore;
    private final AuditPublisher auditPublisher;
    private final RestClient restClient;

    public OAuth2ClientCredentialsManager(CscProperties properties,
                                          TokenStore tokenStore,
                                          AuditPublisher auditPublisher,
                                          RestClient restClient) {
        this.properties = properties;
        this.tokenStore = tokenStore;
        this.auditPublisher = auditPublisher;
        this.restClient = restClient;
    }

    public synchronized String getValidAccessToken() {
        if (!tokenStore.isExpired(TOKEN_KEY)) {
            return tokenStore.get(TOKEN_KEY)
                    .map(OAuth2TokenResponse::getAccessToken)
                    .orElseThrow(() -> new CscTokenException("Token store returned empty"));
        }
        return refreshToken();
    }

    public synchronized String refreshToken() {
        log.debug("Fetching new OAuth2 token via client_credentials");
        String tokenUrl = resolveTokenUrl();

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", properties.getOauth2().getClientId());
        formData.add("client_secret", properties.getOauth2().getClientSecret());
        if (properties.getOauth2().getScope() != null) {
            formData.add("scope", properties.getOauth2().getScope());
        }
        if (properties.getOauth2().getAdditionalParams() != null) {
            properties.getOauth2().getAdditionalParams().forEach(formData::add);
        }

        try {
            OAuth2TokenResponse response = restClient.post()
                    .uri(tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body(OAuth2TokenResponse.class);

            if (response == null || response.getAccessToken() == null) {
                throw new CscTokenException("OAuth2 token response is null or missing access_token");
            }

            tokenStore.store(TOKEN_KEY, response);
            log.info("Successfully acquired OAuth2 token, expires_in={}s", response.getExpiresIn());

            auditPublisher.publish(new TokenAcquiredEvent(
                    Instant.now(),
                    UUID.randomUUID().toString(),
                    response.getExpiresIn()
            ));

            return response.getAccessToken();
        } catch (RestClientException e) {
            throw new CscTokenException("Failed to acquire OAuth2 token from " + tokenUrl, e);
        }
    }

    public void refreshIfExpiring() {
        if (tokenStore.isExpired(TOKEN_KEY)) {
            try {
                refreshToken();
            } catch (CscTokenException e) {
                log.warn("Proactive token refresh failed: {}", e.getMessage());
            }
        }
    }

    private String resolveTokenUrl() {
        String authServerUrl = properties.getAuthServerUrl();
        if (authServerUrl == null || authServerUrl.isBlank()) {
            authServerUrl = properties.getBaseUrl();
        }
        return authServerUrl + properties.getOauth2().getTokenPath();
    }
}
