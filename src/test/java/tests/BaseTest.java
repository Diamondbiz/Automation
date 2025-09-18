package tests;

import io.appium.java_client.AppiumDriver;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.ios.IOSDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.*;
import utils.AppiumServerManager;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Base test class for Appium test automation.
 * Handles common setup and teardown operations for both Android and iOS tests.
 */
public class BaseTest {
    private static final Logger logger = LoggerFactory.getLogger(BaseTest.class);
    
    // Appium server configuration
    protected static final String DEFAULT_APPIUM_SERVER = "http://127.0.0.1:4723";
    protected static final String APPIUM_SERVER_PATH = "/";
    
    // Default test configuration
    private static final int IMPLICIT_WAIT_SECONDS = 10;
    private static final int COMMAND_TIMEOUT_MS = 300000; // 5 minutes
    
    // Default app configurations
    private static final String DEFAULT_ANDROID_DEVICE = "sdk_gphone64_arm64";
    private static final String DEFAULT_ANDROID_VERSION = "16.0";
    private static final String DEFAULT_ANDROID_PACKAGE = "com.zhiliaoapp.musically";
    private static final String DEFAULT_ANDROID_ACTIVITY = "com.ss.android.ugc.aweme.splash.SplashActivity";
    private static final String DEFAULT_ANDROID_UDID = "emulator-5554";
    private static final String SETTINGS_APP_RELATIVE_PATH = "/node_modules/appium-uiautomator2-driver/node_modules/io.appium.settings/apks/settings_apk-debug.apk";
    
    protected AppiumDriver driver;

    @BeforeSuite
    public void beforeSuite() {
        logger.info("Test suite setup started");
        // Note: Server is started manually as per project requirements
        logger.info("Automatic Appium server startup is disabled - ensure server is running");
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        try {
            logger.info("Test suite teardown started");
            AppiumServerManager.stopServer();
            logger.info("Test suite teardown completed");
        } catch (Exception e) {
            logger.error("Error during test suite teardown: {}", e.getMessage(), e);
        }
    }

    /**
     * Sets up the test environment before each test class.
     *
     * @param platformName   The target platform (Android/iOS)
     * @param platformVersion The platform version
     * @param deviceName     The device name or ID
     * @param appPackage     The application package (Android)
     * @param appActivity    The application activity (Android)
     * @throws RuntimeException if setup fails
     */
    @Parameters({"platformName", "platformVersion", "deviceName", "appPackage", "appActivity", "udid"})
    @BeforeClass(alwaysRun = true)
    public void setUp(
            @Optional("Android") String platformName,
            @Optional(DEFAULT_ANDROID_VERSION) String platformVersion,
            @Optional(DEFAULT_ANDROID_DEVICE) String deviceName,
            @Optional("") String appPackage,
            @Optional("") String appActivity,
            @Optional(DEFAULT_ANDROID_UDID) String udid) {
            
        platformName = platformName.toLowerCase();
        
        logger.info("Setting up test for {} platform", platformName);
        
        try {
            // Resolve settings app path relative to user's home directory
            String userHome = System.getProperty("user.home");
            String settingsAppPath = String.format("%s/.appium%s", userHome, SETTINGS_APP_RELATIVE_PATH);
            
            DesiredCapabilities caps = createCapabilities(platformName, platformVersion, 
                deviceName, appPackage, appActivity, udid, settingsAppPath);
            String serverUrl = String.format("%s%s", DEFAULT_APPIUM_SERVER, APPIUM_SERVER_PATH);
            
            logger.debug("Connecting to Appium server at: {}", serverUrl);
            initializeDriver(platformName, caps, serverUrl);
            
            // Configure timeouts
            configureDriverTimeouts();
            
            logger.info("Test setup completed for {} on {}", platformName, deviceName);
        } catch (Exception e) {
            String errorMsg = String.format("Failed to initialize test setup: %s", e.getMessage());
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
    }
    
    /**
     * Creates the appropriate capabilities based on the platform.
     */
    private DesiredCapabilities createCapabilities(String platformName, String platformVersion, 
                                                 String deviceName, String appPackage, 
                                                 String appActivity, String udid, String settingsAppPath) {
        DesiredCapabilities caps = new DesiredCapabilities();
        
        if (platformName.equalsIgnoreCase("android")) {
            configureAndroidCapabilities(caps, platformVersion, deviceName, 
                appPackage, appActivity, udid, settingsAppPath);
        } else {
            configureIosCapabilities(caps);
        }
        
        logger.debug("Created capabilities: {}", caps);
        return caps;
    }
    
    /**
     * Configures Android-specific capabilities.
     */
    private void configureAndroidCapabilities(DesiredCapabilities caps, String platformVersion, 
                                            String deviceName, String appPackage, 
                                            String appActivity, String udid, String settingsAppPath) {
        logger.debug("Configuring Android capabilities with UDID: {}", udid);
        
        // Use provided values or defaults
        String effectivePackage = appPackage.isEmpty() ? DEFAULT_ANDROID_PACKAGE : appPackage;
        String effectiveActivity = appActivity.isEmpty() ? DEFAULT_ANDROID_ACTIVITY : appActivity;
        
        // Common Android capabilities
        caps.setCapability("platformName", "Android");
        caps.setCapability("platformVersion", platformVersion);
        caps.setCapability("deviceName", deviceName);
        caps.setCapability("udid", udid);
        
        // App configuration
        caps.setCapability("appPackage", effectivePackage);
        caps.setCapability("appActivity", effectiveActivity);
        
        // Performance and behavior settings
        caps.setCapability("autoGrantPermissions", true);
        caps.setCapability("noReset", true);
        caps.setCapability("fullReset", false);
        caps.setCapability("newCommandTimeout", TimeUnit.MILLISECONDS.toSeconds(COMMAND_TIMEOUT_MS));
        caps.setCapability("automationName", "UiAutomator2");
        
        // Additional settings
        logger.debug("Using settings app at: {}", settingsAppPath);
        caps.setCapability("appium:settingsApp", settingsAppPath);
        
        logger.info("Android capabilities configured for {}:{}", effectivePackage, effectiveActivity);
    }
    
    /**
     * Configures iOS-specific capabilities.
     */
    private void configureIosCapabilities(DesiredCapabilities caps) {
        caps.setCapability("platformName", "iOS");
        caps.setCapability("automationName", "XCUITest");
        caps.setCapability("bundleId", "com.apple.calculator");
        logger.info("iOS capabilities configured");
    }
    
    /**
     * Initializes the appropriate Appium driver based on the platform.
     * @param platformName The name of the platform (android/ios)
     * @param caps The desired capabilities for the driver
     * @param serverUrl The URL of the Appium server
     */
    private void initializeDriver(String platformName, DesiredCapabilities caps, String serverUrl) {
        logger.info("Initializing {} driver...", platformName);
        
        try {
            URL appiumServerUrl = new URI(serverUrl).toURL();
            
            if (platformName.equalsIgnoreCase("android")) {
                this.driver = new AndroidDriver(appiumServerUrl, caps);
            } else {
                this.driver = new IOSDriver(appiumServerUrl, caps);
            }
        } catch (URISyntaxException | MalformedURLException e) {
            String errorMsg = String.format("Invalid Appium server URL '%s': %s", serverUrl, e.getMessage());
            logger.error(errorMsg, e);
            throw new RuntimeException(errorMsg, e);
        }
        
        logger.debug("{} driver initialized successfully", platformName);
    }
    
    /**
     * Configures driver timeouts.
     */
    private void configureDriverTimeouts() {
        if (driver != null) {
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(IMPLICIT_WAIT_SECONDS));
            logger.debug("Driver timeouts configured");
        }
    }

    /**
     * Cleans up after each test class.
     */
    @AfterClass(alwaysRun = true)
    public void tearDown() {
        logger.info("Tearing down test...");
        
        if (driver != null) {
            try {
                logger.debug("Quitting driver...");
                driver.quit();
                logger.info("Driver quit successfully");
            } catch (WebDriverException e) {
                logger.error("Error while quitting the driver: {}", e.getMessage(), e);
            } finally {
                driver = null;
            }
        }
        
        logger.info("Test teardown completed");
    }
}
