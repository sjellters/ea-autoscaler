package com.uni.ea_autoscaler.ga.evaluation;

import com.uni.ea_autoscaler.ga.model.Individual;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EvaluationCache {
    private static final EvaluationCache INSTANCE = new EvaluationCache();

    private final Map<String, Individual> cache = new ConcurrentHashMap<>();

    private EvaluationCache() {
    }

    public static EvaluationCache getInstance() {
        return INSTANCE;
    }

    public boolean contains(String key) {
        return cache.containsKey(key);
    }

    public void store(String key, Individual ind) {
        cache.putIfAbsent(key, ind.copy());
    }

    public Individual get(String key) {
        Individual cached = cache.get(key);
        if (cached == null) return null;

        Individual copy = cached.copy();
        copy.setNormalizedObjectives(null);
        return copy;
    }
}
