package com.uni.ea_autoscaler.jmeter.dto;

public record JMeterResultMetrics(
        double averageResponseTime,
        double averageLatency,
        double errorRate) {
}
