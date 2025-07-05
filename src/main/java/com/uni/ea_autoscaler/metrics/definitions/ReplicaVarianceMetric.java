package com.uni.ea_autoscaler.metrics.definitions;

import com.uni.ea_autoscaler.core.annotations.MetricDefinition;
import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.core.interfaces.Metric;
import com.uni.ea_autoscaler.metrics.domain.ComputeMetricsInput;

import java.util.List;

@MetricDefinition
public class ReplicaVarianceMetric implements Metric {

    @Override
    public MetricName name() {
        return MetricName.REPLICA_VARIANCE;
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @Override
    public double compute(ComputeMetricsInput input) {
        List<Double> values = input.prometheus().avgReplicas().flattenedValues();

        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        return values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .getAsDouble();
    }
}