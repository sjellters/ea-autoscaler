package com.uni.ea_autoscaler.prometheus.dto;

public record PrometheusResponse(
        String status,
        PrometheusData data) {
}
