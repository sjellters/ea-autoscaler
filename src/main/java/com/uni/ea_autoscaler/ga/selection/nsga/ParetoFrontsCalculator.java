package com.uni.ea_autoscaler.ga.selection.nsga;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ParetoFrontsCalculator {

    public List<List<ScalingConfiguration>> calculateFronts(List<ScalingConfiguration> population) {
        List<List<ScalingConfiguration>> fronts = new ArrayList<>();
        Map<ScalingConfiguration, Integer> dominationCounts = new HashMap<>();
        Map<ScalingConfiguration, List<ScalingConfiguration>> dominatedBy = new HashMap<>();

        List<ScalingConfiguration> firstFront = new ArrayList<>();

        if (population == null || population.isEmpty()) {
            log.warn("⚠️ Population is empty, returning no fronts.");
            return new ArrayList<>();
        }

        for (ScalingConfiguration p : population) {
            List<ScalingConfiguration> dominates = new ArrayList<>();
            int count = 0;

            for (ScalingConfiguration q : population) {
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
            List<ScalingConfiguration> nextFront = new ArrayList<>();
            for (ScalingConfiguration p : fronts.get(i)) {
                for (ScalingConfiguration q : dominatedBy.get(p)) {
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

    private boolean dominates(ScalingConfiguration a, ScalingConfiguration b) {
        double[] aObjectives = a.getNormalizedObjectives();
        double[] bObjectives = b.getNormalizedObjectives();

        boolean betterInAtLeastOne = false;

        for (int i = 0; i < aObjectives.length; i++) {
            double aVal = aObjectives[i];
            double bVal = bObjectives[i];

            if (aVal > bVal) return false;
            if (aVal < bVal) betterInAtLeastOne = true;
        }

        return betterInAtLeastOne;
    }
}
