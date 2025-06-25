package com.uni.ea_autoscaler.old.ga.operators.selection;

import com.uni.ea_autoscaler.old.ga.model.ScalingConfiguration;

import java.util.List;

public interface SelectionStrategy {

    ScalingConfiguration select(List<ScalingConfiguration> population);
}
