package com.uni.ea_autoscaler.metrics.definitions;

import com.uni.ea_autoscaler.core.annotations.MetricDefinition;
import com.uni.ea_autoscaler.core.interfaces.Metric;
import com.uni.ea_autoscaler.metrics.domain.ComputeMetricsInput;
import com.uni.ea_autoscaler.core.enums.MetricName;

@MetricDefinition
public class SLAPercentageMetric implements Metric {

    @Override
    public MetricName name() {
        return MetricName.SLA_PERCENTAGE;
    }

    @Override
    public double compute(ComputeMetricsInput input) {
        double slaThreshold = input.baseline().slaThreshold();

        long passing = input.jmeter().samples().stream()
                .filter(s -> s.success() && s.elapsed() <= slaThreshold)
                .count();

        return (double) passing / input.jmeter().samples().size();
    }
}

