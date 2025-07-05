package com.uni.ea_autoscaler.ga.operators;

import com.uni.ea_autoscaler.ga.model.Individual;
import com.uni.ea_autoscaler.ga.nsga3.NichingSelector;
import com.uni.ea_autoscaler.ga.nsga3.ParetoFrontsCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Component
public class SelectionOperator {

    private final Random random = new Random();
    private final ParetoFrontsCalculator paretoFrontsCalculator;
    private final NichingSelector nichingSelector;

    private final int sampleSize;

    public SelectionOperator(ParetoFrontsCalculator paretoFrontsCalculator,
                             NichingSelector nichingSelector,
                             @Value("${ga.operators.selection.sampleSize:2}") int sampleSize) {
        this.paretoFrontsCalculator = paretoFrontsCalculator;
        this.nichingSelector = nichingSelector;
        this.sampleSize = sampleSize;
    }

    public Individual select(List<Individual> population) {
        if (population.size() < sampleSize) {
            throw new IllegalArgumentException("Population must contain at least " + sampleSize + " individuals");
        }

        List<Individual> tournament = random.ints(0, population.size())
                .distinct()
                .limit(sampleSize)
                .mapToObj(population::get)
                .collect(Collectors.toList());

        List<List<Individual>> fronts = paretoFrontsCalculator.calculateFronts(tournament);
        List<Individual> bestFront = fronts.get(0);

        if (bestFront.size() == 1) {
            return bestFront.get(0);
        } else {
            List<double[]> referencePoints = tournament.stream()
                    .map(Individual::getNormalizedObjectives)
                    .collect(Collectors.toList());

            List<Individual> selected = nichingSelector.selectWithNiching(bestFront, referencePoints, 1);
            return selected.get(0);
        }
    }
}
