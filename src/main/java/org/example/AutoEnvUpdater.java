package org.example;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * AutomationEnvironmentUpdater
 * macOS – Java 17
 *
 * 1.  Homebrew
 * 2.  Node.js & npm (via brew)
 * 3.  Chrome browser (dmg install)
 * 4.  Appium (npm global)
 * 5.  Selenium WebDriver (npm global)
 * 6.  UiAutomator2 driver  (npm global)
 * 7.  XCUITest driver      (npm global)
 *
 * Every action is printed to stdout.
 * At the end a summary is printed what was installed or upgraded.
 */
public class AutoEnvUpdater {

    /* ---------- CONFIGURATION ---------- */
    private static final Path CHROME_DMG = Paths.get(System.getProperty("user.home"))
            .resolve("Downloads/googlechrome.dmg");
    private static final List<String> SUMMARY = new ArrayList<>();
    private static final int COMMAND_TIMEOUT_SECONDS = 120;
    private static final Pattern VERSION_PATTERN = Pattern.compile("^v?(\\d+)\\.(\\d+)\\.(\\d+)");

    /* ---------- ENTRY ---------- */
    public static void main(String[] args) throws Exception {
        print("==========  Automation Environment Updater  ==========");
        ensureHomebrew();
        ensureNodeNpm();
        ensureChrome();
        ensureAppium();
        ensureSelenium();
        ensureUiAutomator2();
        ensureXcuitestDriver();
        print("\n==========  SUMMARY  ==========");
        SUMMARY.forEach(AutoEnvUpdater::print);
        print("==========  ALL DONE  ==========");
    }

    /* ---------- STEP 0 : OPEN TERMINAL ---------- */
    private static void openTerminal() throws IOException {
        print("Opening Terminal …");
        new ProcessBuilder("open", "-a", "Terminal").inheritIO().start();
        sleep(2);
        print("✅ Terminal is open.\n");
    }

    /* ---------- HOMEBREW ---------- */
    private static void ensureHomebrew() throws Exception {
        printStep("Homebrew");
        if (execSuccess("brew --version")) {
            print("Homebrew is already installed.");
            if (execSuccess("brew outdated")) {
                print("Upgrading Homebrew …");
                exec("brew update && brew upgrade");
                SUMMARY.add("Homebrew upgraded.");
            } else {
                SUMMARY.add("Homebrew already up-to-date.");
            }
        } else {
            print("Homebrew not found – installing …");
            String installScript = "/bin/bash -c \"$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)\"";
            exec(installScript);
            SUMMARY.add("Homebrew installed.");
        }
    }

    /* ---------- NODE & NPM ---------- */
    private static void ensureNodeNpm() throws Exception {
        printStep("Node.js & npm");
        
        // Check Node.js version
        Optional<String> nodeVersion = getCommandOutput("node --version");
        if (nodeVersion.isEmpty() || !isNodeVersionCompatible(nodeVersion.get())) {
            print("Node.js missing or incompatible – installing/upgrading via brew…");
            exec("brew install node");
            
            // Verify installation
            nodeVersion = getCommandOutput("node --version");
            if (nodeVersion.isPresent() && isNodeVersionCompatible(nodeVersion.get())) {
                SUMMARY.add("Node.js installed/upgraded to " + nodeVersion.get().trim());
            } else {
                throw new RuntimeException("Failed to install/upgrade Node.js to a compatible version");
            }
        } else {
            SUMMARY.add("Node.js " + nodeVersion.get().trim() + " is already installed and compatible.");
        }

        // Check npm version
        Optional<String> npmVersion = getCommandOutput("npm --version");
        if (npmVersion.isEmpty()) {
            print("npm missing – installing via brew…");
            exec("brew install npm");
            SUMMARY.add("npm installed.");
        } else {
            SUMMARY.add("npm " + npmVersion.get().trim() + " is already installed.");
        }
    }
    
    private static boolean isNodeVersionCompatible(String version) {
        try {
            Matcher m = VERSION_PATTERN.matcher(version.trim());
            if (!m.find()) return false;
            
            int major = Integer.parseInt(m.group(1));
            int minor = Integer.parseInt(m.group(2));
            
            // Check for Node.js >= 24.0.0
            if (major > 24) return true;
            if (major == 24) return true;
            
            // Check for Node.js 22.12.0 or later
            if (major == 22 && minor >= 12) return true;
            
            // Check for Node.js 20.19.0 or later
            if (major == 20 && minor >= 19) return true;
            
            return false;
        } catch (Exception e) {
            print("Error parsing Node.js version: " + e.getMessage());
            return false;
        }
    }

    /* ---------- CHROME ---------- */
    private static void ensureChrome() throws Exception {
        printStep("Google Chrome");
        if (execSuccess("/Applications/Google\\ Chrome.app/Contents/MacOS/Google\\ Chrome --version")) {
            SUMMARY.add("Chrome already installed and up-to-date.");
            return;
        }
        print("Chrome not found – downloading …");
        Files.deleteIfExists(CHROME_DMG);
        exec("curl -L -o " + CHROME_DMG + " https://dl.google.com/chrome/mac/stable/GGRO/googlechrome.dmg");
        print("Mounting and installing Chrome …");
        exec("hdiutil attach " + CHROME_DMG + " -quiet");
        exec("cp -R /Volumes/Google\\ Chrome/Google\\ Chrome.app /Applications/");
        exec("hdiutil detach /Volumes/Google\\ Chrome -quiet");
        Files.deleteIfExists(CHROME_DMG);
        SUMMARY.add("Chrome installed.");
    }

    /* ---------- APPIUM ---------- */
    private static void ensureAppium() throws Exception {
        printStep("Appium");
        
        // Check if Appium is installed and get its version
        Optional<String> appiumVersion = getCommandOutput("appium --version");
        
        if (appiumVersion.isPresent()) {
            print("Appium " + appiumVersion.get().trim() + " found – checking for updates…");
            exec("npm install -g appium@latest");
            
            // Verify the new version
            Optional<String> newVersion = getCommandOutput("appium --version");
            if (newVersion.isPresent() && !newVersion.get().trim().equals(appiumVersion.get().trim())) {
                SUMMARY.add("Appium upgraded from " + appiumVersion.get().trim() + " to " + newVersion.get().trim());
            } else {
                SUMMARY.add("Appium is already the latest version (" + appiumVersion.get().trim() + ")");
            }
        } else {
            print("Appium not found – installing latest version…");
            exec("npm install -g appium@latest");
            
            // Verify installation
            Optional<String> installedVersion = getCommandOutput("appium --version");
            if (installedVersion.isPresent()) {
                SUMMARY.add("Appium " + installedVersion.get().trim() + " installed");
            } else {
                throw new RuntimeException("Failed to verify Appium installation");
            }
        }
    }

    /* ---------- SELENIUM ---------- */
    private static void ensureSelenium() throws Exception {
        printStep("Selenium WebDriver");
        if (execSuccess("selenium-side-runner --version")) {
            exec("npm install -g selenium-webdriver@latest");
            SUMMARY.add("Selenium WebDriver upgraded.");
        } else {
            exec("npm install -g selenium-webdriver");
            SUMMARY.add("Selenium WebDriver installed.");
        }
    }

    /* ---------- UiAutomator2 driver ---------- */
    private static void ensureUiAutomator2() throws Exception {
        printStep("UiAutomator2 driver");
        
        // Check if UiAutomator2 driver is installed
        boolean isInstalled = getCommandOutput("appium driver list --installed --json")
                .map(output -> output.contains("uiautomator2"))
                .orElse(false);
        
        if (isInstalled) {
            print("UiAutomator2 driver found – checking for updates…");
            exec("appium driver update uiautomator2");
            SUMMARY.add("UiAutomator2 driver updated to the latest version.");
        } else {
            print("UiAutomator2 driver not found – installing…");
            exec("appium driver install uiautomator2");
            
            // Verify installation
            if (getCommandOutput("appium driver list --installed --json")
                    .map(output -> output.contains("uiautomator2"))
                    .orElse(false)) {
                SUMMARY.add("UiAutomator2 driver installed successfully.");
            } else {
                throw new RuntimeException("Failed to verify UiAutomator2 driver installation");
            }
        }
    }

    /* ---------- XCUITest driver ---------- */
    private static void ensureXcuitestDriver() throws Exception {
        printStep("XCUITest driver");
        
        // Check if XCUITest driver is installed
        boolean isInstalled = getCommandOutput("appium driver list --installed --json")
                .map(output -> output.contains("xcuitest"))
                .orElse(false);
        
        if (isInstalled) {
            // Only update if there's an update available
            print("XCUITest driver found – checking for updates…");
            Optional<String> updateOutput = getCommandOutput("appium driver update xcuitest --dry-run");
            
            if (updateOutput.isPresent() && updateOutput.get().contains("would be updated")) {
                // Only update if there's actually an update available
                exec("appium driver update xcuitest");
                SUMMARY.add("XCUITest driver updated to the latest version.");
            } else {
                print("XCUITest driver is already up to date.");
                SUMMARY.add("XCUITest driver is already up to date.");
            }
        } else {
            print("XCUITest driver not found – installing…");
            exec("appium driver install xcuitest");
            
            // Verify installation
            if (getCommandOutput("appium driver list --installed --json")
                    .map(output -> output.contains("xcuitest"))
                    .orElse(false)) {
                SUMMARY.add("XCUITest driver installed successfully.");
            } else {
                throw new RuntimeException("Failed to verify XCUITest driver installation");
            }
        }
    }

    /* ---------- HELPERS ---------- */
    private static void printStep(String tool) {
        print("\n-----  " + tool + "  -----");
    }

    private static void print(String msg) {
        System.out.println("[" + java.time.LocalTime.now().withNano(0) + "]  " + msg);
    }

    private static void sleep(int sec) {
        try { Thread.sleep(sec * 1000L); } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Thread was interrupted", e);
        }
    }

    /**
     * Executes a command and returns its output if successful
     * @param cmd The command to execute
     * @return Optional containing the command output if successful, empty otherwise
     */
    private static Optional<String> getCommandOutput(String cmd) throws IOException, InterruptedException {
        Process process = null;
        try {
            process = new ProcessBuilder("zsh", "-c", cmd)
                    .redirectErrorStream(true)
                    .start();
            
            // Read output with timeout
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // Wait for process to complete with timeout
            if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                print("Command timed out: " + cmd);
                return Optional.empty();
            }
            
            if (process.exitValue() == 0) {
                return Optional.of(output.toString().trim());
            } else {
                print("Command failed with exit code " + process.exitValue() + ": " + cmd);
                return Optional.empty();
            }
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    /**
     * Checks if a command executes successfully
     */
    private static boolean execSuccess(String cmd) throws IOException, InterruptedException {
        return getCommandOutput(cmd).isPresent();
    }

    /**
     * Executes a command and throws an exception if it fails
     */
    private static void exec(String cmd) throws IOException, InterruptedException {
        print("Executing: " + cmd);
        
        Process process = null;
        try {
            process = new ProcessBuilder("zsh", "-c", cmd)
                    .inheritIO()
                    .start();
            
            // Wait for process to complete with timeout
            if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                throw new RuntimeException("Command timed out after " + COMMAND_TIMEOUT_SECONDS + " seconds: " + cmd);
            }
            
            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new RuntimeException("Command failed with exit code " + exitCode + ": " + cmd);
            }
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }
}