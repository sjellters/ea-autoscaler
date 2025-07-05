package com.uni.ea_autoscaler.ga.nsga3;

import com.uni.ea_autoscaler.ga.model.Individual;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
public class NextGenerationSelector {

    private final ParetoFrontsCalculator paretoFrontsCalculator;
    private final ReferencePointsGenerator referencePointsGenerator;
    private final NichingSelector nichingSelector;

    public NextGenerationSelector(ParetoFrontsCalculator paretoFrontsCalculator,
                                  ReferencePointsGenerator referencePointsGenerator,
                                  NichingSelector nichingSelector) {
        this.paretoFrontsCalculator = paretoFrontsCalculator;
        this.referencePointsGenerator = referencePointsGenerator;
        this.nichingSelector = nichingSelector;
    }

    private int calculateMinimumDivisions(int numObjectives, int populationSize) {
        int h = 1;
        int maxReferencePoints = 2 * populationSize;

        while (true) {
            long refPoints = combinations(h + numObjectives - 1, numObjectives - 1);
            if (refPoints >= populationSize && refPoints <= maxReferencePoints) {
                return h;
            }
            if (refPoints > maxReferencePoints) {
                return h - 1 > 0 ? h - 1 : 1;
            }
            h++;
        }
    }

    private long combinations(int n, int r) {
        if (r > n) return 0;
        long result = 1;
        for (int i = 1; i <= r; i++) {
            result = result * (n - r + i) / i;
        }
        return result;
    }

    public List<Individual> selectNextGeneration(List<Individual> parents,
                                                 List<Individual> offspring,
                                                 int populationSize) {
        int numberOfObjectives = parents.get(0).getNormalizedObjectives().length;
        int divisions = calculateMinimumDivisions(numberOfObjectives, populationSize);
        log.info("🔢 Calculated minimum divisions: {} for {} objectives and population size {}", divisions, numberOfObjectives, populationSize);

        List<Individual> combined = new ArrayList<>(parents);
        combined.addAll(offspring);

        List<List<Individual>> fronts = paretoFrontsCalculator.calculateFronts(combined);

        List<Individual> nextGen = new ArrayList<>();

        int i = 0;
        while (i < fronts.size() && nextGen.size() + fronts.get(i).size() <= populationSize) {
            nextGen.addAll(fronts.get(i));
            i++;
        }

        if (nextGen.size() < populationSize && i < fronts.size()) {
            int remaining = populationSize - nextGen.size();
            List<Individual> lastFront = fronts.get(i);

            if (lastFront.isEmpty()) {
                log.warn("⚠️ Last front is empty, skipping niching.");
            } else {
                if (lastFront.stream().anyMatch(ind -> ind.getNormalizedObjectives() == null)) {
                    log.warn("⚠️ Some individuals in last front are not normalized.");
                }

                int numObjectives = lastFront.get(0).getNormalizedObjectives().length;
                List<double[]> referencePoints = referencePointsGenerator.generate(numObjectives, divisions);

                List<Individual> selected = nichingSelector.selectWithNiching(lastFront, referencePoints, remaining);
                nextGen.addAll(selected);
            }
        }

        enforceElitism(combined, nextGen);

        log.info("🌱 Next generation selected with {} individuals", nextGen.size());
        return nextGen;
    }

    private void enforceElitism(List<Individual> pool, List<Individual> nextGen) {
        Individual best = pool.stream()
                .filter(ind -> !ind.isEvaluationFailed())
                .min(Comparator.comparingDouble(ElitismScoreCalculator::get))
                .orElse(null);

        if (best != null && !nextGen.contains(best)) {
            Individual worst = nextGen.stream()
                    .max(Comparator.comparingDouble(ElitismScoreCalculator::get))
                    .orElse(null);

            if (worst != null) {
                nextGen.remove(worst);
                nextGen.add(best);
                log.info("🌟 Elitism applied: best individual inserted into next generation.");
            }
        }
    }
}
