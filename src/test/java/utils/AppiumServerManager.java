package utils;

import io.appium.java_client.service.local.AppiumDriverLocalService;
import io.appium.java_client.service.local.AppiumServiceBuilder;
import io.appium.java_client.service.local.flags.GeneralServerFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manages the lifecycle of the Appium server with advanced configuration options.
 * Provides methods to start, stop, and monitor the Appium server with retry logic and health checks.
 */
@SuppressWarnings({"unused", "FieldMayBeFinal"})
public class AppiumServerManager {
    private static final Logger logger = LoggerFactory.getLogger(AppiumServerManager.class);
    private static AppiumServerManager instance;
    private AppiumDriverLocalService service;
    private final ServerConfig config;

    /**
     * Builder class for configuring the Appium server.
     */
    public static class Builder {
        private String host = "127.0.0.1";
        private int port = 4723;
        private int connectionTimeoutMs = 5000;
        private int readTimeoutMs = 10000;
        private int startupRetries = 3;
        private long retryDelayMs = 2000;
        private String logFilePath = "appium-server.log";
        private Map<String, String> environment = new HashMap<>();
        private boolean showServerLogs = false;

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withConnectionTimeout(int timeout, TimeUnit unit) {
            this.connectionTimeoutMs = (int) unit.toMillis(timeout);
            return this;
        }

        public Builder withReadTimeout(int timeout, TimeUnit unit) {
            this.readTimeoutMs = (int) unit.toMillis(timeout);
            return this;
        }

        public Builder withStartupRetries(int retries) {
            this.startupRetries = retries;
            return this;
        }

        public Builder withRetryDelay(long delay, TimeUnit unit) {
            this.retryDelayMs = unit.toMillis(delay);
            return this;
        }

        public Builder withLogFile(String filePath) {
            this.logFilePath = filePath;
            return this;
        }

        public Builder withEnvironmentVariable(String key, String value) {
            this.environment.put(key, value);
            return this;
        }

        public Builder showServerLogs(boolean show) {
            this.showServerLogs = show;
            return this;
        }

        public AppiumServerManager build() {
            return new AppiumServerManager(new ServerConfig(
                    host, port, connectionTimeoutMs, readTimeoutMs,
                    startupRetries, retryDelayMs, logFilePath,
                    environment, showServerLogs
            ));
        }
    }

    /**
     * Configuration for the Appium server.
     *
     * @param host the server host
     * @param port the server port
     * @param connectionTimeoutMs connection timeout in milliseconds
     * @param readTimeoutMs read timeout in milliseconds
     * @param startupRetries number of retry attempts on startup
     * @param retryDelayMs delay between retry attempts in milliseconds
     * @param logFilePath path to the log file
     * @param environment environment variables for the server
     * @param showServerLogs whether to show server logs
     */
    public record ServerConfig(
            String host,
            int port,
            int connectionTimeoutMs,
            int readTimeoutMs,
            int startupRetries,
            long retryDelayMs,
            String logFilePath,
            Map<String, String> environment,
            boolean showServerLogs
    ) {
        // Create a defensive copy of the environment map
        public ServerConfig {
            environment = new HashMap<>(environment);
        }
    }

    private AppiumServerManager(ServerConfig config) {
        this.config = config;
    }

    /**
     * Gets or creates an instance with default configuration.
     *
     * @return the AppiumServerManager instance
     */
    public static synchronized AppiumServerManager getInstance() {
        if (instance == null) {
            instance = new Builder().build();
        }
        return instance;
    }

    /**
     * Creates a new instance with custom configuration.
     */
    public static AppiumServerManager createInstance(Builder builder) {
        return builder.build();
    }

    /**
     * Starts the Appium server with retry logic.
     *
     * @throws AppiumServerException if the server fails to start after all retry attempts
     */
    public synchronized void startServer() throws AppiumServerException {
        if (isServerRunning()) {
            logger.info("Appium server is already running on {}:{}", config.host, config.port);
            return;
        }

        int attempt = 0;
        Exception lastError = null;

        while (attempt < config.startupRetries) {
            try {
                attempt++;
                logger.info("Starting Appium server (attempt {}/{})...", attempt, config.startupRetries);
                
                AppiumServiceBuilder builder = new AppiumServiceBuilder()
                        .withIPAddress(config.host)
                        .usingPort(config.port)
                        .withArgument(GeneralServerFlag.LOG_LEVEL, "debug")
                        .withLogFile(new File(config.logFilePath));

                // Add environment variables if any
                if (!config.environment.isEmpty()) {
                    builder.withEnvironment(config.environment);
                }

                if (config.showServerLogs) {
                    builder.withArgument(GeneralServerFlag.LOG_LEVEL, "debug");
                }

                service = builder.build();
                service.start();

                if (isServerRunning()) {
                    logger.info("Appium server started successfully on {}:{}", config.host, config.port);
                    return;
                }

                // If we get here, the server started but isn't responding
                service.stop();
                throw new AppiumServerException("Appium server started but is not responding");

            } catch (Exception e) {
                lastError = e;
                try {
                    if (service != null && service.isRunning()) {
                        service.stop();
                    }
                } catch (Exception stopEx) {
                    logger.warn("Error while stopping Appium server after failed start attempt: {}", stopEx.getMessage());
                }
                
                if (attempt < config.startupRetries) {
                    logger.warn("Attempt {}/{} failed: {}", attempt, config.startupRetries, e.getMessage());
                    logger.debug("Error details:", e);
                    try {
                        Thread.sleep(config.retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new AppiumServerException("Server startup was interrupted", ie);
                    }
                } else {
                    logger.error("All {} startup attempts failed", config.startupRetries, lastError);
                }
            }
        }

        String errorMsg = String.format("Failed to start Appium server after %d attempts", config.startupRetries);
        logger.error(errorMsg);
        throw new AppiumServerException(errorMsg, lastError);
    }

    /**
     * Stops the Appium server if it's currently running.
     */
    public synchronized void stopServer() {
        if (service == null) {
            return;
        }

        try {
            if (service.isRunning()) {
                logger.info("Stopping Appium server...");
                service.stop();
                logger.info("Appium server stopped successfully");
            }
        } catch (Exception e) {
            logger.error("Error while stopping Appium server: {}", e.getMessage(), e);
        } finally {
            service = null;
        }
    }

    /**
     * Checks if the Appium server is running and responsive.
     */
    public boolean isServerRunning() {
        if (service == null || !service.isRunning()) {
            return false;
        }

        String statusUrl = String.format("http://%s:%d/wd/hub/status", config.host, config.port);
        HttpURLConnection connection = null;

        try {
            URL url = new URI(statusUrl).toURL();
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(config.connectionTimeoutMs);
            connection.setReadTimeout(config.readTimeoutMs);

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                logger.debug("Appium server is running and responsive at {}", statusUrl);
                return true;
            }
            logger.warn("Appium server returned unexpected status code: {}", responseCode);
            return false;
        } catch (Exception e) {
            logger.debug("Appium server is not responding: {}", e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    /**
     * Restarts the Appium server.
     */
    public synchronized void restartServer() throws AppiumServerException {
        stopServer();
        startServer();
    }

    /**
     * Gets the current Appium service instance.
     */
    public AppiumDriverLocalService getService() {
        return service;
    }

    /**
     * Gets the server configuration.
     */
    public ServerConfig getConfig() {
        return config;
    }

    /**
     * Custom exception for Appium server related errors.
     */
    public static final class AppiumServerException extends RuntimeException {
        public AppiumServerException(String message) {
            super(message);
        }

        public AppiumServerException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
