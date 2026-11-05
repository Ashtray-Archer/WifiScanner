# Charcoal WiFi Scanner

Four canonical resource files come from android-NDK
`e8b67dfc67776c2b8da919785d15f38d68c91db0` and are checked by the shared
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
