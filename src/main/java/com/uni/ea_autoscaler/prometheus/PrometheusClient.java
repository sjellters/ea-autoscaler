package com.uni.ea_autoscaler.prometheus;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uni.ea_autoscaler.prometheus.dto.PrometheusResponse;
import com.uni.ea_autoscaler.prometheus.exception.PrometheusQueryException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

@Slf4j
@Repository
public class PrometheusClient {

    private final String baseUrl;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public PrometheusClient(@Value("${prometheus.protocol:http}") String protocol,
                            @Value("${prometheus.host:127.0.0.1}") String host,
                            @Value("${prometheus.port}") String port,
                            ObjectMapper objectMapper) {
        this.baseUrl = String.format("%s://%s:%s/api/v1", protocol, host, port);
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newHttpClient();
    }

    public PrometheusResponse queryRange(String promql, Instant start, Instant end, String step) {
        String url = String.format(
                "%s/query_range?query=%s&start=%d&end=%d&step=%s",
                baseUrl,
                encode(promql),
                start.getEpochSecond(),
                end.getEpochSecond(),
                step
        );

        log.debug("📡 Querying Prometheus: {}", url);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return objectMapper.readValue(response.body(), PrometheusResponse.class);
        } catch (IOException | InterruptedException e) {
            throw new PrometheusQueryException("❌ Error querying Prometheus", e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
