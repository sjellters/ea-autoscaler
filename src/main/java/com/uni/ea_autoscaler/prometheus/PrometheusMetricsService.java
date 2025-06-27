package com.uni.ea_autoscaler.prometheus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrometheusMetricsService {

    private final PrometheusClient prometheusClient;

    public Double averageRange(String promQL, Instant start, Instant end, String step) {
        String startEpoch = String.valueOf(start.getEpochSecond());
        String endEpoch = String.valueOf(end.getEpochSecond());

        List<List<String>> values = prometheusClient.queryRangeMetric(promQL, startEpoch, endEpoch, step);

        if (values.isEmpty()) {
            log.warn("⚠️ No data returned from Prometheus for query: {}", promQL);

            return null;
        }

        return values.stream()
                .mapToDouble(point -> Double.parseDouble(point.get(1)))
                .filter(v -> v > 0)
                .average()
                .orElse(Double.MAX_VALUE);
    }
}
