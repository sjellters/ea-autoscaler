package com.uni.ea_autoscaler.ga.model;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Component
public class ScalingConfigurationValidator {

    public boolean isValid(ScalingConfiguration config) {
        boolean valid = true;

        if (!isThresholdValid(config)) {
            log.warn("Invalid thresholds: cpuThreshold={}, memoryThreshold={}",
                    config.getCpuThreshold(), config.getMemoryThreshold());
            valid = false;
        }

        if (!isReplicaRangeValid(config)) {
            log.warn("Invalid replica range: minReplicas={}, maxReplicas={}",
                    config.getMinReplicas(), config.getMaxReplicas());
            valid = false;
        }

        if (!isCooldownValid(config)) {
            log.warn("Invalid cooldownSeconds: {}", config.getCooldownSeconds());
            valid = false;
        }

        if (!isCpuValid(config)) {
            log.warn("Invalid cpuRequest: {}", config.getCpuRequest());
            valid = false;
        }

        if (!isMemoryValid(config)) {
            log.warn("Invalid memoryRequest: {}", config.getMemoryRequest());
            valid = false;
        }

        return valid;
    }

    private boolean isThresholdValid(ScalingConfiguration config) {
        return config.getCpuThreshold() >= 0.1 && config.getCpuThreshold() <= 0.9 &&
                config.getMemoryThreshold() >= 0.1 && config.getMemoryThreshold() <= 0.9;
    }

    private boolean isReplicaRangeValid(ScalingConfiguration config) {
        return config.getMinReplicas() >= 1 &&
                config.getMaxReplicas() > config.getMinReplicas() &&
                config.getMaxReplicas() <= 10;
    }

    private boolean isCooldownValid(ScalingConfiguration config) {
        return config.getCooldownSeconds() >= ScalingParameterRanges.COOLDOWN_MIN &&
                config.getCooldownSeconds() <= ScalingParameterRanges.COOLDOWN_MAX;
    }

    private boolean isCpuValid(ScalingConfiguration config) {
        return Arrays.stream(ScalingParameterRanges.CPU_REQUEST_TIERS)
                .anyMatch(tier -> tier == config.getCpuRequest());
    }

    private boolean isMemoryValid(ScalingConfiguration config) {
        return Arrays.stream(ScalingParameterRanges.MEMORY_REQUEST_TIERS)
                .anyMatch(tier -> tier == config.getMemoryRequest());
    }
}
