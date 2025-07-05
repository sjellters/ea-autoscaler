package com.uni.ea_autoscaler.evaluation;

import com.uni.ea_autoscaler.common.BaselineValues;
import com.uni.ea_autoscaler.common.ResourceScalingConfig;
import com.uni.ea_autoscaler.evaluation.exception.EvaluationRetryableException;
import com.uni.ea_autoscaler.jmeter.BenchmarkRunner;
import com.uni.ea_autoscaler.jmeter.JTLParser;
import com.uni.ea_autoscaler.jmeter.dto.JTLParseResult;
import com.uni.ea_autoscaler.k8s.KubernetesWorkloadOrchestrator;
import com.uni.ea_autoscaler.metrics.domain.ComputeMetricsInput;
import com.uni.ea_autoscaler.metrics.domain.ComputedMetrics;
import com.uni.ea_autoscaler.metrics.services.MetricsService;
import com.uni.ea_autoscaler.metrics.services.MetricsValidatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;

@Slf4j
@Service
public class EvaluationRunner {

    private final KubernetesWorkloadOrchestrator orchestrator;
    private final BenchmarkRunner benchmarkRunner;
    private final JTLParser jtlParser;
    private final MetricsService metricsService;
    private final MetricsValidatorService validatorService;
    private final PrometheusQueryService prometheusQueryService;

    private final String targetHost;
    private final String targetPort;
    private final String resultFileName;

    public EvaluationRunner(
            KubernetesWorkloadOrchestrator orchestrator,
            BenchmarkRunner benchmarkRunner,
            JTLParser jtlParser,
            MetricsService metricsService,
            MetricsValidatorService validatorService,
            PrometheusQueryService prometheusQueryService,
            @Value("${jmeter.targetHost:127.0.0.1}") String targetHost,
            @Value("${jmeter.targetPort:8080}") String targetPort,
            @Value("${jmeter.resultFileName:result.jtl}") String resultFileName
    ) {
        this.orchestrator = orchestrator;
        this.benchmarkRunner = benchmarkRunner;
        this.jtlParser = jtlParser;
        this.metricsService = metricsService;
        this.validatorService = validatorService;
        this.prometheusQueryService = prometheusQueryService;
        this.targetHost = targetHost;
        this.targetPort = targetPort;
        this.resultFileName = resultFileName;
    }

    public ComputedMetrics runEvaluation(ResourceScalingConfig config, BaselineValues baselineValues) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                orchestrator.prepareEnvironment(config);

                Instant start = Instant.now();
                boolean success = benchmarkRunner.run(targetHost, targetPort, resultFileName);
                Instant end = Instant.now();

                if (!success) {
                    throw new EvaluationRetryableException("JMeter benchmark failed. Skipping metrics computation.");
                }

                JTLParseResult jmeterMetrics = jtlParser.parse(resultFileName);

                if (!jmeterMetrics.valid()) {
                    throw new EvaluationRetryableException("JTL parsing failed or returned invalid data. Skipping metrics computation.");
                }

                Set<String> pods = orchestrator.getPodTracker().getTrackedPods();
                PrometheusMetrics prometheusMetrics = prometheusQueryService.queryMetrics(start, end, pods);

                if (!prometheusMetrics.valid()) {
                    throw new EvaluationRetryableException("Prometheus metrics query failed or returned invalid data. Skipping metrics computation.");
                }

                ComputeMetricsInput input = new ComputeMetricsInput(config, baselineValues, jmeterMetrics, prometheusMetrics);
                ComputedMetrics computed = metricsService.compute(input);

                if (!validatorService.isValid(computed)) {
                    throw new EvaluationRetryableException("Computed metrics are invalid after validation. Skipping evaluation.");
                }

                return computed;
            } catch (EvaluationRetryableException e) {
                log.warn("⚠️ Attempt #{} failed for config: {}", attempt, e.getMessage());
            }
        }

        log.error("❌ Evaluation failed after 2 attempts for config: {}", config);
        throw new EvaluationRetryableException("❌ Both evaluation attempts failed.");
    }
}
