package com.uni.ea_autoscaler.ga.model.objective;

import com.uni.ea_autoscaler.common.ResourceScalingConfig;
import com.uni.ea_autoscaler.core.annotations.ObjectiveDefinition;
import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.core.enums.ObjectiveName;
import com.uni.ea_autoscaler.core.interfaces.Objective;
import com.uni.ea_autoscaler.metrics.domain.ComputedMetrics;

@ObjectiveDefinition
public class CPUEfficiencyLossObjective implements Objective {

    @Override
    public ObjectiveName name() {
        return ObjectiveName.CPU_EFFICIENCY_LOSS;
    }

    @Override
    public double compute(ResourceScalingConfig config, ComputedMetrics metrics) {
        double efficiency = metrics.get(MetricName.CPU_EFFICIENCY);
        return 1.0 - Math.min(efficiency, 1.0);
    }

    @Override
    public double getPenalized(double rawValue, ResourceScalingConfig config, ComputedMetrics metrics) {
        return rawValue + Math.pow(6 * rawValue, 2);
    }
}
