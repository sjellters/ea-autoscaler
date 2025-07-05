package com.uni.ea_autoscaler.core.interfaces;

import com.uni.ea_autoscaler.core.enums.MetricName;

public interface MetricValidator {

    boolean isValid(MetricName name, double value);
}

