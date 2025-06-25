package com.uni.ea_autoscaler.old.ga.evaluation.penalty;

import com.uni.ea_autoscaler.old.ga.model.ScalingConfiguration;

public interface PenaltyStrategy {

    void applyPenalty(ScalingConfiguration individual, PenaltyReason reason);
}
