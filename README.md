[![app icon](app/src/main/res/mipmap-hdpi/ic_launcher.png)](https://github.com/cvzi/ScreenshotTile) ScreenshotTile (NoRoot)
=======================

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Build status](https://github.com/cvzi/ScreenshotTile/workflows/gradleCI/badge.svg)](https://github.com/cvzi/ScreenshotTile/actions?query=workflow%3AgradleCI)
[![F-Droid](https://img.shields.io/f-droid/v/com.github.cvzi.screenshottile.svg)](https://f-droid.org/packages/com.github.cvzi.screenshottile/)
[![Play Store](https://img.shields.io/endpoint?color=green&label=Store&logo=google-play&logoColor=green&url=https%3A%2F%2Fplayshields.herokuapp.com%2Fplay%3Fi%3Dcom.github.cvzi.screenshottile%26m%3D%24rating%2520%25E2%25AD%2590%2520v%24version%2520)](https://play.google.com/store/apps/details?id=com.github.cvzi.screenshottile)
[![Download APK file](https://img.shields.io/github/release/cvzi/ScreenshotTile.svg?label=Download%20.apk&logo=android)](https://github.com/cvzi/ScreenshotTile/releases/latest)
![Downloads](https://img.shields.io/endpoint?color=luigi&url=https%3A%2F%2Fplayshields.herokuapp.com%2Fplay%3Fi%3Dcom.github.cvzi.screenshottile%26l%3DDownloads%26m%3D%24installs)

[<img src="/docs/imgs/get-it-on-f-droid.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.github.cvzi.screenshottile/)            Consider [donating to F-Droid](https://f-droid.org/donate/)

Screenshot Tile for Android 7.0+ without requiring root access

Video:

[![Video screenshot](/docs/imgs/youtube.png)](https://www.youtube.com/watch?v=PX6pVvfYRH0)

Fork of [github.com/ipcjs/ScreenshotTile](https://github.com/ipcjs/ScreenshotTile)

[Changelog](CHANGELOG.md) • [View older releases](https://keybase.pub/cuzi/ScreenshotTileNoRoot_bin/) • [Google store](https://play.google.com/store/apps/details?id=com.github.cvzi.screenshottile)

## Languages

To help translate this app, please visit [crowdin.com](https://crwd.in/screenshottile?utm_source=badge&utm_medium=referral&utm_campaign=badge-add-on), where the localizations are managed. If you like to add a new language, please open an issue or email me and I will add it.

<a href="https://crwd.in/screenshottile?utm_source=badge&utm_medium=referral&utm_campaign=badge-add-on" rel="nofollow"><img style="width:140;height:40px" src="https://badges.crowdin.net/badge/dark/crowdin-on-light.png" srcset="https://badges.crowdin.net/badge/dark/crowdin-on-light.png 1x,https://badges.crowdin.net/badge/crowdin-on-light@2x.png 2x"  alt="Crowdin | Agile localization for tech companies" /></a>

You may translate the resource files directly and open a pull request. The English source is in [/app/src/main/res/values/strings.xml](/app/src/main/res/values/strings.xml) and the translated files are stored in [/app/src/main/res/values-XX/strings.xml](/app/src/main/res/) (XX = language code)

## Contributors

[![Contributors](https://contrib.rocks/image?repo=cvzi/ScreenshotTile)](https://github.com/cvzi/ScreenshotTile/graphs/contributors)

## Technical details

This app supports three different methods to take screenshots

### Legacy/Original method <img src="/docs/imgs/SwitchLegacyMethod.png" alt="Legacy method switch on" height="25"/>
This method uses the screen recording/screen cast capabilities of Android to record a single frame.

Requirements:
*   Android 7 Nougat
*   Storage permission
*   [ScreenCaptureIntent/MediaProjection permission](https://developer.android.com/reference/android/media/projection/MediaProjectionManager.html#createScreenCaptureIntent())
*   <img src="/docs/imgs/StoragePermission.png" alt="Storage permission screenshot" height="90"/>
*   <img src="/docs/imgs/ScreenCaptureIntent.png" alt="MediaProjection permission screenshot" height="130"/>

Properties:
*   Custom storage location
*   Custom format
*   Custom notification
*   Before Android 9 the ScreenCaptureIntent/MediaProjection permission was only asked once when adding the tile (with option "Don't show again"). Since Android 9 it needs to be granted frequently before taking a screenshot

### Native method with system defaults enabled <img src="/docs/imgs/SwitchNativeMethod.png" alt="Native method switch on" height="25"/>
This method uses the screenshot function of the device. It's exactly the same as pressing the Home+Power button or whichever key combination is used for screenshots on that phone.

Requirements:
*   Android 9 Pie
*   Accessibility service needs to be started in the system settings
*   <img src="/docs/imgs/AccessibilityServicePermission.png" alt="Accessibility service screenshot" height="230"/>

Properties:
*   No permissions needed, only activating the accessibility service once
*   Functions like long screenshots, screenshot editor, notifications, thumbnail etc., which the device manufacturer may have added, can be used

### Native method with custom settings <img src="/docs/imgs/SwitchNativeMethod.png" alt="Native method switch on" height="25"/> <img src="/docs/imgs/SwitchSystemDefaultsOff.png" alt="Use system defaults switch off" height="25"/>
This method uses [AccessibilityService#takeScreenshot](https://developer.android.com/reference/android/accessibilityservice/AccessibilityService#takeScreenshot%28int,%20java.util.concurrent.Executor,%20android.accessibilityservice.AccessibilityService.TakeScreenshotCallback%29) which outputs a bitmap that can be compressed and stored on disk.

Requirements:
*   Android 11 R
*   Accessibility service needs to be started in the system settings
*   Storage permission
*   <img src="/docs/imgs/AccessibilityServicePermission.png" alt="Accessibility service screenshot" height="230"/>
*   <img src="/docs/imgs/StoragePermission.png" alt="Storage permission screenshot" height="90"/>

Properties:
*   Custom storage location
*   Custom format
*   Custom notification

## <a name="icon">Cast icon:</a> ![cast icon](/docs/imgs/casticon.png) (only Legacy method)

If you don't want to see the cast icon in the status bar on every screenshot, you can turn
it off on most phones. I do not recommend turning it off, as it is generally
a good idea to know when an app is recording the screen.  
Here's an explanation on how to turn it off:
[PCTattletale.com - How to turn off Android's Pesky Chromecast Icon](https://www.pctattletale.com/blog/3050/how-to-turn-off-androids-pesky-chromecast-icon/)

## <a name="automatic">Automatic screenshots with Broadcast intents</a>

You can automate taking screenshots with apps like [MacroDroid](https://play.google.com/store/apps/details?id=com.arlosoft.macrodroid) or [Tasker](https://play.google.com/store/apps/details?id=net.dinglisch.android.taskerm).
This works via [Broadcast intents](https://developer.android.com/guide/components/broadcasts).

Tutorial video on youtube: [https://youtu.be/q5hQF1nzOzk](https://www.youtube.com/watch?v=q5hQF1nzOzk)

![Macro intent screenshot](/docs/imgs/MacroDroid_overview.png)

First you have to activate this feature by setting a password in the app settings.

Now you can **add a macro** to MacroDroid:
*   Open MacroDroid and tap on *Macros* and then *Add Macro* or the ➕ Symbol
*   Tab ➕ on *Triggers* and add your desired trigger
*   Tab ➕ on *Actions* and go to *Connectivity* -> *Send Intent*
*   Under *Target* select **Broadcast** and fill out the fields:
    *   Action: `com.github.cvzi.screenshottile.SCREENSHOT`
    *   Package: `com.github.cvzi.screenshottile`
    *   Data (class name): `com.github.cvzi.screenshottile.IntentHandler`
    *   Extra 1 parameter: `secret`
    *   Extra 1 value: `yourPasswordFromEarlier`
    *   (Optional: Extra 2 parameter `partial`, value `true` to open the area selector for a partial screenshot instead of taking a screenshot)

![Macro intent screenshot](/docs/imgs/MacroDroid_SendIntent.png)

## Permissions

#### [`android.permission.WRITE_EXTERNAL_STORAGE`](https://developer.android.com/reference/android/Manifest.permission#WRITE_EXTERNAL_STORAGE) "Photos/Media/Files and Storage"
>   Read the contents of your internal storage/USB storage  
>   Modify or delete the contents of your internal storage/USB storage

This is required to save the screenshot files on the internal storage of your device.

#### [`android.permission.FOREGROUND_SERVICE`](https://developer.android.com/reference/android/Manifest.permission#FOREGROUND_SERVICE)

Since Android 9/Pie this permission is required to take screenshots. It basically means that this app can run without showing itself. However the app will always show a notification when it is running.

#### [ScreenCaptureIntent](https://developer.android.com/reference/android/media/projection/MediaProjectionManager.html#createScreenCaptureIntent())

>   ScreenshotTile will start capturing everything that's displayed on your screen.

This is a special permission that is requested before you take a screenshot or when you add the tile to you quick settings. It allows the app to record the screen. In this case, for a screenshot, the recording is only one image/frame.
