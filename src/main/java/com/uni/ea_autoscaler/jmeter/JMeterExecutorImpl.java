package com.uni.ea_autoscaler.jmeter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class JMeterExecutorImpl implements JMeterExecutor {

    private static final String JMETER_BINARY = "jmeter";
    private static final int TIMEOUT_SECONDS = 600;

    @Override
    public JMeterExecutionResult runTest(String targetHost, String targetPort, String testPlanPath, String resultFilePath, String logFilePath) {
        List<String> command = buildCommand(targetHost, targetPort, testPlanPath, resultFilePath, logFilePath);

        log.info("🚀 Launching JMeter with command: {}", String.join(" ", command));

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.inheritIO();

        Path resultPath = Path.of(resultFilePath);
        Process process = null;

        try {
            process = builder.start();
            ProcessHandle handle = process.toHandle();

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);

            if (!finished) {
                log.warn("⏳ JMeter process timed out after {} seconds. Killing process tree...", TIMEOUT_SECONDS);

                handle.descendants().forEach(descendant -> {
                    log.debug("🔪 Killing descendant process: PID {}", descendant.pid());
                    descendant.destroyForcibly();
                });
                handle.destroyForcibly();

                return new JMeterExecutionResult(false, resultPath);
            }

            int exitCode = process.exitValue();
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
            log.warn("🛑 JMeter execution was interrupted. Destroying process...");
            process.destroyForcibly();
            Thread.currentThread().interrupt();
            return new JMeterExecutionResult(false, resultPath);
        }
    }

    private List<String> buildCommand(String targetHost, String targetPort, String testPlanPath, String resultFilePath, String logFilePath) {
        return List.of(
                JMETER_BINARY,
                "-n",
                "-t", testPlanPath,
                "-JtargetHost=" + targetHost,
                "-JtargetPort=" + targetPort,
                "-j", logFilePath,
                "-l", resultFilePath,
                "-f"
        );
    }
}
