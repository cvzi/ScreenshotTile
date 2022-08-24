# Changelog

## 2.0.0-beta1
*   Included [PhotoEditor](https://github.com/burhanrashid52/PhotoEditor) by [Burhanuddin Rashid](https://github.com/burhanrashid52)
*   Crop image after a screenshot was taken
*   Choose what happens after a screenshot was taken
*   Directly open or edit a screenshot (without storing in the gallery)
*   Overview of recent screenshots

## 1.18.5
*   Translations updated from crowdin

## 1.18.3
*   Fix crash when opening advanced settings  [#187](https://github.com/cvzi/ScreenshotTile/issues/187)

## 1.18.2
*   Translations updated from crowdin

## 1.18.1
*   File size in PostActivity
*   Custom compression/quality for file format  [#175](https://github.com/cvzi/ScreenshotTile/issues/170) (in advanced settings)
*   Android 13: back

## 1.18.0
*   Transparency of floating button [#175](https://github.com/cvzi/ScreenshotTile/issues/170) (in advanced settings)
*   Custom delay time (in advanced settings)
*   🇮🇷 Persian translation improved by [MeysamNaghavi](https://github.com/MeysamNaghavi)
*   Don't ask for storage permission on Android 10/Q+
*   Android 13: Offer monochrome icon for "Themed icons" in "Material You" https://developer.android.com/about/versions/13/features?hl=en#themed-app-icons
*   Android 13: Restrict exporting of broadcast receiver and activities https://developer.android.com/about/versions/13/features?hl=en#runtime-receivers
*   Android 13: Ask user to add tiles to quick settings on first start https://developer.android.com/about/versions/13/features?hl=en#quick-settings
*   Android 13: Offer per-app language https://developer.android.com/about/versions/13/features?hl=en#app-languages
*   Android 13: Notification permission https://developer.android.com/about/versions/13/changes/notification-permission

## 1.17.2
*   🇮🇷 Persian translation by [MeysamNaghavi](https://github.com/MeysamNaghavi)
*   Allow installation on external storage [#170](https://github.com/cvzi/ScreenshotTile/issues/170)

## 1.17.1
*   🇷🇺 Russian translation improved by [Ilyas Khaniev](https://github.com/TheOldBlood)
*   🇯🇵 Japanese translation improved by [FrzMtrsprt](https://github.com/FrzMtrsprt)
*   🇻🇳 Vietnamese translation improved by [bruhwut](https://github.com/FrzMtrsprt)

## 1.17.0
*   Choose which action buttons appear in the notification (configurable in app settings)
*   Rename file from notification
*   "Details" activity
*   Update check (in app settings)

## 1.16.5
*   Translations updated from crowdin

## 1.16.4
*   Added a switch for the floating button in the MainActivity
*   Minor UI improvements
*   Minor bugfixes

## 1.16.2
*   Allow legacy method from floating button and from assistant [#117](https://github.com/cvzi/ScreenshotTile/issues/117) [#118](https://github.com/cvzi/ScreenshotTile/issues/118)
*   Minor bugfixes

## 1.16.1
*   Restore state in settings on rotation [#119](https://github.com/cvzi/ScreenshotTile/issues/119)
*   Translations updated

## 1.16.0
*   Assist app: take screenshot via long press on home button [#118](https://github.com/cvzi/ScreenshotTile/issues/118)
*   Partial screenshot: slide edges/corners
*   Partial screenshot: *select full screen* button
*   Partial screenshot: back button resets selection
*   Advanced settings
*   Cloud backup: [dataExtractionRules for Android 12](https://developer.android.com/guide/topics/data/autobackup#xml-syntax-android-12)

## 1.15.0
*   Added widget to toggle floating button

## 1.14.11
*   Restore state in settings on rotation [#119](https://github.com/cvzi/ScreenshotTile/issues/119)

## 1.14.10
*   Translations updated from crowdin

## 1.14.9
*   Translations updated from crowdin

## 1.14.8
*   Enable native method on new installs from F-Droid and don't show the consent dialog

## 1.14.7
*   Translations updated from crowdin

## 1.14.6
*   Translations updated from crowdin

## 1.14.5
*   Prominent disclosure of the Accessibility Services API's usage according to Google's requirements

## 1.14.4
*   Minor bugfixes
*   Unit test for activity links

## 1.14.3
*   Change notification for all versions [Notification icons crash app](https://github.com/cvzi/ScreenshotTile/commit/0c42cb7d2a2a360bf077c52b1a3874aedb4ab96d)

## 1.14.2
*   Android 12 bugfix: [Notification icons crash app](https://github.com/cvzi/ScreenshotTile/commit/0c42cb7d2a2a360bf077c52b1a3874aedb4ab96d)

## 1.14.1
*   Armenian translation by [tigrank08](https://crowdin.com/profile/tigrank08)
*   Unnecessary empty folder Pictures was created [#94](https://github.com/cvzi/ScreenshotTile/issues/94)

## 1.14.0
*   Polish translation by [@gnu-ewm](https://github.com/gnu-ewm) and [@chuckmichael](https://github.com/chuckmichael)
*   Select area from floating button [#81](https://github.com/cvzi/ScreenshotTile/issues/81)
*   Custom file name [#83](https://github.com/cvzi/ScreenshotTile/issues/83)

## 1.13.4
*   Arabic translation by Rex_sa (rex07) and Yousef Khaled Elsayed Espetan (yousef10)

## 1.13.3
*   Romanian translation by [Simona Iacob](https://crowdin.com/profile/simonaiacob)
*   Various translations improved

## 1.13.2
*   Portuguese translation by [@laralem](https://github.com/laralem)
*   Vietnamese translation by [bruhwut](https://crowdin.com/profile/bruhwut)
*   Various translations improved

## 1.13.1
*   Correctly enable/disable settings according to "use native method" status
*   Dutch translation by Hendrik Maryns ([@hamaryns](https://github.com/hamaryns))
*   Greek translation by [fresh](https://crowdin.com/profile/fresh1)

## 1.13.0
*   Close button can be any emoji e.g. ❌ ✅ ❎ ✓ ✔ ⛌ ✖ [#56](https://github.com/cvzi/ScreenshotTile/issues/56)
*   Ukrainian translation by [@Sensetivity](https://github.com/Sensetivity)
*   French translation by [@UncleReaton](https://github.com/UncleReaton)
*   Fix: white font on Android 24-28
*   Tile action: screenshot or partial screenshot [#55](https://github.com/cvzi/ScreenshotTile/issues/55)
*   Use dedicated foreground service for media projection if tile service is not in foreground
*   Fix: partial screenshot rectangle was not correct if there was a cutout or visible status bar
*   Switch dark theme in settings [#65](https://github.com/cvzi/ScreenshotTile/issues/65)
*   Extra delay for floating buttton [#59](https://github.com/cvzi/ScreenshotTile/issues/59)
*   Show a settings button for 2s after drag & drop of floating button
## 1.12.3
*   Hungarian translation by Stefi68
*   Russian translation by rikishi0071
*   Preference layout moved to the left
## 1.12.2
*   Updated Chinese app store translation. Thanks to [@linsui](https://github.com/linsui)
## 1.12.1
*   Added Italian translation. Thanks to [Filippo "Tecnophil" Cervellera](https://twitter.com/Tecnophil)
*   Improved Arabic translation
*   Improved Ukrainian translation
## 1.12.0
*   Android 11: native method with custom storage folder [#43](https://github.com/cvzi/ScreenshotTile/issues/43)
*   Icon color changed
## 1.11.0
*   Enable native method on new installs
*   Permissions management improved
*   Shortcut to toggle floating button
*   Square or round shutter for floating button with animations
*   Completely translated to Portuguese. Thanks to [@mezysinc](https://github.com/mezysinc)
*   Russian translation improved
*   Fix "hide app icon"
*   Add dimension metadata to MediaStore
*   Hide floating button before taking a screenshot
*   Wait longer for quick settings panel to collapse on native method
## 1.10.0
*   Added floating button to take screenshots
*   Added initial activity with text explanation of features
*   Added Portuguese pt-br translation. Thanks to [@mezysinc](https://github.com/mezysinc)
*   Rename package
*   Design improvements
## 1.9.0
*   Added widgets
*   Improvements for Android Q and 11. Start foreground service earlier
## 1.8.6
*   Update Kotlin
## 1.8.5
*   Improvements for Android Q and 11
## 1.8.4
*   some phones show the permission dialog in the screenshot. Added a delay to avoid it
## 1.8.3
*   gradle.properties: android.useAndroidX=true
## 1.8.2
*   Bugfix: [sdcard error after reboot](https://github.com/cvzi/ScreenshotTile/issues/24)
*   Remove "hide app icon" on Android Q. [It's no longer supported since Android 10+](https://github.com/cvzi/ScreenshotTile/issues/25)
## 1.8.1
*   Bugfix: Edit did not work with custom storage location and Google Photos app
## 1.8.0
*   Added support for a custom storage location
## 1.7.1
*   Updated Chinese translations. Thanks to [@linsui](https://github.com/linsui)
*   Several bugfixes
## 1.7.0
*   Simulate Home+Power button press to use native/system screenshot function. Requires enabled accessibility service and Android Pie/9+
## 1.6.5
*   Updated Indonesian translation. Thanks to [@alexschenider](https://github.com/alexschenider/)
## 1.6.4
*   Support night mode on Android Q
*   Disable back gesture on viewpager on Android Q
## 1.6.3
*   Updated Indonesian translation. Thanks to [@alexschenider](https://github.com/alexschenider/)
## 1.6.2
*   Updated translations
*   Compressed images and removed unused resources
## 1.6.1
*   Updated Indonesian translation. Thanks to [@alexschenider](https://github.com/alexschenider/)
*   Added short text to each tutorial screenshot
## 1.6.0
*   Added an option in the long-press menu to select an area before taking a screenshot
*   Added a Broadcast receiver for broadcast intents from other apps like MacroDroid
## 1.5.4
*   Fixed: Delete from notification on Android Q
## 1.5.3
*   Enhancements for Android Q
## 1.5.2
*   Bugfix
## 1.5.1
*   Added Polish translation
*   Translations improved
*   Small bugfix
## 1.5.0
*   Added settings entry for notifications
*   Moved Fragments and Preference to androidx/AppCompat
*   Translations improved
## 1.4.0
*   Redesigned the notification with a big picture and reordered the buttons
*   Added a list of open source projects used to the about section
*   Allow installation to internal storage only
*   Improved existing translations
*   Added incomplete translations for Arabic, Bengali, Gujarati, Hindi, Japanese, Kannada, Malay, Marathi, Persian, Portuguese, Punjabi, Russian, Tamil, Telugu, Thai, Uighur, Urdu, Vietnamese
*   Update gradle
## 1.3.3
*   Removed black borders on screenshots with some devices
*   Fix some nullable crashes
*   Update Kotlin version
## 1.3.2
*   Added file format: png, jpg, webp
*   Moved compressing the bitmap to file in separate thread
## 1.3.0
*   Added "Edit" button
## 1.2.4
*   Added [fastlane](https://docs.fastlane.tools/actions/supply/#images-and-screenshots) metadata
## 1.2.3
*   Fix crash (java.lang.OutOfMemoryError) in the tutorial
## 1.2.2
*   Update gradle
## 1.2.1
*   Fix crash (java.io.IOException) at java.io.File.createNewFile
## 1.2.0
*   Added tutorial with icon on home screen (Option to hide icon in settings)
*   Tile state is now always off
*   Fixed crash (java.lang.IllegalStateException at ScreenshotTileService.onTileAdded)
*   Added app icon shortcut to take a screenshot
*   Fixed bug: countdown/wait before screenshot was creating two screenshots
*   Disabled proguard
*   Translated about section
*   Added Tagalog/filipino translation
## 1.1.0
*   Added "Share" and "Delete" button to the notification
*   Fixed crash when permissions were initially denied or canceled
*   Fixed bug: countdown/wait before screenshot was creating two screenshots
## 1.0.2
*   Fixed Hebrew translation.
## 1.0.1
*   Added Simplified Chinese translation. Thanks to [@linsui](https://github.com/linsui)
*   Added Norwegian translation.
*   Added Spanish translation.
*   Added Hebrew translation.
*   Added French translation.
*   Added German translation.
