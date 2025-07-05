package com.uni.ea_autoscaler.metrics.validators;

import com.uni.ea_autoscaler.core.annotations.ValidatesMetrics;
import com.uni.ea_autoscaler.core.interfaces.MetricValidator;
import com.uni.ea_autoscaler.core.enums.MetricName;

@ValidatesMetrics(MetricName.ERROR_RATE)
public class ErrorRateValidator implements MetricValidator {

    @Override
    public boolean isValid(MetricName name, double value) {
        return value < 1.0;
    }
}

