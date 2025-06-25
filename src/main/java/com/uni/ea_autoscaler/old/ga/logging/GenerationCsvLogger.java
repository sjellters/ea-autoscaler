package com.uni.ea_autoscaler.old.ga.logging;

import com.uni.ea_autoscaler.old.ga.model.ScalingConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;

@Component
public class GenerationCsvLogger {

    private final Path logFilePath;
    private final ScalingCsvWriter scalingCsvWriter;

    public GenerationCsvLogger(
            @Value("${evolution.outputDir}") String outputDir,
            ScalingCsvWriter scalingCsvWriter
    ) {
        this.logFilePath = Path.of(outputDir, "evolution_log.csv");
        this.scalingCsvWriter = scalingCsvWriter;
    }

    public void log(int generation, List<ScalingConfiguration> population) {
        scalingCsvWriter.writeToCsv(logFilePath, population, generation, true, true);
    }
}
