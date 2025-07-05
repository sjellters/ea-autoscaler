package com.uni.ea_autoscaler.ga.model.objective;

import com.uni.ea_autoscaler.common.ResourceScalingConfig;
import com.uni.ea_autoscaler.core.annotations.ObjectiveDefinition;
import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.core.enums.ObjectiveName;
import com.uni.ea_autoscaler.core.interfaces.Objective;
import com.uni.ea_autoscaler.metrics.domain.ComputedMetrics;

@ObjectiveDefinition
public class SLAViolationObjective implements Objective {

    @Override
    public ObjectiveName name() {
        return ObjectiveName.SLA_VIOLATION;
    }

    @Override
    public double compute(ResourceScalingConfig config, ComputedMetrics metrics) {
        return 1 - metrics.get(MetricName.SLA_PERCENTAGE);
    }

    @Override
    public double getPenalized(double rawValue, ResourceScalingConfig config, ComputedMetrics metrics) {
        if (rawValue <= 0.2) return rawValue;
        double penalty = Math.pow(6 * (rawValue - 0.2), 2);
        return rawValue + penalty;
    }
}
