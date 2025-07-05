package com.uni.ea_autoscaler.metrics.domain;

import com.uni.ea_autoscaler.common.BaselineValues;
import com.uni.ea_autoscaler.common.ResourceScalingConfig;
import com.uni.ea_autoscaler.evaluation.PrometheusMetrics;
import com.uni.ea_autoscaler.jmeter.dto.JTLParseResult;

import java.util.stream.Stream;

public record ComputeMetricsInput(
        ResourceScalingConfig config,
        BaselineValues baseline,
        JTLParseResult jmeter,
        PrometheusMetrics prometheus
) {
    public boolean isInvalid() {
        return !Stream.of(jmeter, prometheus)
                .allMatch(v -> v != null && v.valid());
    }
}
