package com.uni.ea_autoscaler.ga.evaluation.penalty;

import com.uni.ea_autoscaler.baseline.BaselineThresholds;
import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

@Slf4j
@Component
public class DefaultPenaltyStrategy implements PenaltyStrategy {

    @Override
    public void applyDiscardPenalty(ScalingConfiguration individual) {
        LinkedHashMap<String, Double> penaltyObjectives = new LinkedHashMap<>();

        penaltyObjectives.put("slaViolation", 1.0);
        penaltyObjectives.put("avgCpu", Double.MAX_VALUE);
        penaltyObjectives.put("avgMemory", Double.MAX_VALUE);
        penaltyObjectives.put("avgReplicas", Double.MAX_VALUE);
        penaltyObjectives.put("cpuEfficiencyLoss", Double.MAX_VALUE);
        penaltyObjectives.put("memoryEfficiencyLoss", Double.MAX_VALUE);
        penaltyObjectives.put("errorRate", 1.0);

        individual.setObjectivesMap(penaltyObjectives);
        individual.setPenalizedObjectivesMap(penaltyObjectives);
        individual.setObjectives(penaltyObjectives.values().stream().mapToDouble(Double::doubleValue).toArray());
    }

    private List<ObjectivePenaltyRule> buildPenaltyRules() {
        BaselineThresholds thresholds = BaselineThresholds.getInstance();

        return List.of(
                new ObjectivePenaltyRule("slaViolation", isPresentAnd(v -> v > 0.2),
                        v -> (v - 0.2) * 100),
                new ObjectivePenaltyRule("avgCpu", isPresentAnd(v -> v > thresholds.getBaselineAvgCpu() * 1.5),
                        v -> (v / (thresholds.getBaselineAvgCpu() * 1.5) - 1.0) * 10.0),
                new ObjectivePenaltyRule("avgMemory", isPresentAnd(v -> v > thresholds.getBaselineAvgMemory() * 1.5),
                        v -> (v / (thresholds.getBaselineAvgMemory() * 1.5) - 1.0) * 10.0),
                new ObjectivePenaltyRule("avgReplicas", isPresentAnd(v -> v > thresholds.getBaselineAvgReplicas() * 1.2),
                        v -> (v / (thresholds.getBaselineAvgReplicas() * 1.2) - 1.0) * 10.0),
                new ObjectivePenaltyRule("cpuEfficiencyLoss", isPresentAnd(v -> v > 2.0),
                        v -> (v / 2.0 - 1.0) * 20.0),
                new ObjectivePenaltyRule("memoryEfficiencyLoss", isPresentAnd(v -> v > 2.0),
                        v -> (v / 2.0 - 1.0) * 20.0),
                new ObjectivePenaltyRule("errorRate", isPresentAnd(v -> v > 0.01), v -> v * 50)
        );
    }

    @Override
    public void applyPenalties(ScalingConfiguration individual, Boolean applyPenalties) {
        if (applyPenalties) {
            List<ObjectivePenaltyRule> rules = buildPenaltyRules();
            LinkedHashMap<String, Double> objectiveMap = individual.getPenalizedObjectivesMap();

            if (objectiveMap == null || objectiveMap.isEmpty()) {
                log.warn("⚠️ No objectives to apply penalties.");
                return;
            }

            for (ObjectivePenaltyRule rule : rules) {
                String key = rule.name();
                Double value = objectiveMap.get(key);

                if (rule.condition().test(value)) {
                    double penalty = rule.penaltyFunction().apply(value);
                    log.warn("⚠️ Penalty rule triggered for '{}': value={} → +{}", key, value, penalty);
                    addPenalty(objectiveMap, key, penalty);
                }
            }
        }

        individual.setObjectives(individual.getPenalizedObjectivesMap().values().stream()
                .mapToDouble(v -> v != null ? v : Double.MAX_VALUE)
                .toArray());
    }

    private Predicate<Double> isPresentAnd(Predicate<Double> condition) {
        return v -> v != null && condition.test(v);
    }

    private void addPenalty(Map<String, Double> map, String key, double value) {
        map.computeIfPresent(key, (k, v) -> v + value);
    }

    public record ObjectivePenaltyRule(
            String name,
            Predicate<Double> condition,
            Function<Double, Double> penaltyFunction) {
    }
}
