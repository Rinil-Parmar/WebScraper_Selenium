package com.example.util;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class WaitUtil {

    /**
     * Wait for element to be visible
     */
    public WebElement waitForElementVisible(WebDriver driver, By locator, int timeoutSeconds) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            return wait.until(ExpectedConditions.visibilityOfElementLocated(locator));
        } catch (TimeoutException e) {
            throw new RuntimeException("Element not visible: " + locator, e);
        }
    }

    /**
     * Wait for element to be clickable
     */
    public WebElement waitForElementClickable(WebDriver driver, By locator, int timeoutSeconds) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            return wait.until(ExpectedConditions.elementToBeClickable(locator));
        } catch (TimeoutException e) {
            throw new RuntimeException("Element not clickable: " + locator, e);
        }
    }

    /**
     * Wait for elements to be present
     */
    public List<WebElement> waitForElementsPresent(WebDriver driver, By locator, int timeoutSeconds) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            return wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(locator));
        } catch (TimeoutException e) {
            throw new RuntimeException("Elements not present: " + locator, e);
        }
    }

    /**
     * Wait for page to load completely
     */
    public void waitForPageLoad(WebDriver driver, int timeoutSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
        wait.until(webDriver -> ((JavascriptExecutor) webDriver)
                .executeScript("return document.readyState").equals("complete"));
    }

    /**
     * Scroll element into view
     */
    public void scrollToElement(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", element);
        sleep(500);
    }

    /**
     * Handle alert/popup if present
     */
    public boolean handleAlert(WebDriver driver, boolean accept) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(3));
            Alert alert = wait.until(ExpectedConditions.alertIsPresent());
            if (accept) {
                alert.accept();
            } else {
                alert.dismiss();
            }
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Sleep for specified milliseconds
     */
    public void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Safe click with retry
     */
    public void safeClick(WebDriver driver, WebElement element) {
        int attempts = 0;
        while (attempts < 3) {
            try {
                element.click();
                break;
            } catch (ElementClickInterceptedException e) {
                attempts++;
                if (attempts >= 3) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
                }
                sleep(500);
            }
        }
    }
}