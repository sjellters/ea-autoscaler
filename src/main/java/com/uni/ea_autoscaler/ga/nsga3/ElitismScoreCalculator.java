package com.uni.ea_autoscaler.ga.nsga3;

import com.uni.ea_autoscaler.core.enums.ObjectiveName;
import com.uni.ea_autoscaler.ga.model.Individual;

public class ElitismScoreCalculator {

    public static double get(Individual ind) {
        if (ind.isEvaluationFailed()) return Double.POSITIVE_INFINITY;

        Double slaViolation = ind.getRawObjectives().get(ObjectiveName.SLA_VIOLATION);
        Double provisionedCpu = ind.getRawObjectives().get(ObjectiveName.PROVISIONED_CPU);
        Double provisionedMemory = ind.getRawObjectives().get(ObjectiveName.PROVISIONED_MEMORY);

        if (slaViolation == null || provisionedCpu == null || provisionedMemory == null) {
            return Double.POSITIVE_INFINITY;
        }

        double cost = provisionedCpu + 4.0 * provisionedMemory;
        return 45.0 * slaViolation + cost;
    }
}
