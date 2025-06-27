package com.uni.ea_autoscaler.ga.logging;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

@Slf4j
@Component
public class ScalingCsvWriter {

    public void writeToCsv(Path filePath, List<ScalingConfiguration> configs, Integer generation, boolean append, boolean includeHeader) {
        try {
            Files.createDirectories(filePath.getParent());

            boolean writeHeader = includeHeader && (!Files.exists(filePath) || Files.size(filePath) == 0);

            try (FileWriter writer = new FileWriter(filePath.toFile(), append)) {
                if (writeHeader && !configs.isEmpty()) {
                    StringBuilder header = new StringBuilder("generation,minReplicas,maxReplicas,cpuThreshold,memoryThreshold,cooldownSeconds,cpuRequest,memoryRequest");
                    LinkedHashMap<String, Double> objectives = configs.get(0).getObjectivesMap();
                    for (String name : objectives.keySet()) {
                        header.append(",").append(name);
                    }
                    header.append("\n");
                    writer.write(header.toString());
                }

                for (int i = 0; i < configs.size(); i++) {
                    ScalingConfiguration sc = configs.get(i);
                    LinkedHashMap<String, Double> objectives = sc.getObjectivesMap();

                    StringBuilder row = new StringBuilder();
                    row.append(generation != null ? generation : i + 1).append(",")
                            .append(sc.getMinReplicas()).append(",")
                            .append(sc.getMaxReplicas()).append(",")
                            .append(sc.getCpuThreshold()).append(",")
                            .append(sc.getMemoryThreshold()).append(",")
                            .append(sc.getCooldownSeconds()).append(",")
                            .append(sc.getCpuRequest()).append(",")
                            .append(sc.getMemoryRequest());

                    for (Double v : objectives.values()) {
                        row.append(",").append(v != null ? String.valueOf(v) : "");
                    }

                    row.append("\n");
                    writer.write(row.toString());
                }
            }

            log.info("📄 Successfully wrote {} individuals to {}", configs.size(), filePath);

        } catch (IOException e) {
            log.error("❌ Error writing to CSV file: {}", filePath, e);
        }
    }
}
