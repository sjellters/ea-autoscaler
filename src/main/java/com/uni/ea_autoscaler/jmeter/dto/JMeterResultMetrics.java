package com.uni.ea_autoscaler.jmeter.dto;

public record JMeterResultMetrics(
        Double averageResponseTime,
        Double averageLatency,
        double errorRate,
        Double slaPercentage,
        Double p95) {
}
