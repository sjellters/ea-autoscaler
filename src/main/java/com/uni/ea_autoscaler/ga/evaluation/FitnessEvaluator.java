package com.uni.ea_autoscaler.ga.evaluation;

import com.uni.ea_autoscaler.cache.EvaluationCache;
import com.uni.ea_autoscaler.cache.ScalingKey;
import com.uni.ea_autoscaler.ga.evaluation.penalty.PenaltyReason;
import com.uni.ea_autoscaler.ga.evaluation.penalty.PenaltyStrategy;
import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.jmeter.JMeterService;
import com.uni.ea_autoscaler.jmeter.dto.JMeterResultMetrics;
import com.uni.ea_autoscaler.k8s.KubernetesScalingService;
import com.uni.ea_autoscaler.prometheus.PrometheusMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
public class FitnessEvaluator {

    private final JMeterService jmeterService;
    private final PrometheusMetricsService prometheusMetricsService;
    private final KubernetesScalingService kubernetesScalingService;
    private final PenaltyStrategy penaltyStrategy;
    private final EvaluationCache evaluationCache;

    private final String targetHost;
    private final String targetPort;
    private final String namespace;
    private final String testPlanPath;

    public FitnessEvaluator(
            JMeterService jmeterService,
            PrometheusMetricsService prometheusMetricsService,
            KubernetesScalingService kubernetesScalingService,
            PenaltyStrategy penaltyStrategy,
            @Qualifier("compositeEvaluationCache") EvaluationCache evaluationCache,
            @Value("${evaluation.targetHost}") String targetHost,
            @Value("${evaluation.targetPort}") String targetPort,
            @Value("${evaluation.namespace}") String namespace,
            @Value("${evaluation.testPlanPath}") String testPlanPath
    ) {
        this.jmeterService = jmeterService;
        this.prometheusMetricsService = prometheusMetricsService;
        this.kubernetesScalingService = kubernetesScalingService;
        this.penaltyStrategy = penaltyStrategy;
        this.evaluationCache = evaluationCache;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.namespace = namespace;
        this.testPlanPath = testPlanPath;
    }

    public void evaluate(ScalingConfiguration individual) {
        String id = "ID-" + System.nanoTime();

        ScalingKey key = new ScalingKey(individual);
        double[] cachedObjectives = evaluationCache.getObjectives(key);
        if (cachedObjectives != null) {
            log.info("♻️ Cached evaluation reused for {}.", id);
            individual.setObjectives(cachedObjectives);
            return;
        }

        log.info("================================================================");
        log.info("🧬 Starting evaluation for {}:", id);
        log.info("Configuration:\n{}", individual);

        boolean success = kubernetesScalingService.applyAndWait(individual);
        if (!success) {
            log.warn("⚠️ Scaling application failed for {}", id);
            penaltyStrategy.applyPenalty(individual, PenaltyReason.CONFIGURATION_FAILURE);
            return;
        }

        String resultFile = "results_" + id + ".jtl";

        Instant startTime = Instant.now();
        JMeterResultMetrics metrics = jmeterService.runTest(targetHost, targetPort, testPlanPath, resultFile);
        Instant endTime = Instant.now();

        if (metrics == null) {
            log.warn("⚠️ JMeter failed for {}", id);
            penaltyStrategy.applyPenalty(individual, PenaltyReason.JMETER_FAILURE);
            return;
        }

        long durationSeconds = Duration.between(startTime, endTime).toSeconds();
        String window = durationSeconds + "s";

        log.info("📡 Querying Prometheus metrics for {} with window={}", id, window);

        double avgCpu = prometheusMetricsService.averageRange(
                String.format("rate(container_cpu_usage_seconds_total{namespace=\"%s\"}[%s])", namespace, window),
                startTime, endTime, "30s");
        double avgMemory = prometheusMetricsService.averageRange(
                String.format("container_memory_usage_bytes{namespace=\"%s\"}", namespace),
                startTime, endTime, "30s");
        double avgReplicas = prometheusMetricsService.averageRange(
                String.format("kube_deployment_status_replicas{namespace=\"%s\"}", namespace),
                startTime, endTime, "30s");

        Map<String, Double> objectiveMap = new LinkedHashMap<>();
        objectiveMap.put("avgResponseTime", metrics.averageResponseTime());
        objectiveMap.put("avgCpu", avgCpu);
        objectiveMap.put("avgMemory", avgMemory);
        objectiveMap.put("avgReplicas", avgReplicas);
        objectiveMap.put("errorRate", metrics.errorRate());
        objectiveMap.put("avgLatency", metrics.averageLatency());

        double[] objectives = objectiveMap.values().stream().mapToDouble(Double::doubleValue).toArray();

        if (objectives.length == 0 || allZeros(objectives)) {
            log.error("❌ Objectives contain only zeros or were not computed. Aborting evaluation for {}", id);
            penaltyStrategy.applyPenalty(individual, PenaltyReason.EVALUATION_INCOMPLETE);
            return;
        }

        individual.setObjectives(objectives);
        evaluationCache.storeObjectives(key, objectives);
        log.info("📦 Stored objectives in cache for key: {}", key);

        StringBuilder sb = new StringBuilder("🎯 Objectives for ").append(id).append(":\n");
        objectiveMap.forEach((name, value) -> sb.append("  - ").append(name).append(": ").append(value).append("\n"));
        log.info(sb.toString());

        log.info("✅ [DONE] Evaluation complete for {}:", id);
        log.info("================================================================");
    }

    private boolean allZeros(double[] arr) {
        for (double v : arr) {
            if (v != 0.0) return false;
        }
        return true;
    }
}
