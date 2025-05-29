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

    public double averageRange(String promQL, Instant start, Instant end, String step) {
        String startEpoch = String.valueOf(start.getEpochSecond());
        String endEpoch = String.valueOf(end.getEpochSecond());

        List<List<String>> values = prometheusClient.queryRangeMetric(promQL, startEpoch, endEpoch, step);
        return values.stream()
                .mapToDouble(point -> Double.parseDouble(point.get(1)))
                .average()
                .orElse(0.0);
    }
}
