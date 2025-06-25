package com.uni.ea_autoscaler.jmeter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
@Component
public class JMeterReportParser {

    public double parseAverageElapsedTime(Path resultsFile) {
        return averageFromColumn(resultsFile, 1, "elapsed time");
    }

    public double parseAverageLatency(Path resultsFile) {
        return averageFromColumn(resultsFile, 14, "latency");
    }

    public double parseErrorRate(Path resultsFile) {
        int[] counters = new int[2]; // [0] = total, [1] = failures

        processLines(resultsFile, parts -> parts.length >= 8, parts -> {
            counters[0]++;
            if (!"true".equalsIgnoreCase(parts[7])) {
                counters[1]++;
            }
        });

        if (counters[0] == 0) {
            log.warn("⚠️ No samples found when calculating error rate for {}", resultsFile);
            return 1.0;
        }

        return (double) counters[1] / counters[0];
    }

    private double averageFromColumn(Path file, int columnIndex, String metricName) {
        int[] count = {0};
        double[] total = {0};

        processLines(file, parts -> columnIndex < parts.length, parts -> {
            try {
                total[0] += Double.parseDouble(parts[columnIndex]);
                count[0]++;
            } catch (NumberFormatException ex) {
                log.warn("⚠️ Invalid number format in {} for column {}: {}", file, columnIndex, parts[columnIndex]);
            }
        });

        if (count[0] == 0) {
            log.warn("⚠️ No valid values found for {} in {}", metricName, file);

            return Double.MAX_VALUE;
        }

        return total[0] / count[0];
    }

    private void processLines(Path file, Predicate<String[]> validator, Consumer<String[]> handler) {
        try (var lines = Files.lines(file)) {
            lines.skip(1) // Skip header
                    .map(line -> line.split(","))
                    .filter(validator)
                    .forEach(handler);
        } catch (IOException e) {
            log.error("❌ Failed to process lines from {}: {}", file, e.getMessage(), e);
        }
    }
}
