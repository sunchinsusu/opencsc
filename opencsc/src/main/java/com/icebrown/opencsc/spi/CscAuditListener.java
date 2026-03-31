package com.icebrown.opencsc.spi;

@FunctionalInterface
public interface CscAuditListener {
    void onEvent(CscEvent event);
}
