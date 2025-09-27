package WebBrowser;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class OpenBrowser {
    
    public static void main(String[] args) {
        String url = "https://www.tiktok.com/@skvirtme/live";
        
        try {
            // Kill any existing Chrome processes
           // System.out.println("Closing any existing Chrome processes...");
           // killChromeProcesses();
            
            // Open Chrome in a new process
            System.out.println("Opening Chrome...");
            ProcessBuilder pb = new ProcessBuilder(
                "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe",
                "--new-window",
                "--start-maximized",
                url
            );
            
            // Start the process
            Process chromeProcess = pb.start();
            
            // Start a separate thread to read process output (prevents hanging)
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(chromeProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("Chrome: " + line);
                    }
                } catch (Exception e) {
                    // Ignore
                }
            }).start();
            
            // Countdown
            System.out.println("Countdown started. Browser will close in 60 seconds...");
            for (int i = 60; i > 0; i--) {
                System.out.print("\rClosing in: " + i + " seconds   ");
                Thread.sleep(1000);
            }
            
            // Close Chrome
            System.out.println("\nClosing Chrome...");
            chromeProcess.destroy();
            if (chromeProcess.isAlive()) {
                chromeProcess.destroyForcibly();
            }
            
            // Final cleanup
            killChromeProcesses();
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            killChromeProcesses();
        }
    }
    
    private static void killChromeProcesses() {
        try {
            // Kill Chrome
            Process process = Runtime.getRuntime().exec("taskkill /F /IM chrome.exe");
            process.waitFor();
            
            // Kill ChromeDriver if it exists
            process = Runtime.getRuntime().exec("taskkill /F /IM chromedriver.exe");
            process.waitFor();
            
            // Give it a moment to close
            Thread.sleep(2000);
        } catch (Exception e) {
            System.err.println("Error killing processes: " + e.getMessage());
        }
    }
}