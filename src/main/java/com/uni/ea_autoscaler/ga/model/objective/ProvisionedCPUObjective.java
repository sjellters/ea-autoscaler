package com.uni.ea_autoscaler.ga.model.objective;

import com.uni.ea_autoscaler.common.ResourceScalingConfig;
import com.uni.ea_autoscaler.core.annotations.ObjectiveDefinition;
import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.core.enums.ObjectiveName;
import com.uni.ea_autoscaler.core.interfaces.Objective;
import com.uni.ea_autoscaler.ga.initialization.ParameterRanges;
import com.uni.ea_autoscaler.metrics.domain.ComputedMetrics;

@ObjectiveDefinition
public class ProvisionedCPUObjective implements Objective {

    @Override
    public ObjectiveName name() {
        return ObjectiveName.PROVISIONED_CPU;
    }

    @Override
    public double compute(ResourceScalingConfig config, ComputedMetrics metrics) {
        double cpuMillicores = metrics.get(MetricName.AVG_REPLICAS) * config.getDeployment().getCpuRequest();
        return cpuMillicores / 1000.0;
    }

    @Override
    public double getPenalized(double rawValue, ResourceScalingConfig config, ComputedMetrics metrics) {
        double minCores = ParameterRanges.CPU_REQUEST_TIERS[0] / 1000.0;
        if (rawValue <= minCores) return rawValue;

        double excess = rawValue - minCores;
        return rawValue + Math.pow(6 * excess, 2);
    }
}
