package com.uni.ea_autoscaler.ga.evaluation.penalty;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;

public interface PenaltyStrategy {

    void applyPenalty(ScalingConfiguration individual, PenaltyReason reason);
}
