package com.uni.ea_autoscaler.ga.nsga3;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class EvolutionHistory {

    private final List<ScalingConfiguration> bestPerGeneration = new ArrayList<>();
    private final Comparator<ScalingConfiguration> comparator;

    public EvolutionHistory() {
        this.comparator = Comparator.comparingDouble(ind -> ind.getObjectives()[0]);
    }

    public void recordGeneration(List<ScalingConfiguration> population) {
        population.stream()
                .min(comparator)
                .map(ScalingConfiguration::copy)
                .ifPresent(bestPerGeneration::add);
    }

    public List<ScalingConfiguration> getBestIndividuals() {
        return List.copyOf(bestPerGeneration);
    }

    public void logSummary() {
        log.info("📈 Evolution Summary (Best per Generation):");
        for (int i = 0; i < bestPerGeneration.size(); i++) {
            ScalingConfiguration sc = bestPerGeneration.get(i);
            log.info("🔹 Generation {}: {} → Objectives: {}", i + 1, sc.toString(), formatObjectives(sc.getObjectives()));
        }
    }

    private String formatObjectives(double[] objectives) {
        return Arrays.stream(objectives)
                .mapToObj(v -> String.format("%.4f", v))
                .collect(Collectors.joining(", ", "[", "]"));
    }
}
