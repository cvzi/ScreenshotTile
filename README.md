ScreenshotTile (NoRoot)
=======================

[![License: GPL v3](https://img.shields.io/badge/License-GPL%20v3-blue.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Build Status](https://travis-ci.org/cvzi/ScreenshotTile.svg?branch=master)](https://travis-ci.org/cvzi/ScreenshotTile)
[![F-Droid](https://img.shields.io/f-droid/v/com.github.cvzi.screenshottile.svg)](https://f-droid.org/packages/com.github.cvzi.screenshottile/)
[![Play Store](https://img.shields.io/endpoint?color=green&label=Store&logo=google-play&logoColor=green&url=https%3A%2F%2Fplayshields.herokuapp.com%2Fplay%3Fi%3Dcom.github.cvzi.screenshottile%26m%3D%24rating%2520%25E2%25AD%2590%2520v%24version%2520)](https://play.google.com/store/apps/details?id=com.github.cvzi.screenshottile)
[![Download APK file](https://img.shields.io/github/release/cvzi/ScreenshotTile.svg?label=Download%20.apk&logo=android)](https://github.com/cvzi/ScreenshotTile/releases/latest)

[<img src="/docs/imgs/get-it-on-f-droid.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.github.cvzi.screenshottile/) [<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" alt="Get it on Google Play" height="80">](https://play.google.com/store/apps/details?id=com.github.cvzi.screenshottile)

Screenshot Tile for Android 7.0+ without requiring root access

Video:

[![Video screenshot](/docs/imgs/youtube.png)](https://www.youtube.com/watch?v=PX6pVvfYRH0)

Fork of [github.com/ipcjs/ScreenshotTile](https://github.com/ipcjs/ScreenshotTile)

[Changelog](CHANGELOG.md)

## <a name="icon">Cast icon:</a> ![cast icon](/docs/imgs/casticon.png)

If you don't want to see the cast icon in the status bar on every screenshot, you can turn
it off on most phones. I do not recommend turning it off, as it is generally
a good idea to know when an app is recording the screen.  
Here's an explanation on how to turn it off:
[PCTattletale.com - How to turn off Android's Pesky Chromecast Icon](https://www.pctattletale.com/blog/3050/how-to-turn-off-androids-pesky-chromecast-icon/)

## <a name="automatic">Automatic screenshots with Broadcast intents</a>

You can automate taking screenshots with apps like [MacroDroid](https://play.google.com/store/apps/details?id=com.arlosoft.macrodroid) or [Tasker](https://play.google.com/store/apps/details?id=net.dinglisch.android.taskerm).
This works via [Broadcast intents](https://developer.android.com/guide/components/broadcasts).

![Macro intent screenshot](/docs/imgs/MacroDroid_overview.png)

First you have to activate this feature by setting a password in the app settings.

Now you can **add a macro** to MacroDroid:
*   Open MacroDroid and tap on *Macros* and then *Add Macro* or the ➕ Symbol
*   Tab ➕ on *Triggers* and add your desired trigger
*   Tab ➕ on *Actions* and go to *Connectivity* -> *Send Intent*
*   Under *Target* select *Broadcast* and fill out the fields:
    *   Action: `com.github.screenshottile.SCREENSHOT`
    *   Package: `com.github.screenshottile`
    *   Data (class name): `com.github.ipcjs.screenshottile.IntentHandler`
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
