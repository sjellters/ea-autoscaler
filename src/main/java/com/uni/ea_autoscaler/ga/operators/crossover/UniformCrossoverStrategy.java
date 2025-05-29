package com.uni.ea_autoscaler.ga.operators.crossover;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.ga.model.ScalingConfigurationValidator;
import com.uni.ea_autoscaler.ga.model.ScalingParameterRanges;
import com.uni.ea_autoscaler.ga.util.ThresholdUtils;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component("uniformCrossover")
public class UniformCrossoverStrategy implements CrossoverStrategy {

    private final ScalingConfigurationValidator validator;
    private final Random random = new Random();

    public UniformCrossoverStrategy(ScalingConfigurationValidator validator) {
        this.validator = validator;
    }

    @Override
    public ScalingConfiguration crossover(ScalingConfiguration p1, ScalingConfiguration p2) {
        int minReplicas = pick(p1.getMinReplicas(), p2.getMinReplicas());
        int maxReplicas = pick(p1.getMaxReplicas(), p2.getMaxReplicas());

        double cpuThreshold = ThresholdUtils.pickNearest(p1.getCpuThreshold(), p2.getCpuThreshold());
        double memoryThreshold = ThresholdUtils.pickNearest(p1.getMemoryThreshold(), p2.getMemoryThreshold());

        int cooldownSeconds = ScalingParameterRanges.discretizeCooldown(
                pick(p1.getCooldownSeconds(), p2.getCooldownSeconds())
        );

        int cpuRequest = ScalingParameterRanges.discretizeCpuRequest(
                pick(p1.getCpuRequest(), p2.getCpuRequest())
        );

        int memoryRequest = ScalingParameterRanges.discretizeMemoryRequest(
                pick(p1.getMemoryRequest(), p2.getMemoryRequest())
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

    private int pick(int a, int b) {
        return random.nextBoolean() ? a : b;
    }
}
