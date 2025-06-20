package com.uni.ea_autoscaler.ga.operators.crossover;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.ga.model.ScalingConfigurationValidator;
import com.uni.ea_autoscaler.ga.model.ScalingParameterRanges;
import com.uni.ea_autoscaler.ga.util.ThresholdUtils;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@Component("blendCrossover")
public class BlendCrossoverStrategy implements CrossoverStrategy {

    private static final double BLX_ALPHA = 0.3;
    private final ScalingConfigurationValidator validator;
    private final Random random = new Random(16);

    public BlendCrossoverStrategy(ScalingConfigurationValidator validator) {
        this.validator = validator;
    }

    @Override
    public ScalingConfiguration crossover(ScalingConfiguration p1, ScalingConfiguration p2) {
        int minReplicas = pick(p1.getMinReplicas(), p2.getMinReplicas());
        int maxReplicas = pick(p1.getMaxReplicas(), p2.getMaxReplicas());

        if (maxReplicas <= minReplicas) {
            maxReplicas = minReplicas + ScalingParameterRanges.MAX_REPLICAS_MIN_OFFSET;
        }

        double cpuThreshold = ThresholdUtils.pickNearest(p1.getCpuThreshold(), p2.getCpuThreshold());
        double memoryThreshold = ThresholdUtils.pickNearest(p1.getMemoryThreshold(), p2.getMemoryThreshold());

        int rawCooldown = pick(p1.getCooldownSeconds(), p2.getCooldownSeconds());
        int cooldownSeconds = ScalingParameterRanges.discretizeCooldown(rawCooldown);

        int rawCpu = (int) blxAlpha(p1.getCpuRequest(), p2.getCpuRequest(),
                ScalingParameterRanges.CPU_REQUEST_MIN, ScalingParameterRanges.CPU_REQUEST_MAX);
        int cpuRequest = ScalingParameterRanges.discretizeCpuRequest(rawCpu);

        int rawMemory = (int) blxAlpha(p1.getMemoryRequest(), p2.getMemoryRequest(),
                ScalingParameterRanges.MEMORY_REQUEST_MIN, ScalingParameterRanges.MEMORY_REQUEST_MAX);
        int memoryRequest = ScalingParameterRanges.discretizeMemoryRequest(rawMemory);

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
        double lower = min - BLX_ALPHA * range;
        double upper = max + BLX_ALPHA * range;
        return clamp(lower + ThreadLocalRandom.current().nextDouble() * (upper - lower), minBound, maxBound);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private int pick(int a, int b) {
        return random.nextBoolean() ? a : b;
    }
}
