# Nomina

**Nomina is an independent Android launcher derived from [ARK Launcher](https://github.com/jrs8205/ARK-launcher).** The ARK source was imported into this repository as a clean starting point. This repository is not a GitHub fork and does not carry ARK's Git history.

Nomina is maintained independently at [insomdroid/Nomina-Launcher](https://github.com/insomdroid/Nomina-Launcher). It is not affiliated with, sponsored by, or endorsed by the ARK Launcher authors.

## Project status

Nomina is in active adaptation from its ARK-based foundation. The existing launcher architecture and core functionality are being retained while the app identity, interface, and selected features are adapted.

- Android application ID: `com.insomdroid.nomina`.
- Interface language: English.
- The planned home-screen direction includes a custom layout, a collapsible large dock, and a small dock that remains visible. This is a design direction, not a claim that the dock redesign is complete.
- GitHub Actions currently produces a debug APK for testing. It is not a signed production release.

## Current functionality

The current source includes:

- Paged home screen with movable app shortcuts and drag-and-drop between pages and the dock.
- App drawer with application search, system-settings search, and calculator/conversion queries.
- Home-screen and app-drawer folders.
- Android home-screen widgets, including placement, resizing, and reconfiguration.
- Built-in clock, weather, battery, notification, and People widgets.
- People widget contact matching, available only when the user grants Contacts permission.
- Notification dots and badges, subject to notification access.
- Home-screen gestures, page management, and desktop locking.
- Appearance controls for fonts, labels, icon sizes, colors, drawer backgrounds, and icon packs.
- Backup and restore for launcher layout and settings.

The app drawer does not search device contacts. Nomina does not provide launcher lock/unlock sound settings, a custom launcher status bar, or network-based place-name lookup. Android's own system status bar remains controlled by the operating system.

## Screenshots

The images in [docs/screenshots](docs/screenshots/) are inherited screenshots of the ARK-based interface. They document the current foundation and are not presented as final Nomina redesign screenshots.

## Installation and builds

### Requirements

- Android 11 (API 30) or newer.
- JDK 21.
- Android SDK with API 37.
- Internet access for Gradle and Android dependency resolution.

The current build configuration uses:

- Gradle wrapper 9.8.2.
- Android Gradle Plugin 9.5.0.
- Kotlin 2.4.21.
- Compose BOM 2026.09.00.
- `minSdk`: 30.
- `compileSdk` and `targetSdk`: 37.

### Build locally

```bash
./gradlew :app:assembleDebug
```

The APK is generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

The debug build uses the application ID `com.insomdroid.nomina.debug`, so it can be installed alongside a production package with application ID `com.insomdroid.nomina`.

### GitHub Actions

The [Android CI workflow](https://github.com/insomdroid/Nomina-Launcher/actions/workflows/android-ci.yml) separates APK builds from unit tests:

- Application source or build-configuration changes trigger a debug APK build and unit tests.
- Test-only changes trigger unit tests without rebuilding the APK.
- Documentation-only changes do not trigger an APK build.

Download the APK from the `nomina-debug-apk` artifact attached to a successful build run. Artifacts are temporary workflow outputs, not GitHub Releases.

[View GitHub Actions runs](https://github.com/insomdroid/Nomina-Launcher/actions).

## Project structure

This is a multi-module Kotlin and Jetpack Compose project:

- `app/` — Android application, launcher activity, manifest, and dependency-injection entry point.
- `core/model/` — shared domain models.
- `core/data/` — persistence, repositories, search providers, and backup data.
- `core/launcher/` — launcher and Android platform integration.
- `core/designsystem/` and `core/ui/` — shared design system and Compose UI.
- `feature/home/` — home workspace, dock, and widgets.
- `feature/appdrawer/` — app drawer and search.
- `feature/settings/` — launcher settings.
- `feature/backup/` — backup and restore.
- `build-logic/` — Gradle convention plugins.
- `baselineprofile/` — Android baseline profile and benchmark configuration.

## License and attribution

Nomina is derived from [ARK Launcher](https://github.com/jrs8205/ARK-launcher), which is licensed under the **Apache License, Version 2.0**. The upstream [LICENSE](LICENSE) and [NOTICE](NOTICE) attributions are retained.

When redistributing this project or a derivative:

- Include a copy of the Apache-2.0 license.
- Retain applicable upstream copyright, patent, trademark, and attribution notices.
- Preserve the applicable NOTICE attributions.
- Mark modified source files with prominent notices describing the changes, as required by Apache-2.0.
- Do not imply endorsement by ARK Launcher or other upstream projects. Apache-2.0 does not grant rights to upstream trademarks.

See [LICENSE](LICENSE) and [NOTICE](NOTICE) for the complete license and attribution information.

## Upstream

- Original source repository: [jrs8205/ARK-launcher](https://github.com/jrs8205/ARK-launcher).
- Original source license: Apache-2.0.
- Upstream and third-party attributions are recorded in [NOTICE](NOTICE).
