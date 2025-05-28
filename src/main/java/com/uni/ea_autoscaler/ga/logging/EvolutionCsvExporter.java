package com.uni.ea_autoscaler.ga.logging;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class EvolutionCsvExporter {

    private final ScalingCsvWriter scalingCsvWriter;

    public EvolutionCsvExporter(ScalingCsvWriter scalingCsvWriter) {
        this.scalingCsvWriter = scalingCsvWriter;
    }

    public void export(List<ScalingConfiguration> bestIndividuals, Path outputFile) {
        scalingCsvWriter.writeToCsv(outputFile, bestIndividuals, false, true);
    }
}
