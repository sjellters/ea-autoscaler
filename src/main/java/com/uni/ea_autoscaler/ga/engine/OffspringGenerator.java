package com.uni.ea_autoscaler.ga.engine;

import com.uni.ea_autoscaler.core.enums.ObjectiveName;
import com.uni.ea_autoscaler.ga.evaluation.IndividualEvaluator;
import com.uni.ea_autoscaler.ga.model.Individual;
import com.uni.ea_autoscaler.ga.nsga3.ElitismScoreCalculator;
import com.uni.ea_autoscaler.ga.operators.CrossoverOperator;
import com.uni.ea_autoscaler.ga.operators.MutationOperator;
import com.uni.ea_autoscaler.ga.operators.SelectionOperator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class OffspringGenerator {

    private final SelectionOperator selectionOperator;
    private final CrossoverOperator crossoverOperator;
    private final MutationOperator mutationOperator;
    private final IndividualEvaluator individualEvaluator;

    public List<Individual> generate(List<Individual> currentPopulation, int targetSize) {
        List<Individual> offspring = new ArrayList<>();
        Set<String> existingConfigs = new HashSet<>();
        currentPopulation.forEach(ind -> existingConfigs.add(ind.getConfig().toString()));

        while (offspring.size() < targetSize) {
            Individual child = createValidUniqueIndividual(currentPopulation, existingConfigs);

            if (child != null) {
                offspring.add(child);
                existingConfigs.add(child.getConfig().toString());
            }
        }

        return offspring;
    }

    private Individual createValidUniqueIndividual(List<Individual> population, Set<String> existingConfigs) {
        for (int attempt = 0; attempt < 3; attempt++) {
            Individual p1 = selectionOperator.select(population);
            Individual p2 = selectionOperator.select(population);

            Individual child = crossoverOperator.crossover(p1, p2);
            mutationOperator.mutate(child);
            individualEvaluator.evaluate(child);

            String key = child.getConfig().toString();
            if (child.isValid() && !existingConfigs.contains(key)) {
                return child;
            }
        }

        Individual p1 = selectionOperator.select(population);
        Individual p2 = selectionOperator.select(population);

        Individual bestParent = selectBest(p1, p2);
        log.info("🧬 Fallback to best parent (even if duplicate)");
        return bestParent.copy();
    }

    private Individual selectBest(Individual a, Individual b) {
        log.info("🔍 Selecting best between two individuals using dominance");
        int dominance = compareDominance(a, b);
        if (dominance < 0) return a;
        if (dominance > 0) return b;

        log.info("🔍 Individuals are non-dominated, using crowding distance");
        double aDist = ElitismScoreCalculator.get(a);
        double bDist = ElitismScoreCalculator.get(b);

        return aDist >= bDist ? a : b;
    }

    private int compareDominance(Individual a, Individual b) {
        boolean aBetter = false;
        boolean bBetter = false;

        for (ObjectiveName obj : a.getObjectives().keySet()) {
            Double aVal = a.getObjective(obj);
            Double bVal = b.getObjective(obj);

            if (aVal == null || bVal == null) continue;

            if (aVal < bVal) aBetter = true;
            else if (aVal > bVal) bBetter = true;
        }

        if (aBetter && !bBetter) return -1;
        if (!aBetter && bBetter) return 1;
        return 0;
    }
}
