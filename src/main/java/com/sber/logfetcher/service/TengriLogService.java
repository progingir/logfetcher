package com.sber.logfetcher.service;

import com.sber.logfetcher.config.AppProperties;
import com.sber.logfetcher.config.TengriProperties;
import com.sber.logfetcher.model.GrafanaAlert;
import com.sber.logfetcher.model.LogFetchRequest;
import com.sber.logfetcher.model.LogFetchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class TengriLogService {

    private static final Logger log = LoggerFactory.getLogger(TengriLogService.class);

    private final HttpClient httpClient;
    private final AppProperties appProperties;

    public TengriLogService(HttpClient httpClient, AppProperties appProperties) {
        this.httpClient = httpClient;
        this.appProperties = appProperties;
    }

    private static final DateTimeFormatter FILE_DATE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss").withZone(ZoneId.systemDefault());

    public LogFetchRequest buildFetchRequest(GrafanaAlert alert) {
        TengriProperties tengriProps = appProperties.tengri();
        Instant alertInstant = alert.getStartsAtInstant();

        long fromEpochMs = alertInstant.minus(Duration.ofMinutes(tengriProps.fromMinutesOffset())).toEpochMilli();
        long toEpochMs = alertInstant.plus(Duration.ofMinutes(tengriProps.toMinutesOffset())).toEpochMilli();
        String keyword = alert.extractLogSearchKeyword();

        return new LogFetchRequest(
                tengriProps.projectId(),
                tengriProps.journalName(),
                tengriProps.indexId(),
                fromEpochMs,
                toEpochMs,
                keyword,
                alert
        );
    }

    public LogFetchResult fetchAndSaveLogs(LogFetchRequest request) {
        TengriProperties tengriProps = appProperties.tengri();
        if (tengriProps == null || !tengriProps.enabled()) {
            log.info("Сбор логов из Tengri отключен в конфигурации.");
            return LogFetchResult.failure(0, "Tengri fetching disabled in config", null, request.originatingAlert());
        }

        try {
            String targetUrl = buildTengriUrl(request);
            log.info("Запрос логов из Tengri: {}", targetUrl);

            HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofSeconds(tengriProps.timeoutSeconds()))
                    .header("Accept", "application/json, text/plain, */*")
                    .GET();

            if (tengriProps.cookie() != null && !tengriProps.cookie().isBlank()) {
                httpRequestBuilder.header("Cookie", tengriProps.cookie());
            }

            if (tengriProps.clientApiToken() != null && !tengriProps.clientApiToken().isBlank()) {
                httpRequestBuilder.header("Authorization", "Bearer " + tengriProps.clientApiToken());
            }

            HttpResponse<String> response = httpClient.send(httpRequestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                log.info("Логи успешно получены из Tengri (HTTP 200, размер: {} символов)", response.body().length());
                Path savedPath = saveLogsToFile(request, response.body());
                return LogFetchResult.success(200, response.body(), savedPath, request.originatingAlert());
            } else {
                log.warn("Tengri вернул ответ со статусом: {}. Тело ответа: {}", response.statusCode(), response.body());
                return LogFetchResult.failure(response.statusCode(), "HTTP status: " + response.statusCode(), response.body(), request.originatingAlert());
            }
        } catch (Exception e) {
            log.error("Сбой при получении логов из Tengri: {}", e.getMessage(), e);
            return LogFetchResult.failure(0, e.getMessage(), null, request.originatingAlert());
        }
    }

    public String buildTengriUrl(LogFetchRequest req) {
        String base = appProperties.tengri().endpoint();

        String rawIndices = req.journalName() + "_" + req.indexId();
        String doubleEncodedIndices = doubleUrlEncode(rawIndices);

        String rawQuery = req.searchKeyword();
        String formattedQuery = String.format(appProperties.tengri().queryFormat(), rawQuery);
        String doubleEncodedKeyword = doubleUrlEncode(formattedQuery);

        StringBuilder url = new StringBuilder(base);
        url.append(base.contains("?") ? "&" : "?");
        url.append("projectId=").append(urlEncode(req.projectId()))
                .append("&indices=").append(doubleEncodedIndices)
                .append("&query=").append(doubleEncodedKeyword)
                .append("&from=").append(req.fromEpochMs())
                .append("&to=").append(req.toEpochMs());

        return url.toString();
    }

    private String urlEncode(String value) {
        if (value == null) return "";
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String doubleUrlEncode(String value) {
        if (value == null) return "";
        String first = URLEncoder.encode(value, StandardCharsets.UTF_8);
        return URLEncoder.encode(first, StandardCharsets.UTF_8);
    }

    private Path saveLogsToFile(LogFetchRequest request, String content) throws IOException {
        String dirPath = appProperties.tengri().outputDir();
        Path dir = Paths.get(dirPath);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        String safeAlertName = request.originatingAlert() != null
                ? request.originatingAlert().getAlertName().replaceAll("[^a-zA-Z0-9_-]", "_")
                : "alert";
        String timestampStr = FILE_DATE_FMT.format(Instant.now());
        String filename = String.format("logs_%s_%s_%d.json", safeAlertName, timestampStr, request.fromEpochMs());

        Path target = dir.resolve(filename);
        Files.writeString(target, content, StandardCharsets.UTF_8);
        log.info("Логи сохранены на диск: {}", target.toAbsolutePath());
        return target;
    }
}
