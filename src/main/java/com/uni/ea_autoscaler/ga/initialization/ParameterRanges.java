package com.uni.ea_autoscaler.ga.initialization;

public class ParameterRanges {

    public static final int MIN_REPLICAS_MIN = 2;
    public static final int MIN_REPLICAS_MAX = 7;

    public static final int MAX_REPLICAS = 8;

    public static final double[] THRESHOLDS = {
            0.3, 0.4, 0.5, 0.6, 0.7
    };

    public static final int[] STABILIZATION_WINDOW = {
            10, 15
    };

    public static final int[] CPU_REQUEST_TIERS = {
            100, 150, 200, 250, 300, 350, 400, 450, 500
    };

    public static final int[] MEMORY_REQUEST_TIERS = {
            256, 384, 512, 576, 640, 768, 896, 1024
    };
}
