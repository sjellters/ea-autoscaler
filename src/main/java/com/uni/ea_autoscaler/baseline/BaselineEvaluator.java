package com.uni.ea_autoscaler.baseline;

import com.uni.ea_autoscaler.jmeter.JMeterService;
import com.uni.ea_autoscaler.k8s.KubernetesPodTracker;
import com.uni.ea_autoscaler.ga.evaluation.ConfigurationEvaluator;
import com.uni.ea_autoscaler.ga.evaluation.penalty.PenaltyStrategy;
import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.prometheus.PrometheusMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;

@Slf4j
@Component
public class BaselineEvaluator extends ConfigurationEvaluator {

    public BaselineEvaluator(JMeterService jmeterService,
                             PrometheusMetricsService prometheusMetricsService,
                             PenaltyStrategy penaltyStrategy,
                             KubernetesPodTracker kubernetesPodTracker,
                             @Value("${evaluation.targetHost}") String targetHost,
                             @Value("${evaluation.targetPort}") String targetPort,
                             @Value("${evaluation.namespace}") String namespace,
                             @Value("${evaluation.testPlanPath}") String testPlanPath,
                             @Value("${k8s.deploymentName}") String deploymentName) {
        super(jmeterService, prometheusMetricsService, kubernetesPodTracker, penaltyStrategy, targetHost, targetPort,
                namespace, testPlanPath, deploymentName);
    }

    @Override
    protected void evaluate(ScalingConfiguration individual) {
        String id = generateEvaluationId();

        fillEvaluationData(individual, id, false);
        fillThresholds(individual);
    }

    private void fillThresholds(ScalingConfiguration individual) {
        LinkedHashMap<String, Double> metrics = individual.getMetrics();
        BaselineThresholds thresholds = BaselineThresholds.getInstance();

        thresholds.setP95Threshold(metrics.get("p95"));
        thresholds.setBaselineAvgCpu(metrics.get("avgCpu"));
        thresholds.setBaselineAvgMemory(metrics.get("avgMemory"));
    }
}
