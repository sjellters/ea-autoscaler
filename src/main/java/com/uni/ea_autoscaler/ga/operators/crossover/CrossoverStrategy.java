package com.uni.ea_autoscaler.ga.operators.crossover;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;

public interface CrossoverStrategy {

    ScalingConfiguration crossover(ScalingConfiguration parent1, ScalingConfiguration parent2);
}
