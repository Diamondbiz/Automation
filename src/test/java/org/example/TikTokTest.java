package org.example;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.time.Duration;

public class TikTokTest {
    private WebDriver driver;
    private WebDriverWait wait;
    private static final String TIKTOK_URL = "https://www.tiktok.com";
    private static final boolean HEADLESS = false; // Set to true to run in headless mode

    @BeforeMethod
    public void setup() {
        try {
            System.out.println("🚀 Setting up ChromeDriver...");
            WebDriverManager.chromedriver().setup();
            
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--start-maximized");
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            
            if (HEADLESS) {
                options.addArguments("--headless=new");
                System.out.println("🔍 Running in headless mode");
            }
            
            driver = new ChromeDriver(options);
            
            // Set timeouts
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            wait = new WebDriverWait(driver, Duration.ofSeconds(15));
            
            System.out.println("🌐 Navigating to TikTok...");
            driver.get(TIKTOK_URL);
            
        } catch (Exception e) {
            System.err.println("❌ Error during setup: " + e.getMessage());
            throw e;
        }
    }

    @Test
    public void testTikTokHomePage() {
        try {
            System.out.println("🔍 Checking page title...");
            String pageTitle = driver.getTitle();
            System.out.println("📄 Page title: " + pageTitle);
            
            boolean containsTikTok = pageTitle.toLowerCase().contains("tiktok");
            System.out.println(containsTikTok ? "✅ Test passed!" : "❌ Test failed!");
            
            Assert.assertTrue(containsTikTok, "Page title should contain 'TikTok'");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed with error: " + e.getMessage());
            throw e;
        }
    }

    @AfterMethod
    public void tearDown() {
        try {
            if (driver != null) {
                System.out.println("🛑 Closing browser...");
                driver.quit();
            }
        } catch (Exception e) {
            System.err.println("⚠️ Error during teardown: " + e.getMessage());
        }
    }
}
