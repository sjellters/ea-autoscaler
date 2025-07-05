package com.uni.ea_autoscaler.ga.nsga3;

import com.uni.ea_autoscaler.ga.model.Individual;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

@Slf4j
@Component
public class NichingSelector {

    public List<Individual> selectWithNiching(
            List<Individual> lastFront,
            List<double[]> referencePoints,
            int remainingSlots
    ) {
        Map<double[], List<Individual>> nicheMap = new HashMap<>();

        for (Individual individual : lastFront) {
            double[] normalized = individual.getNormalizedObjectives();
            double[] closestPoint = referencePoints.stream()
                    .min(Comparator.comparingDouble(rp -> perpendicularDistance(normalized, rp)))
                    .orElseThrow();

            nicheMap.computeIfAbsent(closestPoint, k -> new ArrayList<>()).add(individual);
        }

        List<Individual> selected = new ArrayList<>();
        Set<Individual> remaining = new HashSet<>(lastFront);
        Set<double[]> usedPoints = new HashSet<>();
        Random random = new Random();

        while (selected.size() < remainingSlots && !remaining.isEmpty()) {
            List<Map.Entry<double[], List<Individual>>> niches = nicheMap.entrySet().stream()
                    .filter(e -> !e.getValue().isEmpty())
                    .sorted(Comparator.comparingInt(e -> usedPoints.contains(e.getKey()) ? 1 : 0))
                    .toList();

            for (Map.Entry<double[], List<Individual>> niche : niches) {
                List<Individual> nicheMembers = niche.getValue();
                Individual chosen = nicheMembers.stream()
                        .min(Comparator.comparingDouble(ElitismScoreCalculator::get))
                        .orElseThrow();
                selected.add(chosen);
                nicheMembers.remove(chosen);
                remaining.remove(chosen);
                usedPoints.add(niche.getKey());
                if (selected.size() == remainingSlots) break;
            }
        }

        log.info("🌐 Niching selected {} individuals from last front", selected.size());
        return selected;
    }

    private double perpendicularDistance(double[] point, double[] direction) {
        double dotProduct = 0;
        double norm = 0;
        for (int i = 0; i < point.length; i++) {
            dotProduct += point[i] * direction[i];
            norm += direction[i] * direction[i];
        }

        double scalar = dotProduct / norm;
        double distanceSquared = 0;
        for (int i = 0; i < point.length; i++) {
            double projection = scalar * direction[i];
            distanceSquared += Math.pow(point[i] - projection, 2);
        }

        return Math.sqrt(distanceSquared);
    }
}
