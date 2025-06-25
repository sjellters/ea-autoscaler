package com.uni.ea_autoscaler.old.ga.operators.crossover;

import com.uni.ea_autoscaler.old.ga.model.ScalingConfiguration;

public interface CrossoverStrategy {

    ScalingConfiguration crossover(ScalingConfiguration parent1, ScalingConfiguration parent2);
}
