package com.uni.ea_autoscaler.common;

import com.uni.ea_autoscaler.core.interfaces.Validatable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class HPAConfig implements Validatable {

    private boolean enabled;
    private int minReplicas;
    private int maxReplicas;
    private double cpuThreshold;
    private double memoryThreshold;
    private int stabilizationWindowSeconds;

    public boolean valid() {
        return enabled &&
                minReplicas >= 1 &&
                maxReplicas > minReplicas &&
                cpuThreshold > 0 &&
                memoryThreshold > 0 &&
                stabilizationWindowSeconds > 0;
    }

    @Override
    public String toString() {
        return String.format("%b-%d-%d-%.2f-%.2f-%d",
                enabled, minReplicas, maxReplicas, cpuThreshold, memoryThreshold, stabilizationWindowSeconds);
    }
}
