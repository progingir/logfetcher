package com.sber.logfetcher.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertDeduplicationService {

    private static final Logger log = LoggerFactory.getLogger(AlertDeduplicationService.class);

    private final Map<String, Instant> processedAlerts = new ConcurrentHashMap<>();

    public boolean registerIfNew(String dedupKey) {
        cleanOldEntries();
        if (processedAlerts.containsKey(dedupKey)) {
            return false;
        }
        processedAlerts.put(dedupKey, Instant.now());
        return true;
    }

    private void cleanOldEntries() {
        Instant threshold = Instant.now().minusSeconds(24 * 3600);
        processedAlerts.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
    }
}
