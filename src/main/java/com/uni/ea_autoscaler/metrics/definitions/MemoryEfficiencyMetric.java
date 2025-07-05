package com.uni.ea_autoscaler.metrics.definitions;

import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.core.annotations.MetricDefinition;
import com.uni.ea_autoscaler.core.interfaces.Metric;
import com.uni.ea_autoscaler.metrics.domain.ComputeMetricsInput;

@MetricDefinition
public class MemoryEfficiencyMetric implements Metric {

    @Override
    public MetricName name() {
        return MetricName.MEMORY_EFFICIENCY;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public double compute(ComputeMetricsInput input) {
        double memoryRequestBytes = input.config().getDeployment().getMemoryRequest() * 1024.0 * 1024.0;

        return input.prometheus().avgMemory().flattenedValues().stream()
                .mapToDouble(usage -> usage / memoryRequestBytes)
                .average()
                .getAsDouble();
    }
}

