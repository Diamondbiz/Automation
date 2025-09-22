# Automation Environment Manager

This project provides tools for managing and automating the setup of a complete mobile and web test automation environment. It includes the `AutoEnvUpdater` utility that simplifies the installation and configuration of all necessary dependencies for test automation.

## Features

- **Automated Environment Setup**: One-command installation of all required tools
- **Cross-Platform**: Supports macOS (primary) with Windows/Linux support planned
- **Version Management**: Ensures correct versions of all tools are installed
- **Parallel Execution**: Speeds up environment setup with parallel installations
- **Dry Run Mode**: Preview changes before applying them
- **Configuration File**: Customize versions and settings via JSON configuration

## AutoEnvUpdater

The `AutoEnvUpdater` is a Java-based tool that automates the setup of your test automation environment. It handles installation and updates for:

- **Package Managers**: Homebrew (macOS)
- **Runtime**: Node.js and npm
- **Browsers**: Google Chrome
- **Testing Tools**:
  - Appium
  - Selenium WebDriver
  - UiAutomator2 (Android)
  - XCUITest (iOS)

## Prerequisites

- **Java 21** (JDK 21)
- **macOS** (primary supported platform)
- **Administrator privileges** (for software installation)

## Prerequisites

1. **Java 21** (JDK 21)
2. **Maven** (3.6.0 or higher)
3. **Appium** (2.0.0 or higher)
4. **Android Studio** (for Android emulators)
5. **Xcode** (for iOS simulators, macOS only)
6. **Node.js** (for Appium)
7. **Appium Doctor** (to verify setup)

## Quick Start

1. **Clone and Build**
   ```bash
   git clone <your-repo-url>
   cd Automation
   mvn clean package
   ```

2. **Run AutoEnvUpdater**
   ```bash
   # Basic usage (interactive mode)
   java -cp target/classes tests.AutoEnvUpdater
   
   # Dry run (preview changes)
   java -cp target/classes tests.AutoEnvUpdater --dry-run
   
   # Parallel execution (faster)
   java -cp target/classes tests.AutoEnvUpdater --parallel
   
   # Custom configuration
   java -cp target/classes tests.AutoEnvUpdater --config path/to/custom-config.json
   ```

## Configuration

The tool uses a JSON configuration file (`config/environment.json`) to manage versions and settings. Example:

```json
{
  "node": {
    "min_version": "18.0.0",
    "npm_version": "9.0.0"
  },
  "appium": {
    "version": "3.0.0",
    "uiautomator2_version": "5.0.0",
    "xcuitest_version": "5.0.0"
  },
  "selenium": {
    "version": "4.11.0"
  },
  "chrome": {
    "download_url": "https://dl.google.com/chrome/mac/stable/GGRO/googlechrome.dmg"
  }
}
```

### Command Line Options

| Option | Description | Default |
|--------|-------------|---------|
| `--dry-run` | Preview changes without making them | false |
| `--parallel` | Run installations in parallel | false |
| `--config <path>` | Path to custom configuration file | `config/environment.json` |

## Manual Setup (Alternative)

If you prefer to set up the environment manually, follow these steps:

## Development

### Building from Source

```bash
git clone <your-repo-url>
cd Automation
mvn clean package
```

### Running Tests

#### Unit Tests
```bash
mvn test
```

#### Integration Tests
```bash
mvn verify
```

## Troubleshooting

### Common Issues

1. **Permission Denied**
   - Ensure you have administrator privileges
   - Try running with `sudo` if on macOS/Linux

2. **Network Issues**
   - Check your internet connection
   - Configure proxy settings if needed

3. **Version Conflicts**
   - Check the configuration file for version requirements
   - Update the configuration if needed

## Contributing

1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a new Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Running Tests

### For Android

1. Ensure your environment is set up using AutoEnvUpdater
2. Start an Android emulator or connect a physical device
2. Run the tests:

```bash
mvn test -Dtest=AndroidTest
```

### For iOS (macOS only)

1. Start an iOS simulator or connect a physical device
2. Uncomment the iOS test section in `testng.xml`
3. Run the tests:

```bash
mvn test -Dtest=iOSTest
```

## Project Structure

```
src/
├── main/
│   └── java/
│       └── org/example/
│           └── AutoEnvUpdater.java
└── test/
    ├── java/
    │   └── tests/
    │       ├── AndroidTest.java
    │       ├── BaseTest.java
    │       └── iOSTest.java
    └── resources/
        └── (Test APKs/IPAs go here)
```

## Configuration

- `testng.xml`: Test configuration and parameters
- `pom.xml`: Maven dependencies and build configuration

## Troubleshooting

1. **Appium Server Not Starting**
   - Ensure Appium is installed globally
   - Check if the required ports (4723) are available

2. **Android Device Not Found**
   - Make sure USB debugging is enabled on your device
   - Run `adb devices` to verify device connection

3. **iOS Simulator Issues**
   - Ensure Xcode command line tools are installed
   - Verify the simulator is properly set up in Xcode

## Running with Different Configurations

You can override test parameters using Maven properties:

```bash
mvn test -DplatformName=Android -DplatformVersion=13.0 -DdeviceName=Pixel_6_API_33
```

## CI/CD Integration

This project is ready for CI/CD integration. Example GitHub Actions workflow is provided in `.github/workflows/`.
