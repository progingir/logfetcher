package com.sber.logfetcher.controller;

import com.sber.logfetcher.model.AlertStatus;
import com.sber.logfetcher.model.GrafanaAlert;
import com.sber.logfetcher.service.GrafanaAlertService;
import com.sber.logfetcher.service.LogFetcherScheduler;
import com.sber.logfetcher.service.TengriLogService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LogFetcherController.class)
class LogFetcherControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GrafanaAlertService grafanaAlertService;

    @MockBean
    private TengriLogService tengriLogService;

    @MockBean
    private LogFetcherScheduler scheduler;

    @Test
    @DisplayName("GET /api/alerts/active should return list of active alerts")
    void testGetActiveAlerts() throws Exception {
        GrafanaAlert alert = new GrafanaAlert(
                Collections.emptyMap(),
                Map.of("alertname", "sbbol_application", "severity", "critical"),
                "2026-10-11T12:00:00Z",
                null,
                null,
                "fp123",
                null,
                new AlertStatus("active", Collections.emptyList(), Collections.emptyList())
        );

        when(grafanaAlertService.fetchCriticalFiringAlerts()).thenReturn(List.of(alert));

        mockMvc.perform(get("/api/alerts/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].alertName").value("sbbol_application"))
                .andExpect(jsonPath("$[0].severity").value("critical"));
    }

    @Test
    @DisplayName("POST /api/orchestrator/trigger should invoke scheduler cycle")
    void testTriggerCycle() throws Exception {
        mockMvc.perform(post("/api/orchestrator/trigger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        verify(scheduler).runLogFetcherCycle();
    }
}
