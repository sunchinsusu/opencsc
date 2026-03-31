package com.icebrown.opencsc.spi.event;

import com.icebrown.opencsc.spi.CscEvent;

import java.time.Instant;

public record HashSignedEvent(
        Instant occurredAt,
        String correlationId,
        String credentialId,
        int hashCount
) implements CscEvent {
}
