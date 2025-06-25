package com.uni.ea_autoscaler.old.ga.nsga3;

import com.uni.ea_autoscaler.old.ga.logging.EvolutionCsvExporter;
import com.uni.ea_autoscaler.old.ga.model.ScalingConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;

import java.nio.file.Paths;
import java.util.List;

@Slf4j
//@Component
public class EvolutionRunner implements CommandLineRunner {

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

    @Override
    public void run(String... args) {
        log.info("🚀 Starting evolutionary process...");

        List<ScalingConfiguration> finalPopulation = engine.run();

        log.info("🎯 Final population:");
        finalPopulation.forEach(ind -> log.info(ind.toString()));

        evolutionHistory.logSummary();

        csvExporter.export(
                evolutionHistory.getBestIndividuals(),
                Paths.get(outputDir, "evolution_history.csv")
        );

        log.info("🏁 Evolution process completed.");
    }
}
