package com.icebrown.opencsc.spi.event;

import com.icebrown.opencsc.spi.CscEvent;

import java.time.Instant;

public record CscApiErrorEvent(
        Instant occurredAt,
        String correlationId,
        String endpoint,
        int statusCode,
        String errorMessage
) implements CscEvent {
}
