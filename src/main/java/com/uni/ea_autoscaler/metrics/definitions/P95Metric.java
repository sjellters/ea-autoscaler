package com.uni.ea_autoscaler.metrics.definitions;

import com.uni.ea_autoscaler.core.annotations.MetricDefinition;
import com.uni.ea_autoscaler.core.interfaces.Metric;
import com.uni.ea_autoscaler.jmeter.dto.JTLSample;
import com.uni.ea_autoscaler.metrics.domain.ComputeMetricsInput;
import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.metrics.utils.MetricsUtils;

@MetricDefinition
public class P95Metric implements Metric {

    @Override
    public MetricName name() {
        return MetricName.P95;
    }

    @Override
    public double compute(ComputeMetricsInput input) {
        return MetricsUtils.penalizedPercentile(input.jmeter().samples(), JTLSample::elapsed, 0.95);
    }
}

