package com.uni.ea_autoscaler.evaluation;

import com.uni.ea_autoscaler.core.interfaces.Validatable;
import com.uni.ea_autoscaler.prometheus.dto.PrometheusQueryResult;

import java.util.stream.Stream;

public record PrometheusMetrics(
        PrometheusQueryResult avgCpu,
        PrometheusQueryResult avgMemory,
        PrometheusQueryResult avgReplicas
) implements Validatable {
    public boolean valid() {
        return Stream.of(avgCpu, avgMemory, avgReplicas)
                .allMatch(m -> m != null && m.valid());
    }
}
