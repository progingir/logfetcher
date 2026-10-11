package com.sber.logfetcher.model;

public record LogFetchRequest(
        String projectId,
        String journalName,
        String indexId,
        long fromEpochMs,
        long toEpochMs,
        String searchKeyword,
        GrafanaAlert originatingAlert
) {
    public LogFetchRequest withSearchKeyword(String newKeyword) {
        return new LogFetchRequest(projectId, journalName, indexId, fromEpochMs, toEpochMs, newKeyword, originatingAlert);
    }
}
