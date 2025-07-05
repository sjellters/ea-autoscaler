package com.uni.ea_autoscaler.metrics.utils;

import com.uni.ea_autoscaler.jmeter.dto.JTLSample;

import java.util.List;
import java.util.OptionalLong;
import java.util.function.ToLongFunction;

public final class MetricsUtils {

    private MetricsUtils() {
        // prevent instantiation
    }

    public static double penalizedAverage(List<JTLSample> samples, ToLongFunction<JTLSample> extractor) {
        OptionalLong maxOpt = samples.stream()
                .filter(JTLSample::success)
                .mapToLong(extractor)
                .max();

        if (maxOpt.isEmpty()) {
            return Double.NaN;
        }

        long max = maxOpt.getAsLong();

        return samples.stream()
                .mapToLong(s -> s.success() ? extractor.applyAsLong(s) : max)
                .average()
                .orElse(Double.NaN);
    }

    public static double penalizedPercentile(List<JTLSample> samples, ToLongFunction<JTLSample> extractor, double percentile) {
        OptionalLong maxOpt = samples.stream()
                .filter(JTLSample::success)
                .mapToLong(extractor)
                .max();

        if (maxOpt.isEmpty()) {
            return Double.NaN;
        }

        long max = maxOpt.getAsLong();

        List<Long> adjusted = samples.stream()
                .map(s -> s.success() ? extractor.applyAsLong(s) : max)
                .sorted()
                .toList();

        int index = (int) Math.ceil(percentile * adjusted.size()) - 1;
        return adjusted.get(Math.max(0, Math.min(index, adjusted.size() - 1)));
    }
}
