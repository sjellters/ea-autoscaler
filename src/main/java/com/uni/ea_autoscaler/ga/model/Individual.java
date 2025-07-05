package com.uni.ea_autoscaler.ga.model;

import com.uni.ea_autoscaler.common.DeploymentConfig;
import com.uni.ea_autoscaler.common.HPAConfig;
import com.uni.ea_autoscaler.common.ResourceScalingConfig;
import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.core.enums.ObjectiveName;
import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class Individual {

    private final ResourceScalingConfig config;
    private final Map<ObjectiveName, Double> rawObjectives = new LinkedHashMap<>();
    private final Map<ObjectiveName, Double> objectives = new LinkedHashMap<>();
    private double[] normalizedObjectives;
    private Map<MetricName, Double> computedMetrics = new LinkedHashMap<>();
    private boolean evaluationFailed = false;

    public Individual(ResourceScalingConfig config) {
        this.config = config;
    }

    public Individual copy() {
        ResourceScalingConfig copiedConfig = new ResourceScalingConfig(
                new DeploymentConfig(
                        config.getDeployment().getReplicas(),
                        config.getDeployment().getCpuRequest(),
                        config.getDeployment().getMemoryRequest()
                ),
                new HPAConfig(
                        config.getHpa().isEnabled(),
                        config.getHpa().getMinReplicas(),
                        config.getHpa().getMaxReplicas(),
                        config.getHpa().getCpuThreshold(),
                        config.getHpa().getMemoryThreshold(),
                        config.getHpa().getStabilizationWindowSeconds()
                )
        );

        Individual clone = new Individual(copiedConfig);

        clone.setComputedMetrics(new LinkedHashMap<>(this.computedMetrics));
        clone.getRawObjectives().putAll(this.rawObjectives);
        clone.getObjectives().putAll(this.objectives);
        clone.setEvaluationFailed(this.evaluationFailed);

        if (this.normalizedObjectives != null) {
            clone.setNormalizedObjectives(this.normalizedObjectives.clone());
        }

        return clone;
    }

    public void setRawObjective(ObjectiveName name, double value) {
        rawObjectives.put(name, value);
    }

    public double getRawObjective(ObjectiveName name) {
        return rawObjectives.get(name);
    }

    public void setObjective(ObjectiveName name, double value) {
        objectives.put(name, value);
    }

    public Double getObjective(ObjectiveName name) {
        return objectives.get(name);
    }

    public boolean isValid() {
        return !evaluationFailed;
    }

    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n📦 Configuration:\n")
                .append("  - Min Replicas: ").append(config.getHpa().getMinReplicas()).append("\n")
                .append("  - Max Replicas: ").append(config.getHpa().getMaxReplicas()).append("\n")
                .append("  - CPU Threshold: ").append(config.getHpa().getCpuThreshold()).append("\n")
                .append("  - Memory Threshold: ").append(config.getHpa().getMemoryThreshold()).append("\n")
                .append("  - Stabilization Window: ").append(config.getHpa().getStabilizationWindowSeconds()).append("\n")
                .append("  - CPU Request: ").append(config.getDeployment().getCpuRequest()).append("m\n")
                .append("  - Memory Request: ").append(config.getDeployment().getMemoryRequest()).append("Mi\n");

        sb.append("\n📈 Computed Metrics:\n");
        computedMetrics.forEach((k, v) -> sb.append("  - ").append(k.name().toLowerCase()).append(": ").append(v).append("\n"));

        sb.append("\n🧪 Raw Objectives:\n");
        rawObjectives.forEach((k, v) -> sb.append("  - ").append(k.name().toLowerCase()).append(": ").append(v).append("\n"));

        sb.append("\n📊 Penalized Objectives:\n");
        objectives.forEach((k, v) -> sb.append("  - ").append(k.name().toLowerCase()).append(": ").append(v).append("\n"));

        return sb.toString();
    }
}
