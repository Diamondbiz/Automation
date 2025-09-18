package tests;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

public class TikTokTest extends BaseTest {
    
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(30);
    private static final String HOME_FEED_ID = "com.zhiliaoapp.musically:id/feed_list";
    private WebDriverWait wait;
    
    @BeforeMethod
    public void setupTest() {
        wait = new WebDriverWait(driver, DEFAULT_TIMEOUT);
        // Ensure we start from a known state
        navigateToTab("Home");
    }
    
    @AfterMethod
    public void tearDown() {
        // Reset app state after each test
        if (driver != null && driver instanceof AndroidDriver) {
            String appPackage = "com.zhiliaoapp.musically"; // TikTok's package name
            try {
                ((AndroidDriver) driver).terminateApp(appPackage);
                ((AndroidDriver) driver).activateApp(appPackage);
            } catch (Exception e) {
                System.out.printf("Warning: Could not reset app: %s%n", e.getMessage());
            }
        }
    }
    
    @Test(priority = 1, description = "Verify TikTok app launches successfully")
    public void testTikTokLaunch() {
        System.out.println("Starting TikTok launch test...");
        
        // Wait for the home feed to be visible
        WebElement homeFeed = wait.until(ExpectedConditions.presenceOfElementLocated(
            AppiumBy.id(HOME_FEED_ID)
        ));
        
        Assert.assertTrue(homeFeed.isDisplayed(), "Home feed should be visible");
        System.out.println("✅ TikTok launched successfully");
        
        // Verify the current activity is TikTok's main activity
        String currentActivity = ((AndroidDriver) driver).currentActivity();
        Assert.assertNotNull(currentActivity, "Current activity should not be null");
        Assert.assertTrue(currentActivity.matches(".*(MainActivity|HomePage|MainTabActivity).*"), 
                        "Not on TikTok main screen. Current activity: %s".formatted(currentActivity));
    }
    
    @Test(priority = 2, description = "Test navigation between different tabs")
    public void testNavigationTabs() {
        System.out.println("Testing navigation tabs...");
        
        String[] tabsToTest = {"Discover", "Inbox", "Profile", "Home"};
        
        for (String tab : tabsToTest) {
            navigateToTab(tab);
            // Verify we're on the correct tab by checking some tab-specific element
            verifyTabContent(tab);
        }
        
        System.out.println("✅ Navigation test completed successfully");
    }
    
    @Test(priority = 3, description = "Test video playback functionality")
    public void testVideoPlayback() {
        System.out.println("Testing video playback...");
        
        // Make sure we're on the home feed
        navigateToTab("Home");
        
        // Wait for videos to load
        List<WebElement> videos = wait.until(ExpectedConditions
            .presenceOfAllElementsLocatedBy(AppiumBy.id(HOME_FEED_ID))
        );
        
        Assert.assertFalse(videos.isEmpty(), "No videos found on the home feed");
        System.out.printf("✅ Video feed loaded with %d videos%n", videos.size());
        
        // Tap on the first video to start playback
        videos.getFirst().click();
        
        // Wait for video player to be visible
        WebElement videoPlayer = wait.until(ExpectedConditions
            .visibilityOfElementLocated(AppiumBy.id("com.zhiliaoapp.musically:id/video_player_view"))
        );
        
        Assert.assertTrue(videoPlayer.isDisplayed(), "Video player should be visible");
        System.out.println("✅ Video playback started successfully");
    }
    
    private void navigateToTab(String tabName) {
        System.out.printf("\n=== Attempting to navigate to %s tab ===%n", tabName);
        
        // Map of tab names to their common accessibility IDs or content descriptions
        String tabLocator = switch (tabName.toLowerCase()) {
            case "home" -> "Home";
            case "discover" -> "Discover";
            case "inbox" -> "Inbox";
            case "profile" -> "Profile";
            default -> throw new IllegalArgumentException("Unsupported tab: %s".formatted(tabName));
        };
        
        try {
            // First, try to find if we're already on the desired tab
            try {
                WebElement currentTab = driver.findElement(AppiumBy.accessibilityId(tabLocator));
                if (currentTab != null && currentTab.isDisplayed()) {
                    System.out.printf("Already on %s tab, no navigation needed%n", tabName);
                    return;
                }
            } catch (Exception e) {
                // If we can't find the current tab, continue with navigation
                System.out.printf("Not currently on %s tab, proceeding with navigation%n", tabName);
            }
            
            System.out.printf("Looking for tab with accessibility ID: %s%n", tabLocator);
            
            // Print page source for debugging (first 1000 chars to avoid too much output)
            String pageSource = driver.getPageSource();
            System.out.printf("Current page source (first 1000 chars): %s%n", pageSource.substring(0, Math.min(1000, pageSource.length())));
            
            // Wait for and click the tab
            System.out.println("Waiting for tab to be clickable...");
            WebElement tab = wait.until(ExpectedConditions.elementToBeClickable(
                AppiumBy.accessibilityId(tabLocator)
            ));
            
            System.out.println("Clicking tab...");
            tab.click();
            
            // Wait for tab-specific content to load
            System.out.println("Waiting for tab content to load...");
            wait.until(driver -> !driver.getPageSource().contains("Loading"));
            
            System.out.printf("✅ Successfully navigated to %s tab%n", tabName);
        } catch (Exception e) {
            System.err.printf("❌ Failed to navigate to %s tab: %s%n", tabName, e.getMessage());
            // Take a screenshot on failure
            takeScreenshot("tab_navigation_failure_%s".formatted(tabName));
            throw e;
        }
    }
    
    private void verifyTabContent(String tabName) {
        // Add verification logic for each tab
        switch (tabName.toLowerCase()) {
            case "home":
                wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.id(HOME_FEED_ID)
                ));
                break;
            case "discover":
                wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.id("com.zhiliaoapp.musically:id/discover_feed")
                ));
                break;
            case "inbox":
                wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.id("com.zhiliaoapp.musically:id/message_list")
                ));
                break;
            case "profile":
                wait.until(ExpectedConditions.presenceOfElementLocated(
                    AppiumBy.id("com.zhiliaoapp.musically:id/profile_tab")
                ));
                break;
        }
    }
}

