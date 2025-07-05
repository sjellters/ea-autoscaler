package com.uni.ea_autoscaler.ga.evaluation;

import com.uni.ea_autoscaler.common.BaselineValues;
import com.uni.ea_autoscaler.core.interfaces.Objective;
import com.uni.ea_autoscaler.evaluation.EvaluationRunner;
import com.uni.ea_autoscaler.evaluation.exception.EvaluationRetryableException;
import com.uni.ea_autoscaler.ga.model.Individual;
import com.uni.ea_autoscaler.metrics.domain.ComputedMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class IndividualEvaluator {

    private static final int SLA_THRESHOLD_MS = 15000;

    private final EvaluationRunner evaluationRunner;
    private final List<Objective> objectives;
    private final EvaluationCache cache = EvaluationCache.getInstance();

    public IndividualEvaluator(EvaluationRunner evaluationRunner, List<Objective> objectives) {
        this.evaluationRunner = evaluationRunner;
        this.objectives = objectives;
    }

    public void evaluate(Individual individual) {
        String key = individual.getConfig().toString();

        if (cache.contains(key)) {
            log.info("⚡ Reusing cached evaluation for config {}", key);
            Individual cached = cache.get(key);

            individual.setComputedMetrics(cached.getComputedMetrics());
            individual.getRawObjectives().putAll(cached.getRawObjectives());
            individual.getObjectives().putAll(cached.getObjectives());
            individual.setEvaluationFailed(cached.isEvaluationFailed());
            return;
        }

        try {
            ComputedMetrics metrics = evaluationRunner.runEvaluation(individual.getConfig(), new BaselineValues(SLA_THRESHOLD_MS));
            individual.setComputedMetrics(metrics.getAll());

            for (Objective objective : objectives) {
                try {
                    double raw = objective.compute(individual.getConfig(), metrics);
                    double penalized = objective.getPenalized(raw, individual.getConfig(), metrics);

                    individual.setRawObjective(objective.name(), raw);
                    individual.setObjective(objective.name(), penalized);
                } catch (Exception e) {
                    throw new EvaluationRetryableException("❌ Error evaluating objective " + objective.name(), e);
                }
            }

            cache.store(key, individual);
            log.info("✅ Individual evaluated with {} objectives\n{}", objectives.size(), individual.prettyPrint());
        } catch (EvaluationRetryableException e) {
            log.warn("⚠️ Evaluation failed after retries for individual. Marking as invalid.");
            individual.setEvaluationFailed(true);
        } catch (Exception e) {
            log.error("❌ Unexpected error during individual evaluation: {}", e.getMessage(), e);
            individual.setEvaluationFailed(true);
        }
    }
}
