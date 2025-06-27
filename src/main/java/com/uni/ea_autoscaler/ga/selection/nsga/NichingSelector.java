package com.uni.ea_autoscaler.ga.selection.nsga;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class NichingSelector {

    public List<ScalingConfiguration> selectWithNiching(
            List<ScalingConfiguration> lastFront,
            List<double[]> referencePoints,
            int remainingSlots
    ) {
        Map<double[], List<ScalingConfiguration>> nicheMap = new HashMap<>();

        for (ScalingConfiguration individual : lastFront) {
            double[] normalized = individual.getNormalizedObjectives();
            double[] closestPoint = referencePoints.stream()
                    .min(Comparator.comparingDouble(rp -> perpendicularDistance(normalized, rp)))
                    .orElseThrow();

            nicheMap.computeIfAbsent(closestPoint, k -> new ArrayList<>()).add(individual);
        }

        List<ScalingConfiguration> selected = new ArrayList<>();
        Set<ScalingConfiguration> remaining = new HashSet<>(lastFront);
        Set<double[]> usedPoints = new HashSet<>();

        while (selected.size() < remainingSlots && !remaining.isEmpty()) {
            List<Map.Entry<double[], List<ScalingConfiguration>>> niches = nicheMap.entrySet().stream()
                    .filter(e -> !e.getValue().isEmpty())
                    .sorted(Comparator.comparingInt(e -> usedPoints.contains(e.getKey()) ? 1 : 0))
                    .toList();

            for (Map.Entry<double[], List<ScalingConfiguration>> niche : niches) {
                List<ScalingConfiguration> nicheMembers = niche.getValue();
                ScalingConfiguration chosen = nicheMembers.get(new Random().nextInt(nicheMembers.size()));
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
