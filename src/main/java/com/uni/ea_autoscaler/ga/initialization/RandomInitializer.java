package com.uni.ea_autoscaler.ga.initialization;

import com.uni.ea_autoscaler.common.DeploymentConfig;
import com.uni.ea_autoscaler.common.HPAConfig;
import com.uni.ea_autoscaler.common.ResourceScalingConfig;
import com.uni.ea_autoscaler.ga.model.Individual;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
public class RandomInitializer {

    private final Random random = new Random();

    public Individual generateIndividual() {
        // Common
        int minReplicas = randomInt(ParameterRanges.MIN_REPLICAS_MIN, ParameterRanges.MIN_REPLICAS_MAX);

        // DeploymentConfig
        int cpuRequest = randomElement(ParameterRanges.CPU_REQUEST_TIERS);
        int memoryRequest = randomElement(ParameterRanges.MEMORY_REQUEST_TIERS);

        // HPAConfig
        int maxReplicas = randomInt(minReplicas + 1, ParameterRanges.MAX_REPLICAS);
        double cpuThreshold = randomThreshold();
        double memoryThreshold = randomThreshold();
        int stabilizationWindow = randomElement(ParameterRanges.STABILIZATION_WINDOW);

        DeploymentConfig deployment = new DeploymentConfig(minReplicas, cpuRequest, memoryRequest);
        HPAConfig hpa = new HPAConfig(true, minReplicas, maxReplicas, cpuThreshold, memoryThreshold, stabilizationWindow);
        ResourceScalingConfig config = new ResourceScalingConfig(deployment, hpa);

        return new Individual(config);
    }

    public List<Individual> generatePopulation(int size) {
        List<Individual> population = new ArrayList<>();

        while (population.size() < size) {
            Individual individual = generateIndividual();
            population.add(individual);
        }

        return population;
    }

    private int randomElement(int[] values) {
        return values[random.nextInt(values.length)];
    }

    private double randomThreshold() {
        return ParameterRanges.THRESHOLDS[random.nextInt(ParameterRanges.THRESHOLDS.length)];
    }

    private int randomInt(int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }
}
