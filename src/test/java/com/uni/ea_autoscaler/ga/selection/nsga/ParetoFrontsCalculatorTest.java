package com.uni.ea_autoscaler.ga.selection.nsga;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParetoFrontsCalculatorTest {

    @Test
    void testSingleSmallParetoFront() {
        ScalingConfiguration ind1 = new ScalingConfiguration();
        ind1.setNormalizedObjectives(new double[]{0.1, 0.9});

        ScalingConfiguration ind2 = new ScalingConfiguration();
        ind2.setNormalizedObjectives(new double[]{0.2, 0.8});

        ScalingConfiguration ind3 = new ScalingConfiguration();
        ind3.setNormalizedObjectives(new double[]{0.3, 0.7});

        ScalingConfiguration dominated1 = new ScalingConfiguration();
        dominated1.setNormalizedObjectives(new double[]{0.4, 0.9});

        ScalingConfiguration dominated2 = new ScalingConfiguration();
        dominated2.setNormalizedObjectives(new double[]{0.9, 0.9});

        List<ScalingConfiguration> population = List.of(ind1, ind2, ind3, dominated1, dominated2);

        ParetoFrontsCalculator calculator = new ParetoFrontsCalculator();
        List<List<ScalingConfiguration>> fronts = calculator.calculateFronts(population);

        assertEquals(3, fronts.size(), "Expected 3 Pareto fronts");
        assertEquals(3, fronts.get(0).size(), "Expected 3 individuals in first front");
        assertTrue(fronts.get(0).containsAll(List.of(ind1, ind2, ind3)));
        assertTrue(fronts.get(1).contains(dominated1));
        assertTrue(fronts.get(2).contains(dominated2));
    }
}