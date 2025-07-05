package com.uni.ea_autoscaler.metrics.definitions;

import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.core.annotations.MetricDefinition;
import com.uni.ea_autoscaler.core.interfaces.Metric;
import com.uni.ea_autoscaler.metrics.domain.ComputeMetricsInput;

import java.util.OptionalDouble;

@MetricDefinition
public class CpuEfficiencyMetric implements Metric {

    @Override
    public MetricName name() {
        return MetricName.CPU_EFFICIENCY;
    }

    @Override
    public double compute(ComputeMetricsInput input) {
        double cpuRequestCores = input.config().getDeployment().getCpuRequest() / 1000.0;

        OptionalDouble avgReplicasOpt = input.prometheus().avgReplicas().flattenedValues().stream()
                .mapToDouble(Double::doubleValue)
                .average();

        if (avgReplicasOpt.isEmpty()) {
            return Double.NaN;
        }

        double totalRequestCores = cpuRequestCores * avgReplicasOpt.getAsDouble();

        return input.prometheus().avgCpu().flattenedValues().stream()
                .mapToDouble(usage -> usage / totalRequestCores)
                .average()
                .orElse(Double.NaN);
    }
}

