# LG TV Automator — Next TV Login

Java-based automation for LG Smart TVs (webOS) that drives the Next TV (Israel)
login flow using Chrome DevTools Protocol (CDP) over WebSocket.

The test cold-starts the app, clears any stored login, types credentials, submits
the login and OTP forms, and verifies every screen — both by asserting DOM
elements and by comparing screenshots against reference images.

---

## How It Works

LG webOS apps are Chromium-based web pages running full-screen on the TV. The TV
exposes a DevTools endpoint that speaks the same protocol Chrome DevTools uses:
**CDP over WebSocket**.

The tool:

1. Connects to the TV's CDP endpoint at `ws://<tv-ip>:9998/devtools/page/<id>`.
2. Enables the `Runtime`, `DOM`, and `Page` CDP domains.
3. Reads the DOM via `Runtime.evaluate` to assert screen elements.
4. Sends real key events via `Input.dispatchKeyEvent` — this is what a physical
   remote-control press looks like from the app's perspective.
5. Captures screenshots via `Page.captureScreenshot` and compares them against
   reference images with a pixel-diff.

The DOM is used for element assertions, but **only the screenshot tells the truth**
about what the user actually sees. WebOS apps populate the DOM several seconds
before the pixels are drawn, so screen-readiness is gated on pixel match, not on
DOM presence.

### Two WebSocket Layers

- **`http://<tv-ip>:9998/json`** — a plain HTTP endpoint that lists available
  DevTools targets. Each target has a `webSocketDebuggerUrl` we connect to.
- **`ws://<tv-ip>:9998/devtools/page/<id>`** — the CDP WebSocket itself.

The class `DevToolsDiscovery` handles (1) and `TVWebSocketClient` handles (2).

---

## Project Structure

LG/
├── pom.xml
├── README.md
├── .gitignore
├── screenshots/
│ └── Expected/ reference images for pixel comparison
│ ├── NEXT_LoginScreen.png
│ ├── NEXT_OTPScreen.png
│ └── NEXT_LiveMosaicScreen.png
└── src/
├── main/java/com/cyberjohnny/
│ ├── config/
│ │ └── TVConfig.java IP, credentials, timeouts, expected elements
│ ├── core/
│ │ ├── CDPClient.java typed wrappers for CDP calls
│ │ ├── DevToolsDiscovery.java finds the CDP WebSocket URL
│ │ └── TVWebSocketClient.java WebSocket + CDP transport
│ ├── models/
│ │ ├── KeyCodes.java Windows VK codes + webOS-specific codes
│ │ └── TVTabInfo.java JSON model for /json target list
│ └── utils/
│ └── LaunchAppViaCLI.java shells out to ares-launch (close/launch)
└── test/java/com/cyberjohnny/
├── core/
│ └── ScreenshotComparator.java pixel-diff utility
└── FullLoginTest.java the JUnit 5 end-to-end test

---

## Prerequisites

- **Java 17** or higher
- **Maven 3.6** or higher
- **Node.js** with the webOS CLI tools: `ares-launch`, `ares-novacom`,
  `ares-install` (installed via `npm install -g @webosose/ares-cli`)
- **LG Smart TV** running webOS 3.9+, on the same local network as the Mac
- **Developer Mode** enabled on the TV (the app is pre-installed on the TV)

### TV Developer Mode Setup (once)

On the TV:

1. Install **Developer Mode** from the LG Content Store.
2. Open it, sign in with an LG developer account.
3. Turn **Dev Mode Status: ON**.
4. Turn **Key Server: ON**.
5. Note the **passphrase** shown (e.g. `4855E4`).
6. Note the TV's **IP address** (e.g. `192.168.1.217`).

On the Mac:

```bash
ares-setup-device                       # register the TV as "LG"
ares-novacom --device LG --getkey       # enter the passphrase once
ares-novacom --device LG --run "uname -a"   # confirm connectivity

Configuration
All tunables live in src/main/java/com/cyberjohnny/config/TVConfig.java:

public static final String TV_IP         = "192.168.1.217";
public static final int    DEVTOOLS_PORT = 9998;

public static final String SUBSCRIBER_ID = "999286578";
public static final String PHONE_NUMBER  = "0543501323";
public static final String OTP_CODE      = "123456";

public static final String SCREENSHOTS_ROOT    = "/Users/Johnny/IdeaProjects/LG/screenshots";
public static final String EXPECTED_LOGIN_PNG  = SCREENSHOTS_ROOT + "/Expected/NEXT_LoginScreen.png";
public static final String EXPECTED_OTP_PNG    = SCREENSHOTS_ROOT + "/Expected/NEXT_OTPScreen.png";
public static final String EXPECTED_MOSAIC_PNG = SCREENSHOTS_ROOT + "/Expected/NEXT_LiveMosaicScreen.png";

Adjust TV_IP, credentials, and paths as needed.

Running the Test:
cd ~/IdeaProjects/LG
mvn test -Dtest=FullLoginTest

What the Test Does

Step	                      Action

1	            Close and relaunch the Next TV app on the TV
2	            Find a real (non-shell) CDP target with a populated DOM
3	            Clear localStorage, sessionStorage, cookies, and IndexedDB
4	            Reload the app and wait for the login screen to be visually ready
5	            Assert login-screen elements; screenshot; compare to reference
6	            Type ID + phone, press Enter twice (checkbox + submit)
7	            Wait for OTP screen via pixel match; assert; screenshot
8           	Type OTP, press Enter once
9	            Wait for live mosaic via pixel match + text wait; assert; screenshot

Test outputs go to screenshots/Current/<timestamp>_FullLoginTest/.

Screen Readiness — Why Pixel Match, Not DOM
webOS apps populate the DOM several seconds before the pixels are drawn. When a
screen is still loading:

document.querySelectorAll('*') returns hundreds of elements.

element.offsetParent is non-null for all of them.

But the TV screen is black or shows a loader.

The DOM is a lie about what the user sees. Only Page.captureScreenshot reflects
the composited pixels the TV is actually displaying. So the test waits for each
screen by capturing screenshots and comparing them to reference images:


Screen	Threshold	Reason

Screen	      Threshold	                  Reason
Login	            97 %	     Static layout — must match exactly
OTP	              97 %	     Static layout
Mosaic	          85 %	     Thumbnails change every run, so slight differences are expected

If a screen never reaches its threshold within the timeout, the test proceeds
with a warning — the timeout is a safety net, not a fail condition.


Key Codes — Why 13, Not 461
webOS exposes multiple key codes for the same physical buttons. We tested both
13 (standard Enter) and 461 (webOS-specific Enter/OK):

13 advances the login and OTP screens correctly.

461 navigates backward — it's the "Back" key on this app.

The test uses KeyCodes.VK_ENTER = 13 everywhere. VK_WEBOS_ENTER = 461 is
defined for reference but not used.


Dependencies

Library	                                                                        Purpose

com.neovisionaries:nv-websocket-client:2.14	             WebSocket transport — sends each CDP message as a 
                                                         single unmasked text frame, which the TV's CDP 
                                                         accepts

com.fasterxml.jackson.core:jackson-databind:2.15.2        JSON parsing

org.junit.jupiter:junit-jupiter:5.10.2                    Test framework

com.github.romankh3:image-comparison:4.4.0                Pixel-diff for screenshots


Note: org.java-websocket:Java-WebSocket was initially used but was
replaced by nv-websocket-client. The old library frames messages in a way
the TV's CDP silently drops — same JSON, same target, but no response.


Known Issues
Physical Back button relaunches the app. This is a defect in the Next TV
app itself, not the test. Report it to the app's developers.

The mosaic threshold is 85 %. Channel thumbnails change on every run, so
97 % is unreachable. If a mosaic run reports low similarity, recapture the
reference image from a stable mosaic moment.


Git
Remote: git@github.com:Diamondbiz/LG-TV-Automation.git
The .gitignore excludes:
target/ and other Maven build output
.idea/, *.iml — IntelliJ config
screenshots/Current/ — generated per-run screenshots
*.log — test logs
cdpvenv/, pom.xml.backup — local artifacts

Only source, tests, pom.xml, README.md, .gitignore, and
screenshots/Expected/ are tracked.


---

## What Changed From the Old README

| Old | New |
|-----|-----|
| Referenced `LGTVAutomator.java`, `WebSocketClient.java`, `NextTVLoginAutomator.java` — classes that no longer exist | Reflects the actual current structure (`CDPClient`, `DevToolsDiscovery`, `TVWebSocketClient`, `FullLoginTest`, etc.) |
| Said to run `mvn exec:java -Dexec.mainClass=...` — that plugin was removed | Says to run `mvn test -Dtest=FullLoginTest` — the actual way to run the test |
| Configuration section referenced editing `LGTVAutomator.java` | Points at `TVConfig.java` — where config actually lives now |
| No explanation of CDP, WebSocket, or why the two layers exist | Adds "How It Works" and "Screen Readiness — Why Pixel Match, Not DOM" |
| No mention of key codes, thresholds, or dependencies | Documents all of them |
| No mention of the Back-button bug or thresholds | Documented in "Known Issues" |
| No prerequisites section for TV Developer Mode setup | Added a "TV Developer Mode Setup" subsection |

## How to Apply It

1. Open `README.md` in IntelliJ.
2. ⌘A (select all), Delete.
3. Paste the full block above.
4. ⌘S (save).
5. Commit:

```bash
cd ~/IdeaProjects/LG
git add README.md
git commit -m "Update README to reflect the current project structure and behavior"
git push


