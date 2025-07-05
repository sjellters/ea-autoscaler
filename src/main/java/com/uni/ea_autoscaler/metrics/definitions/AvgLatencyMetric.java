package com.uni.ea_autoscaler.metrics.definitions;

import com.uni.ea_autoscaler.core.annotations.MetricDefinition;
import com.uni.ea_autoscaler.core.interfaces.Metric;
import com.uni.ea_autoscaler.jmeter.dto.JTLSample;
import com.uni.ea_autoscaler.metrics.domain.ComputeMetricsInput;
import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.metrics.utils.MetricsUtils;

@MetricDefinition
public class AvgLatencyMetric implements Metric {

    @Override
    public MetricName name() {
        return MetricName.AVG_LATENCY;
    }

    @Override
    public double compute(ComputeMetricsInput input) {
        return MetricsUtils.penalizedAverage(input.jmeter().samples(), JTLSample::latency);
    }
}

