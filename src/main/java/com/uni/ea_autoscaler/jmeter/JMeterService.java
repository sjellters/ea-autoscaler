package com.uni.ea_autoscaler.jmeter;

import com.uni.ea_autoscaler.jmeter.dto.JMeterResultMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;

@Slf4j
@Service
public class JMeterService {

    private final JMeterExecutor jmeterExecutor;
    private final JMeterReportParser jmeterReportParser;

    public JMeterService(JMeterExecutor jmeterExecutor, JMeterReportParser jmeterReportParser) {
        this.jmeterExecutor = jmeterExecutor;
        this.jmeterReportParser = jmeterReportParser;
    }

    public JMeterResultMetrics runTest(String host, String port, String testPlanPath, String resultFile) {
        log.info("🧪 Running JMeter test: {}", resultFile);
        JMeterExecutionResult result = jmeterExecutor.runTest(host, port, testPlanPath, resultFile);

        if (!result.success()) {
            log.warn("⚠️ JMeter test failed. Result file: {}", resultFile);
            return null;
        }

        double avgResponseTime = jmeterReportParser.parseAverageElapsedTime(result.resultsFile());
        double avgLatency = jmeterReportParser.parseAverageLatency(result.resultsFile());
        double errorRate = jmeterReportParser.parseErrorRate(result.resultsFile());

        try {
            Files.deleteIfExists(result.resultsFile());
            log.debug("🧹 Deleted JTL result file: {}", result.resultsFile());
        } catch (IOException e) {
            log.warn("⚠️ Could not delete JTL file: {}", result.resultsFile(), e);
        }

        return new JMeterResultMetrics(avgResponseTime, avgLatency, errorRate);
    }
}

