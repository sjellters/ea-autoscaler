package com.uni.ea_autoscaler.metrics.services;

import com.uni.ea_autoscaler.core.annotations.ValidatesMetrics;
import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.core.interfaces.MetricValidator;
import com.uni.ea_autoscaler.metrics.domain.ComputedMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MetricsValidatorService {

    private final Map<MetricName, List<MetricValidator>> validatorsByMetric;

    public MetricsValidatorService(List<MetricValidator> validators) {
        this.validatorsByMetric = validators.stream()
                .flatMap(v -> {
                    ValidatesMetrics annotation = v.getClass().getAnnotation(ValidatesMetrics.class);
                    if (annotation == null) {
                        throw new IllegalStateException("❌ Missing @ValidatesMetric on " + v.getClass().getName());
                    }
                    return Arrays.stream(annotation.value()).map(m -> Map.entry(m, v));
                })
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }

    public boolean isValid(ComputedMetrics computedMetrics) {
        List<MetricName> invalidMetrics = Arrays.stream(MetricName.values())
                .filter(name -> {
                    List<MetricValidator> applicable = validatorsByMetric.get(name);
                    if (applicable == null || applicable.isEmpty()) {
                        return false;
                    }
                    double value = computedMetrics.get(name);
                    return applicable.stream().anyMatch(v -> !v.isValid(name, value));
                })
                .toList();

        invalidMetrics.forEach(name -> {
            double value = computedMetrics.get(name);
            log.warn("⚠️ Metric {} is invalid with value {}", name, value);
        });

        return invalidMetrics.isEmpty();
    }
}


