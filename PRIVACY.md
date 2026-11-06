# Nomina Privacy Policy

*Last updated: 2026-11-06. This policy describes the current source in this repository.*

Nomina is a free, open-source Android home-screen launcher licensed under Apache-2.0. The source code for this project is available at:

<https://github.com/insomdroid/Nomina-Launcher>

## The short version

- Your home-screen layout, settings, app list, and notification data stay on your device.
- Nomina has no user accounts, telemetry, analytics, advertising, or tracking.
- The network is used to retrieve weather data from Open-Meteo when the weather feature is enabled and location permission is granted.
- Place names are resolved using Android's on-device geocoder. Nomina does not send reverse-geocoding requests to an external place-name service.
- Backups are local files that you explicitly export. Nomina does not use cloud backup or perform in-app APK updates.

## Data stored on your device

- **Home-screen layout** — app, folder, and widget placements are stored in a local database.
- **Settings and preferences** — grid size, gestures, icon-pack choice, hidden apps, custom app labels and icons, and similar preferences are stored locally.
- **Pictures you pick** — a gallery picture chosen as an app or folder icon is stored in the launcher's private storage. Replacing or clearing the icon deletes the stored image. These images are not included in backups. An icon selected from an icon pack is stored only as a reference.
- **Notification data** — if you grant notification access, notifications are read to show dots/badges and the optional notification and People widgets, including their open, reply, and dismiss actions. Message text is processed in memory and is not persistently stored or transmitted by Nomina; replies are sent through the originating app. People you pin and identity links you create are saved in settings, including any matched contact name, number, photo reference, and contact URI, and are included in local backups. Optional batch delivery delays only launcher surfaces; Android notification sounds and pop-ups remain controlled by the system and originating apps. Notification keys and delivery times are saved locally to retain waiting state after a restart and are excluded from backups.
- **Backups** — an exported backup is written to the file location you choose. It contains your launcher layout and settings. The app opts out of Android system backup with `allowBackup="false"`; the exported file is the backup mechanism provided by Nomina.

## Network use

1. **Weather** — only while the weather feature is enabled and location permission is granted, Nomina requests current temperature and weather conditions from the keyless [Open-Meteo API](https://open-meteo.com/). Coordinates are rounded before the request to reduce location precision; the exact device location is not sent.
2. **Place names** — the place name shown with weather is resolved locally using Android's geocoder. No external reverse-geocoding service is called.

The weather request does not include account information, cookies, or advertising identifiers. As with ordinary internet traffic, the request necessarily reveals the device's IP address to the service. The request uses a generic `Nomina/<version>` User-Agent and does not include device details.

The "Check for updates" row, where present in the app version, opens the project's GitHub Releases page in a browser. Nomina does not download or install updates itself.

## Permissions

| Permission | Purpose |
|---|---|
| `QUERY_ALL_PACKAGES` | Listing and launching installed apps, which is the launcher's core function. |
| Notification access (`BIND_NOTIFICATION_LISTENER_SERVICE`) | Notification dots/badges and the optional notification and People widgets, including supported notification actions. |
| Accessibility service (`BIND_ACCESSIBILITY_SERVICE`, opt-in) | The double-tap-to-lock gesture only. The service does not receive accessibility events or read screen content; it is used only to lock the screen when enabled by the user. |
| `READ_CALENDAR` | Showing the next calendar event in the clock widget. |
| `READ_CONTACTS` | Matching contacts for the optional People widget. Contacts are not used for app-drawer search. |
| `ACCESS_COARSE_LOCATION` | Retrieving weather for the clock widget. |
| `INTERNET` | Retrieving weather data from Open-Meteo. |
| `REQUEST_DELETE_PACKAGES` | The uninstall action in an app icon's long-press menu. |
| `EXPAND_STATUS_BAR` | The swipe-down-for-notifications gesture. |
| `HIDE_OVERLAY_WINDOWS` | Hiding other apps' overlay windows while a backup import confirmation is shown on Android 12 and newer. |

Calendar and contact data are read on demand for the features described above, used on the device, and not transmitted by Nomina. Location is used on the device to obtain weather coordinates; only the rounded coordinates required for the weather request are sent to Open-Meteo.

## Children

Nomina is a general-purpose utility with no user accounts, advertising, or purchases. It is not directed at children and does not knowingly collect personal information from children or other users.

## Changes and contact

This policy is maintained in the public Nomina repository. Questions and reports can be submitted through [GitHub Issues](https://github.com/insomdroid/Nomina-Launcher/issues).
