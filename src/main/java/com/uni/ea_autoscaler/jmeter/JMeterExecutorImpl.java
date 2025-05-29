package com.uni.ea_autoscaler.jmeter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Component
public class JMeterExecutorImpl implements JMeterExecutor {

    private static final String JMETER_BINARY = "jmeter";

    @Override
    public JMeterExecutionResult runTest(String targetHost, String targetPort, String testPlanPath, String resultFilePath) {
        List<String> command = List.of(
                JMETER_BINARY,
                "-n",
                "-t", testPlanPath,
                "-JtargetHost=" + targetHost,
                "-JtargetPort=" + targetPort,
                "-l", resultFilePath
        );

        log.info("🚀 Launching JMeter with command: {}", String.join(" ", command));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.inheritIO();

        Path resultPath = Path.of(resultFilePath);

        try {
            Process process = builder.start();
            int exitCode = process.waitFor();
            boolean success = exitCode == 0;

            if (success) {
                log.info("✅ JMeter test completed successfully");
            } else {
                log.warn("⚠️ JMeter exited with code {}", exitCode);
            }

            return new JMeterExecutionResult(success, resultPath);

        } catch (IOException e) {
            log.error("❌ IO error running JMeter: {}", e.getMessage(), e);
            return new JMeterExecutionResult(false, resultPath);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("❌ JMeter execution was interrupted", e);
            return new JMeterExecutionResult(false, resultPath);
        }
    }
}
