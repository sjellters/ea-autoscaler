package com.uni.ea_autoscaler.ga.util;

import com.uni.ea_autoscaler.ga.model.ScalingParameterRanges;

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

    public static int pickNearestTier(int a, int b, int[] tiers) {
        int midpoint = (a + b) / 2;
        int best = tiers[0];
        int minDiff = Math.abs(midpoint - best);
        for (int tier : tiers) {
            int diff = Math.abs(midpoint - tier);
            if (diff < minDiff) {
                best = tier;
                minDiff = diff;
            }
        }
        return best;
    }
}
