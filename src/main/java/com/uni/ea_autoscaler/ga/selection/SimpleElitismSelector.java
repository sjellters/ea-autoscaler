package com.uni.ea_autoscaler.ga.selection;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class SimpleElitismSelector implements NextGenerationSelector {

    @Override
    public List<ScalingConfiguration> selectNextGeneration(List<ScalingConfiguration> current,
                                                           List<ScalingConfiguration> offspring,
                                                           int populationSize) {
        return Stream.concat(current.stream(), offspring.stream())
                .sorted(Comparator.comparingDouble(ind -> ind.getObjectives()[0]))
                .limit(populationSize)
                .collect(Collectors.toList());
    }
}
