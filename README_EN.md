# tiedun 铁盾

**English** | [中文](README.md)

Android device protection and emergency response tool: when a device is taken over by lockscreen malware or accessibility-abusing lock screens, press a **physical key combo** to pull up the response UI, cover the screen with a full-screen intercept, and use Shizuku / root to stop, disable, or uninstall the offending component. Includes a blacklist shield, root monitor, and accessibility keep-alive for long-running protection on devices you control.

## Core idea

```
Accessibility service watches foreground switches + volume-key combo (long-press vol-down / 4 taps)
  → Launch full-screen response UI
  → Read current top package via accessibility / root channel
  → Safety checks (blacklist, whitelist, critical system packages)
  → Shizuku/root: am force-stop → pm disable-user (or pm uninstall)
  → Optional continuous suppression: force-stop every 2s to prevent auto-restart
```

## Design references

| Capability | Reference project | License | Verified |
|------------|-------------------|---------|----------|
| Hardware key listening | [F111111shhh/Extinguish](https://github.com/F111111shhh/Extinguish) (fork of [Moderpach/Extinguish](https://github.com/Moderpach/Extinguish)) | GPL-3.0 | Yes |
| Full-screen overlay | [coderlidy/Fake-Lock-Screen](https://github.com/coderlidy/Fake-Lock-Screen) | MIT | Yes |
| Root privilege channel | [RikkaApps/Shizuku](https://github.com/RikkaApps/Shizuku) | Apache-2.0 | Yes |
| Disable / freeze | [aistra0528/Hail](https://github.com/aistra0528/Hail) | GPL-3.0 | Yes |

## Excluded / non-compliant sources

| Project | Risk | Handling |
|---------|------|----------|
| [hnpdsyn/PhoneGuard](https://github.com/hnpdsyn/PhoneGuard) | No LICENSE file; redistribution rights unknown | Excluded: no copy, rewrite, or reuse; not an implementation basis |
| [me.lucky/wasted](https://github.com/me.lucky/wasted) | Repository unavailable (HTTP 404); license unknown | Excluded: no copy, rewrite, or reuse; not an implementation basis |

## Upstream list

All upstream sources for this project are listed below. Full copyright and license texts are in [NOTICE](NOTICE); per-item compliance notes are in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md):

| # | Upstream | License | Relation |
|---|----------|---------|----------|
| 1 | F111111shhh/Extinguish | GPL-3.0 | Design ref: hardware key listening |
| 2 | Moderpach/Extinguish | GPL-3.0 | Original upstream of the Extinguish fork |
| 3 | coderlidy/Fake-Lock-Screen | MIT | Design ref: full-screen overlay |
| 4 | RikkaApps/Shizuku | Apache-2.0 | Dependency: privileged command channel |
| 5 | aistra0528/Hail | GPL-3.0 | Design ref: disable / freeze |
| 6 | hnpdsyn/PhoneGuard | No LICENSE | **Excluded**, not an implementation basis |
| 7 | me.lucky/wasted | Unverifiable | **Excluded**, not an implementation basis |

Transitive dependency inventory (AndroidX / Material / Kotlin / Coroutines, etc.) is in [docs/DEPENDENCY_LICENSES.md](docs/DEPENDENCY_LICENSES.md); regenerate with `gradle generateDependencyLicenseReport`.

## License and compliance

- Primary license: **GPL-3.0** (see [LICENSE](LICENSE)).
- Copyright and full license texts for upstream references: [NOTICE](NOTICE).
- All original source is distributed under GPL-3.0 with GPL headers retained; resources, build scripts, docs, and APK distribution materials ship with the project.
- When distributing an APK, provide the matching source version, `LICENSE`, `NOTICE`, and `THIRD_PARTY_NOTICES.md`; a branch link alone does not prove source correspondence.
- `hnpdsyn/PhoneGuard` and `me.lucky/wasted` must not be copied, rewritten, or reused. Current code uses only public Android SDK APIs, the Shizuku dependency, and original GPL-3.0 code.
- APKs embed license and source instructions under `assets/GPL/`; third-party license materials live in [third_party/](third_party/).

## Features

| Capability | Description |
|------------|-------------|
| Blacklist shield | Watches foreground apps; on blacklist hit shows a warning and offers actions |
| Whitelist protection | Launcher, IME, critical components, and user whitelist are never touched |
| Emergency actions | Force-stop / disable / uninstall, each with a confirmation step |
| Continuous suppression | Optional always-on; the console suppresses on open, force-stop every 2s |
| Root monitor | Root daemon samples foreground window; optional accessibility keep-alive |
| Shizuku channel | Runs fixed commands via Shizuku when root is absent |
| Escape trigger | Long-press vol-down 1.5s, or 4 volume-key taps |
| Reliability | Foreground service, boot restore, battery-optimization guidance, crash log |

## Triggers

Target identification requires the user to enable the accessibility service. A package name is kept only when a target app becomes the foreground window. System settings, the launcher, system components, and IME windows clear the previous target; the response UI itself never overwrites it. Records expire after 2 minutes to avoid acting on stale targets. Key triggers only open a human-confirmation UI — they never uninstall or disable anything automatically.

Before any action the app re-validates the target package, capture time, user whitelist, current default launcher, and current IME. If the target changed or expired, the action stops. Command output containing failure text, truncated output, or a timeout is never reported as success; disable and uninstall also require the package manager's success receipt. If a Shizuku command was sent but the result is unknown, the app does not retry via root.

| Trigger | Action |
|---------|--------|
| Long-press vol-down | Hold 1.5s to open the response UI |
| Repeated volume keys | 4 taps within 1.5s to open the response UI |

## Actions

| Action | Command | System-app protection |
|--------|---------|----------------------|
| Force-stop | am force-stop | Allowed with a warning |
| Disable | pm disable-user --user 0 | Blocked for blacklist |
| Uninstall | pm uninstall --user 0 | Blocked for blacklist; on success returns to home |
| Continuous suppression | force-stop every 2s | Blocked for blacklist |

On successful uninstall a dialog confirms and returns to the home screen; on failure it says "Uninstall may have failed" with options to go home or stay and retry.

## Build

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```

## Status

- ✅ Project skeleton (Kotlin + Gradle)
- ✅ Accessibility service + key listening
- ✅ Full-screen response UI
- ✅ Shizuku/root action channels
- ✅ Foreground keep-alive
- ✅ Critical-system blacklist + confirmation
- ✅ Continuous suppression (optional always-on, suppress on open)
- ✅ User whitelist + default launcher protection
- ✅ Root monitor + accessibility keep-alive (master switch)
- ✅ Boot restore
- ✅ Uninstall success → home / failure prompt
- ✅ Transitive dependency license inventory
- ✅ Single launcher shortcut, original icon, terminal-style palette

## Design notes

This project is an original implementation: UI, icons, copy, and code structure are independently designed. Nothing is copied from the reference projects' source, layouts, or visual assets. The upstreams above are recorded only as capability references and license obligations.

Safety boundaries:

- Critical system components, default launcher, IME, Shizuku, and this tool itself are never acted upon.
- Root accessibility keep-alive only runs when the user explicitly enables it; it can be turned off anytime and does not block uninstall.
- Target profiles and scores are advisory only and are not virus verdicts.

---

Released under GPL-3.0. Modifications and extensions are welcome — keep copyright and upstream notices, and follow [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
