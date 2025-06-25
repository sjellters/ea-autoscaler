package com.uni.ea_autoscaler.old.ga.util;

import com.uni.ea_autoscaler.old.ga.model.ScalingParameterRanges;

public class ThresholdUtils {

    public static double pickNearest(double a, double b) {
        double avg = (a + b) / 2.0;
        double[] thresholds = ScalingParameterRanges.THRESHOLDS;
        double closest = thresholds[0];
        double minDiff = Math.abs(thresholds[0] - avg);

        for (int i = 1; i < thresholds.length; i++) {
            double diff = Math.abs(thresholds[i] - avg);
            if (diff < minDiff) {
                minDiff = diff;
                closest = thresholds[i];
            }
        }

        return closest;
    }
}
