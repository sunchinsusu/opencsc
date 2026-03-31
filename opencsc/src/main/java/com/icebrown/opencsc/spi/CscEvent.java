package com.icebrown.opencsc.spi;

import java.time.Instant;

public interface CscEvent {
    Instant occurredAt();
    String correlationId();
}
