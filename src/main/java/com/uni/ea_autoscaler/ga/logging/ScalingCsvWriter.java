package com.uni.ea_autoscaler.ga.logging;

import com.uni.ea_autoscaler.ga.model.ScalingConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
public class ScalingCsvWriter {

    public void writeToCsv(Path filePath, List<ScalingConfiguration> configs, boolean append, boolean includeHeader) {
        try {
            Files.createDirectories(filePath.getParent());

            boolean writeHeader = includeHeader && (!Files.exists(filePath) || Files.size(filePath) == 0);

            try (FileWriter writer = new FileWriter(filePath.toFile(), append)) {
                if (writeHeader) {
                    writer.write("generation,minReplicas,maxReplicas,cpuThreshold,memoryThreshold,cooldownSeconds,cpuRequest,memoryRequest,objective0,objective1,objective2,objective3,objective4,objective5\n");
                }

                for (int i = 0; i < configs.size(); i++) {
                    ScalingConfiguration sc = configs.get(i);
                    double[] o = sc.getObjectives();
                    writer.write(String.format(
                            "%d,%d,%d,%.4f,%.4f,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.4f\n",
                            i + 1,
                            sc.getMinReplicas(),
                            sc.getMaxReplicas(),
                            sc.getCpuThreshold(),
                            sc.getMemoryThreshold(),
                            sc.getCooldownSeconds(),
                            sc.getCpuRequest(),
                            sc.getMemoryRequest(),
                            o[0], o[1], o[2], o[3], o[4], o[5]
                    ));
                }
            }

            log.info("📄 Successfully wrote {} individuals to {}", configs.size(), filePath);

        } catch (IOException e) {
            log.error("❌ Error writing to CSV file: {}", filePath, e);
        }
    }
}
