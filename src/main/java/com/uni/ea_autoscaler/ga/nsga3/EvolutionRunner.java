package com.uni.ea_autoscaler.ga.nsga3;

import com.uni.ea_autoscaler.ga.logging.EvolutionCsvExporter;
import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.util.List;

@Slf4j
@Component
public class EvolutionRunner {

    private final EvolutionEngine engine;
    private final EvolutionHistory evolutionHistory;
    private final EvolutionCsvExporter csvExporter;

    private final String outputDir;

    public EvolutionRunner(
            EvolutionEngine engine,
            EvolutionHistory evolutionHistory,
            EvolutionCsvExporter csvExporter,
            @Value("${evolution.outputDir}") String outputDir
    ) {
        this.engine = engine;
        this.evolutionHistory = evolutionHistory;
        this.csvExporter = csvExporter;
        this.outputDir = outputDir;
    }

    public void run() {
        log.info("🚀 Starting evolutionary process...");

        List<ScalingConfiguration> finalPopulation = engine.run();

        log.info("🎯 Final population:");
        finalPopulation.forEach(ind -> log.info(ind.toString()));

        csvExporter.export(
                evolutionHistory.getBestIndividuals(),
                Paths.get(outputDir, "evolution_history.csv")
        );

        log.info("🏁 Evolution process completed.");
    }
}
