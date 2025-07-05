package com.uni.ea_autoscaler.ga.export;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CsvAggregatorTest {

    @Test
    void testAggregateGenerations_createsValidConsolidatedCsv() throws Exception {
        CsvAggregator aggregator = new CsvAggregator();

        // Ejecuta el merge
        aggregator.aggregateGenerations("output");

        File resultFile = Path.of("output", "all_generations.csv").toFile();
        assertTrue(resultFile.exists(), "El archivo all_generations.csv no fue creado");

        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader() // indica que el encabezado está en la primera fila del archivo
                .setSkipHeaderRecord(true)
                .get();
        // Lee el CSV consolidado
        try (CSVParser parser = CSVParser.parse(new FileReader(resultFile), format)) {
            List<CSVRecord> records = parser.getRecords();
            assertFalse(records.isEmpty(), "El CSV consolidado está vacío");

            List<String> headers = parser.getHeaderNames();
            assertFalse(headers.isEmpty(), "El CSV no tiene encabezados");
            assertEquals("generation", headers.get(0), "La primera columna debe ser 'generation'");
        }
    }
}