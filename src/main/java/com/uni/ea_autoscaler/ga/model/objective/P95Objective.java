package com.uni.ea_autoscaler.ga.model.objective;

import com.uni.ea_autoscaler.common.ResourceScalingConfig;
import com.uni.ea_autoscaler.core.annotations.ObjectiveDefinition;
import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.core.enums.ObjectiveName;
import com.uni.ea_autoscaler.core.interfaces.Objective;
import com.uni.ea_autoscaler.metrics.domain.ComputedMetrics;

@ObjectiveDefinition
public class P95Objective implements Objective {

    @Override
    public ObjectiveName name() {
        return ObjectiveName.P95;
    }

    @Override
    public double compute(ResourceScalingConfig config, ComputedMetrics metrics) {
        return metrics.get(MetricName.P95);
    }

    @Override
    public double getPenalized(double rawValue, ResourceScalingConfig config, ComputedMetrics metrics) {
        return rawValue;
    }
}
