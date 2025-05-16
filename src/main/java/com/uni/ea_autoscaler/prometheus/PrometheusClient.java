package com.uni.ea_autoscaler.prometheus;

import com.uni.ea_autoscaler.prometheus.dto.PrometheusResponse;
import com.uni.ea_autoscaler.prometheus.dto.PrometheusResult;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class PrometheusClient {

    private final RestTemplate restTemplate;
    private final String PROMETHEUS_BASE_URL = "http://localhost:30090";

    public PrometheusClient(RestTemplateBuilder builder) {
        this.restTemplate = builder.build();
    }

    public Map<String, Optional<Double>> queryMultipleInstantMetrics(Map<String, String> metricQueries) {
        Map<String, Optional<Double>> results = new HashMap<>();
        for (Map.Entry<String, String> entry : metricQueries.entrySet()) {
            results.put(entry.getKey(), queryInstantMetric(entry.getValue()));
        }
        return results;
    }

    public Optional<Double> queryInstantMetric(String promQL) {
        String url = String.format("%s/api/v1/query?query=%s", PROMETHEUS_BASE_URL, URLEncoder.encode(promQL, StandardCharsets.UTF_8));
        ResponseEntity<PrometheusResponse> response = restTemplate.getForEntity(url, PrometheusResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            List<PrometheusResult> results = response.getBody().getData().getResult();
            if (!results.isEmpty()) {
                String valueStr = results.get(0).getValue().get(1);
                return Optional.of(Double.parseDouble(valueStr));
            }
        }
        return Optional.empty();
    }

    public List<List<String>> queryRangeMetric(String promQL, String start, String end, String step) {
        String url = String.format(
                "%s/api/v1/query_range?query=%s&start=%s&end=%s&step=%s",
                PROMETHEUS_BASE_URL,
                URLEncoder.encode(promQL, StandardCharsets.UTF_8),
                start,
                end,
                step
        );

        ResponseEntity<PrometheusResponse> response = restTemplate.getForEntity(url, PrometheusResponse.class);

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            List<PrometheusResult> results = response.getBody().getData().getResult();
            if (!results.isEmpty()) {
                return results.get(0).getValues();
            }
        }

        return List.of();
    }
}
