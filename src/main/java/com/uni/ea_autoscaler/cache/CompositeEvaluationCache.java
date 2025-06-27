package com.uni.ea_autoscaler.cache;

import com.uni.ea_autoscaler.cache.impl.InMemoryEvaluationCache;
import com.uni.ea_autoscaler.cache.impl.RedisEvaluationCache;
import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import org.springframework.stereotype.Component;

@Component
public class CompositeEvaluationCache implements EvaluationCache {

    private final InMemoryEvaluationCache inMemory;
    private final RedisEvaluationCache redis;

    public CompositeEvaluationCache(InMemoryEvaluationCache inMemory, RedisEvaluationCache redis) {
        this.inMemory = inMemory;
        this.redis = redis;
    }

    @Override
    public ScalingConfiguration getConfiguration(ScalingKey scalingKey) {
        ScalingConfiguration cached = inMemory.getConfiguration(scalingKey);
        if (cached != null) return cached;

        cached = redis.getConfiguration(scalingKey);

        if (cached != null) {
            inMemory.storeConfiguration(scalingKey, cached);
        }

        return cached;
    }

    @Override
    public void storeConfiguration(ScalingKey scalingKey, ScalingConfiguration configuration) {
        inMemory.storeConfiguration(scalingKey, configuration);
        redis.storeConfiguration(scalingKey, configuration);
    }
}
