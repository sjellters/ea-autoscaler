package com.uni.ea_autoscaler.jmeter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;

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
        int total = 0;
        int failures = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(resultsFile.toFile()))) {
            reader.readLine(); // Skip header

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length < 8) continue;

                total++;
                if (!"true".equalsIgnoreCase(parts[7])) {
                    failures++;
                }
            }

            if (total == 0) {
                log.warn("⚠️ No samples found when calculating error rate for {}", resultsFile);
                return 0.0;
            }

            return (double) failures / total;

        } catch (IOException e) {
            log.error("❌ Failed to parse error rate from {}: {}", resultsFile, e.getMessage(), e);
            return 1.0;
        }
    }

    private double averageFromColumn(Path file, int columnIndex, String metricName) {
        int count = 0;
        double total = 0;

        try (BufferedReader reader = new BufferedReader(new FileReader(file.toFile()))) {
            reader.readLine(); // Skip header

            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length <= columnIndex) continue;

                try {
                    total += Double.parseDouble(parts[columnIndex]);
                    count++;
                } catch (NumberFormatException ex) {
                    log.warn("⚠️ Invalid number format in {} for column {}: {}", file, columnIndex, parts[columnIndex]);
                }
            }

            if (count == 0) {
                log.warn("⚠️ No valid values found for {} in {}", metricName, file);
                return 0.0;
            }

            return total / count;

        } catch (IOException e) {
            log.error("❌ Failed to parse {} from {}: {}", metricName, file, e.getMessage(), e);
            return 0.0;
        }
    }
}
