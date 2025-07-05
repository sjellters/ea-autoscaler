package com.uni.ea_autoscaler.metrics.services;

import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.core.interfaces.Metric;
import com.uni.ea_autoscaler.metrics.domain.ComputeMetricsInput;
import com.uni.ea_autoscaler.metrics.domain.ComputedMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetricsService {

    private final Map<MetricName, Metric> metricMap;

    public MetricsService(List<Metric> metrics) {
        this.metricMap = metrics.stream()
                .collect(Collectors.toMap(Metric::name, m -> m));
    }

    public ComputedMetrics compute(ComputeMetricsInput input) {
        if (input.isInvalid()) {
            throw new IllegalArgumentException("❌ Invalid input for metrics computation");
        }

        return new ComputedMetrics(input, List.copyOf(metricMap.values()));
    }
}

