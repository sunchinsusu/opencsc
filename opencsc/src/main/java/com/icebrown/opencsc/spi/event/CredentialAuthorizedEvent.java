package com.icebrown.opencsc.spi.event;

import com.icebrown.opencsc.spi.CscEvent;

import java.time.Instant;

public record CredentialAuthorizedEvent(
        Instant occurredAt,
        String correlationId,
        String credentialId,
        int numSignatures
) implements CscEvent {
}
