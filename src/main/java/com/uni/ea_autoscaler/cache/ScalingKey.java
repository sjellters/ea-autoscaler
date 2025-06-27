package com.uni.ea_autoscaler.cache;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
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
    public String toString() {
        return String.join("-",
                String.valueOf(minReplicas),
                String.valueOf(maxReplicas),
                String.format("%.1f", cpuThreshold),
                String.format("%.1f", memoryThreshold),
                String.valueOf(cooldownSeconds),
                String.valueOf(cpuRequest),
                String.valueOf(memoryRequest)
        );
    }
}
