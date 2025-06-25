package com.uni.ea_autoscaler.old.ga.selection;

import com.uni.ea_autoscaler.old.ga.model.ScalingConfiguration;

import java.util.List;

public interface NextGenerationSelector {

    List<ScalingConfiguration> selectNextGeneration(List<ScalingConfiguration> current,
                                                    List<ScalingConfiguration> offspring,
                                                    int populationSize);
}
