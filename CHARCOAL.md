# Charcoal WiFi Scanner

Seven canonical resource files come from android-NDK
`38d6899706d855201239d86e1629e2620c0e8821` and are checked by the shared
canonical Grease copy guard. Existing Material3 controls, toolbar and popup
styles use the fixed neutral surface/text/border roles. Both resource modes
resolve to charcoal; the framework parent is explicitly dark and the initial
mode preference defaults to dark. Existing stored mode/accent choices remain
stored, and the optional green highlight is retained. No Java/Kotlin framework,
Gradle plugin or replacement interface is introduced.

Signal and open-network indicators retain their meanings with the common
success/warning/error colors. The pale error badge uses dark foreground for
legibility. Lists, scans, location permissions, favorites, sorting, copying,
export, stored scan data and package/version are unchanged.

The added CI uses the existing Gradle project to compile, test and lint a debug
fixture. The release private key and historical debug identity are unavailable;
that fixture is not an approved signed C67 upgrade. No identity is borrowed or
replaced. API29/API34 both-mode screenshots, dialogs, scan-state and lifecycle
acceptance and all physical C67 checks are NOT_RUN. Build/copy checks alone do
not establish them. No release, merge or installed-app replacement is performed.

The shared and Material3-specific window attributes now use actual API27/29/31
resource inheritance, preserving the API26 floor and full lint checks. The
seven canonical files are required by the updated copy guard; absent version
files are rejected. Current APK/test/lint qualification must rerun on this head.
