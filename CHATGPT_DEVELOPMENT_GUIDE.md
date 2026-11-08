# ChatGPT Development Guide — Nomina

Repository: https://github.com/insomdroid/Nomina-Launcher

This guide defines the safe development workflow for Nomina, a minimalist Android launcher. The current repository implementation is authoritative; historical discussion is context, not proof that a feature still exists.

## Product direction

Nomina is an independently maintained Android launcher based on ARK Launcher, with project-specific changes. Preserve the existing architecture and approved visual direction. Do not assume that older design discussions describe the current code.

## Non-negotiable working rules

- Work only in this repository. Do not mix source, assets, or implementation assumptions from Dislauncher, Icon-Packs, or Omamu.
- Inspect the actual repository structure, package/application ID, Gradle files, manifest, permissions, upstream notices, and workflows before editing.
- Prefer small, focused changes on `main` for routine work; do not create a PR unless requested or required.
- Use Kotlin, Jetpack Compose, official Compose BOM, Material 3, Kotlin DSL, and stable APIs where consistent with the actual project architecture. Do not rewrite existing architecture just to match a preference.
- Do not invent package names, versions, dependencies, file paths, API availability, or current behavior.
- Avoid unnecessary dependencies and broad refactors.
- Preserve upstream license notices and attribution from ARK Launcher and any other dependencies.
- Preserve app data, launcher role behavior, application ID, release signing identity, and update compatibility.
- A successful compile is not proof that launcher behavior works on a physical device.

## Design and interaction direction

- Keep the approved text-first/minimal direction where it exists in the current implementation; verify current screens before changing them.
- Respect Android system light/dark theme behavior.
- Use typography and spacing consistently with the current code; do not introduce a new font or visual system without approval.
- Keep date/time formatting aligned with device 12/24-hour settings and local time.
- Maintain the agreed hierarchy for date/time, alarm, weather, battery, and connectivity only where those features exist in the current implementation.
- Keep dock, drawer, favorites, scrolling, and search behavior consistent with the approved current design.
- Avoid decorative clutter or adding UI that was not requested.
- Use screenshots captured at the same device size, font scale, theme, and state to validate visual changes.

## Android platform and reliability checks

- Verify launcher intent filters and default-home behavior after manifest or navigation changes.
- Check runtime permissions and API-level behavior for every system service access.
- Network state access must use the correct permission and safe null/error handling; prevent SecurityException crashes.
- Treat weather/network/location as unreliable inputs: handle unavailable, denied, offline, and timeout states without blocking the UI.
- Keep network calls off the main thread and cancel work with the appropriate lifecycle.
- Avoid unnecessary polling, location access, leaked callbacks/listeners, and stale coroutine work.
- Verify accessibility basics: readable contrast, sensible touch targets, content descriptions where needed, and font scaling.
- Check app launch, return-to-home, process recreation, and permission-denial paths.

## Recommended task workflow

1. Define one exact behavior or visual outcome.
2. Inspect current source, relevant upstream-derived code, and the call chain/state owner.
3. Identify permission, lifecycle, API-level, and theme implications.
4. Implement the smallest safe change.
5. Review the complete diff and ensure no unrelated files changed.
6. Run the repository's actual GitHub Actions workflow.
7. Install the APK and test on the target Android device.
8. Record commit, workflow URL, test cases, and remaining issues.

## Copy-ready prompts

### Audit before coding
> Audit the current Nomina-Launcher implementation for this requested change. Inspect source files, state flow, manifest permissions, upstream-derived architecture, API-level constraints, and lifecycle behavior. Do not edit yet. Explain the exact current behavior, smallest safe file set, risks, and device test plan. Do not infer implementation from older chat messages.

### Implement a focused fix
> Implement only the approved Nomina change in this repository. Preserve existing architecture, upstream notices, text-first design, and theme behavior. Check permission handling, lifecycle, null/error states, and API compatibility where relevant. Do not add dependencies or unrelated UI. Review the diff and report commit SHA and GitHub Actions run.

### Diagnose a crash
> Diagnose this Nomina crash from the complete stack trace and current source. Identify the exact throwing call and why current permission/null/lifecycle handling is insufficient. Make the smallest robust fix, including manifest changes only if required. Verify the build and provide a physical-device reproduction/verification checklist.

### Review visual consistency
> Compare these Nomina screenshots. Check typography, alignment, spacing, hierarchy, theme contrast, font scaling, and unintended layout shifts. Separate observed facts from hypotheses. Do not change code until I approve the specific correction.

## Checkpoint template

- App version:
- Last known-good commit:
- Last successful Actions run:
- APK artifact:
- Current home/drawer behavior:
- Permissions and platform APIs touched:
- Device and Android version tested:
- Known bugs:
- Current task:
- Next single task:

## Definition of done

The intended code is committed, the relevant Actions workflow is verified, the APK installs/updates correctly, and affected launcher flows are tested on-device. Permission denial, offline/unavailable data, theme changes, and returning to Home must be considered where relevant. If only compilation was checked, label device behavior as unverified.
