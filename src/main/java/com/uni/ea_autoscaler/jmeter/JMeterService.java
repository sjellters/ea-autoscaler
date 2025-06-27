package com.uni.ea_autoscaler.jmeter;

import com.uni.ea_autoscaler.baseline.BaselineThresholds;
import com.uni.ea_autoscaler.jmeter.dto.JMeterResultMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class JMeterService {

    private final JMeterExecutor jmeterExecutor;
    private final JMeterReportParser jmeterReportParser;

    public JMeterResultMetrics runTest(String host, String port, String testPlanPath, String resultFile) {
        JMeterUtils.deleteAllJMeterArtifacts();

        Path outputDir = Path.of("jmeter-output");
        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            log.error("❌ Failed to create output directory: {}", e.getMessage(), e);

            return null;
        }

        String resultFilePath = outputDir.resolve(resultFile).toString();
        String logFilePath = outputDir.resolve("jmeter.log").toString();

        log.info("🧪 Running JMeter test: {}", resultFile);
        JMeterExecutionResult result = jmeterExecutor.runTest(host, port, testPlanPath, resultFilePath, logFilePath);

        if (!result.success()) {
            log.warn("⚠️ JMeter test failed. Result file: {}", resultFile);
            JMeterUtils.deleteAllJMeterArtifacts();

            return null;
        }

        Double avgResponseTime = jmeterReportParser.parseAverageElapsedTime(result.resultsFile());
        Double avgLatency = jmeterReportParser.parseAverageLatency(result.resultsFile());
        double errorRate = jmeterReportParser.parseErrorRate(result.resultsFile());

        BaselineThresholds thresholds = BaselineThresholds.getInstance();
        Double slaThreshold = thresholds.getP95Threshold();

        Double slaPercentage = slaThreshold != null
                ? jmeterReportParser.calculateSlaPercentage(result.resultsFile(), slaThreshold)
                : null;


        Double p95 = jmeterReportParser.calculatePercentile(result.resultsFile(), slaThreshold == null ? 0.5 : 0.95);

        return new JMeterResultMetrics(avgResponseTime, avgLatency, errorRate, slaPercentage, p95);
    }
}

