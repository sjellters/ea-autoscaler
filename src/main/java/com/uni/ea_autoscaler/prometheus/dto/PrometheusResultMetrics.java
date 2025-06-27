package com.uni.ea_autoscaler.prometheus.dto;

public record PrometheusResultMetrics(
        Double avgCpu,
        Double avgMemory,
        Double avgReplicas) {
}
