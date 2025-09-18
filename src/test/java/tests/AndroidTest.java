package tests;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.Objects;

public class AndroidTest extends BaseTest {
    
    private AndroidDriver androidDriver;
    
    @BeforeClass
    public void setUpTest() {
        // Initialize the test with DeskClock app
        super.setUp(
            "Android",  // platformName
            "16.0",     // platformVersion
            "sdk_gphone64_arm64",  // deviceName
            "com.google.android.deskclock",  // appPackage
            "com.android.deskclock.DeskClock",  // appActivity
            "emulator-5554"  // udid
        );
        
        // Cast the driver to AndroidDriver
        this.androidDriver = (AndroidDriver) driver;
        
        // Add a delay to ensure app launches
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    @Test
    public void testDeskClock() {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            logger.info("Test started - waiting for DeskClock app to load...");
            
            // Get current activity to verify app is running
            String currentActivity = androidDriver.currentActivity();
            logger.info("Current activity: {}", currentActivity);
            
            // If we're not in the DeskClock app, try to launch it
            if (!Objects.requireNonNull(currentActivity).contains("DeskClock")) {
                logger.info("DeskClock not active, attempting to launch it...");
                androidDriver.activateApp("com.google.android.deskclock");
                Thread.sleep(5000); // Wait for app to launch
                currentActivity = androidDriver.currentActivity();
                logger.info("New activity after launch attempt: {}", currentActivity);
            }
            
            // Try to find any element that indicates the app is loaded
            logger.info("Looking for app content...");
            
            // Try multiple possible locators for the Alarm tab
            By[] possibleAlarmLocators = {
                AppiumBy.xpath("//android.widget.TextView[@text='Alarm']"),
                AppiumBy.xpath("//android.widget.TextView[contains(@content-desc, 'Alarm')]"),
                AppiumBy.id("com.google.android.deskclock:id/tab_icon"),
                AppiumBy.accessibilityId("Alarm"),
                AppiumBy.xpath("//*[contains(@text, 'Alarm') or contains(@content-desc, 'Alarm')]")
            };
            
            WebElement alarmTab = null;
            for (By locator : possibleAlarmLocators) {
                try {
                    alarmTab = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
                    logger.info("Found element with locator: {}", locator);
                    break;
                } catch (Exception e) {
                    logger.debug("Element not found with locator: {}", locator);
                }
            }
            
            if (alarmTab == null) {
                String pageSource = driver.getPageSource();
                logger.error("Could not find Alarm tab. Current page source:\n{}", pageSource);
                Assert.fail("Could not find Alarm tab in the app");
            }
            
            logger.info("Clicking Alarm tab...");
            alarmTab.click();
            
            // Wait for alarm screen with multiple possible locators
            logger.info("Waiting for alarm screen...");
            By[] addAlarmButtonLocators = {
                AppiumBy.id("com.google.android.deskclock:id/fab"),
                AppiumBy.accessibilityId("Add alarm"),
                AppiumBy.id("fab"),
                AppiumBy.xpath("//*[contains(@content-desc, 'Add alarm')]")
            };
            
            WebElement addAlarmButton = null;
            for (By locator : addAlarmButtonLocators) {
                try {
                    addAlarmButton = wait.until(ExpectedConditions.presenceOfElementLocated(locator));
                    logger.info("Found add alarm button with locator: {}", locator);
                    break;
                } catch (Exception e) {
                    logger.debug("Add alarm button not found with locator: {}", locator);
                }
            }
            
            if (addAlarmButton == null) {
                String pageSource = driver.getPageSource();
                logger.error("Could not find add alarm button. Current page source:\n{}", pageSource);
                Assert.fail("Could not find add alarm button in the app");
            }
            
            // Verify we're on the alarm screen
            Assert.assertTrue(addAlarmButton.isDisplayed(), "Add alarm button should be visible on alarm screen");
            logger.info("Successfully navigated to the Alarm tab in DeskClock");
            
            // Take a screenshot for verification
            takeScreenshot("alarm_screen");
            
        } catch (Exception e) {
            // Take screenshot and log page source on failure
            takeScreenshot("test_failure");
            String pageSource = driver.getPageSource();
            String currentActivity = androidDriver.currentActivity();
            logger.error("Test failed. Current activity: {}", currentActivity);
            logger.error("Page source at time of failure:\n{}", pageSource, e);
            Assert.fail(String.format("Test failed: %s", e.getMessage()), e);
        }
    }
}
