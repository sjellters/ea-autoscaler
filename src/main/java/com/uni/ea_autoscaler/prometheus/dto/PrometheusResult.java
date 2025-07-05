package com.uni.ea_autoscaler.prometheus.dto;

import java.util.List;
import java.util.Map;

public record PrometheusResult(
        Map<String, String> metric,
        List<String> value,
        List<List<Object>> values
) {
}
