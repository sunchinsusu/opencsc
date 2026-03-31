package com.icebrown.opencsc.spi.event;

import com.icebrown.opencsc.spi.CscEvent;

import java.time.Instant;

public record PdfSignedEvent(
        Instant occurredAt,
        String correlationId,
        String credentialId,
        long pdfSizeBytes
) implements CscEvent {
}
