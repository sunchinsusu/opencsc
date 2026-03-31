package com.icebrown.opencsc.demo.config;

import com.icebrown.opencsc.domain.AuthObject;
import com.icebrown.opencsc.spi.AuthDataProvider;
import com.icebrown.opencsc.spi.CscAuditListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class DemoConfig {

    private static final Logger log = LoggerFactory.getLogger(DemoConfig.class);

    /**
     * Provides PIN/OTP for credential authorization.
     * In production, this would integrate with your OTP delivery mechanism.
     */
    @Bean
    public AuthDataProvider authDataProvider() {
        return context -> {
            log.info("Providing auth data for credential={}, corrId={}",
                    context.getCredentialId(), context.getCorrelationId());
            // Demo: static PIN. Replace with actual logic.
            return List.of(new AuthObject("mobile", null));
        };
    }

    /**
     * Audit listener to log all CSC events.
     * In production, you could persist to DB, send to Kafka, etc.
     */
    @Bean
    public CscAuditListener auditListener() {
        return event -> log.info("[CSC-AUDIT] {} | corrId={} | at={}",
                event.getClass().getSimpleName(),
                event.correlationId(),
                event.occurredAt());
    }
}
