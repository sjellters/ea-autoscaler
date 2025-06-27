package com.uni.ea_autoscaler.ga.evaluation;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.ga.util.ObjectiveNormalizer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ObjectiveNormalizerTest {

    @Test
    void testNormalizationExcludesPenalizedValues() {
        ScalingConfiguration ind1 = new ScalingConfiguration();
        ind1.setObjectives(new double[]{100, 0.5, 256});

        ScalingConfiguration ind2 = new ScalingConfiguration();
        ind2.setObjectives(new double[]{200, 0.7, 512});

        ScalingConfiguration ind3 = new ScalingConfiguration();
        ind3.setObjectives(new double[]{150, 0.3, 384});

        ScalingConfiguration penalized = new ScalingConfiguration();
        penalized.setObjectives(new double[]{Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE});

        ObjectiveNormalizer normalizer = new ObjectiveNormalizer();
        normalizer.normalize(List.of(ind1, ind2, ind3, penalized));

        double[] norm1 = ind1.getNormalizedObjectives();
        double[] norm2 = ind2.getNormalizedObjectives();
        double[] norm3 = ind3.getNormalizedObjectives();
        double[] normP = penalized.getNormalizedObjectives();

        assertArrayEquals(new double[]{0.0, 0.5, 0.0}, norm1, 1e-6);
        assertArrayEquals(new double[]{1.0, 1.0, 1.0}, norm2, 1e-6);
        assertArrayEquals(new double[]{0.5, 0.0, 0.5}, norm3, 1e-6);
        assertArrayEquals(new double[]{1.0, 1.0, 1.0}, normP, 1e-6);
    }
}