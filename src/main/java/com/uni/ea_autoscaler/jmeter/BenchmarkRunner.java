package com.uni.ea_autoscaler.jmeter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class BenchmarkRunner {

    private final String jmeterCommand;
    private final Path testPlanPath;
    private final Path outputDir;
    private final int timeoutSeconds;

    public BenchmarkRunner(
            @Value("${jmeter.command:jmeter}") String jmeterCommand,
            @Value("${jmeter.testPlanPath}") Path testPlanPath,
            @Value("${jmeter.outputDir:jmeter-output}") Path outputDir,
            @Value("${jmeter.timeoutSeconds:600}") int timeoutSeconds
    ) {
        this.jmeterCommand = jmeterCommand;
        this.testPlanPath = testPlanPath;
        this.outputDir = outputDir;
        this.timeoutSeconds = timeoutSeconds;
    }

    public boolean run(String targetHost, String targetPort, String resultFileName) {
        if (!prepareCleanOutputDir()) return false;

        Path logPath = outputDir.resolve("jmeter.log");
        Path resultPath = outputDir.resolve(resultFileName);

        ProcessBuilder builder = prepareProcess(targetHost, targetPort, logPath, resultPath);
        return executeProcess(builder);
    }

    private ProcessBuilder prepareProcess(String host, String port, Path logPath, Path resultPath) {
        List<String> command = List.of(
                jmeterCommand,
                "-n",
                "-t", testPlanPath.toAbsolutePath().toString(),
                "-JtargetHost=" + host,
                "-JtargetPort=" + port,
                "-j", logPath.toAbsolutePath().toString(),
                "-l", resultPath.toAbsolutePath().toString(),
                "-f"
        );

        log.info("🚀 Launching JMeter with command: {}", String.join(" ", command));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(outputDir.toFile());
        builder.redirectOutput(outputDir.resolve("jmeter_stdout.log").toFile());
        builder.redirectError(outputDir.resolve("jmeter_stderr.log").toFile());

        return builder;
    }

    private boolean executeProcess(ProcessBuilder builder) {
        try {
            Process process = builder.start();

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (process.isAlive()) {
                    log.warn("🛑 JVM is shutting down. Killing JMeter process...");
                    process.destroy();
                }
            }));

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                log.warn("⏱️ JMeter process exceeded timeout ({}s). Killing it.", timeoutSeconds);
                process.destroyForcibly();

                return false;
            }
            int exitCode = process.exitValue();
            log.info("✅ JMeter finished with exit code {}", exitCode);

            return exitCode == 0;
        } catch (IOException | InterruptedException e) {
            log.error("❌ JMeter execution failed", e);
            return false;
        }
    }

    private boolean prepareCleanOutputDir() {
        try {
            Files.createDirectories(outputDir);

            try (var files = Files.list(outputDir)) {
                files.forEach(path -> {
                    try {
                        Files.deleteIfExists(path);
                        log.debug("🧹 Deleted: {}", path);
                    } catch (IOException e) {
                        log.warn("⚠️ Could not delete: {}", path, e);
                    }
                });
            }

            return true;
        } catch (IOException e) {
            log.error("❌ Could not prepare output directory: {}", outputDir, e);
            return false;
        }
    }
}
