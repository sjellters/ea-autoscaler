package com.uni.ea_autoscaler.ga.model;

public class ScalingParameterRanges {

    public static final int MIN_REPLICAS_MIN = 1;
    public static final int MIN_REPLICAS_MAX = 4;

    public static final int MAX_REPLICAS_MIN_OFFSET = 1;
    public static final int MAX_REPLICAS_MAX = 8;

    public static final double[] THRESHOLDS = {
            0.3, 0.4, 0.5, 0.6, 0.7, 0.8
    };

    public static final int COOLDOWN_MIN = 10;
    public static final int COOLDOWN_MAX = 60;
    public static final int COOLDOWN_STEP = 5;

    public static final int CPU_REQUEST_MIN = 50;
    public static final int CPU_REQUEST_MAX = 500;
    public static final int CPU_REQUEST_STEP = 50;
    public static final int CPU_REQUEST_LIMIT = 1000;

    public static final int MEMORY_REQUEST_MIN = 128;
    public static final int MEMORY_REQUEST_MAX = 1024;
    public static final int MEMORY_REQUEST_STEP = 128;
    public static final int MEMORY_REQUEST_LIMIT = 1536;

    public static int discretizeCpuRequest(int value) {
        return roundToStep(value, CPU_REQUEST_STEP);
    }

    public static int discretizeMemoryRequest(int value) {
        return roundToStep(value, MEMORY_REQUEST_STEP);
    }

    public static int discretizeCooldown(int value) {
        return roundToStep(value, COOLDOWN_STEP);
    }

    private static int roundToStep(int value, int step) {
        return Math.round((float) value / step) * step;
    }
}
