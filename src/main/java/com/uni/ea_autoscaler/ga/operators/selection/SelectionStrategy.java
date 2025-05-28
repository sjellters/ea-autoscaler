package com.uni.ea_autoscaler.ga.operators.selection;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;

import java.util.List;

public interface SelectionStrategy {

    ScalingConfiguration select(List<ScalingConfiguration> population);
}
