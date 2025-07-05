package com.uni.ea_autoscaler.prometheus.dto;

public record PrometheusResponse(
        String status,
        PrometheusData data
) {
    public boolean isValid() {
        return "success".equals(status)
                && data != null
                && data.result() != null
                && !data.result().isEmpty()
                && data.result().stream().anyMatch(r -> r.values() != null && !r.values().isEmpty());
    }
}
