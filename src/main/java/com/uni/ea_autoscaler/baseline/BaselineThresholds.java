package com.uni.ea_autoscaler.baseline;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BaselineThresholds {

    private static final BaselineThresholds INSTANCE = new BaselineThresholds();

    private Double p95Threshold;
    private Double baselineAvgCpu;
    private Double baselineAvgMemory;

    @Setter(value = AccessLevel.NONE)
    private Double baselineAvgReplicas = 3.0;

    private BaselineThresholds() {
    }

    public static BaselineThresholds getInstance() {
        return INSTANCE;
    }
}

