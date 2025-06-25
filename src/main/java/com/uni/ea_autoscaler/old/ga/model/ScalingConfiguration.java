package com.uni.ea_autoscaler.old.ga.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
        StringBuilder sb = new StringBuilder("ScalingConfiguration:\n");

        sb.append("  minReplicas: ").append(minReplicas).append("\n");
        sb.append("  maxReplicas: ").append(maxReplicas).append("\n");
        sb.append("  cpuThreshold: ").append(String.format("%.1f", cpuThreshold)).append("\n");
        sb.append("  memoryThreshold: ").append(String.format("%.1f", memoryThreshold)).append("\n");
        sb.append("  cooldownSeconds: ").append(cooldownSeconds).append("\n");
        sb.append("  cpuRequest: ").append(cpuRequest).append("\n");
        sb.append("  memoryRequest: ").append(memoryRequest).append("\n");

        sb.append("  objectives: ");
        if (objectives != null) {
            sb.append("[");
            for (int i = 0; i < objectives.length; i++) {
                sb.append(String.format("%.7f", objectives[i]));
                if (i < objectives.length - 1) sb.append(", ");
            }
            sb.append("]");
        } else {
            sb.append("null");
        }

        return sb.toString();
    }
}
