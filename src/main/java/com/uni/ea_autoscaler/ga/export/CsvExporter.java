package com.uni.ea_autoscaler.ga.export;

import com.uni.ea_autoscaler.common.ResourceScalingConfig;
import com.uni.ea_autoscaler.core.enums.MetricName;
import com.uni.ea_autoscaler.core.enums.ObjectiveName;
import com.uni.ea_autoscaler.ga.model.Individual;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Component
public class CsvExporter {

    private File outputDir;

    public void export(String fileName, List<Individual> individuals) throws IOException {
        if (individuals == null || individuals.isEmpty()) return;
        if (outputDir == null) initOutputDirectory();

        File csvFile = new File(outputDir, fileName);

        List<String> headers = new ArrayList<>(List.of(
                "minReplicas", "maxReplicas",
                "cpuThreshold", "memoryThreshold", "stabilizationWindowSeconds",
                "cpuRequest", "memoryRequest"
        ));

        List<String> computedMetricKeys = Arrays.stream(MetricName.values())
                .map(Enum::name)
                .sorted()
                .toList();
        headers.addAll(computedMetricKeys);

        List<String> rawObjectiveKeys = Arrays.stream(ObjectiveName.values())
                .map(name -> "raw_" + name.name())
                .sorted()
                .toList();
        headers.addAll(rawObjectiveKeys);

        List<String> penalizedObjectiveKeys = Arrays.stream(ObjectiveName.values())
                .map(name -> "penalized_" + name.name())
                .sorted()
                .toList();
        headers.addAll(penalizedObjectiveKeys);

        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(headers.toArray(String[]::new))
                .setSkipHeaderRecord(false)
                .get();

        try (CSVPrinter printer = new CSVPrinter(new FileWriter(csvFile), csvFormat)) {
            for (Individual individual : individuals) {
                List<String> row = new ArrayList<>();

                ResourceScalingConfig config = individual.getConfig();
                row.add(String.valueOf(config.getHpa().getMinReplicas()));
                row.add(String.valueOf(config.getHpa().getMaxReplicas()));
                row.add(format(config.getHpa().getCpuThreshold()));
                row.add(format(config.getHpa().getMemoryThreshold()));
                row.add(String.valueOf(config.getHpa().getStabilizationWindowSeconds()));
                row.add(String.valueOf(config.getDeployment().getCpuRequest()));
                row.add(String.valueOf(config.getDeployment().getMemoryRequest()));

                Map<MetricName, Double> computed = new TreeMap<>(individual.getComputedMetrics());
                for (String key : computedMetricKeys) {
                    row.add(format(computed.getOrDefault(MetricName.valueOf(key), null)));
                }

                Map<ObjectiveName, Double> raw = new TreeMap<>(individual.getRawObjectives());
                for (String key : rawObjectiveKeys) {
                    ObjectiveName obj = ObjectiveName.valueOf(key.replace("raw_", ""));
                    row.add(format(raw.getOrDefault(obj, null)));
                }

                Map<ObjectiveName, Double> penalized = new TreeMap<>(individual.getObjectives());
                for (String key : penalizedObjectiveKeys) {
                    ObjectiveName obj = ObjectiveName.valueOf(key.replace("penalized_", ""));
                    row.add(format(penalized.getOrDefault(obj, null)));
                }

                printer.printRecord(row);
            }
        }
    }

    private String format(Double value) {
        if (value == null) return "";
        return new BigDecimal(value.toString()).stripTrailingZeros().toPlainString();
    }

    private File prepareOutputDirectory() throws IOException {
        File baseDir = new File("output");

        if (!baseDir.exists()) {
            if (!baseDir.mkdirs()) throw new IOException("Failed to create directory: " + "output");
            return baseDir;
        }

        if (Objects.requireNonNull(baseDir.list()).length == 0) {
            return baseDir;
        }

        int suffix = 1;
        File renamed;
        do {
            renamed = new File("output" + "_" + suffix++);
        } while (renamed.exists());

        if (!baseDir.renameTo(renamed)) {
            throw new IOException("Failed to rename " + "output" + " to " + renamed.getName());
        }

        File newBaseDir = new File("output");
        if (!newBaseDir.mkdirs()) throw new IOException("Failed to create new directory: " + "output");

        return newBaseDir;
    }

    public void initOutputDirectory() throws IOException {
        if (outputDir != null) return;

        outputDir = prepareOutputDirectory();
    }
}
