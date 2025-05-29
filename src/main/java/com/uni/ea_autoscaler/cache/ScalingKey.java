package com.uni.ea_autoscaler.cache;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;

import java.util.Objects;

public class ScalingKey {

    private final int minReplicas;
    private final int maxReplicas;
    private final double cpuThreshold;
    private final double memoryThreshold;
    private final int cooldownSeconds;
    private final int cpuRequest;
    private final int memoryRequest;

    public ScalingKey(ScalingConfiguration config) {
        this.minReplicas = config.getMinReplicas();
        this.maxReplicas = config.getMaxReplicas();
        this.cpuThreshold = config.getCpuThreshold();
        this.memoryThreshold = config.getMemoryThreshold();
        this.cooldownSeconds = config.getCooldownSeconds();
        this.cpuRequest = config.getCpuRequest();
        this.memoryRequest = config.getMemoryRequest();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ScalingKey that)) return false;
        return minReplicas == that.minReplicas &&
                maxReplicas == that.maxReplicas &&
                Double.compare(cpuThreshold, that.cpuThreshold) == 0 &&
                Double.compare(memoryThreshold, that.memoryThreshold) == 0 &&
                cooldownSeconds == that.cooldownSeconds &&
                cpuRequest == that.cpuRequest &&
                memoryRequest == that.memoryRequest;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minReplicas, maxReplicas, cpuThreshold, memoryThreshold, cooldownSeconds, cpuRequest, memoryRequest);
    }

    @Override
    public String toString() {
        return minReplicas + "-" + maxReplicas + "-" + cpuThreshold + "-" + memoryThreshold + "-" +
                cooldownSeconds + "-" + cpuRequest + "-" + memoryRequest;
    }
}
