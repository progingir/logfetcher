package com.sber.logfetcher.service;

import com.sber.logfetcher.model.GrafanaAlert;
import com.sber.logfetcher.model.LogFetchRequest;
import com.sber.logfetcher.model.LogFetchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LogFetcherScheduler {

    private static final Logger log = LoggerFactory.getLogger(LogFetcherScheduler.class);

    private final GrafanaAlertService grafanaAlertService;
    private final TengriLogService tengriLogService;
    private final AlertDeduplicationService deduplicationService;
    private final EmailNotificationService emailNotificationService;

    public LogFetcherScheduler(GrafanaAlertService grafanaAlertService,
                               TengriLogService tengriLogService,
                               AlertDeduplicationService deduplicationService,
                               EmailNotificationService emailNotificationService) {
        this.grafanaAlertService = grafanaAlertService;
        this.tengriLogService = tengriLogService;
        this.deduplicationService = deduplicationService;
        this.emailNotificationService = emailNotificationService;
    }

    @Scheduled(fixedDelayString = "${logfetcher.grafana.poll-interval-ms:60000}")
    public void runLogFetcherCycle() {
        log.info("=== Запуск цикла мониторинга алертов и сбора логов ===");

        try {
            List<GrafanaAlert> firingAlerts = grafanaAlertService.fetchCriticalFiringAlerts();

            if (firingAlerts.isEmpty()) {
                log.info("На данный момент активных критичных алертов не обнаружено.");
                return;
            }

            for (GrafanaAlert alert : firingAlerts) {
                String dedupKey = alert.getDedupKey();

                if (!deduplicationService.registerIfNew(dedupKey)) {
                    log.info("Алерт уже был обработан ранее, пропуск: alertname='{}', startsAt='{}'",
                            alert.getAlertName(), alert.startsAt());
                    continue;
                }

                log.info("Обработка нового критичного алерта: alertname='{}', service='{}', startsAt='{}'",
                        alert.getAlertName(), alert.getService(), alert.startsAt());

                LogFetchRequest request = tengriLogService.buildFetchRequest(alert);
                log.info("Сформирован поисковый запрос: ключевое слово='{}', fromEpochMs={}, toEpochMs={}",
                        request.searchKeyword(), request.fromEpochMs(), request.toEpochMs());

                LogFetchResult result = tengriLogService.fetchAndSaveLogs(request);

                if (result.success()) {
                    log.info("Логи успешно собраны и сохранены: {}",
                            result.savedFilePath() != null ? result.savedFilePath().toAbsolutePath() : "в памяти");
                    emailNotificationService.sendLogsNotification(result);
                } else {
                    log.warn("Не удалось выгрузить логи для алерта '{}': {}",
                            alert.getAlertName(), result.errorMessage());
                }
            }

        } catch (Exception e) {
            log.error("Непредвиденная ошибка в цикле мониторинга: {}", e.getMessage(), e);
        } finally {
            log.info("=== Цикл мониторинга алертов завершен ===");
        }
    }
}
