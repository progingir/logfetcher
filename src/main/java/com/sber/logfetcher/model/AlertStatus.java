package com.sber.logfetcher.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AlertStatus(
        String state,
        List<String> inhibitedBy,
        List<String> silencedBy
) {
    public AlertStatus {
        if (inhibitedBy == null) inhibitedBy = Collections.emptyList();
        if (silencedBy == null) silencedBy = Collections.emptyList();
    }
}
