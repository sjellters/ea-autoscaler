package com.uni.ea_autoscaler.old.ga.evaluation.penalty;

import com.uni.ea_autoscaler.old.ga.model.ScalingConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultPenaltyStrategy implements PenaltyStrategy {

    @Override
    public void applyPenalty(ScalingConfiguration individual, PenaltyReason reason) {
        double[] objectives = new double[6];

        log.warn("🚫 Applying penalty to individual due to {}:\n{}", reason, individual);

        switch (reason) {
            case CONFIGURATION_FAILURE:
            case POD_TIMEOUT:
            case JMETER_FAILURE:
                objectives[0] = Double.MAX_VALUE; // Response Time
                objectives[4] = 1.0;              // Error Rate
                objectives[5] = Double.MAX_VALUE; // Latency
                break;
            case METRIC_FAILURE:
                objectives[1] = -1.0; // CPU
                objectives[2] = -1.0; // Memory
                objectives[3] = -1.0; // Replicas
                break;
        }

        individual.setObjectives(objectives);
    }
}
