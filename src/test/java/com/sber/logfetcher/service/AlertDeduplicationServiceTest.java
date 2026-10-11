package com.sber.logfetcher.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertDeduplicationServiceTest {

    @Test
    @DisplayName("Should register new alert key once and reject duplicates")
    void testDeduplication() {
        AlertDeduplicationService service = new AlertDeduplicationService();

        String alertKey = "fingerprint_123_2026-10-11T12:00:00Z";

        boolean firstAttempt = service.registerIfNew(alertKey);
        boolean secondAttempt = service.registerIfNew(alertKey);

        assertThat(firstAttempt).isTrue();
        assertThat(secondAttempt).isFalse();
    }
}
