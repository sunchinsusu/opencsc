package com.icebrown.opencsc.autoconfigure;

import com.icebrown.opencsc.api.CscRemoteSigningFacade;
import com.icebrown.opencsc.client.CscApiClient;
import com.icebrown.opencsc.crypto.BouncyCastleInitializer;
import com.icebrown.opencsc.crypto.CmsSignatureContainerBuilder;
import com.icebrown.opencsc.crypto.HashAlgorithmRegistry;
import com.icebrown.opencsc.facade.CscRemoteSigningFacadeImpl;
import com.icebrown.opencsc.oauth2.InMemoryTokenStore;
import com.icebrown.opencsc.oauth2.OAuth2ClientCredentialsManager;
import com.icebrown.opencsc.oauth2.TokenStore;
import com.icebrown.opencsc.pdf.PdfHashExtractor;
import com.icebrown.opencsc.spi.AuditPublisher;
import com.icebrown.opencsc.spi.AuthDataProvider;
import com.icebrown.opencsc.spi.CscAuditListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.client.RestClient;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(CscProperties.class)
@ConditionalOnProperty(prefix = "opencsc", name = "base-url")
@EnableScheduling
public class CscAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuditPublisher cscAuditPublisher(ObjectProvider<CscAuditListener> listeners) {
        return new AuditPublisher(listeners.stream().toList());
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenStore cscTokenStore(CscProperties properties) {
        return new InMemoryTokenStore(properties.getOauth2().getTokenExpirySkew());
    }

    @Bean
    @ConditionalOnMissingBean
    public RestClient cscRestClient(CscProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeout());
        factory.setReadTimeout(properties.getReadTimeout());

        return RestClient.builder()
                .requestFactory(factory)
                .build();
    }

    @Bean
    @ConditionalOnMissingBean
    public OAuth2ClientCredentialsManager cscTokenManager(CscProperties properties,
                                                           TokenStore tokenStore,
                                                           AuditPublisher auditPublisher,
                                                           RestClient cscRestClient) {
        return new OAuth2ClientCredentialsManager(properties, tokenStore, auditPublisher, cscRestClient);
    }

    @Bean
    @ConditionalOnMissingBean
    public CscApiClient cscApiClient(RestClient cscRestClient,
                                      OAuth2ClientCredentialsManager tokenManager,
                                      CscProperties properties,
                                      AuditPublisher auditPublisher) {
        return new CscApiClient(cscRestClient, tokenManager, properties, auditPublisher);
    }

    @Bean
    @ConditionalOnMissingBean
    public HashAlgorithmRegistry cscHashAlgorithmRegistry() {
        BouncyCastleInitializer.ensureInstalled();
        return new HashAlgorithmRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public CmsSignatureContainerBuilder cscCmsSignatureContainerBuilder(HashAlgorithmRegistry registry) {
        return new CmsSignatureContainerBuilder(registry);
    }

    @Bean
    @ConditionalOnMissingBean(AuthDataProvider.class)
    public AuthDataProvider cscDefaultAuthDataProvider() {
        return context -> {
            throw new UnsupportedOperationException(
                    "No AuthDataProvider bean registered. Implement AuthDataProvider to supply PIN/OTP.");
        };
    }

    // === PDF Configuration (conditional on PDFBox) ===

    @Configuration
    @ConditionalOnClass(name = "org.apache.pdfbox.pdmodel.PDDocument")
    @ConditionalOnProperty(prefix = "opencsc.pdf", name = "enabled", havingValue = "true", matchIfMissing = true)
    static class PdfAutoConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public PdfHashExtractor cscPdfHashExtractor(HashAlgorithmRegistry registry, CscProperties properties) {
            return new PdfHashExtractor(registry, properties.getPdf().getSignatureReservedSpaceBytes());
        }
    }

    // === Facade ===

    @Bean
    @ConditionalOnMissingBean
    public CscRemoteSigningFacade cscRemoteSigningFacade(CscApiClient apiClient,
                                                          AuthDataProvider authDataProvider,
                                                          AuditPublisher auditPublisher,
                                                          CmsSignatureContainerBuilder cmsBuilder,
                                                          ObjectProvider<PdfHashExtractor> pdfHashExtractor) {
        return new CscRemoteSigningFacadeImpl(
                apiClient,
                authDataProvider,
                auditPublisher,
                cmsBuilder,
                pdfHashExtractor.getIfAvailable());
    }

    // === Token Refresh Scheduler ===

    @Configuration
    static class TokenRefreshSchedulerConfig {

        private final OAuth2ClientCredentialsManager tokenManager;

        TokenRefreshSchedulerConfig(OAuth2ClientCredentialsManager tokenManager) {
            this.tokenManager = tokenManager;
        }

        @Scheduled(fixedDelayString = "${opencsc.oauth2.refresh-check-interval:30000}")
        public void refreshTokenIfExpiring() {
            tokenManager.refreshIfExpiring();
        }
    }
}
