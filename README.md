# Ratio Launcher

A minimalist Android launcher inspired by the discontinued Blloc Ratio. Built from scratch with focus on digital wellbeing and clean design.

## Features

**Home (Root)**
- 7 clock styles: Minimal, Bold, Flip Clock, Word Clock, Binary, Day Progress, Analog
- Screen time tracking with per-app breakdown
- Weather (temperature, humidity, wind, rain, tomorrow forecast)
- Calendar events
- Media player controls (play/pause/skip)
- Quick notes with full-screen editor
- Customizable dock with pinned apps
- Tap clock → Alarm | Long-press clock → Settings

**App Drawer (Tiles)**
- 4-column grid with rounded tiles
- Auto-categorized by app type
- Custom categories (long-press → Move to category)
- Search bar to filter apps
- Grayscale icons toggle
- Hide apps, uninstall, add to dock

**Notifications (Tree)**
- Groups messages by sender via NotificationListenerService
- Tap to open app, long-press to dismiss
- Supports all notification types (messaging, Home Assistant, etc.)

**Settings**
- Theme system (Dark / Focus / Light / Sun)
- Clock style picker with live carousel preview
- 24h/12h format, show seconds toggle
- Hide status bar (custom WiFi/battery overlay)
- Double-tap to lock screen
- Usage goals with notifications
- Focus hours with auto-reply
- Icon pack support
- Backup & restore configuration
- Reorder Root widgets (drag & drop)
- Reorder app drawer categories (drag & drop)

## Screenshots

Visit the [project page](https://bloedboemmel.github.io/ratio-launcher/) for a preview.

## Build

```bash
# Clone
git clone https://github.com/bloedboemmel/ratio-launcher.git
cd ratio-launcher

# Build debug APK
./gradlew assembleDebug

# Install
adb install app/build/outputs/apk/debug/app-debug.apk
```

Requires JDK 17 and Android SDK 35.

## Download

Get the latest APK from [Releases](https://github.com/bloedboemmel/ratio-launcher/releases).

## Tech Stack

- Kotlin
- Android SDK 35 (min SDK 26)
- ViewPager2 for swipe navigation
- NotificationListenerService for Tree
- UsageStatsManager for screen time
- wttr.in for weather (no API key needed)
- Sentry for crash reporting & session replay
- Inter font family (from original Ratio APK)

## CI/CD

- **Code Quality** — Android Lint + Kotlin compile on every push
- **Prerelease** — Builds debug APK, publishes GitHub prerelease on main
- **Release** — Signs APK + publishes to Google Play on version tag (`v*.*.*`)
- **Quality Report** — Published to [GitHub Pages](https://bloedboemmel.github.io/ratio-launcher/quality/)

## License

This project is an independent recreation for educational purposes.
