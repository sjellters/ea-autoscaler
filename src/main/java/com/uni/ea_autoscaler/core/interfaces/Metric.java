package com.uni.ea_autoscaler.core.interfaces;

import com.uni.ea_autoscaler.metrics.domain.ComputeMetricsInput;
import com.uni.ea_autoscaler.core.enums.MetricName;

public interface Metric {

    MetricName name();

    double compute(ComputeMetricsInput input);
}
