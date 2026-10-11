package com.sber.logfetcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sber.logfetcher.config.AppProperties;
import com.sber.logfetcher.config.GrafanaProperties;
import com.sber.logfetcher.model.GrafanaAlert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GrafanaAlertServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private AppProperties appProperties;
    private ObjectMapper objectMapper;
    private GrafanaAlertService alertService;

    @BeforeEach
    void setUp() {
        GrafanaProperties grafanaProps = new GrafanaProperties(
                true, "https://tslds-cibes0001.cloud.delta.sbrf.ru:3000", "/api/alerts",
                "", "", List.of("critical", "CRITICAL", "fatal", "FATAL", "high", "HIGH"), 60000, 15
        );
        appProperties = new AppProperties(true, grafanaProps, null);
        objectMapper = new ObjectMapper();
        alertService = new GrafanaAlertService(httpClient, appProperties, objectMapper);
    }

    @Test
    @DisplayName("Should fetch and filter only firing critical alerts from Grafana Alertmanager response")
    void testFetchCriticalFiringAlerts() throws Exception {
        String mockGrafanaJson = """
            [
              {
                "status": { "state": "active" },
                "labels": {
                  "alertname": "sbbol_application",
                  "severity": "critical",
                  "service": "pprb_sbbol"
                },
                "startsAt": "2026-10-11T12:00:00Z"
              },
              {
                "status": { "state": "active" },
                "labels": {
                  "alertname": "HighDisk_Warning",
                  "severity": "warning",
                  "service": "pprb_sbbol"
                },
                "startsAt": "2026-10-11T12:00:00Z"
              },
              {
                "status": { "state": "suppressed" },
                "labels": {
                  "alertname": "DatabaseDeadlock",
                  "severity": "critical",
                  "service": "pprb_sbbol"
                },
                "startsAt": "2026-10-11T12:00:00Z"
              }
            ]
            """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(mockGrafanaJson);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(inv -> httpResponse);

        List<GrafanaAlert> results = alertService.fetchCriticalFiringAlerts();

        assertThat(results).hasSize(1);
        GrafanaAlert alert = results.get(0);
        assertThat(alert.getAlertName()).isEqualTo("sbbol_application");
        assertThat(alert.getSeverity()).isEqualTo("critical");
        assertThat(alert.extractLogSearchKeyword()).isEqualTo("application");
        assertThat(alert.isFiring()).isTrue();
    }
}
