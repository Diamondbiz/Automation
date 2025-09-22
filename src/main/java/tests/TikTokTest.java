package tests;

public class TikTokTest {
    
    public static void main(String[] args) {
        openTikTokLiveInDebugMode();
    }
    
    public static void openTikTokLiveInDebugMode() {
        try {
            // Path to Chrome executable
            String chromePath = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome";
            
            // Print debug information
            System.out.println("🛠️  Opening Chrome in debug mode...");
            System.out.println("🔗 URL: https://www.tiktok.com/@auto_monako/live");
            System.out.println("🔌 Remote debugging port: 9222");
            System.out.println("\n✅ How to verify debug mode:");
            System.out.println("1. Open a new Chrome window");
            System.out.println("2. Go to: chrome://inspect");
            System.out.println("3. Look for 'Remote Target' with the TikTok URL");
            
            // Command to open Chrome with debug mode and your active profile
            String[] command = {
                chromePath,
                "--remote-debugging-port=9222",
                "--user-data-dir=" + System.getProperty("user.home") + "/Library/Application Support/Google/Chrome",
                "--profile-directory=Default",
                "--new-window",  // Open in a new window
                "https://www.tiktok.com/@auto_monako/live"
            };
            
            // Execute the command
            Process process = Runtime.getRuntime().exec(command);
            
        } catch (Exception e) {
            System.err.println("❌ Error opening Chrome in debug mode:");
            e.printStackTrace();
        }
    }
}