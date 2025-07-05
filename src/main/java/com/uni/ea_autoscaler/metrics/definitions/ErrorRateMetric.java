package com.uni.ea_autoscaler.metrics.definitions;

import com.uni.ea_autoscaler.core.annotations.MetricDefinition;
import com.uni.ea_autoscaler.core.interfaces.Metric;
import com.uni.ea_autoscaler.jmeter.dto.JTLSample;
import com.uni.ea_autoscaler.metrics.domain.ComputeMetricsInput;
import com.uni.ea_autoscaler.core.enums.MetricName;

import java.util.List;

@MetricDefinition
public class ErrorRateMetric implements Metric {

    @Override
    public MetricName name() {
        return MetricName.ERROR_RATE;
    }

    @Override
    public double compute(ComputeMetricsInput input) {
        List<JTLSample> samples = input.jmeter().samples();

        if (samples.isEmpty()) {
            return Double.NaN;
        }

        long failed = samples.stream()
                .filter(s -> !s.success())
                .count();

        return (double) failed / samples.size();
    }
}
