package com.sber.logfetcher.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GrafanaAlert(
        Map<String, String> annotations,
        Map<String, String> labels,
        String startsAt,
        String endsAt,
        String updatedAt,
        String fingerprint,
        String generatorURL,
        AlertStatus status
) {
    private static final Logger log = LoggerFactory.getLogger(GrafanaAlert.class);

    public GrafanaAlert {
        if (annotations == null) annotations = Collections.emptyMap();
        if (labels == null) labels = Collections.emptyMap();
    }

    public String getStartsAt() {
        return startsAt();
    }

    public String getAlertName() {
        if (labels.containsKey("alertname")) return labels.get("alertname");
        if (labels.containsKey("alert")) return labels.get("alert");
        if (labels.containsKey("name")) return labels.get("name");
        if (annotations.containsKey("alertname")) return annotations.get("alertname");
        return "UnknownAlert";
    }

    public String getService() {
        if (labels.containsKey("service")) return labels.get("service");
        if (labels.containsKey("job")) return labels.get("job");
        if (labels.containsKey("app")) return labels.get("app");
        if (labels.containsKey("application")) return labels.get("application");
        return "UnknownService";
    }

    public String getSeverity() {
        if (labels.containsKey("severity")) return labels.get("severity");
        if (labels.containsKey("level")) return labels.get("level");
        if (labels.containsKey("priority")) return labels.get("priority");
        if (annotations.containsKey("severity")) return annotations.get("severity");
        if (annotations.containsKey("level")) return annotations.get("level");
        if (annotations.containsKey("priority")) return annotations.get("priority");
        return "unknown";
    }

    public boolean isFiring() {
        if (status != null && status.state() != null) {
            String state = status.state().trim().toLowerCase();
            if (state.equals("active") || state.equals("firing")) {
                return true;
            }
        }
        if (labels.containsKey("alertstate")) {
            return "firing".equalsIgnoreCase(labels.get("alertstate"));
        }
        if (labels.containsKey("state")) {
            String state = labels.get("state").trim().toLowerCase();
            return state.equals("active") || state.equals("firing");
        }
        return false;
    }

    public Instant getStartsAtInstant() {
        if (startsAt == null || startsAt.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(startsAt);
        } catch (DateTimeParseException e) {
            log.warn("Не удалось распарсить метку времени startsAt '{}', используется текущее время", startsAt);
            return Instant.now();
        }
    }

    public long getStartsAtEpochMs() {
        return getStartsAtInstant().toEpochMilli();
    }

    public String extractLogSearchKeyword() {
        if (labels.containsKey("app") && !labels.get("app").isBlank()) {
            return labels.get("app");
        }
        if (labels.containsKey("application") && !labels.get("application").isBlank()) {
            return labels.get("application");
        }
        String alertName = getAlertName();
        if (alertName != null && alertName.contains("_")) {
            int underscoreIdx = alertName.indexOf('_');
            String afterPrefix = alertName.substring(underscoreIdx + 1);
            if (!afterPrefix.isBlank()) {
                return afterPrefix;
            }
        }
        String service = getService();
        if (service != null && !service.equalsIgnoreCase("UnknownService")) {
            return service;
        }
        return alertName != null ? alertName : "all";
    }

    public String getDedupKey() {
        if (fingerprint != null && !fingerprint.isBlank()) {
            return fingerprint + "_" + startsAt;
        }
        return getAlertName() + "_" + getService() + "_" + startsAt;
    }
}
