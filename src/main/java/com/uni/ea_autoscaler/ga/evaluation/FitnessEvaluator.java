package com.uni.ea_autoscaler.ga.evaluation;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.jmeter.JMeterService;
import com.uni.ea_autoscaler.jmeter.dto.JMeterResultMetrics;
import com.uni.ea_autoscaler.k8s.KubernetesScalingService;
import com.uni.ea_autoscaler.ga.evaluation.penalty.PenaltyReason;
import com.uni.ea_autoscaler.ga.evaluation.penalty.PenaltyStrategy;
import com.uni.ea_autoscaler.prometheus.PrometheusMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FitnessEvaluator {

    private final JMeterService jmeterService;
    private final PrometheusMetricsService prometheusMetricsService;
    private final KubernetesScalingService kubernetesScalingService;
    private final PenaltyStrategy penaltyStrategy;

    private final String targetHost;
    private final String targetPort;
    private final String namespace;
    private final String testPlanPath;

    public FitnessEvaluator(
            JMeterService jmeterService,
            PrometheusMetricsService prometheusMetricsService,
            KubernetesScalingService kubernetesScalingService,
            PenaltyStrategy penaltyStrategy,
            @Value("${evaluation.targetHost}") String targetHost,
            @Value("${evaluation.targetPort}") String targetPort,
            @Value("${evaluation.namespace}") String namespace,
            @Value("${evaluation.testPlanPath}") String testPlanPath
    ) {
        this.jmeterService = jmeterService;
        this.prometheusMetricsService = prometheusMetricsService;
        this.kubernetesScalingService = kubernetesScalingService;
        this.penaltyStrategy = penaltyStrategy;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.namespace = namespace;
        this.testPlanPath = testPlanPath;
    }

    public void evaluate(ScalingConfiguration individual) {
        String id = "ID-" + System.nanoTime();

        log.info("================================================================");
        log.info("🧬 Starting evaluation for {}:", id);
        log.info("Configuration:\n{}", individual);

        boolean success = kubernetesScalingService.applyAndWait(individual);
        if (!success) {
            log.warn("⚠️  Scaling application failed for {}", id);
            penaltyStrategy.applyPenalty(individual, PenaltyReason.CONFIGURATION_FAILURE);
            return;
        }

        String resultFile = "results_" + id + ".jtl";

        JMeterResultMetrics metrics = jmeterService.runTest(targetHost, targetPort, testPlanPath, resultFile);
        if (metrics == null) {
            log.warn("⚠️  JMeter failed for {}", id);
            penaltyStrategy.applyPenalty(individual, PenaltyReason.JMETER_FAILURE);
            return;
        }

        log.info("📡 Querying Prometheus metrics for {}", id);
        double avgCpu = prometheusMetricsService.averageRange(
                String.format("rate(container_cpu_usage_seconds_total{namespace=\"%s\"}[1m])", namespace)
        );
        double avgMemory = prometheusMetricsService.averageRange(
                String.format("container_memory_usage_bytes{namespace=\"%s\"}", namespace)
        );
        double avgReplicas = prometheusMetricsService.averageRange(
                String.format("kube_deployment_status_replicas{namespace=\"%s\"}", namespace)
        );

        double[] objectives = new double[6];
        objectives[0] = metrics.getAverageResponseTime();
        objectives[1] = avgCpu;
        objectives[2] = avgMemory;
        objectives[3] = avgReplicas;
        objectives[4] = metrics.getErrorRate();
        objectives[5] = metrics.getAverageLatency();
        individual.setObjectives(objectives);

        log.info("🎯 Objectives for {}:\n  - avgResponseTime: {}\n  - avgCpu: {}\n  - avgMemory: {}\n  - avgReplicas: {}\n  - errorRate: {}\n  - avgLatency: {}",
                id, objectives[0], objectives[1], objectives[2], objectives[3], objectives[4], objectives[5]);

        log.info("✅ [DONE] Evaluation complete for {}:\n{}", id, individual);
        log.info("================================================================");
    }
}
