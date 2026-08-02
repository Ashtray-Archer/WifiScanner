# Changelog

All notable changes to WiFi Scanner are documented here.
Format loosely follows [Keep a Changelog](https://keepachangelog.com/).

## [1.1] - 2026-08-02

### Added
- Project documentation: expanded `README.md` and this `CHANGELOG.md`.

### Notes
- No functional app changes since 1.0; this release packages the documentation
  and establishes versioned release notes going forward.

## [1.0] - 2026-08-02

### Added
- Initial release.
- Continuous Wi-Fi access-point scanner with live auto-scan (re-scans on a
  throttle-friendly 20 s interval and refreshes from system scan broadcasts).
- Per-network detail: SSID, BSSID (MAC), signal (RSSI in dBm, colour-coded),
  channel, band (2.4 / 5 / 6 GHz), and security
  (Open / WEP / WPA / WPA2 / WPA3 / Enterprise / Enhanced Open).
- Offline vendor lookup from the bundled IEEE OUI database (no network calls).
- Open-network flagging: unsecured APs shown with a red **OPEN** badge and an
  open-lock icon.
- Sort by signal, name (SSID), vendor, channel, security or BSSID; filter to
  open networks only; pin favourites to the top.
- Favourites, anchored to an access point's BSSID.
- Stores the last scan on-device (reloaded on next launch); export any scan to
  CSV or JSON via the share sheet.
- Two themes — Ocean blue (default) and Terminal green — each with light/dark
  variants.

### Technical
- `minSdk` 26, `targetSdk` 35, Java 17, AGP 8.7.3.
- Requires `ACCESS_FINE_LOCATION` (Android gates Wi-Fi scan results behind a
  location permission on every API level; the app never uses the location);
  also requests `NEARBY_WIFI_DEVICES` on API 33+ for the scan trigger.
