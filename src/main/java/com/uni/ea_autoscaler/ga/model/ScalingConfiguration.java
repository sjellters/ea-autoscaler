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
    @Builder.Default
    private double[] objectives = new double[6];

    public ScalingConfiguration(ScalingConfiguration other) {
        this.minReplicas = other.minReplicas;
        this.maxReplicas = other.maxReplicas;
        this.cpuThreshold = other.cpuThreshold;
        this.memoryThreshold = other.memoryThreshold;
        this.cooldownSeconds = other.cooldownSeconds;
        this.cpuRequest = other.cpuRequest;
        this.memoryRequest = other.memoryRequest;
        this.objectives = Arrays.copyOf(other.objectives, other.objectives.length);
    }

    public void setObjectives(double[] objectives) {
        if (objectives == null || objectives.length != 6) {
            throw new IllegalArgumentException("Objectives array must have length 6");
        }
        this.objectives = Arrays.copyOf(objectives, 6);
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
