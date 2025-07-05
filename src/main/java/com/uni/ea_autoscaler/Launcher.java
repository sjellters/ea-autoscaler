package com.uni.ea_autoscaler;

import com.uni.ea_autoscaler.ga.engine.EvolutionEngine;
import com.uni.ea_autoscaler.ga.export.CsvAggregator;
import com.uni.ea_autoscaler.ga.export.CsvExporter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class Launcher {

    private final int populationSize;
    private final int generations;
    private final EvolutionEngine evolutionEngine;

    private final CsvExporter csvExporter;
    private final CsvAggregator csvAggregator;

    public Launcher(@Value("${ga.populationSize}") int populationSize,
                    @Value("${ga.generations}") int generations,
                    EvolutionEngine evolutionEngine,
                    CsvExporter csvExporter,
                    CsvAggregator csvAggregator) {
        this.populationSize = populationSize;
        this.generations = generations;
        this.evolutionEngine = evolutionEngine;
        this.csvExporter = csvExporter;
        this.csvAggregator = csvAggregator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void runWhenReady() {
        try {
            csvExporter.initOutputDirectory();
            evolutionEngine.run(
                    populationSize,
                    generations,
                    (gen, pop) -> {
                        try {
                            csvExporter.export(String.format("gen_%02d.csv", gen), pop);
                        } catch (IOException e) {
                            throw new RuntimeException("Error exporting generation " + gen, e);
                        }
                    });
            csvAggregator.aggregateGenerations("output");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
