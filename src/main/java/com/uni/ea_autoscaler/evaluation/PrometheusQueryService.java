package com.uni.ea_autoscaler.evaluation;

import com.uni.ea_autoscaler.prometheus.PrometheusClient;
import com.uni.ea_autoscaler.prometheus.PrometheusResultProcessor;
import com.uni.ea_autoscaler.prometheus.dto.PrometheusQueryResult;
import com.uni.ea_autoscaler.prometheus.dto.PrometheusResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Slf4j
@Service
public class PrometheusQueryService {

    private final PrometheusClient client;
    private final PrometheusResultProcessor processor;
    private final String namespace;
    private final String deploymentName;

    public PrometheusQueryService(PrometheusClient client,
                                  PrometheusResultProcessor processor,
                                  @Value("${k8s.namespace}") String namespace,
                                  @Value("${k8s.deploymentName}") String deploymentName) {
        this.client = client;
        this.processor = processor;
        this.namespace = namespace;
        this.deploymentName = deploymentName;
    }

    public PrometheusMetrics queryMetrics(Instant start, Instant end, Set<String> pods) {
        if (pods == null || pods.isEmpty()) {
            log.error("\uD83D\uDD34 No pods provided for metric query. Skipping.");
            return null;
        }

        log.info("🔍 Querying metrics from {} to {} for pods: {}", start, end, pods);

        String podRegex = String.join("|", pods);
        PrometheusQueryResult cpuResult = queryCpu(start, end, podRegex);
        PrometheusQueryResult memoryResult = queryMemory(start, end, podRegex);
        PrometheusQueryResult replicasResult = queryReplicas(start, end);

        return new PrometheusMetrics(cpuResult, memoryResult, replicasResult);
    }

    private PrometheusQueryResult queryCpu(Instant start, Instant end, String podRegex) {
        long windowSeconds = Duration.between(start, end).toSeconds();
        String query = String.format(
                "sum(rate(container_cpu_usage_seconds_total{namespace=\"%s\", pod=~\"%s\"}[%ds]))",
                namespace, podRegex, windowSeconds
        );
        return executeQuery(query, start, end);
    }

    public PrometheusQueryResult queryMemory(Instant start, Instant end, String podRegex) {
        String query = String.format(
                "avg(container_memory_usage_bytes{namespace=\"%s\", pod=~\"%s\"})",
                namespace, podRegex
        );
        return executeQuery(query, start, end);
    }

    public PrometheusQueryResult queryReplicas(Instant start, Instant end) {
        String query = String.format("avg(kube_deployment_status_replicas{namespace=\"%s\", deployment=\"%s\"})",
                namespace,
                deploymentName);
        return executeQuery(query, start, end);
    }

    private PrometheusQueryResult executeQuery(String query, Instant start, Instant end) {
        PrometheusResponse response = client.queryRange(query, start, end, "15s");
        return processor.process(response);
    }
}
