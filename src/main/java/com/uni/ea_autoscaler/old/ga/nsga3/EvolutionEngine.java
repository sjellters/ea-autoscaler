package com.uni.ea_autoscaler.old.ga.nsga3;

import com.uni.ea_autoscaler.old.ga.evaluation.FitnessEvaluator;
import com.uni.ea_autoscaler.old.ga.evaluation.ObjectiveNormalizer;
import com.uni.ea_autoscaler.old.ga.initialization.RandomInitializer;
import com.uni.ea_autoscaler.old.ga.logging.GenerationCsvLogger;
import com.uni.ea_autoscaler.old.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.old.ga.model.ScalingConfigurationValidator;
import com.uni.ea_autoscaler.old.ga.operators.MutationOperator;
import com.uni.ea_autoscaler.old.ga.operators.crossover.CrossoverOperator;
import com.uni.ea_autoscaler.old.ga.operators.selection.SelectionOperator;
import com.uni.ea_autoscaler.old.ga.selection.NextGenerationSelector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class EvolutionEngine {

    private final RandomInitializer initializer;
    private final CrossoverOperator crossoverOperator;
    private final MutationOperator mutationOperator;
    private final SelectionOperator selectionOperator;
    private final FitnessEvaluator fitnessEvaluator;
    private final ObjectiveNormalizer objectiveNormalizer;
    private final EvolutionHistory evolutionHistory;
    private final GenerationCsvLogger generationCSVLogger;
    private final EvolutionConfigValidator configValidator;
    private final ScalingConfigurationValidator scalingConfigurationValidator;
    private final NextGenerationSelector nextGenerationSelector;

    private final int populationSize;
    private final int maxGenerations;
    private final double mutationRate;

    public EvolutionEngine(
            RandomInitializer initializer,
            CrossoverOperator crossoverOperator,
            MutationOperator mutationOperator,
            SelectionOperator selectionOperator,
            FitnessEvaluator fitnessEvaluator,
            ObjectiveNormalizer objectiveNormalizer,
            EvolutionHistory evolutionHistory,
            GenerationCsvLogger generationCSVLogger,
            EvolutionConfigValidator configValidator,
            ScalingConfigurationValidator scalingConfigurationValidator,
            NextGenerationSelector nextGenerationSelector,
            @Value("${evolution.populationSize}") int populationSize,
            @Value("${evolution.maxGenerations}") int maxGenerations,
            @Value("${evolution.mutationRate}") double mutationRate
    ) {
        this.initializer = initializer;
        this.crossoverOperator = crossoverOperator;
        this.mutationOperator = mutationOperator;
        this.selectionOperator = selectionOperator;
        this.fitnessEvaluator = fitnessEvaluator;
        this.objectiveNormalizer = objectiveNormalizer;
        this.evolutionHistory = evolutionHistory;
        this.generationCSVLogger = generationCSVLogger;
        this.configValidator = configValidator;
        this.scalingConfigurationValidator = scalingConfigurationValidator;
        this.nextGenerationSelector = nextGenerationSelector;
        this.populationSize = populationSize;
        this.maxGenerations = maxGenerations;
        this.mutationRate = mutationRate;
    }

    public List<ScalingConfiguration> run() {
        long engineStartTime = System.currentTimeMillis();

        configValidator.validate(populationSize, maxGenerations, mutationRate);
        log.info("⚙️ Starting evolution with configuration:");
        log.info(" - Population Size: {}", populationSize);
        log.info(" - Max Generations: {}", maxGenerations);
        log.info(" - Mutation Rate: {}", mutationRate);

        List<ScalingConfiguration> population = initializer.generatePopulation(populationSize);
        evaluatePopulation(population);
        objectiveNormalizer.normalize(population);

        for (int generation = 1; generation <= maxGenerations; generation++) {
            long start = System.currentTimeMillis();

            List<ScalingConfiguration> offspring = new ArrayList<>();

            while (offspring.size() < populationSize) {
                ScalingConfiguration parent1 = selectionOperator.select(population);
                ScalingConfiguration parent2 = selectionOperator.select(population);

                ScalingConfiguration child = crossoverOperator.crossover(parent1, parent2);
                child = mutationOperator.mutate(child, mutationRate);

                if (scalingConfigurationValidator.isValid(child)) {
                    fitnessEvaluator.evaluate(child);
                    offspring.add(child);
                } else {
                    log.debug("❌ Invalid individual discarded:\n{}", child);
                }
            }

            objectiveNormalizer.normalize(offspring);

            population = nextGenerationSelector.selectNextGeneration(population, offspring, populationSize);
            evolutionHistory.recordGeneration(population);
            generationCSVLogger.log(generation, population);

            long duration = System.currentTimeMillis() - start;
            log.info("⏱️ Generation {} completed in {} ms", generation, duration);
        }

        long totalDuration = System.currentTimeMillis() - engineStartTime;

        log.info("🏁 Evolution completed in {} ms", totalDuration);

        return population;
    }

    private void evaluatePopulation(List<ScalingConfiguration> population) {
        for (ScalingConfiguration individual : population) {
            fitnessEvaluator.evaluate(individual);
        }
    }
}
