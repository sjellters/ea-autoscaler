package com.uni.ea_autoscaler.old.ga.evaluation;

import com.uni.ea_autoscaler.cache.EvaluationCache;
import com.uni.ea_autoscaler.cache.ScalingKey;
import com.uni.ea_autoscaler.jmeter.JMeterService;
import com.uni.ea_autoscaler.jmeter.dto.JMeterResultMetrics;
import com.uni.ea_autoscaler.k8s.KubernetesPodTracker;
import com.uni.ea_autoscaler.k8s.KubernetesScalingService;
import com.uni.ea_autoscaler.old.ga.evaluation.penalty.PenaltyReason;
import com.uni.ea_autoscaler.old.ga.evaluation.penalty.PenaltyStrategy;
import com.uni.ea_autoscaler.old.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.prometheus.PrometheusMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class FitnessEvaluator {

    private final JMeterService jmeterService;
    private final PrometheusMetricsService prometheusMetricsService;
    private final KubernetesScalingService kubernetesScalingService;
    private final PenaltyStrategy penaltyStrategy;
    private final EvaluationCache evaluationCache;
    private final KubernetesPodTracker kubernetesPodTracker;

    private final String targetHost;
    private final String targetPort;
    private final String namespace;
    private final String testPlanPath;
    private final String deploymentName;

    public FitnessEvaluator(JMeterService jmeterService,
                            PrometheusMetricsService prometheusMetricsService,
                            KubernetesScalingService kubernetesScalingService,
                            PenaltyStrategy penaltyStrategy,
                            @Qualifier("compositeEvaluationCache") EvaluationCache evaluationCache, KubernetesPodTracker kubernetesPodTracker,
                            @Value("${evaluation.targetHost}") String targetHost,
                            @Value("${evaluation.targetPort}") String targetPort,
                            @Value("${evaluation.namespace}") String namespace,
                            @Value("${evaluation.testPlanPath}") String testPlanPath,
                            @Value("${k8s.deploymentName}") String deploymentName) {
        this.jmeterService = jmeterService;
        this.prometheusMetricsService = prometheusMetricsService;
        this.kubernetesScalingService = kubernetesScalingService;
        this.penaltyStrategy = penaltyStrategy;
        this.evaluationCache = evaluationCache;
        this.kubernetesPodTracker = kubernetesPodTracker;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.namespace = namespace;
        this.testPlanPath = testPlanPath;
        this.deploymentName = deploymentName;
    }

    public static String format(Instant startTime, Instant endTime) {
        Duration duration = Duration.between(startTime, endTime);
        long seconds = duration.getSeconds();

        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, secs);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, secs);
        } else {
            return String.format("%ds", secs);
        }
    }

    private String generateEvaluationId() {
        return "ID_" + System.nanoTime();
    }

    private void logSeparator() {
        log.info("================================================================");
    }

    private boolean checkCacheAndReuse(ScalingConfiguration individual, String id) {
        ScalingKey key = new ScalingKey(individual);

        double[] cachedObjectives = evaluationCache.getObjectives(key);

        if (cachedObjectives != null) {
            logSeparator();
            log.info("♻️ Cached evaluation reused for {}.", id);

            individual.setObjectives(cachedObjectives);

            return true;
        }

        return false;
    }

    private boolean applyScaling(ScalingConfiguration individual, String id) {
        logSeparator();
        log.info("🧬 Starting evaluation for {}:", id);
        log.info("Configuration:\n{}", individual);

        boolean success = kubernetesScalingService.applyAndWait(individual);

        if (!success) {
            log.warn("⚠️ Scaling application failed for {}", id);
            penaltyStrategy.applyPenalty(individual, PenaltyReason.CONFIGURATION_FAILURE);
            return false;
        }
        return true;
    }

    private void applyPenalty(ScalingConfiguration individual, PenaltyReason reason, String id) {
        log.warn("⚠️ Penalty applied to {} due to {}", id, reason);
        penaltyStrategy.applyPenalty(individual, reason);
    }

    private BenchmarkResult runBenchmark(String id) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(kubernetesPodTracker::pollPods, 0, 2, TimeUnit.SECONDS);

        String resultFile = "results_" + id + ".jtl";
        Instant startTime = Instant.now();
        JMeterResultMetrics metrics = jmeterService.runTest(targetHost, targetPort, testPlanPath, resultFile);
        Instant endTime = Instant.now();

        log.info("⏱️ Benchmark completed for {} in {}", id, format(startTime, endTime));

        return new BenchmarkResult(metrics, startTime, endTime);
    }

    private Map<String, Double> collectObjectives(JMeterResultMetrics metrics, Instant start, Instant end) {
        long durationSeconds = Duration.between(start, end).toSeconds();
        String window = durationSeconds + "s";

        log.info("📡 Querying Prometheus metrics with window={}", window);

        Set<String> trackedPods = kubernetesPodTracker.getTrackedPods();

        if (trackedPods.isEmpty()) {
            log.warn("⚠️ No tracked pods found during benchmark window. Skipping Prometheus metrics.");
            return Map.of(
                    "avgResponseTime", metrics.averageResponseTime(),
                    "avgCpu", Double.MAX_VALUE,
                    "avgMemory", Double.MAX_VALUE,
                    "avgReplicas", 0.0,
                    "errorRate", 1.0,
                    "avgLatency", metrics.averageLatency()
            );
        }

        String podRegex = String.join("|", trackedPods);

        double avgCpu = prometheusMetricsService.averageRange(
                String.format("sum by (pod) (rate(container_cpu_usage_seconds_total{namespace=\"%s\", pod=~\"%s\"}[%s]))", namespace, podRegex, window),
                start, end, "30s"
        );

        double avgMemory = prometheusMetricsService.averageRange(
                String.format("avg by (pod) (container_memory_usage_bytes{namespace=\"%s\", pod=~\"%s\"})", namespace, podRegex),
                start, end, "30s"
        );

        double avgReplicas = prometheusMetricsService.averageRange(
                String.format("kube_deployment_status_replicas{namespace=\"%s\", deployment=\"%s\"}", namespace, deploymentName),
                start, end, "30s"
        );

        Map<String, Double> objectiveMap = new LinkedHashMap<>();
        objectiveMap.put("avgResponseTime", metrics.averageResponseTime());
        objectiveMap.put("avgCpu", avgCpu);
        objectiveMap.put("avgMemory", avgMemory / (1024 * 1024));
        objectiveMap.put("avgReplicas", avgReplicas);
        objectiveMap.put("errorRate", metrics.errorRate());
        objectiveMap.put("avgLatency", metrics.averageLatency());

        return objectiveMap;
    }

    private double[] extractObjectiveArray(Map<String, Double> map) {
        return map.values().stream().mapToDouble(Double::doubleValue).toArray();
    }

    private void logEvaluationResults(String id, ScalingConfiguration individual, Map<String, Double> map) {
        log.info("📦 Stored objectives in cache for key: {}", new ScalingKey(individual));
        StringBuilder sb = new StringBuilder("🎯 Objectives for ").append(id).append(":\n");
        map.forEach((k, v) -> sb.append("  - ").append(k).append(": ").append(v).append("\n"));
        log.info(sb.toString());
        log.info("✅ [DONE] Evaluation complete for {}:", id);
        logSeparator();
    }

    public void evaluate(ScalingConfiguration individual) {
        String id = generateEvaluationId();

        if (checkCacheAndReuse(individual, id)) return;
        if (!applyScaling(individual, id)) return;

        BenchmarkResult benchmarkResult = runBenchmark(id);

        Instant startTime = benchmarkResult.startTime();
        JMeterResultMetrics metrics = benchmarkResult.metrics();
        Instant endTime = benchmarkResult.endTime();

        if (metrics == null) {
            applyPenalty(individual, PenaltyReason.JMETER_FAILURE, id);
            return;
        }

        Map<String, Double> objectiveMap = collectObjectives(metrics, startTime, endTime);
        double[] objectives = extractObjectiveArray(objectiveMap);

        if (objectives.length == 0 || allZeros(objectives)) {
            applyPenalty(individual, PenaltyReason.EVALUATION_INCOMPLETE, id);
            return;
        }

        individual.setObjectives(objectives);
//        evaluationCache.storeObjectives(new ScalingKey(individual), objectives);
        logEvaluationResults(id, individual, objectiveMap);
    }

    private boolean allZeros(double[] arr) {
        for (double v : arr) {
            if (v != 0.0) return false;
        }
        return true;
    }
}
