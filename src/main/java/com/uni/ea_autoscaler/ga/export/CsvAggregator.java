package com.uni.ea_autoscaler.ga.export;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class CsvAggregator {

    public void aggregateGenerations(String outputDirName) throws IOException {
        File outputDir = new File(outputDirName);
        if (!outputDir.exists() || !outputDir.isDirectory()) {
            throw new IOException("No existe el directorio: " + outputDirName);
        }

        Pattern pattern = Pattern.compile("gen_(\\d+)\\.csv");
        File[] files = Optional.ofNullable(outputDir.listFiles())
                .map(Arrays::stream)
                .orElseThrow(() -> new IOException("No se pudo listar archivos en: " + outputDirName))
                .filter(f -> pattern.matcher(f.getName()).matches())
                .sorted(Comparator.comparingInt(f -> extractGenerationNumber(f.getName(), pattern)))
                .toArray(File[]::new);

        if (files.length == 0) {
            throw new IOException("No se encontraron archivos gen_*.csv en: " + outputDirName);
        }

        File aggregatedFile = new File(outputDir, "all_generations.csv");

        try (BufferedWriter writer = Files.newBufferedWriter(aggregatedFile.toPath());
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().get())) {

            boolean headerWritten = false;

            for (File file : files) {
                int generation = extractGenerationNumber(file.getName(), pattern);

                // Lee encabezado
                String[] headers;
                try (BufferedReader headerReader = Files.newBufferedReader(file.toPath())) {
                    String headerLine = headerReader.readLine();
                    if (headerLine == null) continue;
                    headers = headerLine.split(",");
                }

                CSVFormat parserFormat = CSVFormat.DEFAULT.builder()
                        .setHeader(headers)
                        .setSkipHeaderRecord(true)
                        .get();

                try (BufferedReader dataReader = Files.newBufferedReader(file.toPath());
                     CSVParser parser = CSVParser.parse(dataReader, parserFormat)) {

                    if (!headerWritten) {
                        List<String> newHeader = new ArrayList<>();
                        newHeader.add("generation");
                        newHeader.addAll(parser.getHeaderNames());
                        printer.printRecord(newHeader);
                        headerWritten = true;
                    }

                    for (CSVRecord record : parser) {
                        List<String> row = new ArrayList<>();
                        row.add(String.valueOf(generation));
                        record.forEach(row::add);
                        printer.printRecord(row);
                    }
                }
            }
        }
    }

    private int extractGenerationNumber(String filename, Pattern pattern) {
        Matcher matcher = pattern.matcher(filename);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : -1;
    }
}
