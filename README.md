# WiFi Scanner

A fast, private Android app for discovering and reviewing nearby Wi-Fi access points.

Latest release: **v1.1** · see [CHANGELOG.md](CHANGELOG.md) ·
[download the signed APK](https://github.com/PortableDiag/WifiScanner/releases/latest)

Scans continuously, updates live, and lists every access point around you with its
name, MAC address, manufacturer, signal strength, channel, band and security — then
stores the last scan for review and lets you export it.

## Features

- **Continuous auto-scan** — starts scanning on launch and refreshes live; pauses in
  the background and resumes when you return. Tap **Scan / Stop** to control it.
- **Rich per-network detail** — SSID, BSSID (MAC), signal (RSSI in dBm, colour-coded),
  channel, band (2.4 / 5 / 6 GHz), security (Open / WEP / WPA / WPA2 / WPA3 /
  Enterprise) and vendor.
- **Offline vendor lookup** — the bundled IEEE OUI database maps each MAC prefix to its
  manufacturer with no network calls.
- **Open-network flagging** — unsecured access points are marked with a red **OPEN**
  badge and an open-lock icon.
- **Sort & filter** — by signal, name, vendor, channel, security or BSSID; filter to
  open networks only; pin favourites to the top.
- **Favourites** — star an access point (anchored to its BSSID).
- **Store & export** — the last scan is saved on-device and reloaded on next launch;
  export any scan to CSV or JSON via the share sheet.
- **Two themes** — Ocean blue (default) and Terminal green, each with light / dark
  variants that follow the system or a manual override.

All scanning happens locally on the device. The app never uses or transmits your
location — Android only requires the location permission to *return* Wi-Fi scan
results, and the app surfaces a hint if Location services are switched off.

## Permissions

| Permission | Why |
|---|---|
| `ACCESS_FINE_LOCATION` | Android gates Wi-Fi scan results behind this permission on every API level. The app never reads your location. |
| `NEARBY_WIFI_DEVICES` (API 33+) | Lets the scan trigger on devices that route `startScan()` through the nearby-devices permission. |
| `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE` | Read scan results and request a scan. |

## Build

```sh
./gradlew assembleRelease
```

The release build is signed from a local `keystore.properties` + keystore file, which
are intentionally **not** committed. To build a signed release yourself, create your
own keystore and a matching `keystore.properties`:

```properties
storeFile=your-release.jks
storePassword=…
keyAlias=…
keyPassword=…
```

Without those, the build still produces an unsigned/debuggable APK.

- **minSdk** 26 · **targetSdk** 35 · Java 17

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for release history.
