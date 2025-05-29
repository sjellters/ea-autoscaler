package com.uni.ea_autoscaler.ga.selection.nsga;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.ga.selection.NextGenerationSelector;
import com.uni.ea_autoscaler.ga.selection.nsga.reference.ReferencePointGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component("nsga3")
@RequiredArgsConstructor
public class NSGAIIISelectionStrategy implements NextGenerationSelector {

    private final ParetoFrontsCalculator paretoFrontsCalculator;
    private final ReferencePointGenerator referencePointGenerator;
    private final NichingSelector nichingSelector;

    @Value("${nsga3.referencePointDivisions:12}")
    private int divisions;

    @Override
    public List<ScalingConfiguration> selectNextGeneration(List<ScalingConfiguration> parents,
                                                           List<ScalingConfiguration> offspring,
                                                           int populationSize) {

        List<ScalingConfiguration> combined = new ArrayList<>(parents);
        combined.addAll(offspring);

        List<List<ScalingConfiguration>> fronts = paretoFrontsCalculator.calculateFronts(combined);
        log.info("✅ Total Pareto fronts calculated: {}", fronts.size());

        List<ScalingConfiguration> nextGen = new ArrayList<>();

        int i = 0;
        while (i < fronts.size() && nextGen.size() + fronts.get(i).size() <= populationSize) {
            nextGen.addAll(fronts.get(i));
            i++;
        }

        if (nextGen.size() < populationSize && i < fronts.size()) {
            int remaining = populationSize - nextGen.size();
            List<ScalingConfiguration> lastFront = fronts.get(i);

            if (lastFront.isEmpty()) {
                log.warn("⚠️ Last front is empty, skipping niching.");
            } else {
                int numObjectives = lastFront.get(0).getNormalizedObjectives().length;
                List<double[]> referencePoints = referencePointGenerator.generate(numObjectives, divisions);
                log.info("🎯 Generated {} reference points for {} objectives ({} divisions)", referencePoints.size(), numObjectives, divisions);

                List<ScalingConfiguration> selected = nichingSelector.selectFromLastFront(lastFront, referencePoints, remaining);
                log.info("🌐 Niching selected {} individuals from last front", selected.size());
                nextGen.addAll(selected);
            }
        }

        log.info("🌱 Next generation selected with {} individuals", nextGen.size());
        return nextGen;
    }
}
