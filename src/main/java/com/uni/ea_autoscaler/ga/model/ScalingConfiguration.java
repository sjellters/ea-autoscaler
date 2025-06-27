package com.uni.ea_autoscaler.ga.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.LinkedHashMap;

@Slf4j
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

    private LinkedHashMap<String, Double> metrics = new LinkedHashMap<>();

    private LinkedHashMap<String, Double> objectivesMap = new LinkedHashMap<>();

    private LinkedHashMap<String, Double> penalizedObjectivesMap = new LinkedHashMap<>();

    @Getter
    private double[] objectives;

    @Getter
    private double[] normalizedObjectives;

    public ScalingConfiguration(ScalingConfiguration other) {
        if (other.getPenalizedObjectivesMap() == null) {
            log.warn("⚠️ Copying ScalingConfiguration with null objectivesMap. This may lead to unexpected behavior.");
        }

        this.minReplicas = other.getMinReplicas();
        this.maxReplicas = other.getMaxReplicas();
        this.cpuThreshold = other.getCpuThreshold();
        this.memoryThreshold = other.getMemoryThreshold();
        this.cooldownSeconds = other.getCooldownSeconds();
        this.cpuRequest = other.getCpuRequest();
        this.memoryRequest = other.getMemoryRequest();
        this.metrics = (other.getMetrics() != null)
                ? new LinkedHashMap<>(other.getMetrics())
                : new LinkedHashMap<>();
        this.objectivesMap = (other.getObjectivesMap() != null)
                ? new LinkedHashMap<>(other.getObjectivesMap())
                : new LinkedHashMap<>();
        this.penalizedObjectivesMap = (other.getPenalizedObjectivesMap() != null)
                ? new LinkedHashMap<>(other.getPenalizedObjectivesMap())
                : new LinkedHashMap<>();
        this.objectives = (other.getObjectives() != null)
                ? Arrays.copyOf(other.getObjectives(), other.getObjectives().length)
                : null;
        this.normalizedObjectives = (other.getNormalizedObjectives() != null)
                ? Arrays.copyOf(other.getNormalizedObjectives(), other.getNormalizedObjectives().length)
                : null;
    }

    public void setObjectives(double[] objectives) {
        if (objectives == null || objectives.length == 0) {
            log.warn("⚠️ Attempting to set null or empty objectives. This may lead to unexpected behavior.");
            return;
        }
        this.objectives = Arrays.copyOf(objectives, objectives.length);
    }

    public void setNormalizedObjectives(double[] normalized) {
        if (normalized == null || normalized.length == 0) {
            log.warn("⚠️ Attempting to set null or empty normalized objectives. This may lead to unexpected behavior.");
            return;
        }
        this.normalizedObjectives = Arrays.copyOf(normalized, normalized.length);
    }

    public ScalingConfiguration copy() {
        return new ScalingConfiguration(this);
    }

    @Override
    public String toString() {
        return "ScalingConfiguration:\n" + "  minReplicas: " + minReplicas + "\n" +
                "  maxReplicas: " + maxReplicas + "\n" +
                "  cpuThreshold: " + String.format("%.1f", cpuThreshold) + "\n" +
                "  memoryThreshold: " + String.format("%.1f", memoryThreshold) + "\n" +
                "  cooldownSeconds: " + cooldownSeconds + "\n" +
                "  cpuRequest: " + cpuRequest + "\n" +
                "  memoryRequest: " + memoryRequest + "\n";
    }
}
