package tests;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.URI;
import java.time.Duration;

public class BrowserTest extends BaseTest {
    
    private static final Logger logger = LoggerFactory.getLogger(BrowserTest.class);

    @Test
    public void testAndroidBrowser() {
        try {
            logger.info("Android Browser test started");
            
            // Set Browser-specific capabilities
            UiAutomator2Options options = new UiAutomator2Options()
                .setUdid("emulator-5554")
                .setPlatformName("Android")
                .setPlatformVersion("16.0")
                .withBrowserName("Browser")  // Using the built-in Android Browser
                .setAutoGrantPermissions(true)
                .setNoReset(true);
            
            // Initialize the Android driver
            AndroidDriver driver = new AndroidDriver(URI.create("http://127.0.0.1:4723/").toURL(), options);
            
            try {
                // Navigate to a test page
                driver.get("https://www.google.com");
                System.out.println("Navigated to Google");
                
                // Wait for the page to load
                WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
                
                // Accept cookies if the dialog appears
                try {
                    WebElement acceptButton = wait.until(ExpectedConditions.presenceOfElementLocated(
                        AppiumBy.xpath("//button[contains(., 'Accept all') or contains(., 'I agree')]")
                    ));
                    if (acceptButton.isDisplayed()) {
                        acceptButton.click();
                        System.out.println("Accepted cookies");
                    }
                } catch (Exception e) {
                    System.out.println("No cookie acceptance dialog found");
                }
                
                // Find the search box - using a more flexible locator
                WebElement searchBox = wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.xpath("//*[@name='q' or @title='Search' or @aria-label='Search']")
                ));
                
                // Verify the search box is displayed
                Assert.assertTrue(searchBox.isDisplayed(), "Search box is not displayed");
                System.out.println("Successfully loaded Google search page");
                
                // Type a search query
                searchBox.sendKeys("Appium automation");
                System.out.println("Entered search query");
                
                // Submit the search
                searchBox.submit();
                System.out.println("Submitted search");
                
                // Wait for search results
                WebElement results = wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.xpath("//*[contains(text(),'results') or contains(text(),'Results')]")
                ));
                
                // Verify search results are displayed
                Assert.assertTrue(results.isDisplayed(), "Search results are not displayed");
                System.out.println("Search results are displayed");
                
                // Add a small delay to observe the results
                Thread.sleep(3000);
                
            } finally {
                // Quit the driver
                driver.quit();
            }
            
        } catch (Exception e) {
            String errorMessage = String.format("Browser test failed: %s", e.getMessage());
            logger.error(errorMessage, e);
            Assert.fail(errorMessage);
        }
    }
}
