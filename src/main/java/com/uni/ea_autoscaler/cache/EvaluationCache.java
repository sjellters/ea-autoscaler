package com.uni.ea_autoscaler.cache;

public interface EvaluationCache {

    double[] getObjectives(ScalingKey scalingKey);

    void storeObjectives(ScalingKey scalingKey, double[] objectives);
}
