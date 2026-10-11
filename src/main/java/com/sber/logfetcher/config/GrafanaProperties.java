package com.sber.logfetcher.config;

import java.util.List;

public record GrafanaProperties(
        boolean enabled,
        String baseUrl,
        String alertsEndpoint,
        String token,
        String cookie,
        List<String> criticalSeverities,
        long pollIntervalMs,
        int timeoutSeconds
) {}
