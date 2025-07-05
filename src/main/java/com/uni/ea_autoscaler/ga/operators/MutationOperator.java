package com.uni.ea_autoscaler.ga.operators;

import com.uni.ea_autoscaler.ga.initialization.ParameterRanges;
import com.uni.ea_autoscaler.ga.model.Individual;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class MutationOperator {

    private final Random random = new Random();

    private final double mutationRate;

    public MutationOperator(@Value("${ga.operators.mutation.rate:0.2}") double mutationRate) {
        this.mutationRate = mutationRate;
    }

    public void mutate(Individual original) {
        Individual mutated = original.copy();

        if (random.nextDouble() < mutationRate) {
            int newMin = randomInt(ParameterRanges.MIN_REPLICAS_MIN, ParameterRanges.MIN_REPLICAS_MAX);
            mutated.getConfig().getHpa().setMinReplicas(newMin);
            mutated.getConfig().getDeployment().setReplicas(newMin);

            if (mutated.getConfig().getHpa().getMaxReplicas() <= newMin) {
                mutated.getConfig().getHpa().setMaxReplicas(randomInt(newMin + 1, ParameterRanges.MAX_REPLICAS));
            }
        }

        if (random.nextDouble() < mutationRate) {
            mutated.getConfig().getHpa().setCpuThreshold(randomThreshold());
        }

        if (random.nextDouble() < mutationRate) {
            mutated.getConfig().getHpa().setMemoryThreshold(randomThreshold());
        }

        if (random.nextDouble() < mutationRate) {
            int stabilizationWindow = randomElement(ParameterRanges.STABILIZATION_WINDOW);
            mutated.getConfig().getHpa().setStabilizationWindowSeconds(stabilizationWindow);
        }

        if (random.nextDouble() < mutationRate) {
            mutated.getConfig().getDeployment().setCpuRequest(randomElement(ParameterRanges.CPU_REQUEST_TIERS));
        }

        if (random.nextDouble() < mutationRate) {
            mutated.getConfig().getDeployment().setMemoryRequest(randomElement(ParameterRanges.MEMORY_REQUEST_TIERS));
        }

    }

    private int randomInt(int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    private int randomElement(int[] values) {
        return values[random.nextInt(values.length)];
    }

    private double randomThreshold() {
        return ParameterRanges.THRESHOLDS[random.nextInt(ParameterRanges.THRESHOLDS.length)];
    }
}
