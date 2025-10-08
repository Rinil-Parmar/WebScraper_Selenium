package com.example.service;

import com.example.model.Product;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Service
public class CsvExportService {

    private static final Logger logger = LoggerFactory.getLogger(CsvExportService.class);

    @Value("${app.scraper.csv-output-path:./output/Products_ALl_Task_.csv}")
    private String csvOutputPath;

    /**
     * Export products to CSV file
     */
    public String exportToCSV(List<Product> products) {
        if (products == null || products.isEmpty()) {
            logger.warn("No products to export");
            return null;
        }

        try {
            // Create output directory if it doesn't exist
            Path outputPath = Paths.get(csvOutputPath);
            Files.createDirectories(outputPath.getParent());

            // Define CSV headers - 6 columns now
            String[] headers = {
                    "Product Name", "Price", "Description", "Image URL", "Availability", "Category"
            };

            // Create CSV printer
            try (FileWriter writer = new FileWriter(csvOutputPath);
                 CSVPrinter csvPrinter = new CSVPrinter(writer,
                         CSVFormat.DEFAULT.builder()
                                 .setHeader(headers)
                                 .build())) {

                // Write product data - 6 fields now
                for (Product product : products) {
                    csvPrinter.printRecord(
                            product.getName(),
                            product.getPrice(),
                            product.getDescription(),
                            product.getImageUrl(),
                            product.getAvailability(),
                            product.getCategory()
                    );
                }

                csvPrinter.flush();
                logger.info("Successfully exported {} products to {}", products.size(), csvOutputPath);
                return csvOutputPath;

            }
        } catch (IOException e) {
            logger.error("Error exporting products to CSV", e);
            throw new RuntimeException("Failed to export CSV", e);
        }
    }

    /**
     * Append products to existing CSV file
     */
    public void appendToCSV(List<Product> products) {
        if (products == null || products.isEmpty()) {
            return;
        }

        try {
            Path outputPath = Paths.get(csvOutputPath);
            boolean fileExists = Files.exists(outputPath);

            try (FileWriter writer = new FileWriter(csvOutputPath, true);
                 CSVPrinter csvPrinter = new CSVPrinter(writer,
                         CSVFormat.DEFAULT.builder()
                                 .setSkipHeaderRecord(fileExists)
                                 .build())) {

                if (!fileExists) {
                    csvPrinter.printRecord("Product Name", "Price", "Description", "Image URL", "Availability", "Category");
                }

                for (Product product : products) {
                    csvPrinter.printRecord(
                            product.getName(),
                            product.getPrice(),
                            product.getDescription(),
                            product.getImageUrl(),
                            product.getAvailability(),
                            product.getCategory()
                    );
                }

                csvPrinter.flush();
                logger.info("Appended {} products to CSV", products.size());
            }
        } catch (IOException e) {
            logger.error("Error appending products to CSV", e);
            throw new RuntimeException("Failed to append to CSV", e);
        }
    }
}