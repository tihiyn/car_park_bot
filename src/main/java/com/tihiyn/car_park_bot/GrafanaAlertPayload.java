package com.tihiyn.car_park_bot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GrafanaAlertPayload(
    String receiver,
    String status,
    String title,
    String message,
    String externalURL,
    String groupKey,
    Integer truncatedAlerts,
    Map<String, String> groupLabels,
    Map<String, String> commonLabels,
    Map<String, String> commonAnnotations,
    List<Alert> alerts
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Alert(
        String status,
        Map<String, String> labels,       // alertname, severity, service, class, grafana_folder
        Map<String, String> annotations,  // summary, description, runbook
        OffsetDateTime startsAt,
        OffsetDateTime endsAt,            // у firing равен нулевому времени Go, выводить не нужно
        String generatorURL,
        String dashboardURL,
        String panelURL,
        String silenceURL,
        String fingerprint,
        Map<String, Double> values        // значения refId'ов правила: {"C": 1.8, "D": 0.35}
    ) {
    }
}
