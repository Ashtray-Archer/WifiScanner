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
- **Per-network actions** — tap a row to copy its BSSID or SSID, share the network's
  details as text, or search the web for it.
- **Store & export** — the last scan is saved on-device and reloaded on next launch;
  export any scan to CSV or JSON via the share sheet. Both use the same JSON format,
  so an exported file and a saved session are interchangeable.
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

Dropping `ACCESS_FINE_LOCATION` in favour of `NEARBY_WIFI_DEVICES` alone (with
`neverForLocation`) was tried and does not work: `WifiManager.getScanResults()` is
refused outright — *"Permission violation — getScanResults not allowed … has no
location permission"*. The nearby-devices location exemption is not honoured for
scan-result reads on many builds, so the location permission stays.

Note that the device's **Location toggle must also be switched on**, independently of
the permission grant, or scans return an empty list. The app detects that and offers a
shortcut to the setting.

## Limitations

- **Scan rate is capped by the platform.** Android throttles foreground apps to roughly
  four scan requests every two minutes. The app re-requests on a timer and also picks up
  the system's own scan broadcasts, so the list keeps refreshing either way — but a fresh
  scan is not on demand.
- **Access points accumulate for the session.** Networks are merged into the list as
  they are seen and are not removed when they go out of range. Each row shows how long
  ago it was last seen; the list is cleared when the app process ends.
- **Hidden networks** broadcast no SSID and appear as `<Hidden network>`, identified only
  by BSSID and vendor.
- **Mesh systems and multi-band routers broadcast several BSSIDs** from one physical unit,
  often one per radio. Those are separate rows by design — a BSSID identifies a radio,
  not a box.
- **Vendor is the OUI owner** — the maker of the radio or the hardware, which is
  frequently not the brand on the label.
- Exports are written to the app's cache directory and shared out immediately; Android
  may reclaim them, so they are not a long-term archive.

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

- **minSdk** 26 · **compileSdk / targetSdk** 35 · **Java** 17
- **Gradle** 8.11.1 (wrapper) · **Android Gradle Plugin** 8.7.3 · **build-tools** 34.0.0
- Java + Android Views (Material 3, RecyclerView) — no Kotlin, no Compose, no
  code shrinking

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for release history.
