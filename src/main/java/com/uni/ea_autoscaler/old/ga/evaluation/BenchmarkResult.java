package com.uni.ea_autoscaler.old.ga.evaluation;

import com.uni.ea_autoscaler.jmeter.dto.JMeterResultMetrics;

import java.time.Instant;

public record BenchmarkResult(
        JMeterResultMetrics metrics,
        Instant startTime,
        Instant endTime) {
}
