package com.uni.ea_autoscaler.ga.initialization;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.ga.model.ScalingConfigurationValidator;
import com.uni.ea_autoscaler.ga.model.ScalingParameterRanges;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
public class RandomInitializer {

    private final ScalingConfigurationValidator scalingConfigurationValidator;
    private final Random random = new Random();

    public RandomInitializer(ScalingConfigurationValidator scalingConfigurationValidator) {
        this.scalingConfigurationValidator = scalingConfigurationValidator;
    }

    public ScalingConfiguration generateIndividual() {
        int minReplicas = randomInt(ScalingParameterRanges.MIN_REPLICAS_MIN, ScalingParameterRanges.MIN_REPLICAS_MAX);
        int maxReplicas = randomInt(minReplicas + ScalingParameterRanges.MAX_REPLICAS_MIN_OFFSET, ScalingParameterRanges.MAX_REPLICAS_MAX);

        double cpuThreshold = randomThreshold();
        double memoryThreshold = randomThreshold();

        int cooldownSeconds = ScalingParameterRanges.discretizeCooldown(
                randomInt(ScalingParameterRanges.COOLDOWN_MIN, ScalingParameterRanges.COOLDOWN_MAX)
        );

        int cpuRequest = randomElement(ScalingParameterRanges.CPU_REQUEST_TIERS);
        int memoryRequest = randomElement(ScalingParameterRanges.MEMORY_REQUEST_TIERS);

        return ScalingConfiguration.builder()
                .minReplicas(minReplicas)
                .maxReplicas(maxReplicas)
                .cpuThreshold(cpuThreshold)
                .memoryThreshold(memoryThreshold)
                .cooldownSeconds(cooldownSeconds)
                .cpuRequest(cpuRequest)
                .memoryRequest(memoryRequest)
                .build();
    }

    public List<ScalingConfiguration> generatePopulation(int size) {
        List<ScalingConfiguration> population = new ArrayList<>();

        while (population.size() < size) {
            ScalingConfiguration individual = generateIndividual();
            if (scalingConfigurationValidator.isValid(individual)) {
                population.add(individual);
            } else {
                log.debug("❌ Invalid generated individual discarded:\n{}", individual);
            }
        }

        return population;
    }

    private int randomInt(int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    private double randomThreshold() {
        return ScalingParameterRanges.THRESHOLDS[random.nextInt(ScalingParameterRanges.THRESHOLDS.length)];
    }

    private int randomElement(int[] values) {
        return values[random.nextInt(values.length)];
    }
}

