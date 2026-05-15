# ScreenshotTile Features List

Note: Some of these features depend on a minimum or maximum Android version.

## Screenshot Capture Methods
*   **Legacy Method (Android 7.0+):** Uses screen recording capabilities (`MediaProjection`) to capture a single frame.
*   **Native Method - System Defaults (Android 9.0+):** Triggers the system's default screenshot mechanism (identical to physical button shortcuts) from an `AccessibilityService`.
*   **Native Method - Custom (Android 11.0+):** Uses `AccessibilityService#takeScreenshot` for full control over the capture and saving process without relying on system defaults.

## Triggers & Interfaces
*   **Quick Settings Tile:** The primary interface for quickly capturing screenshots from the notification shade.
*   **Floating Button:**
    *   **Customizable:** Adjustable size, transparency (alpha), and color.
    *   **Positioning:** Draggable anywhere on the screen; remembers different positions for portrait and landscape orientations.
    *   **Camera notch:** Tap camera to take screenshot. Option to hide the floating button behind the camera notch.
    *   **Multi-Action Support:** Configurable actions for single tap, double tap, and long press (e.g., Take Screenshot, Partial Screenshot, Open Settings).
    *   **App Filtering:** Can be set to auto-hide or auto-show only in specific applications (Whitelist/Blacklist).
    *   **Auto-Hide:** Automatically hides during the screenshot process or after a screenshot is taken.
    *   **Visibility Controls:** Options to show/hide on the lockscreen or when Quick Settings are open.
*   **Home Screen Widgets:** Provides widgets for Screenshot capture, Settings access, and Floating Button toggle.
*   **Digital Assistant:** Can be set as the device's default Digital Assistant app, allowing screenshots to be triggered via assistant shortcuts (e.g., long-pressing the home button).
*   **Broadcast Intents (Automation):** Integration with apps like Tasker or MacroDroid using a password-protected broadcast action (`com.github.cvzi.screenshottile.SCREENSHOT`). [Tutorial Video](https://www.youtube.com/watch?v=q5hQF1nzOzk).
*   **App Functions (Android 15+):** Integration with system-level shortcuts and AI assistants like Gemini for voice-triggered capture.
*   **Terminal / ADB:** Ability to trigger (remote) captures via command line with broadcast actions.

## Capture Options
*   **Partial Screenshot:** Interactive area selector to crop and capture only a specific part of the screen.
*   **Auto-Crop:** Pre-configure fixed margins (Top, Bottom, Left, Right) to automatically crop every screenshot.
*   **Delay Timer:** Configurable delay before capture
    *   **Countdown Display:** Optional visual on-screen countdown timer.
    *   **Tap to Cancel:** Quickly cancel a pending screenshot during the countdown.
*   **Shutter Sound:** Play a tone upon successful capture.

## Saving & Storage
*   **Custom Formats:** Support for PNG, JPEG and WEBP.
*   **Image Quality:** Adjustable compression levels for JPEG and WEBP formats.
*   **Custom Naming Patterns:** Use placeholders to automate file naming:
    *   `%timestamp%`: Current date and time.
    *   `%counter%`: Sequential number.
    *   `%randint%` / `%random%`: Random numbers or UUIDs.
    *   `%app%` / `%package%`: Name or package ID of the foreground app.
*   **Custom Storage Location:** Save screenshots to any folder, SD card or cloud storage.
*   **Private App Storage:** Option to save screenshots only within the app's private data folder to keep them out of the public gallery.
*   **History Management:**
    *   Internal gallery/history of recent screenshots.
    *   Automatic cleanup of old files.

## Post-Capture Actions
*   **Notifications:** Configurable notification buttons for immediate actions: Share, Edit, Rename, Crop, Delete, Details, and Photo Editor.
*   **Clipboard:** Copy the captured screenshot directly to the clipboard for easy pasting into other apps.
*   **Built-in Image Editor:**
    *   **Drawing:** Freehand drawing with custom colors and brush sizes.
    *   **Text:** Add text overlays with various colors.
    *   **Emojis:** Insert emojis directly onto the screenshot.
    *   **Filters:** Apply image filters like Grayscale, Sepia, Blur, etc.
    *   **Eraser:** Tool to remove edits.
    *   **Undo/Redo:** Full history of edits.
    *   **Auto-Rotate:** Automatically rotates landscape screenshots for easier editing.
    *   **Overwrite Option:** Choose to overwrite the original screenshot or save the edited version as a new file.
*   **Quick Rename:** Dedicated activity to rename screenshots with:
    *   Suggestions for ISO and Local date formats.
    *   List of recently used names.
    *   "Starred" names for frequent use.
    *   Move the file to recently used directories.

## App Settings & Maintenance
*   **Backup & Restore:** Full export and import of all application settings to a file.
*   **Theming:** Support for Light and Dark theme.
*   **Localization:** Languages not natively supported by Android system can be activated in the in-app language switcher. Help with translations is welcome: [crowdin.com](https://crowdin.com/project/screenshottile)
*   **Hide Launcher Icon:** Option to hide the app icon from the app drawer.
*   **Permission Guide:** Integrated status checks, notifications for lost permissions and shortcuts to system permissions.
*   **Restricted Settings Bypass:** In-app instructions for enabling Accessibility services on Android 13+ devices where they might be restricted.
