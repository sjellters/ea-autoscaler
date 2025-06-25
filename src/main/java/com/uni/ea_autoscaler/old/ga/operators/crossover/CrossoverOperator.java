package com.uni.ea_autoscaler.old.ga.operators.crossover;

import com.uni.ea_autoscaler.old.ga.model.ScalingConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class CrossoverOperator {

    private final CrossoverStrategy crossoverStrategy;

    public CrossoverOperator(@Qualifier("blendCrossover") CrossoverStrategy crossoverStrategy) {
        this.crossoverStrategy = crossoverStrategy;
    }

    public ScalingConfiguration crossover(ScalingConfiguration parent1, ScalingConfiguration parent2) {
        return crossoverStrategy.crossover(parent1, parent2);
    }
}

