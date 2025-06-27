package com.uni.ea_autoscaler.cache;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;

public interface EvaluationCache {

    ScalingConfiguration getConfiguration(ScalingKey scalingKey);

    void storeConfiguration(ScalingKey scalingKey, ScalingConfiguration configuration);
}
