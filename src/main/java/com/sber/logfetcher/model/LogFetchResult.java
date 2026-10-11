package com.sber.logfetcher.model;

import java.nio.file.Path;

public record LogFetchResult(
        boolean success,
        int httpStatusCode,
        String responseBody,
        Path savedFilePath,
        String errorMessage,
        GrafanaAlert originatingAlert
) {
    public static LogFetchResult success(int httpStatusCode, String responseBody, Path savedFilePath, GrafanaAlert alert) {
        return new LogFetchResult(true, httpStatusCode, responseBody, savedFilePath, null, alert);
    }

    public static LogFetchResult failure(int httpStatusCode, String errorMessage, String responseBody, GrafanaAlert alert) {
        return new LogFetchResult(false, httpStatusCode, responseBody, null, errorMessage, alert);
    }
}
