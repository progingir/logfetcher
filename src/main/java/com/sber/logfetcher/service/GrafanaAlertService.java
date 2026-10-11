package com.sber.logfetcher.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sber.logfetcher.config.AppProperties;
import com.sber.logfetcher.config.GrafanaProperties;
import com.sber.logfetcher.model.GrafanaAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GrafanaAlertService {

    private static final Logger log = LoggerFactory.getLogger(GrafanaAlertService.class);

    private final HttpClient httpClient;
    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;

    public GrafanaAlertService(HttpClient httpClient, AppProperties appProperties, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
    }

    public List<GrafanaAlert> fetchCriticalFiringAlerts() {
        GrafanaProperties grafanaProps = appProperties.grafana();
        if (grafanaProps == null || !grafanaProps.enabled()) {
            log.debug("Опрос Grafana отключен в конфигурации.");
            return Collections.emptyList();
        }

        String targetUrl = grafanaProps.baseUrl() + grafanaProps.alertsEndpoint();
        log.info("Запрос алертов из Grafana: {}", targetUrl);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(grafanaProps.timeoutSeconds()))
                    .header("Accept", "application/json")
                    .GET();

            if (grafanaProps.token() != null && !grafanaProps.token().isBlank()) {
                requestBuilder.header("Authorization", "Bearer " + grafanaProps.token());
            }

            if (grafanaProps.cookie() != null && !grafanaProps.cookie().isBlank()) {
                requestBuilder.header("Cookie", grafanaProps.cookie());
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                log.error("Не удалось получить алерты из Grafana. HTTP статус: {}, тело: {}",
                        response.statusCode(), response.body());
                return Collections.emptyList();
            }

            GrafanaAlert[] alertsArray = objectMapper.readValue(response.body(), GrafanaAlert[].class);
            List<GrafanaAlert> allAlerts = alertsArray != null ? Arrays.asList(alertsArray) : Collections.emptyList();

            log.info("Получено всего {} алертов от Grafana", allAlerts.size());

            List<GrafanaAlert> criticalFiringAlerts = allAlerts.stream()
                    .filter(GrafanaAlert::isFiring)
                    .filter(this::isCriticalAlert)
                    .collect(Collectors.toList());

            log.info("Отфильтровано {} активных критичных алертов", criticalFiringAlerts.size());
            for (GrafanaAlert alert : criticalFiringAlerts) {
                log.info(">> ОБНАРУЖЕН КРИТИЧНЫЙ АЛЕРТ: alertname='{}', service='{}', severity='{}', startsAt='{}'",
                        alert.getAlertName(),
                        alert.getService(),
                        alert.getSeverity(),
                        alert.startsAt());
            }

            return criticalFiringAlerts;

        } catch (Exception e) {
            log.error("Ошибка при обращении к Grafana: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    private boolean isCriticalAlert(GrafanaAlert alert) {
        String severity = alert.getSeverity();
        if (severity == null) {
            return false;
        }

        List<String> criticalSeverities = appProperties.grafana().criticalSeverities();
        if (criticalSeverities == null) {
            return false;
        }
        return criticalSeverities.stream()
                .anyMatch(crit -> crit.equalsIgnoreCase(severity));
    }
}
