package com.uni.ea_autoscaler.jmeter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

@Slf4j
@Component
public class JMeterReportParser {

    public Double parseAverageElapsedTime(Path resultsFile) {
        return averageFromColumnWithErrorPenalty(resultsFile, 1);
    }

    public Double parseAverageLatency(Path resultsFile) {
        return averageFromColumnWithErrorPenalty(resultsFile, 14);
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

    public double calculateSlaPercentage(Path resultsFile, double slaThreshold) {
        int[] total = {0};
        int[] passed = {0};

        processLines(resultsFile, parts -> parts.length > 8, parts -> {
            total[0]++;
            boolean isSuccess = "true".equalsIgnoreCase(parts[7]);
            try {
                double elapsed = Double.parseDouble(parts[1]);
                if (isSuccess && elapsed <= slaThreshold) {
                    passed[0]++;
                }
            } catch (NumberFormatException ignored) {
            }
        });

        if (total[0] == 0) {
            log.warn("⚠️ No valid samples found for SLA calculation in {}", resultsFile);
            return 0.0;
        }

        return (double) passed[0] / total[0];
    }

    public Double calculatePercentile(Path resultsFile, double percentile) {
        return calculatePercentileWithPenalty(resultsFile, percentile);
    }

    private Double averageFromColumnWithErrorPenalty(Path file, int columnIndex) {
        List<String[]> lines = collectLines(file);
        if (lines.isEmpty()) return null;

        double maxValid = lines.stream()
                .filter(parts -> parts.length > 8 && "true".equalsIgnoreCase(parts[7]))
                .mapToDouble(parts -> {
                    try {
                        return Double.parseDouble(parts[columnIndex]);
                    } catch (NumberFormatException ignored) {
                        return 0;
                    }
                }).max().orElse(0);

        int count = 0;
        double total = 0;

        for (String[] parts : lines) {
            if (parts.length <= 8) continue;
            try {
                boolean success = "true".equalsIgnoreCase(parts[7]);
                double value = Double.parseDouble(parts[columnIndex]);
                total += success ? value : maxValid;
                count++;
            } catch (NumberFormatException ignored) {
            }
        }

        return count == 0 ? null : total / count;
    }

    private Double calculatePercentileWithPenalty(Path resultsFile, double percentile) {
        List<String[]> lines = collectLines(resultsFile);
        if (lines.isEmpty()) return null;

        List<Double> responseTimes = new ArrayList<>();
        double maxValid = 0;

        for (String[] parts : lines) {
            if (parts.length <= 8) continue;
            try {
                double elapsed = Double.parseDouble(parts[1]);
                boolean success = "true".equalsIgnoreCase(parts[7]);

                if (success) {
                    responseTimes.add(elapsed);
                    if (elapsed > maxValid) {
                        maxValid = elapsed;
                    }
                } else {
                    responseTimes.add(null);
                }
            } catch (NumberFormatException ex) {
                log.warn("⚠️ Invalid elapsed time value in {}: {}", resultsFile, parts[1]);
            }
        }

        if (responseTimes.isEmpty() || maxValid == 0) {
            log.warn("⚠️ No valid elapsed times found for percentile calculation in {}", resultsFile);
            return null;
        }

        for (int i = 0; i < responseTimes.size(); i++) {
            if (responseTimes.get(i) == null) {
                responseTimes.set(i, maxValid);
            }
        }

        responseTimes.sort(Double::compare);
        int index = (int) Math.ceil(percentile * responseTimes.size()) - 1;

        return responseTimes.get(Math.max(0, Math.min(index, responseTimes.size() - 1)));
    }

    private void processLines(Path file, Predicate<String[]> validator, Consumer<String[]> handler) {
        try (var lines = Files.lines(file)) {
            lines.skip(1)
                    .map(line -> line.split(","))
                    .filter(validator)
                    .forEach(handler);
        } catch (IOException e) {
            log.error("❌ Failed to process lines from {}: {}", file, e.getMessage(), e);
        }
    }

    private List<String[]> collectLines(Path file) {
        List<String[]> lines = new ArrayList<>();
        processLines(file, parts -> true, lines::add);
        return lines;
    }
}
