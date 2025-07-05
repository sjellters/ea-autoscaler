package com.uni.ea_autoscaler.ga.evaluation;

import com.uni.ea_autoscaler.core.enums.ObjectiveName;
import com.uni.ea_autoscaler.ga.model.Individual;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class ObjectiveNormalizer {

    public void normalize(List<Individual> population) {
        if (population.isEmpty()) {
            log.warn("⚠️ Empty population. Skipping normalization.");
            return;
        }

        List<ObjectiveName> objectiveNames = new ArrayList<>(population.get(0).getObjectives().keySet());
        int numObjectives = objectiveNames.size();
        Map<ObjectiveName, Double> mins = new HashMap<>();
        Map<ObjectiveName, Double> maxs = new HashMap<>();

        for (ObjectiveName obj : objectiveNames) {
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;

            for (Individual ind : population) {
                double value = ind.getObjective(obj);
                if (Double.isNaN(value)) continue;
                min = Math.min(min, value);
                max = Math.max(max, value);
            }

            mins.put(obj, min);
            maxs.put(obj, max);
        }

        for (Individual ind : population) {
            double[] normalized = new double[numObjectives];
            for (int i = 0; i < numObjectives; i++) {
                ObjectiveName obj = objectiveNames.get(i);
                double raw = ind.getObjective(obj);
                double min = mins.get(obj);
                double max = maxs.get(obj);

                if (Double.compare(max, min) == 0) {
                    normalized[i] = 0.0;
                } else {
                    normalized[i] = (raw - min) / (max - min);
                }
            }
            ind.setNormalizedObjectives(normalized);
        }

        log.info("📐 Normalization complete for {} individuals", population.size());
    }
}
