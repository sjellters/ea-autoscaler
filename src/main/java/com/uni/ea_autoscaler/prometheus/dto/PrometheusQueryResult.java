package com.uni.ea_autoscaler.prometheus.dto;

import com.uni.ea_autoscaler.core.interfaces.Validatable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record PrometheusQueryResult(
        List<PrometheusResult> results,
        boolean valid
) implements Validatable {
    private static Double parseDoubleSafe(Object raw) {
        try {
            return raw != null ? Double.parseDouble(raw.toString()) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public List<Double> flattenedValues() {
        return results == null ? List.of() : results.stream()
                .flatMap(r -> r.values() != null ? r.values().stream() : Stream.empty())
                .map(v -> parseDoubleSafe(v.get(1)))
                .filter(Objects::nonNull)
                .toList();
    }
}
