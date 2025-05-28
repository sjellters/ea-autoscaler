package com.uni.ea_autoscaler.ga.nsga3.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class EvolutionConfigValidator {

    public void validate(int populationSize, int maxGenerations, double mutationRate) {
        List<String> errors = new ArrayList<>();

        if (populationSize < 3) errors.add("populationSize must be >= 3");
        if (maxGenerations < 1) errors.add("maxGenerations must be >= 1");
        if (mutationRate < 0.0 || mutationRate > 1.0) errors.add("mutationRate must be between 0.0 and 1.0");

        if (!errors.isEmpty()) {
            log.error("❌ Invalid evolution configuration detected:");
            errors.forEach(err -> log.error(" - {}", err));
            throw new IllegalStateException("Invalid evolution engine configuration. See logs for details.");
        }
    }
}
