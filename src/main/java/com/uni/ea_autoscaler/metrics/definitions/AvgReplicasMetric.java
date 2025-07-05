package com.uni.ea_autoscaler.metrics.definitions;

import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.core.annotations.MetricDefinition;
import com.uni.ea_autoscaler.core.interfaces.Metric;
import com.uni.ea_autoscaler.metrics.domain.ComputeMetricsInput;

@MetricDefinition
public class AvgReplicasMetric implements Metric {

    @Override
    public MetricName name() {
        return MetricName.AVG_REPLICAS;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public double compute(ComputeMetricsInput input) {
        return input.prometheus().avgReplicas().flattenedValues().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .getAsDouble();
    }
}
