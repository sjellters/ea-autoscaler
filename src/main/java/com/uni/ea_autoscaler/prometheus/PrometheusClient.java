package com.uni.ea_autoscaler.prometheus;

import com.uni.ea_autoscaler.prometheus.dto.PrometheusResponse;
import com.uni.ea_autoscaler.prometheus.dto.PrometheusResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Slf4j
@Repository
public class PrometheusClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public PrometheusClient(
            RestTemplateBuilder builder,
            @Value("${prometheus.url}") String baseUrl
    ) {
        this.restTemplate = builder.build();
        this.baseUrl = baseUrl;
    }

    public List<List<String>> queryRangeMetric(String promQL, String start, String end, String step) {
        try {
            URI uri = buildQueryRangeUri(promQL, start, end, step);

            log.info("📡 Prometheus range query: start={}, end={}, step={}, query={}", start, end, step, promQL);

            ResponseEntity<PrometheusResponse> response = restTemplate.getForEntity(uri, PrometheusResponse.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<PrometheusResult> results = response.getBody().data().result();

                if (!results.isEmpty()) {
                    return results.get(0).values();
                } else {
                    log.warn("⚠️ No results found in Prometheus response.");
                }
            } else {
                log.warn("⚠️ Invalid Prometheus response: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ Error querying Prometheus with query: {}", promQL, e);
        }

        return Collections.emptyList();
    }

    private URI buildQueryRangeUri(String promQL, String start, String end, String step) {
        String encodedQuery = URLEncoder.encode(promQL, StandardCharsets.UTF_8).replace("+", "%20");
        String url = String.format(
                "%s/api/v1/query_range?query=%s&start=%s&end=%s&step=%s",
                baseUrl, encodedQuery, start, end, step
        );

        return URI.create(url);
    }
}
