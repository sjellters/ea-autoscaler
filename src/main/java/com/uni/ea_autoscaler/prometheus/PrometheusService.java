package com.uni.ea_autoscaler.prometheus;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PrometheusService {

    private final PrometheusClient prometheusClient;

    public PrometheusService(PrometheusClient prometheusClient) {
        this.prometheusClient = prometheusClient;
    }

    public void executeGlobalMetricsQuery() {
        Map<String, String> queries = new HashMap<>();
        queries.put("cpuUsage", "rate(container_cpu_usage_seconds_total{namespace=\"tesis-simulation\"}[1m])");
        queries.put("memoryUsage", "container_memory_usage_bytes{namespace=\"tesis-simulation\"}");
        queries.put("podsReady", "count(kube_pod_container_status_ready{namespace=\"tesis-simulation\",condition=\"true\"})");
        queries.put("replicasAvailable", "kube_deployment_status_replicas_available{namespace=\"tesis-simulation\"}");
        queries.put("cpuRequest", "kube_pod_container_resource_requests_cpu_cores{namespace=\"tesis-simulation\"}");
        queries.put("memoryRequest", "kube_pod_container_resource_requests_memory_bytes{namespace=\"tesis-simulation\"}");

        Map<String, Optional<Double>> results = prometheusClient.queryMultipleInstantMetrics(queries);

        results.forEach((clave, valor) ->
                System.out.println(clave + ": " + valor.orElse(null))
        );
    }
}
