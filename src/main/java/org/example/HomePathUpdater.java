package org.example;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;
import static java.awt.Color.*;

/**
 * HomePathUpdater
 * 
 * This class is responsible for managing the home path in the ~/.zshrc file.
 * It checks, installs, updates, and verifies the home path configuration.
 */
public class HomePathUpdater {
    // Color constants for console output
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String CHECK_MARK = "✓";
    
    private static final String ZSHRC_PATH = System.getProperty("user.home") + "/.zshrc";
    private static final String BACKUP_DIR = System.getProperty("user.home") + "/.zsh_backups";
    private static final List<String> SUMMARY = new ArrayList<>();
    private static final List<String> SUGGESTIONS = new ArrayList<>();
    
    // Patterns to identify path-related configurations
    private static final Pattern EXPORT_PATH_PATTERN = Pattern.compile("^export PATH=.*");
    private static final Pattern SOURCE_PATTERN = Pattern.compile("^source\s+.*");
    
    public static void main(String[] args) {
        try {
            print("🚀 Starting Home Path Configuration Update");
            print("=".repeat(50));
            
            // Check if .zshrc exists, create if it doesn't
            ensureZshrcExists();
            
            // Process the .zshrc file
            processZshrcFile();
            
            // Print summary
            print("\n📋 Update Summary:");
            print("-".repeat(30));
            if (SUMMARY.isEmpty()) {
                print("✅ No changes were needed. Everything is up to date.");
            } else {
                SUMMARY.forEach(HomePathUpdater::print);
            }
            
            print("\n✅ Home path configuration completed successfully!");
        } catch (Exception e) {
            print("\n❌ Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void ensureZshrcExists() throws IOException {
        File zshrc = new File(ZSHRC_PATH);
        if (!zshrc.exists()) {
            print("⚠️  .zshrc file not found. Creating a new one...");
            if (zshrc.createNewFile()) {
                SUMMARY.add("✅ Created new .zshrc file");
            } else {
                throw new IOException("Failed to create .zshrc file");
            }
        }
    }
    
    private static void processZshrcFile() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(ZSHRC_PATH));
        boolean modified = false;
        
        // Check for existing PATH export
        boolean hasPathExport = lines.stream()
            .anyMatch(line -> EXPORT_PATH_PATTERN.matcher(line.trim()).matches());
            
        // Check for source commands
        boolean hasSourceCommands = lines.stream()
            .anyMatch(line -> SOURCE_PATTERN.matcher(line.trim()).matches());
            
        // Add or update PATH export if needed
        String homePath = System.getProperty("user.home");
        String pathExport = String.format("export PATH=\"%s/.local/bin:$PATH\"", homePath);
        
        if (!hasPathExport) {
            print("➕ Adding PATH export to .zshrc");
            lines.add("");
            lines.add("# Set PATH to include user's local bin if it exists");
            lines.add(pathExport);
            modified = true;
            SUMMARY.add("✅ Added PATH export to .zshrc");
        } else {
            // Update existing PATH export if needed
            for (int i = 0; i < lines.size(); i++) {
                if (EXPORT_PATH_PATTERN.matcher(lines.get(i).trim()).matches() && 
                    !lines.get(i).contains("$HOME/.local/bin")) {
                    print("🔄 Updating PATH export in .zshrc");
                    lines.set(i, pathExport);
                    modified = true;
                    SUMMARY.add("✅ Updated PATH export in .zshrc");
                    break;
                }
            }
        }
        
        // Add common source commands if they don't exist
        List<String> sourceCommands = Arrays.asList(
            "",
            "# Source additional configurations",
            "if [ -f ~/.bash_aliases ]; then",
            "    source ~/.bash_aliases",
            "fi",
            "",
            "if [ -f ~/.profile ]; then",
            "    source ~/.profile",
            "fi"
        );
        
        if (!hasSourceCommands) {
            print("➕ Adding common source commands to .zshrc");
            lines.addAll(sourceCommands);
            modified = true;
            SUMMARY.add("✅ Added common source commands to .zshrc");
        }
        
        // Write changes back to file if modified
        if (modified) {
            Files.write(Paths.get(ZSHRC_PATH), lines);
            print("\n🔍 Verifying changes...");
            verifyZshrcFile();
        } else {
            print("ℹ️  No changes needed. .zshrc is already properly configured.");
        }
    }
    
    private static void verifyZshrcFile() throws IOException {
        print("\n🔍 Verifying .zshrc file...");
        
        // Read the file again to ensure we're seeing the latest changes
        List<String> lines = Files.readAllLines(Paths.get(ZSHRC_PATH));
        
        // Find the PATH export line
        Optional<String> pathLine = lines.stream()
            .filter(line -> line.trim().startsWith("export PATH="))
            .findFirst();
            
        if (pathLine.isPresent()) {
            String pathValue = pathLine.get();
            print("ℹ️  Found PATH export: " + pathValue);
            
            // Check for different possible path formats
            boolean containsLocalBin = pathValue.contains("$HOME/.local/bin") || 
                                     pathValue.contains("~/.local/bin") ||
                                     pathValue.contains(System.getProperty("user.home") + "/.local/bin");
            
            if (containsLocalBin) {
                print("✅ Verified: PATH export includes user's local bin directory");
                
                // Check if the path is properly quoted
                if ((pathValue.contains("\"") && pathValue.contains("$PATH")) ||
                    (pathValue.contains("'$") && pathValue.contains("PATH'"))) {
                    print("✅ Verified: PATH export is properly quoted");
                } else {
                    print("⚠️  Warning: PATH export might not be properly quoted. Consider using: \"$HOME/.local/bin:$PATH\"");
                }
                
                // Check for duplicate PATH entries
                long pathCount = lines.stream()
                    .filter(line -> line.trim().startsWith("export PATH="))
                    .count();
                    
                if (pathCount > 1) {
                    print("⚠️  Warning: Found " + pathCount + " PATH exports. There should be only one.");
                }
                
                // Verify the file is readable and writable
                File zshrc = new File(ZSHRC_PATH);
                if (!zshrc.canRead()) {
                    print("❌ Error: Cannot read .zshrc file");
                } else if (!zshrc.canWrite()) {
                    print("❌ Error: Cannot write to .zshrc file");
                } else {
                    print("✅ Verified: .zshrc has correct permissions");
                }
                
                // Check if the file ends with a newline
                String fileContent = String.join("\n", lines);
                if (!fileContent.endsWith("\n") && !lines.isEmpty()) {
                    print("⚠️  Warning: .zshrc does not end with a newline. Some shells might have issues.");
                }
                
                return; // Success
            }
        }
        
        // If we get here, verification failed
        print("\n❌ Verification failed: PATH export not properly configured in .zshrc");
        print("   Expected to find a line like one of these:");
        print("   export PATH=\"$HOME/.local/bin:$PATH\"");
        print("   export PATH=~/.local/bin:$PATH");
        print("   export PATH=" + System.getProperty("user.home") + "/.local/bin:$PATH");
        print("\n   You can manually verify by running:");
        print("   cat ~/.zshrc | grep PATH=");
        print("   or");
        print("   nano ~/.zshrc");
    }
    
    // File backup methods
    private static void createBackupDirectory() throws IOException {
        File backupDir = new File(BACKUP_DIR);
        if (!backupDir.exists()) {
            if (backupDir.mkdirs()) {
                info("Created backup directory: " + BACKUP_DIR);
            } else {
                warning("Failed to create backup directory: " + BACKUP_DIR);
            }
        }
    }
    
    private static void createBackup() throws IOException {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String backupPath = BACKUP_DIR + "/zshrc_backup_" + timestamp;
        
        File original = new File(ZSHRC_PATH);
        if (!original.exists()) {
            return; // No need to backup if file doesn't exist yet
        }
        
        Files.copy(original.toPath(), Paths.get(backupPath), StandardCopyOption.REPLACE_EXISTING);
        info("Created backup at: " + backupPath);
    }
    
    // Output formatting methods
    private static void print(String message) {
        System.out.println(message);
    }
    
    private static void printHeader(String message) {
        print("\n" + BLUE + "=".repeat(80) + RESET);
        print(BLUE + message + RESET);
        print(BLUE + "=".repeat(80) + RESET);
    }
    
    private static void printSection(String message) {
        print("\n" + CYAN + message + RESET);
        print(CYAN + "-".repeat(Math.min(80, message.length())) + RESET);
    }
    
    private static void success(String message) {
        SUMMARY.add(GREEN + CHECK_MARK + " " + message + RESET);
        print(GREEN + "[SUCCESS] " + message + RESET);
    }
    
    private static void info(String message) {
        print(BLUE + "[INFO] " + message + RESET);
    }
    
    private static void warning(String message) {
        print(YELLOW + "[WARNING] " + message + RESET);
    }
    
    private static void error(String message) {
        print(RED + "[ERROR] " + message + RESET);
    }
    
    private static void suggestion(String suggestion) {
        SUGGESTIONS.add(suggestion);
        print(YELLOW + "  → " + suggestion + RESET);
    }
}
