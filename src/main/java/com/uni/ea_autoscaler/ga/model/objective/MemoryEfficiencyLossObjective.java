package com.uni.ea_autoscaler.ga.model.objective;

import com.uni.ea_autoscaler.common.ResourceScalingConfig;
import com.uni.ea_autoscaler.core.annotations.ObjectiveDefinition;
import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.core.enums.ObjectiveName;
import com.uni.ea_autoscaler.core.interfaces.Objective;
import com.uni.ea_autoscaler.metrics.domain.ComputedMetrics;

//@ObjectiveDefinition
public class MemoryEfficiencyLossObjective implements Objective {

    @Override
    public ObjectiveName name() {
//        return ObjectiveName.MEMORY_EFFICIENCY_LOSS;
        return null;
    }

    @Override
    public double compute(ResourceScalingConfig config, ComputedMetrics metrics) {
        double efficiency = metrics.get(MetricName.MEMORY_EFFICIENCY);
        return 1.0 - Math.min(efficiency, 1.0);
    }

    @Override
    public double getPenalized(double rawValue, ResourceScalingConfig config, ComputedMetrics metrics) {
        if (rawValue < 0.6) {
            return rawValue + Math.pow(4 * rawValue, 2);
        } else {
            return rawValue + Math.pow(7 * rawValue, 2);
        }
    }
}
