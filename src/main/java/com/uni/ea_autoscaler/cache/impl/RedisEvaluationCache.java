package com.uni.ea_autoscaler.cache.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.ea_autoscaler.cache.EvaluationCache;
import com.uni.ea_autoscaler.cache.ScalingKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisEvaluationCache implements EvaluationCache {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public double[] getObjectives(ScalingKey scalingKey) {
        try {
            String json = redisTemplate.opsForValue().get(scalingKey.toString());

            return json != null ? objectMapper.readValue(json, new TypeReference<>() {
            }) : null;
        } catch (Exception e) {
            log.error(e.getMessage());

            return null;
        }
    }

    @Override
    public void storeObjectives(ScalingKey scalingKey, double[] objectives) {
        try {
            redisTemplate.opsForValue().set(scalingKey.toString(), objectMapper.writeValueAsString(objectives));
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
