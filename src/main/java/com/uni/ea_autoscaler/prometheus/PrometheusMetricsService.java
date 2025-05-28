package com.uni.ea_autoscaler.prometheus;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class PrometheusMetricsService {

    private final PrometheusClient prometheusClient;

    public PrometheusMetricsService(PrometheusClient prometheusClient) {
        this.prometheusClient = prometheusClient;
    }

    public double averageRange(String promQL) {
        String end = String.valueOf(Instant.now().getEpochSecond());
        String start = String.valueOf(Instant.now().minusSeconds(300).getEpochSecond());
        String step = "15s";

        List<List<String>> values = prometheusClient.queryRangeMetric(promQL, start, end, step);
        return values.stream()
                .mapToDouble(point -> Double.parseDouble(point.get(1)))
                .average()
                .orElse(0.0);
    }
}
