package com.sber.logfetcher.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "logfetcher")
public record AppProperties(
        boolean trustAllSsl,
        GrafanaProperties grafana,
        TengriProperties tengri
) {}
