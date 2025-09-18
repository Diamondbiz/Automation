package org.example;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * AutoEnvUpdater - A utility class for setting up and maintaining an automation environment.
 */
public class AutoEnvUpdater {
    
    // Configuration
    private static final List<String> SUMMARY = new ArrayList<>();
    private static final int TIMEOUT_SECONDS = 60;

    static void main() {
        try {
            String separator = "=".repeat(50);
            print("""
                
                %s
                🚀 Starting Automation Environment Setup
                %s
                """.formatted(separator, separator));
            
            // Install required tools
            installTool("Homebrew", AutoEnvUpdater::installHomebrew);
            installTool("Node.js & npm", AutoEnvUpdater::installNodeNpm);
            installTool("Chrome", AutoEnvUpdater::installChrome);
            installTool("Appium", AutoEnvUpdater::installAppium);
            installTool("Selenium", AutoEnvUpdater::installSelenium);
            installTool("UiAutomator2", AutoEnvUpdater::installUiAutomator2);
            installTool("XCUITest", AutoEnvUpdater::installXcuitest);
            
            print("""
                
                %s
                ✅ Setup completed successfully!
                %s""".formatted(separator, separator));
            
            // Print summary
            if (!SUMMARY.isEmpty()) {
                print("\nSummary of operations:");
                SUMMARY.forEach(System.out::println);
            }
            
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            try (PrintWriter pw = new PrintWriter(sw)) {
                e.printStackTrace(pw);
                String errorDetails = String.format("%n❌ Error: %s%n%s", e.getMessage(), sw);
                print(errorDetails);
            }
            System.exit(1);
        }
    }

    private static void installTool(String name, Runnable installer) {
        String separator = "=".repeat(50);
        print("""
            
            %s
            🔧 SETTING UP: %s
            %s""".formatted(separator, name, separator));
        
        try {
            long startTime = System.currentTimeMillis();
            installer.run();
            long endTime = System.currentTimeMillis();
            String duration = String.format("%.2f", (endTime - startTime) / 1000.0);
            
            String successMsg = "✅ %s - Completed successfully in %ss".formatted(name, duration);
            print(successMsg);
            SUMMARY.add(successMsg);
            
        } catch (Exception e) {
            String errorMsg = "❌ %s - Failed: %s".formatted(name, e.getMessage());
            print(errorMsg);
            SUMMARY.add(errorMsg);
            throw e;
        }
    }

    /* ---------- HOMEBREW ---------- */
    private static boolean isInteractive() {
        Console console = System.console();
        if (console == null) {
            return false;
        }
        try (Reader _ = console.reader()) {
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    private static void installHomebrew() {
        try {
            // Check if Homebrew is already installed
            if (exec("which brew")) {
                print("✓ Homebrew is already installed");
                return;
            }
            
            print("Installing Homebrew...");
            String installCmd = "/bin/bash -c \"$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\"";
            
            if (isInteractive()) {
                // Run interactively if possible
                ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", installCmd);
                pb.inheritIO();
                Process process = null;
                try {
                    process = pb.start();
                    int exitCode = process.waitFor();
                    
                    if (exitCode != 0) {
                        throw new RuntimeException(String.format("Homebrew installation failed with exit code: %d", exitCode));
                    }
                } finally {
                    if (process != null) {
                        process.destroy();
                    }
                }
            } else {
                // Non-interactive installation
                exec(installCmd);
            }
            
            // Add Homebrew to PATH
            String shellConfig = String.format("%s/.zshrc", System.getProperty("user.home"));
            String pathCmd = String.format("echo 'eval \"$(/opt/homebrew/bin/brew shellenv)\"' >> %s", shellConfig);
            exec(pathCmd);
            
            // Reload shell config
            exec(String.format("source %s", shellConfig));
            
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to install Homebrew: %s", e.getMessage()), e);
        }
    }

    /* ---------- NODE & NPM ---------- */
    private static void installNodeNpm() {
        try {
            if (exec("which node")) {
                print("✓ Node.js is already installed");
                return;
            }
            
            print("Installing Node.js and npm...");
            exec("brew install node");
            
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to install Node.js and npm: %s", e.getMessage()), e);
        }
    }

    /* ---------- CHROME ---------- */
    private static void installChrome() {
        try {
            if (exec("which google-chrome")) {
                print("✓ Chrome is already installed");
                return;
            }
            
            print("Installing Google Chrome...");
            exec("brew install --cask google-chrome");
            
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to install Chrome: %s", e.getMessage()), e);
        }
    }

    /* ---------- APPIUM ---------- */
    private static void installAppium() {
        try {
            print("Installing Appium...");
            exec("npm install -g appium");
            exec("npm install -g appium-doctor");
            
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to install Appium: %s", e.getMessage()), e);
        }
    }

    /* ---------- SELENIUM ---------- */
    private static void installSelenium() {
        try {
            print("Installing Selenium WebDriver...");
            exec("npm install -g selenium-webdriver");
            
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to install Selenium: %s", e.getMessage()), e);
        }
    }

    /* ---------- UIAUTOMATOR2 ---------- */
    private static void installUiAutomator2() {
        try {
            print("Installing UiAutomator2 driver...");
            exec("npm install -g appium-uiautomator2-driver");
            
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to install UiAutomator2 driver: %s", e.getMessage()), e);
        }
    }

    /* ---------- XCUITEST ---------- */
    private static void installXcuitest() {
        try {
            print("Installing XCUITest driver...");
            exec("npm install -g appium-xcuitest-driver");
            
        } catch (Exception e) {
            throw new RuntimeException(String.format("Failed to install XCUITest driver: %s", e.getMessage()), e);
        }
    }

    /* ---------- HELPER METHODS ---------- */
    private static void print(String message) {
        System.out.println(message);
    }

    private static boolean exec(String command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // Read the output
            try (InputStream inputStream = process.getInputStream();
                 InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                 BufferedReader reader = new BufferedReader(inputStreamReader)) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    print("  | %s".formatted(line));
                }
            }
            
            // Wait for the process to complete
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                throw new RuntimeException(String.format("Command timed out after %d seconds: %s", TIMEOUT_SECONDS, command));
            }
            
            int exitCode = process.exitValue();
            return exitCode == 0;
            
        } catch (Exception e) {
            throw new RuntimeException(String.format("Command failed: %s - %s", command, e.getMessage()), e);
        }
    }
}
