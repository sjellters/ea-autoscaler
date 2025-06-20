package com.uni.ea_autoscaler.ga.operators;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.ga.model.ScalingConfigurationValidator;
import com.uni.ea_autoscaler.ga.model.ScalingParameterRanges;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class MutationOperator {

    private final ScalingConfigurationValidator scalingConfigurationValidator;
    private final Random random = new Random(16);

    public MutationOperator(ScalingConfigurationValidator scalingConfigurationValidator) {
        this.scalingConfigurationValidator = scalingConfigurationValidator;
    }

    public ScalingConfiguration mutate(ScalingConfiguration original, double mutationRate) {
        ScalingConfiguration mutated = original.copy();

        if (random.nextDouble() < mutationRate) {
            int newMin = randomInt(ScalingParameterRanges.MIN_REPLICAS_MIN, ScalingParameterRanges.MIN_REPLICAS_MAX);
            mutated.setMinReplicas(newMin);

            if (mutated.getMaxReplicas() <= newMin) {
                mutated.setMaxReplicas(randomInt(newMin + ScalingParameterRanges.MAX_REPLICAS_MIN_OFFSET, ScalingParameterRanges.MAX_REPLICAS_MAX));
            }
        }

        if (random.nextDouble() < mutationRate) {
            int min = mutated.getMinReplicas() + ScalingParameterRanges.MAX_REPLICAS_MIN_OFFSET;
            mutated.setMaxReplicas(randomInt(min, ScalingParameterRanges.MAX_REPLICAS_MAX));
        }

        if (random.nextDouble() < mutationRate) {
            mutated.setCpuThreshold(randomThreshold());
        }

        if (random.nextDouble() < mutationRate) {
            mutated.setMemoryThreshold(randomThreshold());
        }

        if (random.nextDouble() < mutationRate) {
            int rawCooldown = randomInt(ScalingParameterRanges.COOLDOWN_MIN, ScalingParameterRanges.COOLDOWN_MAX);
            mutated.setCooldownSeconds(ScalingParameterRanges.discretizeCooldown(rawCooldown));
        }

        if (random.nextDouble() < mutationRate) {
            int rawCpu = randomInt(ScalingParameterRanges.CPU_REQUEST_MIN, ScalingParameterRanges.CPU_REQUEST_MAX);
            mutated.setCpuRequest(ScalingParameterRanges.discretizeCpuRequest(rawCpu));
        }

        if (random.nextDouble() < mutationRate) {
            int rawMemory = randomInt(ScalingParameterRanges.MEMORY_REQUEST_MIN, ScalingParameterRanges.MEMORY_REQUEST_MAX);
            mutated.setMemoryRequest(ScalingParameterRanges.discretizeMemoryRequest(rawMemory));
        }

        return scalingConfigurationValidator.isValid(mutated) ? mutated : original.copy();
    }

    private int randomInt(int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    private double randomThreshold() {
        return ScalingParameterRanges.THRESHOLDS[random.nextInt(ScalingParameterRanges.THRESHOLDS.length)];
    }
}
