package com.uni.ea_autoscaler.ga.nsga3;

import com.uni.ea_autoscaler.ga.model.Individual;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ParetoFrontsCalculator {

    public List<List<Individual>> calculateFronts(List<Individual> population) {
        List<List<Individual>> fronts = new ArrayList<>();
        Map<Individual, Integer> dominationCounts = new HashMap<>();
        Map<Individual, List<Individual>> dominatedBy = new HashMap<>();

        List<Individual> firstFront = new ArrayList<>();

        if (population == null || population.isEmpty()) {
            log.warn("⚠️ Population is empty, returning no fronts.");
            return List.of();
        }

        for (Individual p : population) {
            List<Individual> dominates = new ArrayList<>();
            int count = 0;

            for (Individual q : population) {
                if (p == q) continue;
                if (dominates(p, q)) {
                    dominates.add(q);
                } else if (dominates(q, p)) {
                    count++;
                }
            }

            dominatedBy.put(p, dominates);
            dominationCounts.put(p, count);

            if (count == 0) {
                firstFront.add(p);
            }
        }

        fronts.add(firstFront);
        log.info("🥇 Front 1: {} individuals", firstFront.size());

        int i = 0;
        while (i < fronts.size()) {
            List<Individual> nextFront = new ArrayList<>();
            for (Individual p : fronts.get(i)) {
                for (Individual q : dominatedBy.getOrDefault(p, List.of())) {
                    int newCount = dominationCounts.get(q) - 1;
                    dominationCounts.put(q, newCount);
                    if (newCount == 0) {
                        nextFront.add(q);
                    }
                }
            }
            if (!nextFront.isEmpty()) {
                log.info("🥈 Front {}: {} individuals", i + 2, nextFront.size());
                fronts.add(nextFront);
            }
            i++;
        }

        log.info("✅ Total Pareto fronts calculated: {}", fronts.size());

        return fronts;
    }

    private boolean dominates(Individual a, Individual b) {
        double[] aObjectives = a.getNormalizedObjectives();
        double[] bObjectives = b.getNormalizedObjectives();

        boolean betterInAtLeastOne = false;

        for (int i = 0; i < aObjectives.length; i++) {
            double aVal = round(aObjectives[i]);
            double bVal = round(bObjectives[i]);

            if (aVal > bVal) return false;
            if (aVal < bVal) betterInAtLeastOne = true;
        }

        return betterInAtLeastOne;
    }

    private double round(double value) {
        double factor = Math.pow(10, 4);
        return Math.round(value * factor) / factor;
    }
}
