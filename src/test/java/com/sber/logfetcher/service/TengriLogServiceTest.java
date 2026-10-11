package com.sber.logfetcher.service;

import com.sber.logfetcher.config.AppProperties;
import com.sber.logfetcher.config.GrafanaProperties;
import com.sber.logfetcher.config.TengriProperties;
import com.sber.logfetcher.model.AlertStatus;
import com.sber.logfetcher.model.GrafanaAlert;
import com.sber.logfetcher.model.LogFetchRequest;
import com.sber.logfetcher.model.LogFetchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TengriLogServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private AppProperties appProperties;
    private TengriLogService tengriLogService;

    @BeforeEach
    void setUp(@TempDir Path tempDir) {
        GrafanaProperties grafanaProps = new GrafanaProperties(
                true, "https://tslds-cibes0001.cloud.delta.sbrf.ru:3000", "/api/alerts",
                "", "", List.of("critical"), 60000, 15
        );
        TengriProperties tengriProps = new TengriProperties(
                true, "https://ci02587203-ift-geo-iam-proxy-user.delta.sbrf.ru/indicator-ift-client/a/logger/logs",
                "", "", "CI03261475", "pprb_sbbol", "144", "%s", 5, 2, tempDir.toString(), 30
        );
        appProperties = new AppProperties(true, grafanaProps, tengriProps);
        tengriLogService = new TengriLogService(httpClient, appProperties);
    }

    private GrafanaAlert createSampleAlert() {
        return new GrafanaAlert(
                Collections.emptyMap(),
                Map.of("alertname", "sbbol_application", "service", "pprb_sbbol", "severity", "critical"),
                "2026-10-11T12:10:00Z",
                null,
                null,
                "fp123",
                null,
                new AlertStatus("active", Collections.emptyList(), Collections.emptyList())
        );
    }

    @Test
    @DisplayName("Should build LogFetchRequest with correct -5m / +2m time window in epoch ms")
    void testBuildFetchRequest() {
        GrafanaAlert alert = createSampleAlert();

        LogFetchRequest request = tengriLogService.buildFetchRequest(alert);

        Instant startsAt = Instant.parse("2026-10-11T12:10:00Z");
        long expectedFrom = startsAt.minusSeconds(5 * 60).toEpochMilli();
        long expectedTo = startsAt.plusSeconds(2 * 60).toEpochMilli();

        assertThat(request.fromEpochMs()).isEqualTo(expectedFrom);
        assertThat(request.toEpochMs()).isEqualTo(expectedTo);
        assertThat(request.searchKeyword()).isEqualTo("application");
        assertThat(request.projectId()).isEqualTo("CI03261475");
        assertThat(request.journalName()).isEqualTo("pprb_sbbol");
        assertThat(request.indexId()).isEqualTo("144");
    }

    @Test
    @DisplayName("Should construct Tengri URL with project, indices and time parameters")
    void testBuildTengriUrl() {
        GrafanaAlert alert = createSampleAlert();

        LogFetchRequest request = tengriLogService.buildFetchRequest(alert);
        String url = tengriLogService.buildTengriUrl(request);

        assertThat(url).contains("projectId=CI03261475");
        assertThat(url).contains("indices=pprb_sbbol_144");
        assertThat(url).contains("query=application");
        assertThat(url).contains("&from=" + request.fromEpochMs());
        assertThat(url).contains("&to=" + request.toEpochMs());
    }

    @Test
    @DisplayName("Should fetch logs from Tengri and save them to local disk")
    void testFetchAndSaveLogs() throws Exception {
        GrafanaAlert alert = createSampleAlert();

        LogFetchRequest request = tengriLogService.buildFetchRequest(alert);

        String sampleLogs = "{\"records\":[{\"message\":\"NullPointerException at Service.java:42\"}]}";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(sampleLogs);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenAnswer(inv -> httpResponse);

        LogFetchResult result = tengriLogService.fetchAndSaveLogs(request);

        assertThat(result.success()).isTrue();
        assertThat(result.httpStatusCode()).isEqualTo(200);
        assertThat(result.savedFilePath()).isNotNull();
        assertThat(Files.exists(result.savedFilePath())).isTrue();
        assertThat(Files.readString(result.savedFilePath())).isEqualTo(sampleLogs);
    }
}
