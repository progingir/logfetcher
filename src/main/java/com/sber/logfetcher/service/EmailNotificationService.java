package com.sber.logfetcher.service;

import com.sber.logfetcher.model.LogFetchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    public void sendLogsNotification(LogFetchResult result) {
        if (!result.success()) {
            log.warn("Пропуск почтового уведомления, так как выгрузка логов завершилась ошибкой.");
            return;
        }

        String alertName = result.originatingAlert() != null ?
                result.originatingAlert().getAlertName() : "Unknown";

        log.info("[ПЕРСПЕКТИВНАЯ ИНТЕГРАЦИЯ] Подготовлено почтовое уведомление по алерту '{}'. Файл логов: {}",
                alertName,
                result.savedFilePath() != null ? result.savedFilePath().toAbsolutePath() : "нет");
    }
}
