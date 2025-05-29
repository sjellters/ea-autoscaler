package com.uni.ea_autoscaler.cache.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.ea_autoscaler.cache.EvaluationCache;
import com.uni.ea_autoscaler.cache.ScalingKey;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class RedisEvaluationCache implements EvaluationCache {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisEvaluationCache(RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public double[] getObjectives(ScalingKey scalingKey) {
        try {
            String json = redisTemplate.opsForValue().get(scalingKey.toString());
            return json != null ? objectMapper.readValue(json, new TypeReference<>() {
            }) : null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void storeObjectives(ScalingKey scalingKey, double[] objectives) {
        try {
            redisTemplate.opsForValue().set(scalingKey.toString(), objectMapper.writeValueAsString(objectives), 24, TimeUnit.HOURS);
        } catch (Exception ignored) {
        }
    }
}
