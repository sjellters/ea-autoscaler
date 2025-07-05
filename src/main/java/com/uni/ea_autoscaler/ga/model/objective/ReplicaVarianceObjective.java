package com.uni.ea_autoscaler.ga.model.objective;

import com.uni.ea_autoscaler.common.ResourceScalingConfig;
import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.core.enums.ObjectiveName;
import com.uni.ea_autoscaler.core.interfaces.Objective;
import com.uni.ea_autoscaler.metrics.domain.ComputedMetrics;

//@ObjectiveDefinition
public class ReplicaVarianceObjective implements Objective {

    @Override
    public ObjectiveName name() {
//        return ObjectiveName.REPLICA_VARIANCE;
        return null;
    }

    @Override
    public double compute(ResourceScalingConfig config, ComputedMetrics metrics) {
        return metrics.get(MetricName.REPLICA_VARIANCE);
    }

    @Override
    public double getPenalized(double rawValue, ResourceScalingConfig config, ComputedMetrics metrics) {
        if (rawValue < 0.1) return rawValue;
        else return Math.pow(rawValue, 2) * 5;
    }
}
