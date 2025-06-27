package com.uni.ea_autoscaler.ga.evaluation;

import com.uni.ea_autoscaler.cache.ScalingKey;
import com.uni.ea_autoscaler.ga.evaluation.penalty.PenaltyStrategy;
import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.jmeter.JMeterService;
import com.uni.ea_autoscaler.jmeter.dto.JMeterResultMetrics;
import com.uni.ea_autoscaler.k8s.KubernetesPodTracker;
import com.uni.ea_autoscaler.prometheus.PrometheusMetricsService;
import com.uni.ea_autoscaler.prometheus.dto.PrometheusResultMetrics;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class ConfigurationEvaluator {

    protected final JMeterService jmeterService;
    protected final PrometheusMetricsService prometheusMetricsService;
    protected final KubernetesPodTracker kubernetesPodTracker;
    protected final String targetHost;
    protected final String targetPort;
    protected final String namespace;
    protected final String testPlanPath;
    protected final String deploymentName;
    private final PenaltyStrategy penaltyStrategy;

    protected ConfigurationEvaluator(JMeterService jmeterService,
                                     PrometheusMetricsService prometheusMetricsService,
                                     KubernetesPodTracker kubernetesPodTracker,
                                     PenaltyStrategy penaltyStrategy,
                                     String targetHost,
                                     String targetPort,
                                     String namespace,
                                     String testPlanPath,
                                     String deploymentName) {
        this.jmeterService = jmeterService;
        this.prometheusMetricsService = prometheusMetricsService;
        this.kubernetesPodTracker = kubernetesPodTracker;
        this.penaltyStrategy = penaltyStrategy;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.namespace = namespace;
        this.testPlanPath = testPlanPath;
        this.deploymentName = deploymentName;
    }

    private static String format(Instant startTime, Instant endTime) {
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

    protected abstract void evaluate(ScalingConfiguration individual);

    protected String generateEvaluationId() {
        return "ID_" + System.nanoTime();
    }

    protected void logSeparator() {
        log.info("================================================================");
    }

    private BenchmarkResult runBenchmark(String id) {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        kubernetesPodTracker.reset();

        scheduler.scheduleAtFixedRate(() -> {
            try {
                kubernetesPodTracker.pollPods();
            } catch (Exception e) {
                log.warn("⚠️ Exception during pod polling: {}", e.getMessage());
            }
        }, 0, 2, TimeUnit.SECONDS);

        String resultFile = "results_" + id + ".jtl";
        Instant startTime = Instant.now();
        JMeterResultMetrics metrics = jmeterService.runTest(targetHost, targetPort, testPlanPath, resultFile);
        Instant endTime = Instant.now();

        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }

        scheduler.shutdown();

        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                log.warn("⚠️ Polling scheduler did not terminate cleanly.");
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        log.info("⏱️ Benchmark completed for {} in {}", id, format(startTime, endTime));

        Set<String> podNames = kubernetesPodTracker.getTrackedPods();
        return new BenchmarkResult(metrics, startTime, endTime, podNames);
    }

    private PrometheusResultMetrics getPrometheusMetrics(Instant start, Instant end, Set<String> trackedPods) {
        long durationSeconds = Duration.between(start, end).toSeconds();
        String window = durationSeconds + "s";

        log.info("📡 Querying Prometheus metrics with window={}", window);

        if (trackedPods == null || trackedPods.isEmpty()) {
            log.warn("⚠️ No pods were tracked during benchmark. Skipping Prometheus query.");
            return null;
        }

        String podRegex = String.join("|", trackedPods);

        Double avgCpu = prometheusMetricsService.averageRange(
                String.format("sum(rate(container_cpu_usage_seconds_total{namespace=\"%s\", pod=~\"%s\"}[%s]))",
                        namespace,
                        podRegex,
                        window),
                start, end, "30s"
        );

        Double avgMemory = prometheusMetricsService.averageRange(
                String.format("avg(container_memory_usage_bytes{namespace=\"%s\", pod=~\"%s\"})",
                        namespace,
                        podRegex),
                start, end, "30s"
        );
        avgMemory = avgMemory != null ? avgMemory / (1024 * 1024) : null;

        Double avgReplicas = prometheusMetricsService.averageRange(
                String.format("avg(kube_deployment_status_replicas{namespace=\"%s\", deployment=\"%s\"})",
                        namespace,
                        deploymentName),
                start, end, "30s"
        );

        return new PrometheusResultMetrics(avgCpu, avgMemory, avgReplicas);
    }

    private void logEvaluationResults(String id, ScalingConfiguration individual) {
        log.info("📦 Stored objectives in cache for key: {}", new ScalingKey(individual));

        LinkedHashMap<String, Double> metrics = individual.getMetrics();
        LinkedHashMap<String, Double> objectives = individual.getObjectivesMap();
        LinkedHashMap<String, Double> penalizedObjectives = individual.getPenalizedObjectivesMap();

        StringBuilder sb = new StringBuilder();
        sb.append("\n🧾 Evaluation Results for ").append(id).append(":\n");
        sb.append("--------------------------------------------------\n");
        sb.append("📊 Raw Metrics:\n");
        metrics.forEach((k, v) -> sb.append(String.format("  - %-22s : %s%n", k, v)));

        sb.append("--------------------------------------------------\n");
        sb.append("🎯 Optimization Objectives:\n");
        objectives.forEach((k, v) -> sb.append(String.format("  - %-22s : %s%n", k, v)));

        sb.append("--------------------------------------------------");
        sb.append("\n");
        sb.append("🔧 Penalized Objectives:\n");
        penalizedObjectives.forEach((k, v) -> sb.append(String.format("  - %-22s : %s%n", k, v)));
        sb.append("--------------------------------------------------\n");

        log.info(sb.toString());
        log.info("✅ [DONE] Evaluation complete for {}.", id);
        logSeparator();
    }

    protected void applyDiscardPenalty(ScalingConfiguration individual, String id) {
        log.warn("⚠️ Discard penalty applied to {}", id);
        penaltyStrategy.applyDiscardPenalty(individual);
    }

    private void saveMetricsToConfiguration(ScalingConfiguration individual,
                                            @NonNull JMeterResultMetrics jMeterResultMetrics,
                                            @NonNull PrometheusResultMetrics prometheusResultMetrics) {
        LinkedHashMap<String, Double> metrics = new LinkedHashMap<>();

        Double avgCpu = prometheusResultMetrics.avgCpu();
        Double avgMemory = prometheusResultMetrics.avgMemory();
        Double avgReplicas = prometheusResultMetrics.avgReplicas();

        Double avgResponseTime = jMeterResultMetrics.averageResponseTime();
        Double errorRate = jMeterResultMetrics.errorRate();
        Double avgLatency = jMeterResultMetrics.averageLatency();
        Double slaPercentage = jMeterResultMetrics.slaPercentage();
        Double p95 = jMeterResultMetrics.p95();

        Double cpuEfficiency = (avgCpu != null && avgReplicas != null && individual.getCpuRequest() > 0)
                ? avgCpu / (individual.getCpuRequest() * avgReplicas / 1000.0)
                : null;

        Double memoryEfficiency = (avgMemory != null && avgReplicas != null && individual.getMemoryRequest() > 0)
                ? avgMemory / (individual.getMemoryRequest() * avgReplicas * 1024 * 1024)
                : null;

        metrics.put("avgResponseTime", avgResponseTime);
        metrics.put("avgCpu", avgCpu);
        metrics.put("avgMemory", avgMemory != null ? avgMemory / (1024 * 1024) : null);
        metrics.put("avgReplicas", avgReplicas);
        metrics.put("errorRate", errorRate);
        metrics.put("avgLatency", avgLatency);
        metrics.put("cpuEfficiency", cpuEfficiency);
        metrics.put("memoryEfficiency", memoryEfficiency);
        metrics.put("slaPercentage", slaPercentage);
        metrics.put("p95", p95);

        individual.setMetrics(metrics);
    }

    protected void fillObjectivesMap(ScalingConfiguration individual) {
        LinkedHashMap<String, Double> metrics = individual.getMetrics();

        LinkedHashMap<String, Double> objectiveMap = new LinkedHashMap<>();

        Double slaPercentage = metrics.get("slaPercentage");
        objectiveMap.put("slaViolation", 1.0 - (slaPercentage != null ? slaPercentage : 0.0));

        objectiveMap.put("avgCpu", metrics.get("avgCpu"));
        objectiveMap.put("avgMemory", metrics.get("avgMemory"));
        objectiveMap.put("avgReplicas", metrics.get("avgReplicas"));

        Double cpuEfficiency = metrics.get("cpuEfficiency");
        objectiveMap.put("cpuEfficiencyLoss", (cpuEfficiency != null && cpuEfficiency > 0) ? 1.0 / cpuEfficiency : Double.MAX_VALUE);

        Double memoryEfficiency = metrics.get("memoryEfficiency");
        objectiveMap.put("memoryEfficiencyLoss", (memoryEfficiency != null && memoryEfficiency > 0) ? 1.0 / memoryEfficiency : Double.MAX_VALUE);
        objectiveMap.put("errorRate", metrics.getOrDefault("errorRate", 1.0));

        individual.setObjectivesMap(new LinkedHashMap<>(objectiveMap));
        individual.setPenalizedObjectivesMap(new LinkedHashMap<>(objectiveMap));
    }

    protected void fillEvaluationData(ScalingConfiguration individual, String id, Boolean baseline) {
        if (individual.getMetrics() == null || individual.getMetrics().isEmpty()) {
            BenchmarkResult benchmarkResult = runBenchmark(id);

            Instant startTime = benchmarkResult.startTime();
            JMeterResultMetrics jMeterResultMetrics = benchmarkResult.metrics();
            Instant endTime = benchmarkResult.endTime();

            if (jMeterResultMetrics == null || jMeterResultMetrics.averageResponseTime() == null || jMeterResultMetrics.averageLatency() == null) {
                log.warn("⚠️ JMeter metrics are incomplete for {}. Applying discard penalty.", id);
                applyDiscardPenalty(individual, id);
                return;
            }

            PrometheusResultMetrics prometheusResultMetrics = getPrometheusMetrics(startTime, endTime, benchmarkResult.podNames());

            if (prometheusResultMetrics == null) {
                log.warn("⚠️ Prometheus metrics are incomplete for {}. Applying discard penalty.", id);
                applyDiscardPenalty(individual, id);
                return;
            }

            saveMetricsToConfiguration(individual, jMeterResultMetrics, prometheusResultMetrics);
        }

        fillObjectivesMap(individual);
        penaltyStrategy.applyPenalties(individual, baseline);

        logEvaluationResults(id, individual);
    }
}
