package com.uni.ea_autoscaler.metrics.validators;

import com.uni.ea_autoscaler.core.annotations.ValidatesMetrics;
import com.uni.ea_autoscaler.core.interfaces.MetricValidator;
import com.uni.ea_autoscaler.core.enums.MetricName;

@ValidatesMetrics({
        MetricName.AVG_RESPONSE_TIME,
        MetricName.AVG_LATENCY,
        MetricName.P95
})
public class NotNaNValidator implements MetricValidator {

    @Override
    public boolean isValid(MetricName name, double value) {
        return !Double.isNaN(value);
    }
}

