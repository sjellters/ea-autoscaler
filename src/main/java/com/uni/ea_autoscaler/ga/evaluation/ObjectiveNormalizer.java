package com.uni.ea_autoscaler.ga.evaluation;

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
            ind.setNormalizedObjectives(normalized);
        }
    }

    private double[] computeMin(List<ScalingConfiguration> population, int numObjectives) {
        double[] min = new double[numObjectives];
        System.arraycopy(population.get(0).getObjectives(), 0, min, 0, numObjectives);

        for (ScalingConfiguration ind : population) {
            double[] obj = ind.getObjectives();
            for (int i = 0; i < numObjectives; i++) {
                min[i] = Math.min(min[i], obj[i]);
            }
        }

        log.debug("🔽 Min values: {}", Arrays.toString(min));
        return min;
    }

    private double[] computeMax(List<ScalingConfiguration> population, int numObjectives) {
        double[] max = new double[numObjectives];
        System.arraycopy(population.get(0).getObjectives(), 0, max, 0, numObjectives);

        for (ScalingConfiguration ind : population) {
            double[] obj = ind.getObjectives();
            for (int i = 0; i < numObjectives; i++) {
                max[i] = Math.max(max[i], obj[i]);
            }
        }

        log.debug("🔼 Max values: {}", Arrays.toString(max));
        return max;
    }

    private double[] normalizeObjectives(double[] values, double[] min, double[] max) {
        double[] norm = new double[values.length];

        for (int i = 0; i < values.length; i++) {
            double range = max[i] - min[i];
            norm[i] = (range == 0) ? 0.0 : (values[i] - min[i]) / range;
        }

        return norm;
    }
}