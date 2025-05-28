package com.uni.ea_autoscaler.ga.operators.crossover;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.ga.model.ScalingConfigurationValidator;
import com.uni.ea_autoscaler.ga.model.ScalingParameterRanges;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component("blendCrossover")
public class BlendCrossoverStrategy implements CrossoverStrategy {

    private final ScalingConfigurationValidator validator;
    private final Random random = new Random();

    public BlendCrossoverStrategy(ScalingConfigurationValidator validator) {
        this.validator = validator;
    }

    @Override
    public ScalingConfiguration crossover(ScalingConfiguration p1, ScalingConfiguration p2) {
        int minReplicas = pick(p1.getMinReplicas(), p2.getMinReplicas());
        int maxReplicas = pick(p1.getMaxReplicas(), p2.getMaxReplicas());

        double cpuThreshold = pickNearestThreshold(p1.getCpuThreshold(), p2.getCpuThreshold());
        double memoryThreshold = pickNearestThreshold(p1.getMemoryThreshold(), p2.getMemoryThreshold());

        int cooldownSeconds = pick(p1.getCooldownSeconds(), p2.getCooldownSeconds());

        int cpuRequest = (int) Math.min(
                blxAlpha(p1.getCpuRequest(), p2.getCpuRequest(),
                        ScalingParameterRanges.CPU_REQUEST_MIN, ScalingParameterRanges.CPU_REQUEST_MAX),
                ScalingParameterRanges.CPU_REQUEST_MAX
        );

        int memoryRequest = (int) Math.min(
                blxAlpha(p1.getMemoryRequest(), p2.getMemoryRequest(),
                        ScalingParameterRanges.MEMORY_REQUEST_MIN, ScalingParameterRanges.MEMORY_REQUEST_MAX),
                ScalingParameterRanges.MEMORY_REQUEST_MAX
        );

        ScalingConfiguration child = ScalingConfiguration.builder()
                .minReplicas(minReplicas)
                .maxReplicas(maxReplicas)
                .cpuThreshold(cpuThreshold)
                .memoryThreshold(memoryThreshold)
                .cooldownSeconds(cooldownSeconds)
                .cpuRequest(cpuRequest)
                .memoryRequest(memoryRequest)
                .build();

        return validator.isValid(child) ? child : p1.copy();
    }

    private double blxAlpha(double a, double b, double minBound, double maxBound) {
        double min = Math.min(a, b);
        double max = Math.max(a, b);
        double range = max - min;
        double alpha = 0.3;
        double lower = min - alpha * range;
        double upper = max + alpha * range;
        return clamp(lower + random.nextDouble() * (upper - lower), minBound, maxBound);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private int pick(int a, int b) {
        return random.nextBoolean() ? a : b;
    }

    private double pickNearestThreshold(double t1, double t2) {
        double avg = (t1 + t2) / 2.0;
        double[] thresholds = ScalingParameterRanges.THRESHOLDS;
        double closest = thresholds[0];
        double minDiff = Math.abs(thresholds[0] - avg);

        for (int i = 1; i < thresholds.length; i++) {
            double diff = Math.abs(thresholds[i] - avg);
            if (diff < minDiff) {
                minDiff = diff;
                closest = thresholds[i];
            }
        }

        return closest;
    }
}
