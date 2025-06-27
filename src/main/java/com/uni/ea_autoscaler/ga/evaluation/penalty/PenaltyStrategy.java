package com.uni.ea_autoscaler.ga.evaluation.penalty;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;

public interface PenaltyStrategy {

    void applyDiscardPenalty(ScalingConfiguration individual);

    void applyPenalties(ScalingConfiguration individual, Boolean applyDefaultPenalties);
}
