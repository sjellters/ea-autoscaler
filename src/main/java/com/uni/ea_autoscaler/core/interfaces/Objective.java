package com.uni.ea_autoscaler.core.interfaces;

import com.uni.ea_autoscaler.common.ResourceScalingConfig;
import com.uni.ea_autoscaler.core.enums.ObjectiveName;
import com.uni.ea_autoscaler.metrics.domain.ComputedMetrics;

public interface Objective {

    ObjectiveName name();

    double compute(ResourceScalingConfig config, ComputedMetrics metrics);

    double getPenalized(double rawValue, ResourceScalingConfig config, ComputedMetrics metrics);
}
