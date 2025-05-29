package com.uni.ea_autoscaler.cache.impl;

import com.uni.ea_autoscaler.cache.EvaluationCache;
import com.uni.ea_autoscaler.cache.ScalingKey;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryEvaluationCache implements EvaluationCache {

    private final Map<ScalingKey, double[]> cache = new ConcurrentHashMap<>();

    @Override
    public double[] getObjectives(ScalingKey scalingKey) {
        return cache.get(scalingKey);
    }

    @Override
    public void storeObjectives(ScalingKey scalingKey, double[] objectives) {
        cache.put(scalingKey, objectives);
    }
}
