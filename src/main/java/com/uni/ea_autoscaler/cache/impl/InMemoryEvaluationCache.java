package com.uni.ea_autoscaler.cache.impl;

import com.uni.ea_autoscaler.cache.EvaluationCache;
import com.uni.ea_autoscaler.cache.ScalingKey;
import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryEvaluationCache implements EvaluationCache {

    private final Map<ScalingKey, ScalingConfiguration> cache = new ConcurrentHashMap<>();

    @Override
    public ScalingConfiguration getConfiguration(ScalingKey scalingKey) {
        return cache.get(scalingKey);
    }

    @Override
    public void storeConfiguration(ScalingKey scalingKey, ScalingConfiguration configuration) {
        cache.put(scalingKey, configuration);
    }
}
