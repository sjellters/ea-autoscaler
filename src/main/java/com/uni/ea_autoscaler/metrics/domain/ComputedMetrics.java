package com.uni.ea_autoscaler.metrics.domain;

import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.core.interfaces.Metric;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ComputedMetrics {

    private final Map<MetricName, Double> computed;

    public ComputedMetrics(ComputeMetricsInput input, List<Metric> metricList) {
        this.computed = metricList.stream()
                .collect(Collectors.toMap(
                        Metric::name,
                        m -> m.compute(input)
                ));
    }

    public double get(MetricName name) {
        Double value = computed.get(name);
        if (value == null) {
            throw new IllegalStateException("❌ Metric not available: " + name);
        }
        return value;
    }

    public Map<MetricName, Double> getAll() {
        return new HashMap<>(computed);
    }

    @Override
    public String toString() {
        return computed.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> "- " + e.getKey().name().toLowerCase() + ": " + e.getValue())
                .collect(Collectors.joining("\n", "📊 Metrics:\n", ""));
    }
}
