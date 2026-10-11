package com.sber.logfetcher.config;

public record TengriProperties(
        boolean enabled,
        String endpoint,
        String cookie,
        String clientApiToken,
        String projectId,
        String journalName,
        String indexId,
        String queryFormat,
        int fromMinutesOffset,
        int toMinutesOffset,
        String outputDir,
        int timeoutSeconds
) {}
