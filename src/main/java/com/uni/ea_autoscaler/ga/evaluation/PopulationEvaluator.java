package com.uni.ea_autoscaler.ga.evaluation;

import com.uni.ea_autoscaler.ga.model.Individual;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopulationEvaluator {

    private final IndividualEvaluator individualEvaluator;

    public void evaluate(List<Individual> population) {
        List<Individual> evaluated = new ArrayList<>();

        for (Individual individual : population) {
            individualEvaluator.evaluate(individual);

            if (individual.getObjectives() == null || individual.getObjectives().isEmpty()) {
                log.warn("⚠️ Individual evaluation failed. Excluding from population.");
                continue;
            }

            evaluated.add(individual);
        }

        log.info("📊 Evaluated population: {} valid of {}", evaluated.size(), population.size());
    }
}
