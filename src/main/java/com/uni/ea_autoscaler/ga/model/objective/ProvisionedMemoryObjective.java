package com.uni.ea_autoscaler.ga.model.objective;

import com.uni.ea_autoscaler.common.ResourceScalingConfig;
import com.uni.ea_autoscaler.core.annotations.ObjectiveDefinition;
import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.core.enums.ObjectiveName;
import com.uni.ea_autoscaler.core.interfaces.Objective;
import com.uni.ea_autoscaler.ga.initialization.ParameterRanges;
import com.uni.ea_autoscaler.metrics.domain.ComputedMetrics;

@ObjectiveDefinition
public class ProvisionedMemoryObjective implements Objective {

    @Override
    public ObjectiveName name() {
        return ObjectiveName.PROVISIONED_MEMORY;
    }

    @Override
    public double compute(ResourceScalingConfig config, ComputedMetrics metrics) {
        double memoryMiB = metrics.get(MetricName.AVG_REPLICAS) * config.getDeployment().getMemoryRequest();
        return memoryMiB / 1024.0;
    }

    @Override
    public double getPenalized(double rawValue, ResourceScalingConfig config, ComputedMetrics metrics) {
        double minGiB = ParameterRanges.MEMORY_REQUEST_TIERS[0] / 1024.0;
        if (rawValue <= minGiB) return rawValue;

        double diff = rawValue - minGiB;

        if (rawValue > 1.0) {
            return rawValue + Math.pow(10 * diff, 2);
        } else if (rawValue > 0.75) {
            return rawValue + Math.pow(7 * diff, 2);
        } else {
            return rawValue + Math.pow(5 * diff, 2);
        }
    }
}
