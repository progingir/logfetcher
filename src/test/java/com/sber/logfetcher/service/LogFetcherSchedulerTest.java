package com.sber.logfetcher.service;

import com.sber.logfetcher.model.AlertStatus;
import com.sber.logfetcher.model.GrafanaAlert;
import com.sber.logfetcher.model.LogFetchRequest;
import com.sber.logfetcher.model.LogFetchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LogFetcherSchedulerTest {

    @Mock
    private GrafanaAlertService grafanaAlertService;

    @Mock
    private TengriLogService tengriLogService;

    @Mock
    private AlertDeduplicationService deduplicationService;

    @Mock
    private EmailNotificationService emailNotificationService;

    @InjectMocks
    private LogFetcherScheduler scheduler;

    @Test
    @DisplayName("Should execute end-to-end cycle: poll alert -> dedup -> fetch logs -> notify email")
    void testSchedulerPipeline() {
        GrafanaAlert alert = new GrafanaAlert(
                Collections.emptyMap(),
                Map.of("alertname", "sbbol_application", "service", "pprb_sbbol", "severity", "critical"),
                "2026-10-11T12:00:00Z",
                null,
                null,
                "fp123",
                null,
                new AlertStatus("active", Collections.emptyList(), Collections.emptyList())
        );

        LogFetchRequest request = new LogFetchRequest(
                "CI03261475",
                "pprb_sbbol",
                "144",
                1000L,
                2000L,
                "application",
                alert
        );

        LogFetchResult result = LogFetchResult.success(200, "{\"records\":[]}", Path.of("./test_log.json"), alert);

        when(grafanaAlertService.fetchCriticalFiringAlerts()).thenReturn(List.of(alert));
        when(deduplicationService.registerIfNew(any())).thenReturn(true);
        when(tengriLogService.buildFetchRequest(alert)).thenReturn(request);
        when(tengriLogService.fetchAndSaveLogs(request)).thenReturn(result);

        scheduler.runLogFetcherCycle();

        verify(grafanaAlertService, times(1)).fetchCriticalFiringAlerts();
        verify(deduplicationService, times(1)).registerIfNew(any());
        verify(tengriLogService, times(1)).buildFetchRequest(alert);
        verify(tengriLogService, times(1)).fetchAndSaveLogs(request);
        verify(emailNotificationService, times(1)).sendLogsNotification(result);
    }
}
