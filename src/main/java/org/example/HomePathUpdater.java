package org.example;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * This class is responsible for managing the home path in the ~/.zshrc file.
 * It checks, installs, updates, and verifies the home path configuration.
 */
public class HomePathUpdater {
    // Color constants for console output
    private static final Logger logger = LoggerFactory.getLogger(HomePathUpdater.class);
    private static final String RESET = "\u001B[0m";
    private static final String CYAN = "\u001B[36m";
    private static final String CHECK_MARK = "✓";
    
    private static final String ZSHRC_PATH = String.format("%s/.zshrc", System.getProperty("user.home"));
    private static final Path ZSHRC = Paths.get(ZSHRC_PATH);
    private static final String BACKUP_DIR = String.format("%s/.zsh_backups", System.getProperty("user.home"));
    private static final List<String> SUMMARY = new ArrayList<>();
    private static final List<String> SUGGESTIONS = new ArrayList<>();
    
    // Patterns to identify path-related configurations
    private static final Pattern EXPORT_PATH_PATTERN = Pattern.compile("^export PATH=.*");
    private static final Pattern SOURCE_PATTERN = Pattern.compile("^source\\s+.*");
    
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
            logger.error("An error occurred: {}", e.getMessage(), e);
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
        List<String> lines = Files.readAllLines(ZSHRC);
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
            Files.write(ZSHRC, lines);
            print("\n🔍 Verifying changes...");
            verifyZshrcFile();
        } else {
            print("ℹ️  No changes needed. .zshrc is already properly configured.");
        }
    }
    
    private static void verifyZshrcFile() throws IOException {
        print("\n🔍 Verifying .zshrc file...");
        
        // Read the file again to ensure we're seeing the latest changes
        List<String> lines = Files.readAllLines(ZSHRC);
        
        // Clear any previous suggestions
        SUGGESTIONS.clear();
        
        // Find the PATH export line
        Optional<String> pathLine = lines.stream()
            .filter(line -> line.trim().startsWith("export PATH="))
            .findFirst();
            
        if (pathLine.isPresent()) {
            String pathValue = pathLine.get();
            print(String.format("ℹ️  Found PATH export: %s", pathValue));
            
            // Check for different possible path formats
            boolean containsLocalBin = pathValue.contains("$HOME/.local/bin") || 
                                     pathValue.contains("~/.local/bin") ||
                                     pathValue.contains(String.format("%s/.local/bin", System.getProperty("user.home")));
            
            if (containsLocalBin) {
                print("✅ Verified: PATH export includes user's local bin directory");
                
                // Check if the path is properly quoted
                if ((pathValue.contains("\"") && pathValue.contains("$PATH")) ||
                    (pathValue.contains("'$") && pathValue.contains("PATH'"))) {
                    print("✅ Verified: PATH export is properly quoted");
                } else {
                    suggestion("Consider using double quotes around your PATH export: \"$HOME/.local/bin:$PATH\"");
                }
                
                // Check for duplicate PATH entries
                long pathCount = lines.stream()
                    .filter(line -> line.trim().startsWith("export PATH="))
                    .count();
                    
                if (pathCount > 1) {
                    suggestion(String.format("Found %d PATH exports. Consider consolidating them into a single export statement.", pathCount));
                }
                
                // Verify the file is readable and writable
                File zshrc = new File(ZSHRC_PATH);
                if (!zshrc.canRead()) {
                    suggestion("Cannot read .zshrc file. Check file permissions with: chmod u+r ~/.zshrc");
                } else if (!zshrc.canWrite()) {
                    suggestion("Cannot write to .zshrc file. Check file permissions with: chmod u+w ~/.zshrc");
                } else {
                    print("✅ Verified: .zshrc has correct permissions");
                }
                
                // Check if the file ends with a newline
                String fileContent = String.join("\n", lines);
                if (!fileContent.endsWith("\n") && !lines.isEmpty()) {
                    suggestion("Your .zshrc file doesn't end with a newline. This might cause issues. Add a newline at the end of the file.");
                }
                
                // Print any suggestions if they exist
                if (!SUGGESTIONS.isEmpty()) {
                    print("\n💡 Suggestions for improvement:");
                    SUGGESTIONS.forEach(s -> print(String.format("  • %s", s)));
                }
                
                return; // Success
            }
        }
        
        // If we get here, verification failed
        print("\n❌ Verification failed: PATH export not properly configured in .zshrc");
        print("   Expected to find a line like one of these:");
        print("   export PATH=\"$HOME/.local/bin:$PATH\"");
        print("   export PATH=~/.local/bin:$PATH");
        print(String.format("   export PATH=%s/.local/bin:$PATH", System.getProperty("user.home")));
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
                info(String.format("Created backup directory: %s", BACKUP_DIR));
            } else {
                warning(String.format("Failed to create backup directory: %s", BACKUP_DIR));
            }
        }
    }
    
    private static final int MAX_BACKUPS = 10;  // Maximum number of backups to keep
    
    /**
     * Creates a backup of the .zshrc file with timestamp and manages backup rotation.
     * @throws IOException if backup creation fails
     */
    private static void createBackup() throws IOException {
        File original = new File(ZSHRC_PATH);
        if (!original.exists()) {
            info("No existing .zshrc file found. No backup needed.");
            return;
        }
        
        // Ensure backup directory exists
        createBackupDirectory();
        
        // Create timestamped backup
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String backupPath = String.format("%s/zshrc_backup_%s", BACKUP_DIR, timestamp);
        
        try {
            // Create the backup
            Files.copy(original.toPath(), Paths.get(backupPath), StandardCopyOption.REPLACE_EXISTING);
            info(String.format("Created backup at: %s", backupPath));
            
            // Clean up old backups if necessary
            cleanUpOldBackups();
            
        } catch (IOException e) {
            String errorMsg = String.format("Failed to create backup: %s", e.getMessage());
            error(errorMsg);
            throw new IOException(errorMsg, e);
        }
    }
    
    /**
     * Cleans up old backups, keeping only the most recent MAX_BACKUPS files.
     */
    private static void cleanUpOldBackups() {
        try {
            File backupDir = new File(BACKUP_DIR);
            File[] backupFiles = backupDir.listFiles((dir, name) -> name.startsWith("zshrc_backup_"));
            
            if (backupFiles == null || backupFiles.length <= MAX_BACKUPS) {
                return; // No cleanup needed
            }
            
            // Sort by last modified time (oldest first)
            Arrays.sort(backupFiles, Comparator.comparingLong(File::lastModified));
            
            // Delete oldest backups
            for (int i = 0; i < backupFiles.length - MAX_BACKUPS; i++) {
                if (backupFiles[i].delete()) {
                    info(String.format("Cleaned up old backup: %s", backupFiles[i].getName()));
                } else {
                    warning(String.format("Failed to clean up old backup: %s", backupFiles[i].getName()));
                }
            }
        } catch (Exception e) {
            warning("Failed to clean up old backups: %s".formatted(e.getMessage()));
        }
    }
    
    // Output formatting methods
    private static void print(String message) {
        System.out.println(message);
    }
    
    private static void printHeader(String message) {
        print(String.format("%n%s%s%s", BLUE, "=".repeat(80), RESET));
        print(String.format("%s%s%s", BLUE, message, RESET));
        print(BLUE + "=".repeat(80) + RESET);
    }
    
    private static void printSection(String message) {
        print(String.format("%n%s%s%s", CYAN, message, RESET));
        print(String.format("%s%s", CYAN, "-".repeat(Math.min(80, message.length())) + RESET));
    }
    
    private static void success(String message) {
        SUMMARY.add(String.format("%s%s %s%s", GREEN, CHECK_MARK, message, RESET));
        print(String.format("%s[SUCCESS] %s%s", GREEN, message, RESET));
    }
    
    private static void info(String message) {
        print(String.format("%s[INFO] %s%s", BLUE, message, RESET));
    }
    
    private static void warning(String message) {
        print(String.format("%s[WARNING] %s%s", YELLOW, message, RESET));
    }
    
    private static void error(String message) {
        print(String.format("%s[ERROR] %s%s", RED, message, RESET));
    }
    
    private static void suggestion(String suggestion) {
        SUGGESTIONS.add(suggestion);
        print(String.format("%s  → %s%s", YELLOW, suggestion, RESET));
    }
}
