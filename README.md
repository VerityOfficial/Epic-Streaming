# Epic Streaming 1.7

Epic Streaming is a native Android music player for Navidrome servers. It connects over the Navidrome/Subsonic API and streams your library directly from your server. Google Play Services are not required.

## Download

Choose the APK for your Android version. Both editions can be installed together because they use separate Android package IDs and keep separate settings.

| Edition | Android support | Package ID | Launcher icon | APK |
| --- | --- | --- | --- | --- |
| **Regular** | Android 5.1/API 22 and newer | `com.epicstreaming.app` | Flat blue music-record icon | [EpicStreaming-Regular-API22-v1.7.apk](releases/v1.7/EpicStreaming-Regular-API22-v1.7.apk) |
| **Classic** | Android 2.3/API 9 and newer | `com.epicstreaming.app.legacy` | Glossy, beveled skeuomorphic record icon | [EpicStreaming-Classic-API9-v1.7.apk](releases/v1.7/EpicStreaming-Classic-API9-v1.7.apk) |

The APKs in this release folder are debug-signed sideload packages. They are ready to install with ADB; they are not Google Play Store packages. Earlier APK releases remain archived in `releases/v1.0/` through `releases/v1.6/`.

## Features

- Browse Navidrome albums, artists, and songs, with album artwork when enabled.
- Search the library for albums and songs by title, album, or artist.
- Stream MP3 audio from Navidrome. Choose a requested maximum bitrate of 64, 128, or 192 kbps in Settings.
- Use the mini-player to pause or resume. Swipe up on it to open the full player; swipe down or tap the small **swipe down** cue to return to the library.
- View enlarged album art, track title, artist, album, elapsed and remaining time, and playback progress in the full player. Its cover is scaled up to 1.3× when the display has room.
- Control playback from the full player: previous track from playback history, 10-second rewind, play/pause, 10-second forward, and a random next track.
- Continue playback when the screen is off. On Android 5.0/API 21 and newer, Epic Streaming publishes track title, artist, album, cover art, and playback controls to Android’s media session, lock screen, and media notification. Older Android versions show a basic playback notification.
- Automatically try a related random song when playback ends. This uses Navidrome’s `getSimilarSongs` endpoint when available, then falls back to searching for songs by the same artist. Related-song results depend on the server’s Last.fm integration; the same-artist fallback does not.
- Identify the configured server address with a badge: **Secure - Official** for `https://epicsclient.chunkp.workers.dev/`, **Secure** for other HTTPS addresses, **Intranet** for recognized private/local addresses, and **Not Secure** for other HTTP addresses. The badge is based on the address and protocol; it is not a separate publisher verification. HTTPS certificates are checked by Android normally.
- Adjust streaming quality, artwork loading, keep-screen-on behavior, startup tab, connection badge visibility, and compact layout in Settings. Classic also has a setting to force its glossy skeuomorphic interface.
- Use a compact layout that is enabled automatically on narrow screens and can also be changed in Settings.

Regular uses the flat blue interface; Android 5.1/API 22 and Android 6/API 23 use the blue-and-black 2015-style palette. Its full player animates into view on swipe-up. Classic is designed for Android 2.3 and newer, has its own skeuomorphic launcher icon, and can use the optional beveled interface. Both editions include the same library, search, and playback features; Android’s richer lock-screen media controls are available only on Android 5.0/API 21 and newer.

## Install both editions with ADB

These commands are for Windows PowerShell, run from the Epic Streaming project folder that contains `gradlew.bat` and `releases/`. Install Android Platform Tools on the computer, enable **USB debugging** on the phone, connect it by USB, and accept the phone’s debugging authorization prompt.

First check that ADB can see the phone:

```powershell
adb devices
```

The device should appear with the status `device`. Then install both v1.7 APKs:

```powershell
adb install -r ".\releases\v1.7\EpicStreaming-Regular-API22-v1.7.apk"
adb install -r ".\releases\v1.7\EpicStreaming-Classic-API9-v1.7.apk"
```

If PowerShell cannot find `adb`, run these commands from the Android SDK `platform-tools` folder or add that folder to `PATH`.

To install only one edition, run only its command. The `-r` option updates an existing installation while keeping that edition’s saved settings. The two package IDs allow both editions to coexist on the same phone.

Check that they are installed:

```powershell
adb shell pm list packages com.epicstreaming.app
adb shell pm list packages com.epicstreaming.app.legacy
```

Open either edition from the launcher, or start it from ADB:

```powershell
adb shell monkey -p com.epicstreaming.app 1
adb shell monkey -p com.epicstreaming.app.legacy 1
```

For an Android 5.1.1/API 22 phone, install **Regular**. Classic can also be installed alongside it.

## Connect to Navidrome

1. Connect the phone and Navidrome server to the same Wi-Fi network, or make sure the phone can reach the server address.
2. Open Epic Streaming. The default server address is `http://10.0.0.148:4000`; change it to your Navidrome server’s address if needed.
3. Enter your Navidrome username and password, then save and connect.
4. Browse the Albums, Artists, or Songs tabs. Use the search icon to find albums or songs.

Use HTTPS when connecting over an untrusted network. HTTP sends the connection without transport encryption and should be limited to a trusted local network. If the server does not load, check the server address and port, Wi-Fi connectivity, Navidrome login, and the computer’s firewall.

## Build from source

The project uses Java and the Android Gradle Plugin. Install JDK 17 and Android SDK Platform 35, configure `ANDROID_HOME` or `sdk.dir` in `local.properties`, then build both debug APKs from the project folder:

```powershell
.\gradlew.bat assembleModernDebug assembleLegacyDebug
```

Gradle 8.9 is supplied by the wrapper. Build outputs are written under `app/build/outputs/apk/modern/debug/` and `app/build/outputs/apk/legacy/debug/`. The v1.7 APKs are archived under `releases/v1.7/`; keep older release folders when preparing a later version.
