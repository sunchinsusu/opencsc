package com.icebrown.opencsc.spi.event;

import com.icebrown.opencsc.spi.CscEvent;

import java.time.Instant;

public record TokenAcquiredEvent(
        Instant occurredAt,
        String correlationId,
        Long expiresIn
) implements CscEvent {
}
