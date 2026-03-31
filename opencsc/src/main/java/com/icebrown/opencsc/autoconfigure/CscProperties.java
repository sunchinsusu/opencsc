package com.icebrown.opencsc.autoconfigure;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.Map;

@Data
@ConfigurationProperties(prefix = "opencsc")
public class CscProperties {

    /**
     * Base URL of the CSC Resource Server (credentials, signatures endpoints).
     */
    private String baseUrl;

    /**
     * Authorization Server URL. If not set, falls back to baseUrl.
     * Use this when the OAuth2 token endpoint is on a different server.
     */
    private String authServerUrl;

    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(30);

    private OAuth2Properties oauth2 = new OAuth2Properties();
    private CredentialsEndpoints credentials = new CredentialsEndpoints();
    private SignaturesEndpoints signatures = new SignaturesEndpoints();
    private RetryProperties retry = new RetryProperties();
    private CircuitBreakerProperties circuitBreaker = new CircuitBreakerProperties();
    private PdfProperties pdf = new PdfProperties();

    @Data
    public static class OAuth2Properties {
        private String tokenPath = "/oauth2/token";
        private String clientId;
        private String clientSecret;
        private String scope;
        private int tokenExpirySkew = 30;
        private long refreshCheckInterval = 30000;
        private Map<String, String> additionalParams;
    }

    @Data
    public static class CredentialsEndpoints {
        private String listPath = "/credentials/list";
        private String infoPath = "/credentials/info";
        private String authorizePath = "/credentials/authorize";
        private String getChallengePath = "/credentials/getChallenge";
    }

    @Data
    public static class SignaturesEndpoints {
        private String signHashPath = "/signatures/signHash";
    }

    @Data
    public static class RetryProperties {
        private boolean enabled = true;
        private int maxAttempts = 3;
        private Duration waitDuration = Duration.ofMillis(500);
        private double exponentialBackoffMultiplier = 2.0;
    }

    @Data
    public static class CircuitBreakerProperties {
        private boolean enabled = true;
        private int slidingWindowSize = 10;
        private float failureRateThreshold = 50;
        private Duration waitDurationInOpenState = Duration.ofSeconds(30);
        private int permittedCallsInHalfOpen = 3;
    }

    @Data
    public static class PdfProperties {
        private boolean enabled = true;
        private String defaultHashAlgorithm = "2.16.840.1.101.3.4.2.1";
        private int signatureReservedSpaceBytes = 32768;
        private String defaultReason = "Digitally signed";
        private String defaultLocation = "";
    }
}
