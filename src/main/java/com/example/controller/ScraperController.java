package com.example.controller;

import com.example.model.Product;
import com.example.service.ScraperService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/scraper")
public class ScraperController {

    private static final Logger logger = LoggerFactory.getLogger(ScraperController.class);

    @Autowired
    private ScraperService scraperService;

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("message", "FreshCo Scraper is running");
        return ResponseEntity.ok(response);
    }

    /**
     * Scrape all categories - SUPPORTS GET REQUEST
     * URL: http://localhost:8080/api/scraper/scrape
     */
    @GetMapping("/scrape")
    public ResponseEntity<Map<String, Object>> scrapeAllCategoriesGet() {
        logger.info("Starting scraping of all categories (GET request)");
        return performScraping();
    }

    /**
     * Scrape all categories - SUPPORTS POST REQUEST
     * URL: http://localhost:8080/api/scraper/scrape
     */
    @PostMapping("/scrape")
    public ResponseEntity<Map<String, Object>> scrapeAllCategoriesPost() {
        logger.info("Starting scraping of all categories (POST request)");
        return performScraping();
    }

    /**
     * Common scraping logic
     */
    private ResponseEntity<Map<String, Object>> performScraping() {
        try {
            List<Product> products = scraperService.scrapeAllCategories();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Scraping completed successfully! CSV file created.");
            response.put("totalProducts", products.size());
            response.put("csvLocation", "./output/products.csv");
            response.put("note", "Check the ./output/products.csv file for all scraped data");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during scraping", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Scraping failed: " + e.getMessage());
            errorResponse.put("error", e.getClass().getSimpleName());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Scrape a single category - GET
     * URL: http://localhost:8080/api/scraper/scrape/Snacks & Candy
     */
    @GetMapping("/scrape/{categoryName}")
    public ResponseEntity<Map<String, Object>> scrapeSingleCategoryGet(@PathVariable String categoryName) {
        logger.info("Starting scraping of category: {} (GET request)", categoryName);
        return performCategoryScraping(categoryName);
    }

    /**
     * Scrape a single category - POST
     */
    @PostMapping("/scrape/{categoryName}")
    public ResponseEntity<Map<String, Object>> scrapeSingleCategoryPost(@PathVariable String categoryName) {
        logger.info("Starting scraping of category: {} (POST request)", categoryName);
        return performCategoryScraping(categoryName);
    }

    /**
     * Common category scraping logic
     */
    private ResponseEntity<Map<String, Object>> performCategoryScraping(String categoryName) {
        try {
            List<Product> products = scraperService.scrapeSingleCategory(categoryName);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Category scraped successfully! CSV file created.");
            response.put("category", categoryName);
            response.put("totalProducts", products.size());
            response.put("csvLocation", "./output/products.csv");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", e.getMessage());
            errorResponse.put("availableCategories", scraperService.getCategories().keySet());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            logger.error("Error during scraping", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "Scraping failed: " + e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * Get available categories
     * URL: http://localhost:8080/api/scraper/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<Map<String, Object>> getCategories() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("categories", scraperService.getCategories());

        return ResponseEntity.ok(response);
    }

    /**
     * Welcome endpoint
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> welcome() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to FreshCo Scraper API");
        response.put("instructions", "Open this URL to start scraping: http://localhost:8080/api/scraper/scrape");
        response.put("endpoints", Map.of(
                "GET /api/scraper/scrape", "Scrape all categories and create CSV",
                "GET /api/scraper/scrape/{categoryName}", "Scrape specific category",
                "GET /api/scraper/categories", "Get available categories",
                "GET /api/scraper/health", "Health check"
        ));

        return ResponseEntity.ok(response);
    }
}