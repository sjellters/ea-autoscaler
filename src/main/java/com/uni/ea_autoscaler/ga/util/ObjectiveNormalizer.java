package com.uni.ea_autoscaler.ga.util;

import com.uni.ea_autoscaler.cache.ScalingKey;
import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class ObjectiveNormalizer {

    public void normalize(List<ScalingConfiguration> population) {
        if (population == null || population.isEmpty()) return;

        int numObjectives = population.get(0).getObjectives().length;
        double[] min = computeMin(population, numObjectives);
        double[] max = computeMax(population, numObjectives);

        for (ScalingConfiguration ind : population) {
            double[] normalized = normalizeObjectives(ind.getObjectives(), min, max);
            ScalingKey scalingKey = new ScalingKey(ind);
            log.debug("Normalized objectives for configuration {} -> {}", scalingKey, Arrays.toString(normalized));
            ind.setNormalizedObjectives(normalized);
        }
    }

    private double[] computeMin(List<ScalingConfiguration> population, int numObjectives) {
        double[] min = new double[numObjectives];
        Arrays.fill(min, Double.POSITIVE_INFINITY);

        for (ScalingConfiguration ind : population) {
            double[] obj = ind.getObjectives();
            for (int i = 0; i < numObjectives; i++) {
                if (obj[i] != Double.MAX_VALUE) {
                    min[i] = Math.min(min[i], obj[i]);
                }
            }
        }

        log.debug("🔽 Min values (excluding penalties): {}", Arrays.toString(min));
        return min;
    }

    private double[] computeMax(List<ScalingConfiguration> population, int numObjectives) {
        double[] max = new double[numObjectives];
        Arrays.fill(max, Double.NEGATIVE_INFINITY);

        for (ScalingConfiguration ind : population) {
            double[] obj = ind.getObjectives();
            for (int i = 0; i < numObjectives; i++) {
                if (obj[i] != Double.MAX_VALUE) {
                    max[i] = Math.max(max[i], obj[i]);
                }
            }
        }

        log.debug("🔼 Max values (excluding penalties): {}", Arrays.toString(max));
        return max;
    }

    private double[] normalizeObjectives(double[] values, double[] min, double[] max) {
        double[] norm = new double[values.length];

        for (int i = 0; i < values.length; i++) {
            if (values[i] == Double.MAX_VALUE) {
                norm[i] = 1.0;
            } else {
                double range = max[i] - min[i];
                norm[i] = (range == 0) ? 0.0 : (values[i] - min[i]) / range;
            }
        }

        return norm;
    }
}