package com.icebrown.opencsc.spi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AuditPublisher {

    private static final Logger log = LoggerFactory.getLogger(AuditPublisher.class);
    private final List<CscAuditListener> listeners;

    public AuditPublisher(List<CscAuditListener> listeners) {
        this.listeners = listeners != null ? listeners : List.of();
    }

    public void publish(CscEvent event) {
        for (CscAuditListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.warn("Audit listener {} threw exception for event {}: {}",
                        listener.getClass().getSimpleName(),
                        event.getClass().getSimpleName(),
                        e.getMessage());
            }
        }
    }
}
