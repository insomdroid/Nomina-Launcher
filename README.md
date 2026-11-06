# Nomina

**Nomina is an independent Android launcher project derived from [ARK Launcher](https://github.com/jrs8205/Nomina).** This repository was created by importing the ARK source into a separate repository; it is not a GitHub fork and does not retain ARK's Git history.

Nomina is currently in its base-integration stage. The imported ARK implementation is being reviewed before Nomina's app identity and visual design are adapted. The Android package identity is being migrated to `com.insomdroid.nomina`. Some visual elements and ARK-branded resources will remain until the Nomina redesign is complete.

Nomina is an independent project and is not affiliated with, sponsored by, or endorsed by the ARK Launcher authors.

## Current project status

- ARK's multi-module Kotlin and Jetpack Compose codebase is the starting point.
- Existing launcher capabilities are being retained as the functional foundation while Nomina-specific changes are developed.
- The intended Nomina home-screen direction is a custom home layout with a collapsible large dock and a small dock that remains visible.
- The package identity is `com.insomdroid.nomina`; release signing configuration and the final visual design remain to be completed. Do not treat the current APK as a finished Nomina release.

## Features in the imported base

The current source includes the following ARK-derived functionality:

- Paged home screen with movable app shortcuts and drag-and-drop between pages, dock and app drawer.
- App drawer with app search, system-settings shortcuts and calculator/conversion queries.
- Home-screen and app-drawer folders.
- Android home-screen widgets, including widget placement, resizing and reconfiguration.
- Notification dots and badges, subject to notification access.
- Home-screen gestures, page management and desktop locking.
- Appearance controls for fonts, labels, icon sizes, colors, drawer background and icon packs.
- Backup and restore for launcher layout and settings.

These describe the imported base, not a claim that Nomina's own redesign is complete. Some behavior and presentation may change during adaptation.

## Screenshots

The screenshots currently present under `docs/screenshots/` belong to the imported ARK base. They are intentionally not presented as final Nomina screenshots. Nomina screenshots will be added after the app identity and interface have been adapted.

## Build

### Requirements

- JDK 21
- Android SDK with API 37
- Internet access for Gradle and Android dependency resolution

The imported build configuration currently uses:

- Gradle wrapper: 9.8.2
- Android Gradle Plugin: 9.5.0
- Kotlin: 2.4.21
- Compose BOM: 2026.09.00
- `minSdk`: 30
- `targetSdk` and `compileSdk`: 37

Build a debug APK:

```bash
./gradlew :app:assembleDebug
```

The APK is generated at:

```
app/build/outputs/apk/debug/app-debug.apk
```

A GitHub Actions workflow is provided to build the debug APK and publish it as a workflow artifact. The debug variant uses a separate application ID suffix and can be installed alongside a release variant.

## Project structure

This project uses a multi-module Gradle layout:

- `app/` — Android application, launcher activity, manifest and dependency-injection entry point.
- `core/model/` — shared domain models.
- `core/data/` — persistence, repositories, search providers and backup data.
- `core/launcher/` — launcher/platform integration.
- `core/designsystem/` and `core/ui/` — shared design system and Compose UI.
- `feature/home/` — home workspace, dock and widgets.
- `feature/appdrawer/` — app drawer and search.
- `feature/settings/` — launcher settings.
- `feature/backup/` — backup and restore.
- `build-logic/` — Gradle convention plugins.
- `baselineprofile/` — Android baseline profile and benchmark configuration.

## License and attribution

Nomina is based on ARK Launcher, which is licensed under the **Apache License, Version 2.0**. The original [LICENSE](LICENSE) and [NOTICE](NOTICE) files are retained. The NOTICE file also preserves the upstream attributions for AOSP Launcher3, Open-Meteo and BigDataCloud.

When redistributing this project or a derivative:

- Include a copy of the Apache-2.0 license.
- Retain applicable copyright, patent, trademark and attribution notices from the upstream source.
- Preserve the applicable NOTICE attributions.
- Mark modified source files with prominent notices describing the changes, as required by Apache-2.0.
- Do not imply endorsement by ARK Launcher or other upstream projects. Apache-2.0 does not grant rights to upstream trademarks.

See [LICENSE](LICENSE) and [NOTICE](NOTICE) for the complete license and attribution information.

## Upstream

- Original project: [ARK Launcher](https://github.com/jrs8205/Nomina)
- Original source license: Apache-2.0
- Original upstream contributors and notices are credited in [NOTICE](NOTICE).
