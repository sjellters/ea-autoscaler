package com.uni.ea_autoscaler.ga.evaluation;

import com.uni.ea_autoscaler.jmeter.dto.JMeterResultMetrics;

import java.time.Instant;
import java.util.Set;

public record BenchmarkResult(
        JMeterResultMetrics metrics,
        Instant startTime,
        Instant endTime,
        Set<String> podNames) {
}
