package com.uni.ea_autoscaler.cache.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.ea_autoscaler.cache.ScalingKey;
import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisEvaluationCache {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public ScalingConfiguration getConfiguration(ScalingKey scalingKey) {
        try {
            String json = redisTemplate.opsForValue().get(scalingKey.toString());
            return json != null ? objectMapper.readValue(json, ScalingConfiguration.class) : null;
        } catch (Exception e) {
            log.error("❌ Failed to read ScalingConfiguration from Redis: {}", e.getMessage());
            return null;
        }
    }

    public void storeConfiguration(ScalingKey scalingKey, ScalingConfiguration config) {
        try {
            redisTemplate.opsForValue().set(scalingKey.toString(), objectMapper.writeValueAsString(config));
        } catch (Exception e) {
            log.error("❌ Failed to write ScalingConfiguration to Redis: {}", e.getMessage());
        }
    }
}
