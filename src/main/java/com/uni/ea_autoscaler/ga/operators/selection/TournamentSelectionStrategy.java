package com.uni.ea_autoscaler.ga.operators.selection;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Random;

@Component("tournamentSelection")
public class TournamentSelectionStrategy implements SelectionStrategy {

    private final Random random = new Random();
    private final int tournamentSize;

    public TournamentSelectionStrategy(@Value("${selection.tournamentSize}") int tournamentSize) {
        this.tournamentSize = tournamentSize;
    }

    @Override
    public ScalingConfiguration select(List<ScalingConfiguration> population) {
        if (population.size() < tournamentSize) {
            throw new IllegalArgumentException("Population must contain at least " + tournamentSize + " individuals");
        }

        ScalingConfiguration best = randomIndividual(population);

        for (int i = 1; i < tournamentSize; i++) {
            ScalingConfiguration contender = randomIndividual(population);
            if (fitness(contender) < fitness(best)) {
                best = contender;
            }
        }

        return best;
    }

    private ScalingConfiguration randomIndividual(List<ScalingConfiguration> population) {
        return population.get(random.nextInt(population.size()));
    }

    private double fitness(ScalingConfiguration config) {
        double[] objectives = config.getObjectives();
        double sum = 0.0;
        for (double o : objectives) {
            sum += o;
        }
        return sum;
    }
}
