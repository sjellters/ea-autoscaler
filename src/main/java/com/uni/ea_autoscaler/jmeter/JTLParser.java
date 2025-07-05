package com.uni.ea_autoscaler.jmeter;

import com.uni.ea_autoscaler.jmeter.dto.JTLParseResult;
import com.uni.ea_autoscaler.jmeter.dto.JTLSample;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class JTLParser {

    private final Path outputDir;

    public JTLParser(@Value("${jmeter.outputDir:jmeter-output}") Path outputDir) {
        this.outputDir = outputDir;
    }

    public JTLParseResult parse(String resultFileName) {
        List<JTLSample> samples = new ArrayList<>();

        Path resultFilePath = outputDir.resolve(resultFileName);

        try (Reader reader = Files.newBufferedReader(resultFilePath);
             CSVParser csv = CSVParser.builder()
                     .setFormat(
                             CSVFormat.DEFAULT.builder()
                                     .setHeader()
                                     .setSkipHeaderRecord(true)
                                     .get()
                     )
                     .setReader(reader)
                     .get()) {

            for (CSVRecord record : csv) {
                Long elapsed = parseLongOrNull(record.get("elapsed"));
                Long latency = parseLongOrNull(record.get("Latency"));
                Long connect = parseLongOrNull(record.get("Connect"));
                Long idleTime = parseLongOrNull(record.get("IdleTime"));
                Boolean success = parseBooleanOrNull(record.get("success"));

                samples.add(new JTLSample(elapsed, latency, connect, idleTime, success));
            }

        } catch (IOException e) {
            log.error("❌ Failed to read JTL file: {}", resultFileName, e);
            return new JTLParseResult(List.of(), false);
        }

        try {
            Files.deleteIfExists(resultFilePath);
            log.debug("🧹 Deleted processed JTL file: {}", resultFileName);
        } catch (IOException e) {
            log.warn("⚠️ Could not delete JTL file: {}", resultFileName, e);
        }

        boolean allValid = samples.stream().noneMatch(JTLSample::hasNullFields);
        return new JTLParseResult(samples, allValid);
    }

    private Long parseLongOrNull(String value) {
        try {
            return (value != null && !value.isBlank()) ? Long.parseLong(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean parseBooleanOrNull(String value) {
        if (value == null || value.isBlank()) return null;
        return Boolean.parseBoolean(value);
    }
}
