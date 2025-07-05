package com.uni.ea_autoscaler.ga.operators;

import com.uni.ea_autoscaler.common.DeploymentConfig;
import com.uni.ea_autoscaler.common.HPAConfig;
import com.uni.ea_autoscaler.common.ResourceScalingConfig;
import com.uni.ea_autoscaler.ga.model.Individual;
import com.uni.ea_autoscaler.ga.nsga3.ElitismScoreCalculator;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
public class CrossoverOperator {

    private final Random random = new Random();

    public Individual crossover(Individual p1, Individual p2) {
        boolean favorP1 = ElitismScoreCalculator.get(p1) < ElitismScoreCalculator.get(p2);

        ResourceScalingConfig c1 = p1.getConfig();
        ResourceScalingConfig c2 = p2.getConfig();

        int minReplicas = pick(c1.getHpa().getMinReplicas(), c2.getHpa().getMinReplicas(), favorP1);
        int maxReplicas = pick(c1.getHpa().getMaxReplicas(), c2.getHpa().getMaxReplicas(), favorP1);
        if (maxReplicas <= minReplicas) {
            maxReplicas = minReplicas + 1;
        }

        double cpuThreshold = pick(c1.getHpa().getCpuThreshold(), c2.getHpa().getCpuThreshold(), favorP1);
        double memoryThreshold = pick(c1.getHpa().getMemoryThreshold(), c2.getHpa().getMemoryThreshold(), favorP1);
        int stabilizationWindow = pick(c1.getHpa().getStabilizationWindowSeconds(), c2.getHpa().getStabilizationWindowSeconds(), favorP1);
        int cpuRequest = pick(c1.getDeployment().getCpuRequest(), c2.getDeployment().getCpuRequest(), favorP1);
        int memoryRequest = pick(c1.getDeployment().getMemoryRequest(), c2.getDeployment().getMemoryRequest(), favorP1);

        DeploymentConfig deployment = new DeploymentConfig(minReplicas, cpuRequest, memoryRequest);
        HPAConfig hpa = new HPAConfig(true, minReplicas, maxReplicas, cpuThreshold, memoryThreshold, stabilizationWindow);
        ResourceScalingConfig childConfig = new ResourceScalingConfig(deployment, hpa);

        return new Individual(childConfig);
    }

    private int pick(int a, int b, boolean favorA) {
        return (favorA || random.nextBoolean()) ? a : b;
    }

    private double pick(double a, double b, boolean favorA) {
        return (favorA || random.nextBoolean()) ? a : b;
    }
}