package com.uni.ea_autoscaler.cache;

import com.uni.ea_autoscaler.cache.impl.InMemoryEvaluationCache;
import com.uni.ea_autoscaler.cache.impl.RedisEvaluationCache;
import org.springframework.stereotype.Component;

@Component
public class CompositeEvaluationCache implements EvaluationCache {

    private final EvaluationCache inMemory;
    private final EvaluationCache redis;

    public CompositeEvaluationCache(InMemoryEvaluationCache inMemory, RedisEvaluationCache redis) {
        this.inMemory = inMemory;
        this.redis = redis;
    }

    @Override
    public double[] getObjectives(ScalingKey scalingKey) {
        double[] cached = inMemory.getObjectives(scalingKey);
        if (cached != null) return cached;

        cached = redis.getObjectives(scalingKey);

        if (cached != null) {
            inMemory.storeObjectives(scalingKey, cached);
        }

        return cached;
    }

    @Override
    public void storeObjectives(ScalingKey scalingKey, double[] objectives) {
        inMemory.storeObjectives(scalingKey, objectives);
        redis.storeObjectives(scalingKey, objectives);
    }
}
