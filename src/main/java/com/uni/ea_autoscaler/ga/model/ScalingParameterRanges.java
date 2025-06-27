package com.uni.ea_autoscaler.ga.model;

public class ScalingParameterRanges {

    public static final int MIN_REPLICAS_MIN = 1;
    public static final int MIN_REPLICAS_MAX = 4;

    public static final int MAX_REPLICAS_MIN_OFFSET = 1;
    public static final int MAX_REPLICAS_MAX = 6;

    public static final double[] THRESHOLDS = {
            0.3, 0.4, 0.5, 0.6, 0.7, 0.8
    };

    public static final int COOLDOWN_MIN = 10;
    public static final int COOLDOWN_MAX = 20;
    public static final int COOLDOWN_STEP = 5;

    public static final int[] CPU_REQUEST_TIERS = {
            100, 150, 200, 250, 300, 350, 400, 450, 500
    };

    public static final int[] MEMORY_REQUEST_TIERS = {
            256, 384, 512, 576, 640, 768, 896, 1024
    };

    public static int discretizeCooldown(int value) {
        return roundToStep(value);
    }

    private static int roundToStep(int value) {
        return Math.round((float) value / ScalingParameterRanges.COOLDOWN_STEP) * ScalingParameterRanges.COOLDOWN_STEP;
    }
}

