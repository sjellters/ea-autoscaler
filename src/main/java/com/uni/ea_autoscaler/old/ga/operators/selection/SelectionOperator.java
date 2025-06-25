package com.uni.ea_autoscaler.old.ga.operators.selection;

import com.uni.ea_autoscaler.old.ga.model.ScalingConfiguration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SelectionOperator {

    private final SelectionStrategy selectionStrategy;

    public SelectionOperator(@Qualifier("tournamentSelection") SelectionStrategy selectionStrategy) {
        this.selectionStrategy = selectionStrategy;
    }

    public ScalingConfiguration select(List<ScalingConfiguration> population) {
        return selectionStrategy.select(population);
    }
}
