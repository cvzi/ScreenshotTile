[![app icon](app/src/main/res/mipmap-hdpi/ic_launcher.png)](https://github.com/cvzi/ScreenshotTile) ScreenshotTile (NoRoot)
=======================

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![F-Droid](https://img.shields.io/f-droid/v/com.github.cvzi.screenshottile.svg)](https://f-droid.org/packages/com.github.cvzi.screenshottile/)
[![Play Store](https://img.shields.io/endpoint?color=green&label=Store&logo=google-play&logoColor=green&url=https%3A%2F%2Fplay.cuzi.workers.dev%2Fplay%3Fi%3Dcom.github.cvzi.screenshottile%26m%3D%24rating%2520%25E2%25AD%2590%2520v%24version%2520)](https://play.google.com/store/apps/details?id=com.github.cvzi.screenshottile)
[![Download APK file](https://img.shields.io/github/release/cvzi/ScreenshotTile.svg?label=Download%20.apk&logo=android)](https://github.com/cvzi/ScreenshotTile/releases/latest)
![Downloads](https://img.shields.io/endpoint?color=luigi&url=https%3A%2F%2Fplay.cuzi.workers.dev%2Fplay%3Fi%3Dcom.github.cvzi.screenshottile%26l%3DDownloads%26m%3D%24totalinstalls)
[![F-Droid build status](https://img.shields.io/endpoint?logo=textpattern&logoColor=blue&url=https%3A%2F%2Ff-droid-build.cuzi.workers.dev%2Fcom.github.cvzi.screenshottile)](https://monitor.f-droid.org/)
[![🔨 Gradle Build](https://github.com/cvzi/ScreenshotTile/actions/workflows/gradleCI.yml/badge.svg)](https://github.com/cvzi/ScreenshotTile/actions/workflows/gradleCI.yml)

[<img src="/docs/imgs/get-it-on-f-droid.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.github.cvzi.screenshottile/)            Consider [donating to F-Droid](https://f-droid.org/donate/)

Screenshot Tile for Android 7.0+ without requiring root access

Video:

[![Video screenshot](/docs/imgs/youtube.png)](https://www.youtube.com/watch?v=PX6pVvfYRH0)

Fork of [github.com/ipcjs/ScreenshotTile](https://github.com/ipcjs/ScreenshotTile)

## Table of Contents
- [Languages](#languages)
- [Contributors & libraries](#contributors--libraries)
- [Technical details](#technical-details)
   - [Legacy/Original method](#legacyoriginal-method-)
   - [Native method](#native-method-with-system-defaults-enabled-)
   - [Native method with custom settings](#native-method-with-custom-settings--)
   - [Restricted Settings](#restricted-settings)
   - [Cast icon](#cast-icon--only-legacy-method)
   - [Permissions](#permissions)
- [Automatic screenshots](#automatic-screenshots-with-broadcast-intents)
- [Miscellaneous data](#miscellaneous-data)

[Changelog](CHANGELOG.md) • [View older releases](https://gitlab.com/cvzi/binaries/-/tree/main/ScreenshotTile) • [Google store](https://play.google.com/store/apps/details?id=com.github.cvzi.screenshottile)


## Languages

To help translate this app, please visit [crowdin.com](https://crwd.in/screenshottile?utm_source=badge&utm_medium=referral&utm_campaign=badge-add-on), where the localizations are managed. If you like to add a new language, please open an issue or email me and I will add it.

<a href="https://crwd.in/screenshottile?utm_source=badge&utm_medium=referral&utm_campaign=badge-add-on" rel="nofollow"><img style="width:140;height:40px" src="https://badges.crowdin.net/badge/dark/crowdin-on-light.png" srcset="https://badges.crowdin.net/badge/dark/crowdin-on-light.png 1x,https://badges.crowdin.net/badge/crowdin-on-light@2x.png 2x"  alt="Crowdin | Agile localization for tech companies" /></a>

You may translate the resource files directly and open a pull request. The English source is in [/app/src/main/res/values/strings.xml](/app/src/main/res/values/strings.xml) and the translated files are stored in [/app/src/main/res/values-XX/strings.xml](/app/src/main/res/) (XX = language code)

## Contributors & libraries

[![Contributors](https://contrib.rocks/image?repo=cvzi/ScreenshotTile)](https://github.com/cvzi/ScreenshotTile/graphs/contributors)

The internal image editor and image library used is [github.com/burhanrashid52/PhotoEditor](https://github.com/burhanrashid52/PhotoEditor) (MIT license) by [Burhanuddin Rashid](https://github.com/burhanrashid52).

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

### Restricted Settings

On Android 13 and above, device manufacturers may restrict the settings that can be enabled for an app. In such cases, the option to enable the app in the accessibility settings will appear grayed out. This means that both the "floating button" and the "native method" cannot be enabled. Notably, this is the case on Pixel phones running Android 14:

![Grayed Out Accessibility Settings](/docs/imgs/AccessibilityOff.png)

To circumvent this protection, you need to enable the "restricted settings" option. First, try to enable the accessibility service. Tap on the grayed-out option, and you will see a dialog regarding the restricted settings:

![Dialog About Restricted Settings](/docs/imgs/AccessibilityGrayedOut.png)

Now, open the Android system settings and go to "Apps" > "All apps." Find "ScreenshotTile," and in the app info, tap the three dots in the top right corner, and select "Allow Restricted Settings":

![Allow Restricted Settings](/docs/imgs/AllowRestrictedSettings.png)

With "restricted settings" allowed, you can reopen the app and enable either the "native method" or the "floating button." The accessibility settings will open, and you can enable the app.

Read more about this: https://support.google.com/android/answer/12623953#allowrestrictedsettings


### <a name="icon">Cast icon:</a> ![cast icon](/docs/imgs/casticon.png) (only Legacy method)

If you don't want to see the cast icon in the status bar on every screenshot, you can turn
it off on most phones. I do not recommend turning it off, as it is generally
a good idea to know when an app is recording the screen.  
Here's an explanation on how to turn it off:
[PCTattletale.com - How to turn off Android's Pesky Chromecast Icon](https://www.pctattletale.com/blog/3050/how-to-turn-off-androids-pesky-chromecast-icon/)


### Permissions

#### [`android.permission.WRITE_EXTERNAL_STORAGE`](https://developer.android.com/reference/android/Manifest.permission#WRITE_EXTERNAL_STORAGE) "Photos/Media/Files and Storage"
>   Read the contents of your internal storage/USB storage  
>   Modify or delete the contents of your internal storage/USB storage

This is required to save the screenshot files on the internal storage of your device.
Since Android 10/Q this permission is no longer used.

#### [`android.permission.FOREGROUND_SERVICE`](https://developer.android.com/reference/android/Manifest.permission#FOREGROUND_SERVICE)

Since Android 9/Pie this permission is required to take screenshots. It basically means that this app can run without showing itself. However the app will always show a notification when it is running.

#### [`android.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION`](https://developer.android.com/reference/android/Manifest.permission#FOREGROUND_SERVICE_MEDIA_PROJECTION)

Since Android 14/U this permission is required to take screenshots. It is a more specific version of the `android.permission.FOREGROUND_SERVICE` permission that allows to mirror the screen.

#### [ScreenCaptureIntent](https://developer.android.com/reference/android/media/projection/MediaProjectionManager.html#createScreenCaptureIntent())

>   ScreenshotTile will start capturing everything that's displayed on your screen.

This is a special permission that is requested before you take a screenshot or when you add the tile to you quick settings. It allows the app to record the screen. In this case, for a screenshot, the recording is only one image/frame.

#### [`android.permission.POST_NOTIFICATIONS`](https://developer.android.com/about/versions/13/changes/notification-permission)

Since Android 13/Tiramisu this permission can be used to request the ability to show notifications. You can choose "Don't allow" to block all notifications.

# <a name="automatic">Automatic screenshots with Broadcast intents</a>

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

## <a name="terminal">Automatic screenshots from terminal</a>

You can also take screenshots from the terminal:

```bash
# Take a screenshot
am broadcast -a com.github.cvzi.screenshottile.SCREENSHOT -e secret MY_PASSWORD com.github.cvzi.screenshottile
# Open the area selector for a partial screenshot
am broadcast -a com.github.cvzi.screenshottile.SCREENSHOT -e secret MY_PASSWORD --ez partial true com.github.cvzi.screenshottile
```

Or via [adb](https://developer.android.com/tools/adb) from a computer:

```bash
adb shell am broadcast -a com.github.cvzi.screenshottile.SCREENSHOT -e secret MY_PASSWORD com.github.cvzi.screenshottile
```

## Miscellaneous data
Some miscellaneous files (mostly images) that don't need to be in the main repository of ScreenshotTile were moved to a separate repository: https://github.com/cvzi/ScreenshotTile_miscellaneous
