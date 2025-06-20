package com.uni.ea_autoscaler.ga.operators.selection;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.ga.selection.nsga.NichingSelector;
import com.uni.ea_autoscaler.ga.selection.nsga.ParetoFrontsCalculator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Component("tournamentSelection")
public class TournamentSelectionStrategy implements SelectionStrategy {

    private final Random random = new Random(16);
    private final int tournamentSize;
    private final ParetoFrontsCalculator paretoFrontsCalculator;
    private final NichingSelector nichingSelector;

    public TournamentSelectionStrategy(
            @Value("${selection.tournamentSize}") int tournamentSize,
            ParetoFrontsCalculator paretoFrontsCalculator,
            NichingSelector nichingSelector
    ) {
        this.tournamentSize = tournamentSize;
        this.paretoFrontsCalculator = paretoFrontsCalculator;
        this.nichingSelector = nichingSelector;
    }

    @Override
    public ScalingConfiguration select(List<ScalingConfiguration> population) {
        if (population.size() < tournamentSize) {
            throw new IllegalArgumentException("Population must contain at least " + tournamentSize + " individuals");
        }

        List<ScalingConfiguration> tournament = random.ints(0, population.size())
                .distinct()
                .limit(tournamentSize)
                .mapToObj(population::get)
                .collect(Collectors.toList());

        List<List<ScalingConfiguration>> fronts = paretoFrontsCalculator.calculateFronts(tournament);
        List<ScalingConfiguration> bestFront = fronts.get(0);

        if (bestFront.size() == 1) {
            return bestFront.get(0);
        } else {
            List<double[]> referencePoints = tournament.stream()
                    .map(ScalingConfiguration::getNormalizedObjectives)
                    .collect(Collectors.toList());

            List<ScalingConfiguration> selected = nichingSelector.selectWithNiching(bestFront, referencePoints, 1);
            return selected.get(0);
        }
    }
}
