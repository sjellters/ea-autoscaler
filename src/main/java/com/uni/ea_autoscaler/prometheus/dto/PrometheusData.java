package com.uni.ea_autoscaler.prometheus.dto;

import java.util.List;

public record PrometheusData(
        String resultType,
        List<PrometheusResult> result
) {
}
