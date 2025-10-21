package com.example.service;

import com.example.model.Product;
import com.example.util.WaitUtil;
import org.openqa.selenium.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ScraperService {

    private static final Logger logger = LoggerFactory.getLogger(ScraperService.class);

    @Autowired
    private ApplicationContext context;

    @Autowired
    private WaitUtil waitUtil;

    @Autowired
    private CsvExportService csvExportService;

    @Value("${app.scraper.base-url}")
    private String baseUrl;

    private static final int MAX_PRODUCTS_PER_CATEGORY = 15;

    private final Map<String, String> categories = new HashMap<>() {{
        put("Fruits & Vegetables", "/products/category/Fresh__Fruits__&__Vegetables/Fruits");
        put("Snacks & Candy", "/products/category/Snacks__&__Candy");
        put("Dairy & Eggs", "/products/category/Dairy__&__Eggs");
        put("Bakery", "/products/category/Bakery");
        put("Meat", "/products/category/Meat");
        put("Frozen", "/products/category/Frozen");
        put("Drinks", "/products/category/Drinks");
    }};


    public List<Product> scrapeAllCategories() {
        List<Product> allProducts = new ArrayList<>();
        WebDriver driver = context.getBean(WebDriver.class);

        try {
            for (Map.Entry<String, String> category : categories.entrySet()) {
                logger.info("Scraping category: {}", category.getKey());
                List<Product> categoryProducts = scrapeCategoryPages(driver, category.getValue());
                allProducts.addAll(categoryProducts);
                logger.info("Scraped {} products from {}", categoryProducts.size(), category.getKey());
            }

            String csvPath = csvExportService.exportToCSV(allProducts);
            logger.info("Total products scraped: {}. Saved to: {}", allProducts.size(), csvPath);

        } catch (Exception e) {
            logger.error("Error during scraping", e);
            throw new RuntimeException("Scraping failed", e);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }

        return allProducts;
    }

    public List<Product> scrapeSingleCategory(String categoryName) {
        String categoryUrl = categories.get(categoryName);
        if (categoryUrl == null) {
            throw new IllegalArgumentException("Invalid category: " + categoryName);
        }

        WebDriver driver = context.getBean(WebDriver.class);
        List<Product> products = new ArrayList<>();

        try {
            products = scrapeCategoryPages(driver, categoryUrl);
            csvExportService.exportToCSV(products);
        } finally {
            if (driver != null) {
                driver.quit();
            }
        }

        return products;
    }

    private List<Product> scrapeCategoryPages(WebDriver driver, String categoryUrl) {
        List<Product> allProducts = new ArrayList<>();

        try {
            String fullUrl = baseUrl + categoryUrl;
            logger.info("Navigating to: {}", fullUrl);
            driver.get(fullUrl);

            // Extract category name from URL
            String categoryName = extractCategoryName(categoryUrl);

            // Reduced waits for speed
            waitUtil.waitForPageLoad(driver, 5);
            waitUtil.sleep(1000);

            handleCookiePopup(driver);
            scrollToLoadAllProducts(driver);

            List<Product> products = scrapeProductsFromPage(driver, categoryName);
            allProducts.addAll(products);

            logger.info("Scraped {} products", products.size());

        } catch (Exception e) {
            logger.error("Error scraping category: {}", categoryUrl, e);
        }

        return allProducts;
    }

    private List<Product> scrapeProductsFromPage(WebDriver driver, String categoryName) {
        List<Product> products = new ArrayList<>();

        try {
            waitUtil.sleep(800);

            List<WebElement> productLinks = driver.findElements(
                    By.cssSelector("a[href*='/products/'][class*='absolute']")
            );

            logger.info("Found {} product links", productLinks.size());

            List<String> productUrls = new ArrayList<>();
            for (WebElement link : productLinks) {
                try {
                    String href = link.getAttribute("href");
                    if (href != null && !href.isEmpty() && !productUrls.contains(href)) {
                        productUrls.add(href);
                    }
                } catch (StaleElementReferenceException e) {
                    logger.debug("Stale element, skipping");
                }
            }

            logger.info("Extracted {} unique product URLs", productUrls.size());

            int productsToScrape = Math.min(productUrls.size(), MAX_PRODUCTS_PER_CATEGORY);
            logger.info("Will scrape {} products (limited from {})", productsToScrape, productUrls.size());

            for (int i = 0; i < productsToScrape; i++) {
                try {
                    Product product = scrapeProductDetails(driver, productUrls.get(i), categoryName);
                    if (product != null) {
                        products.add(product);
                        logger.info("Scraped product {}/{}: {}", i + 1, productsToScrape, product.getName());
                    }
                } catch (Exception e) {
                    logger.error("Error scraping product at index {}: {}", i, e.getMessage());
                }
            }

        } catch (Exception e) {
            logger.error("Error scraping products from page", e);
        }

        return products;
    }

    private Product scrapeProductDetails(WebDriver driver, String productUrl, String categoryName) {
        try {
            logger.debug("Visiting product: {}", productUrl);
            driver.get(productUrl);

            // Reduced waits
            waitUtil.waitForPageLoad(driver, 5);
            waitUtil.sleep(600);

            Product product = Product.builder().build();
            product.setCategory(categoryName);

            // Extract all data with a single JS call
            Map<String, Object> data = extractAllDataAtOnce((JavascriptExecutor) driver);

            // Name
            String name = safeToString(data.get("name"));
            product.setName(name.isEmpty() ? "N/A" : cleanText(name));

            // Price
            String price = safeToString(data.get("price"));
            product.setPrice(price.isEmpty() ? "N/A" : cleanText(price));

            // Description: if empty -> "No Description Available"
            String desc = safeToString(data.get("description"));
            desc = cleanText(desc);
            product.setDescription(desc.isEmpty() ? "No Description Available" : desc);

            // Image: prefer value returned; filter logos; if empty -> "N/A"
            String imgUrl = safeToString(data.get("image"));
            imgUrl = (imgUrl == null) ? "" : imgUrl.trim();
            if (imgUrl.isEmpty() || imgUrl.contains("freshcoLogo") || imgUrl.contains("logo") || imgUrl.contains("icon")) {
                product.setImageUrl("N/A");
            } else {
                product.setImageUrl(imgUrl);
            }

            // Availability
            String availability = safeToString(data.get("availability"));
            availability = cleanText(availability);
            product.setAvailability(availability == null || availability.isEmpty() ? "In Stock" : availability);

            return product;

        } catch (Exception e) {
            logger.error("Error scraping product details: {}", productUrl, e);
            return null;
        }
    }

    /**
     * Extract all data using one JavaScript execution to speed up scraping.
     * Returns a Map<String,Object> with keys: name, price, description, image, availability
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractAllDataAtOnce(JavascriptExecutor js) {
        Map<String, Object> result = new HashMap<>();
        try {
            String script =
                    "var data = {};" +

                            // name
                            "var nameEl = document.querySelector('h1.text-brandBlack.font-bold, h2.text-brandBlack.font-bold, h2[class*=\\'text-xl\\']');" +
                            "if(!nameEl) nameEl = document.querySelector('h1, h2');" +
                            "data.name = nameEl ? (nameEl.textContent || nameEl.innerText).trim() : '';" +

                            // price
                            // price
                            "var priceEl = document.querySelector(\"span.text-red200.font-bold, span.font-bold.text-xl, span[class*='font-bold'][class*='text-xl'], div[class*='price']\");" +
                            "data.price = priceEl ? (priceEl.textContent || priceEl.innerText).trim() : '';" +



                            // description - check several spots; use textContent (works even if hidden)
                            "var d = ''; " +
                            "var descSelectors = ['#description-panel span.font-normal', 'div.p-4 span.font-normal', \"div[class*='description']\", \"div[class*='prose']\", \"section[id*='description']\", 'div.product-description', 'div.product__description'];" +
                            "for(var i=0;i<descSelectors.length && d.length<10;i++){ " +
                            "  var el = document.querySelector(descSelectors[i]); " +
                            "  if(el){ " +
                            "    var t = el.textContent || el.innerText || ''; " +
                            "    if(t && t.trim().length>0) { d = t.trim(); break; } " +
                            "  } " +
                            "} " +
// safeguard: remove leading 'Description' if still present
                            "d = d.replace(/^Description\\s*/i, '').trim();" +
                            "data.description = d || '';"+


            // image - prefer og:image then first non-logo image
                            "var img = ''; " +
                            "var metaOg = document.querySelector(\"meta[property='og:image'], meta[name='og:image']\");" +
                            "if(metaOg && metaOg.content) img = metaOg.content;" +
                            "if(!img){ var imgs = document.querySelectorAll('img[src]'); for(var j=0;j<imgs.length;j++){ var s = imgs[j].src || imgs[j].getAttribute('data-src') || imgs[j].getAttribute('srcset'); if(!s) continue; s = s.toString(); if(s.indexOf('logo')>-1||s.indexOf('icon')>-1||s.indexOf('freshcoLogo')>-1||s.indexOf('/header/')>-1) continue; img = s; break; } }" +
                            "data.image = img || '';" +

                            // availability - try a few selectors
                            "var avail = ''; var availEl = document.querySelector('button.add-to-cart, div[class*=\"availability\"], span[class*=\"availability\"], strong');" +
                            "if(availEl) avail = (availEl.textContent || availEl.innerText || '').trim(); " +
                            "data.availability = (avail && avail.toLowerCase().indexOf('out')>=0) ? 'Out of Stock' : (avail ? avail : 'In Stock');" +

                            "return data;";

            Object jsResult = js.executeScript(script);

            if (jsResult instanceof Map) {
                // typical: Selenium returns a Map (LinkedHashMap)
                result = (Map<String, Object>) jsResult;
            } else if (jsResult != null) {
                // try to convert if webdriver returned a JS object represented differently
                // best-effort: cast via toString (not ideal), but keep defaults empty
                logger.debug("JS returned unexpected type: {}", jsResult.getClass().getName());
            }
        } catch (Exception e) {
            logger.debug("JS extraction failed", e);
        }
        return result;
    }

    private String safeToString(Object o) {
        if (o == null) return "";
        return String.valueOf(o);
    }

    private String cleanText(String text) {
        if (text == null) return "";
        return text.replaceAll("\\s+", " ").trim();
    }

    /**
     * Map URL path to category name
     */
    private String extractCategoryName(String categoryUrl) {
        for (Map.Entry<String, String> entry : categories.entrySet()) {
            if (entry.getValue().equals(categoryUrl)) {
                return entry.getKey();
            }
        }
        return "Unknown";
    }

    private void handleCookiePopup(WebDriver driver) {
        try {
            List<WebElement> cookieButtons = driver.findElements(
                    By.cssSelector("button[id*='cookie'], button[class*='cookie'], button[id*='accept']")
            );
            if (!cookieButtons.isEmpty()) {
                waitUtil.safeClick(driver, cookieButtons.get(0));
                waitUtil.sleep(300);
            }
        } catch (Exception e) {
            logger.debug("No cookie popup found");
        }
    }

    private void scrollToLoadAllProducts(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            long lastHeight = (long) js.executeScript("return document.body.scrollHeight");
            int scrollAttempts = 0;

            while (scrollAttempts < 3) {
                js.executeScript("window.scrollTo(0, document.body.scrollHeight);");
                waitUtil.sleep(500);
                long newHeight = (long) js.executeScript("return document.body.scrollHeight");
                if (newHeight == lastHeight) break;
                lastHeight = newHeight;
                scrollAttempts++;
            }

            js.executeScript("window.scrollTo(0, 0);");
            waitUtil.sleep(300);

        } catch (Exception e) {
            logger.error("Error during scrolling", e);
        }
    }

    public Map<String, String> getCategories() {
        return new HashMap<>(categories);
    }
}
