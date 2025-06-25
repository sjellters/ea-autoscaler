package com.uni.ea_autoscaler.jmeter;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

@Slf4j
public class JMeterUtils {

    public static void deleteAllJMeterArtifacts() {
        Path outputDir = Path.of("jmeter-output");

        if (!Files.exists(outputDir)) {
            log.info("🗑️ No JMeter output directory found to delete: {}", outputDir);

            return;
        }

        try (Stream<Path> files = Files.list(outputDir)) {
            files.forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                    log.info("🗑️ Deleted file: {}", path.getFileName());
                } catch (IOException e) {
                    log.warn("⚠️ Failed to delete file {}: {}", path.getFileName(), e.getMessage());
                }
            });
        } catch (IOException e) {
            log.warn("⚠️ Could not list files in jmeter-output: {}", e.getMessage());
        }
    }
}

