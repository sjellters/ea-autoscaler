package com.uni.ea_autoscaler.ga.model;

import lombok.*;

import java.util.Arrays;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScalingConfiguration {

    private int minReplicas;
    private int maxReplicas;
    private double cpuThreshold;
    private double memoryThreshold;
    private int cooldownSeconds;
    private int cpuRequest;
    private int memoryRequest;

    @Setter(AccessLevel.NONE)
    @Getter
    private double[] objectives;

    @Setter(AccessLevel.NONE)
    @Getter
    private double[] normalizedObjectives;

    public ScalingConfiguration(ScalingConfiguration other) {
        this.minReplicas = other.minReplicas;
        this.maxReplicas = other.maxReplicas;
        this.cpuThreshold = other.cpuThreshold;
        this.memoryThreshold = other.memoryThreshold;
        this.cooldownSeconds = other.cooldownSeconds;
        this.cpuRequest = other.cpuRequest;
        this.memoryRequest = other.memoryRequest;
        this.objectives = (other.objectives != null)
                ? Arrays.copyOf(other.objectives, other.objectives.length)
                : null;
        this.normalizedObjectives = (other.normalizedObjectives != null)
                ? Arrays.copyOf(other.normalizedObjectives, other.normalizedObjectives.length)
                : null;
    }

    public void setObjectives(double[] objectives) {
        if (objectives == null || objectives.length == 0) {
            throw new IllegalArgumentException("Objectives array must not be null or empty");
        }
        this.objectives = Arrays.copyOf(objectives, objectives.length);
    }

    public void setNormalizedObjectives(double[] normalized) {
        this.normalizedObjectives = Arrays.copyOf(normalized, normalized.length);
    }

    public ScalingConfiguration copy() {
        return new ScalingConfiguration(this);
    }

    @Override
    public String toString() {
        return "ScalingConfiguration:\n" +
                "  minReplicas: " + minReplicas + "\n" +
                "  maxReplicas: " + maxReplicas + "\n" +
                "  cpuThreshold: " + cpuThreshold + "\n" +
                "  memoryThreshold: " + memoryThreshold + "\n" +
                "  cooldownSeconds: " + cooldownSeconds + "\n" +
                "  cpuRequest: " + cpuRequest + "\n" +
                "  memoryRequest: " + memoryRequest + "\n" +
                "  objectives: " + Arrays.toString(objectives);
    }
}
