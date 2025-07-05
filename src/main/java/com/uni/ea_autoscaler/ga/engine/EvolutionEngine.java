package com.uni.ea_autoscaler.ga.engine;

import com.uni.ea_autoscaler.ga.evaluation.ObjectiveNormalizer;
import com.uni.ea_autoscaler.ga.evaluation.PopulationEvaluator;
import com.uni.ea_autoscaler.ga.initialization.RandomInitializer;
import com.uni.ea_autoscaler.ga.model.Individual;
import com.uni.ea_autoscaler.ga.nsga3.NextGenerationSelector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvolutionEngine {

    private final RandomInitializer initializer;
    private final PopulationEvaluator evaluator;
    private final ObjectiveNormalizer normalizer;
    private final OffspringGenerator offspringGenerator;
    private final NextGenerationSelector nextGenerationSelector;

    public void run(int populationSize, int generations, BiConsumer<Integer, List<Individual>> generationCallback) {
        List<Individual> population = initializer.generatePopulation(populationSize);
        log.info("🌱 Initial population size: {}", population.size());
        evaluator.evaluate(population);
        population = filterValid(population);
        generationCallback.accept(0, population);

        for (int gen = 1; gen <= generations; gen++) {
            log.info("🌀 Generation {}/{}", gen, generations);

            normalizer.normalize(population);
            List<Individual> offspring = offspringGenerator.generate(population, populationSize);
            List<Individual> combined = new ArrayList<>();
            combined.addAll(population);
            combined.addAll(offspring);

            normalizer.normalize(combined);
            population = nextGenerationSelector.selectNextGeneration(population, offspring, populationSize);
            generationCallback.accept(gen, population);
        }

        log.info("🏁 Evolution completed after {} generations", generations);
    }

    private List<Individual> filterValid(List<Individual> individuals) {
        return individuals.stream()
                .filter(Individual::isValid)
                .collect(Collectors.toList());
    }
}
