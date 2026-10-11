package com.sber.logfetcher.controller;

import com.sber.logfetcher.model.AlertStatus;
import com.sber.logfetcher.model.GrafanaAlert;
import com.sber.logfetcher.model.LogFetchRequest;
import com.sber.logfetcher.model.LogFetchResult;
import com.sber.logfetcher.service.GrafanaAlertService;
import com.sber.logfetcher.service.LogFetcherScheduler;
import com.sber.logfetcher.service.TengriLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class LogFetcherController {

    private static final Logger log = LoggerFactory.getLogger(LogFetcherController.class);

    private final GrafanaAlertService grafanaAlertService;
    private final TengriLogService tengriLogService;
    private final LogFetcherScheduler scheduler;

    public LogFetcherController(GrafanaAlertService grafanaAlertService,
                                TengriLogService tengriLogService,
                                LogFetcherScheduler scheduler) {
        this.grafanaAlertService = grafanaAlertService;
        this.tengriLogService = tengriLogService;
        this.scheduler = scheduler;
    }

    @GetMapping("/alerts/active")
    public ResponseEntity<List<GrafanaAlert>> getActiveCriticalAlerts() {
        log.info("REST: Ручной запрос активных критичных алертов из Grafana");
        List<GrafanaAlert> alerts = grafanaAlertService.fetchCriticalFiringAlerts();
        return ResponseEntity.ok(alerts);
    }

    @PostMapping("/orchestrator/trigger")
    public ResponseEntity<Map<String, String>> triggerCycle() {
        log.info("REST: Принудительный запуск цикла мониторинга и выгрузки логов");
        scheduler.runLogFetcherCycle();
        return ResponseEntity.ok(Map.of("status", "SUCCESS", "message", "Monitoring cycle completed"));
    }

    @PostMapping("/logs/test-fetch")
    public ResponseEntity<LogFetchResult> testTengriFetch(
            @RequestParam(defaultValue = "application") String keyword,
            @RequestParam(defaultValue = "5") int minutesBack) {
        log.info("REST: Тестовый запрос выгрузки логов из Tengri с ключевым словом='{}', минут назад={}", keyword, minutesBack);

        Instant now = Instant.now();
        GrafanaAlert mockAlert = new GrafanaAlert(
                Collections.emptyMap(),
                Map.of("alertname", "test_" + keyword, "severity", "critical"),
                now.toString(),
                null,
                null,
                "test-fingerprint",
                null,
                new AlertStatus("active", Collections.emptyList(), Collections.emptyList())
        );

        LogFetchRequest request = tengriLogService.buildFetchRequest(mockAlert).withSearchKeyword(keyword);
        LogFetchResult result = tengriLogService.fetchAndSaveLogs(request);
        return ResponseEntity.ok(result);
    }
}
