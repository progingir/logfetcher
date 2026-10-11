package com.sber.logfetcher;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "SERVER_PORT=8080",
        "LOGFETCHER_TRUST_ALL_SSL=true",
        "GRAFANA_ENABLED=false",
        "GRAFANA_BASE_URL=https://test-grafana",
        "GRAFANA_ALERTS_ENDPOINT=/api/alerts",
        "GRAFANA_CRITICAL_SEVERITIES=critical",
        "GRAFANA_POLL_INTERVAL_MS=60000",
        "GRAFANA_TIMEOUT_SECONDS=15",
        "TENGRI_ENABLED=false",
        "TENGRI_ENDPOINT=https://test-tengri",
        "TENGRI_PROJECT_ID=CI03261475",
        "TENGRI_JOURNAL_NAME=pprb_sbbol",
        "TENGRI_INDEX_ID=144",
        "TENGRI_QUERY_FORMAT=%s",
        "TENGRI_FROM_MINUTES_OFFSET=5",
        "TENGRI_TO_MINUTES_OFFSET=2",
        "TENGRI_OUTPUT_DIR=./fetched_logs",
        "TENGRI_TIMEOUT_SECONDS=30"
})
class LogfetcherApplicationTests {

    @Test
    void contextLoads() {
    }

}
