package tests;

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
            
            // Install required tools with their respective checkers
            installTool("Homebrew", 
                AutoEnvUpdater::installHomebrew,
                AutoEnvUpdater::checkHomebrew);
                
            installTool("Node.js & npm", 
                AutoEnvUpdater::installNodeNpm,
                AutoEnvUpdater::checkNodeNpm);
                
            installTool("Chrome", 
                AutoEnvUpdater::installChrome,
                AutoEnvUpdater::checkChrome);
                
            installTool("Appium", 
                AutoEnvUpdater::installAppium,
                AutoEnvUpdater::checkAppium);
                
            installTool("Selenium", 
                AutoEnvUpdater::installSelenium,
                AutoEnvUpdater::checkSelenium);
                
            installTool("UiAutomator2", 
                AutoEnvUpdater::installUiAutomator2,
                AutoEnvUpdater::checkUiAutomator2);
                
            installTool("XCUITest", 
                AutoEnvUpdater::installXcuitest,
                AutoEnvUpdater::checkXcuitest);
            
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

    private static void installTool(String name, Runnable installer, Runnable checker) {
        String separator = "=".repeat(50);
        print("""
            
            %s
            🔧 CHECKING: %s
            %s""".formatted(separator, name, separator));
        
        try {
            // First check if already installed
            if (checker != null) {
                try {
                    checker.run();
                    print("✅ %s is already installed and up to date. Skipping installation.".formatted(name));
                    return;
                } catch (Exception e) {
                    print("ℹ️ %s needs to be installed or updated: %s".formatted(name, e.getMessage()));
                }
            }
            
            // If not installed or checker not provided, proceed with installation
            print("\n🔧 INSTALLING: %s".formatted(name));
            long startTime = System.currentTimeMillis();
            installer.run();
            long endTime = System.currentTimeMillis();
            String duration = String.format("%.2f", (endTime - startTime) / 1000.0);
            
            // Verify installation after completion
            if (checker != null) {
                try {
                    checker.run();
                    String successMsg = "✅ %s - Installed/Updated successfully in %ss".formatted(name, duration);
                    print(successMsg);
                    SUMMARY.add(successMsg);
                } catch (Exception e) {
                    String warningMsg = "⚠️ %s - Installation completed but verification failed: %s".formatted(name, e.getMessage());
                    print(warningMsg);
                    SUMMARY.add(warningMsg);
                }
            } else {
                String successMsg = "✅ %s - Installation completed in %ss (no verification)".formatted(name, duration);
                print(successMsg);
                SUMMARY.add(successMsg);
            }
            
        } catch (Exception e) {
            String errorMsg = "❌ %s - Installation failed: %s".formatted(name, e.getMessage());
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

    /* ---------- CHECKER METHODS ---------- */
    private static void checkHomebrew() {
        if (!exec("which brew")) {
            throw new RuntimeException("Homebrew is not installed or not in PATH");
        }
        print("Homebrew is installed at: " + execWithOutput("which brew").trim());
    }
    
    private static void checkNodeNpm() {
        if (!exec("which node")) {
            throw new RuntimeException("Node.js is not installed or not in PATH");
        }
        print("Node.js version: " + execWithOutput("node --version").trim());
        print("npm version: " + execWithOutput("npm --version").trim());
    }
    
    private static void checkChrome() {
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            if (!exec("osascript -e 'exists application \"Google Chrome\"'")) {
                throw new RuntimeException("Google Chrome is not installed");
            }
            print("Google Chrome is installed");
        } else {
            print("Chrome check not implemented for this OS");
        }
    }
    
    private static void checkAppium() {
        if (!exec("which appium")) {
            throw new RuntimeException("Appium is not installed or not in PATH");
        }
        print("Appium version: " + execWithOutput("appium --version").trim());
    }
    
    private static void checkSelenium() {
        // Selenium is a Java library, so we can't easily check its installation from the command line
        print("Note: Selenium is a Java library and will be managed by Maven");
    }
    
    private static void checkUiAutomator2() {
        if (!exec("npm list -g appium-uiautomator2-driver")) {
            throw new RuntimeException("Appium UiAutomator2 driver is not installed globally");
        }
        print("UiAutomator2 driver is installed");
    }
    
    private static void checkXcuitest() {
        if (!exec("npm list -g appium-xcuitest-driver")) {
            throw new RuntimeException("Appium XCUITest driver is not installed globally");
        }
        print("XCUITest driver is installed");
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
            
            // Read the output silently for exec (used in checks)
            try (InputStream inputStream = process.getInputStream();
                 InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                 BufferedReader reader = new BufferedReader(inputStreamReader)) {
                
                // Read all lines but don't print them for silent checks
                while (reader.readLine() != null) {
                    // Just consume the output
                }
            }
            
            // Wait for the process to complete
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                return false;
            }
            
            return process.exitValue() == 0;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    private static String execWithOutput(String command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
            processBuilder.redirectErrorStream(true);
            
            Process process = processBuilder.start();
            
            // Read the output
            StringBuilder output = new StringBuilder();
            try (InputStream inputStream = process.getInputStream();
                 InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                 BufferedReader reader = new BufferedReader(inputStreamReader)) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // Wait for the process to complete
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                throw new RuntimeException(String.format("Command timed out after %d seconds: %s", TIMEOUT_SECONDS, command));
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException(String.format("Command failed with exit code %d: %s", exitCode, command));
            }
            
            return output.toString().trim();
            
        } catch (Exception e) {
            throw new RuntimeException(String.format("Command failed: %s - %s", command, e.getMessage()), e);
        }
    }
}
