package com.uni.ea_autoscaler.ga.evaluation;

import com.uni.ea_autoscaler.cache.EvaluationCache;
import com.uni.ea_autoscaler.cache.ScalingKey;
import com.uni.ea_autoscaler.ga.evaluation.penalty.PenaltyStrategy;
import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import com.uni.ea_autoscaler.jmeter.JMeterService;
import com.uni.ea_autoscaler.k8s.KubernetesPodTracker;
import com.uni.ea_autoscaler.k8s.KubernetesScalingService;
import com.uni.ea_autoscaler.prometheus.PrometheusMetricsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Objects;

@Slf4j
@Component
public class FitnessEvaluator extends ConfigurationEvaluator {

    private final KubernetesScalingService kubernetesScalingService;
    private final EvaluationCache evaluationCache;

    public FitnessEvaluator(JMeterService jmeterService,
                            PrometheusMetricsService prometheusMetricsService,
                            KubernetesScalingService kubernetesScalingService,
                            PenaltyStrategy penaltyStrategy,
                            @Qualifier("compositeEvaluationCache") EvaluationCache evaluationCache,
                            KubernetesPodTracker kubernetesPodTracker,
                            @Value("${evaluation.targetHost}") String targetHost,
                            @Value("${evaluation.targetPort}") String targetPort,
                            @Value("${evaluation.namespace}") String namespace,
                            @Value("${evaluation.testPlanPath}") String testPlanPath,
                            @Value("${k8s.deploymentName}") String deploymentName) {
        super(jmeterService, prometheusMetricsService, kubernetesPodTracker, penaltyStrategy, targetHost, targetPort,
                namespace, testPlanPath, deploymentName);
        this.kubernetesScalingService = kubernetesScalingService;
        this.evaluationCache = evaluationCache;
    }

    private void checkCacheAndReuse(ScalingConfiguration individual, String id) {
        ScalingKey key = new ScalingKey(individual);

        ScalingConfiguration cached = evaluationCache.getConfiguration(key);

        if (cached != null) {
            logSeparator();
            log.info("♻️ Cached evaluation reused for {}.", id);

            individual.setMetrics(new LinkedHashMap<>(cached.getMetrics()));
        }
    }


    private boolean applyScaling(ScalingConfiguration individual, String id) {
        logSeparator();
        log.info("🧬 Starting evaluation for {}:", id);
        log.info("Configuration:\n{}", individual);

        boolean success = kubernetesScalingService.applyAndWait(individual);

        if (!success) {
            log.warn("⚠️ Scaling application failed for {}", id);
            applyDiscardPenalty(individual, id);

            return false;
        }

        return true;
    }

    public void evaluate(ScalingConfiguration individual) {
        String id = generateEvaluationId();

        if (!applyScaling(individual, id)) return;
        checkCacheAndReuse(individual, id);

        fillEvaluationData(individual, id, true);

        if (individual.getMetrics().isEmpty()) {
            log.warn("⚠️ No metrics collected for {}. Skipping storage.", id);
            return;
        }

        if (individual.getMetrics().values().stream().anyMatch(Objects::isNull)) {
            log.warn("⚠️ Some metrics are null for {}. Skipping storage.", id);
            return;
        }

        evaluationCache.storeConfiguration(new ScalingKey(individual), individual);
    }
}
