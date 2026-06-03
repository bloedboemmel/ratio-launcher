# Ratio Launcher

A recreation of the discontinued Blloc Ratio Launcher for Android. Minimalist, distraction-free, focused on digital wellbeing.

## Design

- **Monochrome**: Near-black background (#0B0B0B) with white text and electric yellow-green accent (#E8FC40)
- **Three views**: Tree (messaging placeholder) | Root (widgets/dashboard) | Tiles (app drawer)
- **Digital wellbeing**: Screen time tracking, minimal distractions, organized apps

## Features

- Real Android launcher (registers as HOME screen)
- Swipe navigation between 3 views (ViewPager2)
- **Root**: Live clock, screen time tracker, weather card, functional notes
- **Tiles**: All installed apps organized into auto-categorized collapsible drawers
- App search, monochrome icons, long-press to uninstall
- Immersive full-screen experience

## Build

Requires:
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34

```bash
# Clone and open in Android Studio, or:
./gradlew assembleDebug

# APK output:
# app/build/outputs/apk/debug/app-debug.apk
```

## Set as Default Launcher

After installing, press Home and select "Ratio" from the launcher picker.
To grant screen time tracking: tap the "Tap to enable" text on the Root screen.

## Architecture

```
com.ratio.launcher/
├── MainActivity.kt          — ViewPager2 host + navigation
├── fragments/
│   ├── TreeFragment.kt      — Messaging placeholder
│   ├── RootFragment.kt      — Clock, usage, weather, notes
│   └── TilesFragment.kt     — App drawer with search
├── adapters/
│   ├── ViewPagerAdapter.kt  — Fragment adapter
│   ├── AppListAdapter.kt    — Categorized app list
│   └── NotesAdapter.kt      — Notes RecyclerView
├── models/
│   ├── AppInfo.kt           — App data model
│   └── Note.kt              — Note data model
└── utils/
    └── UsageStatsHelper.kt  — Screen time via UsageStatsManager
```

## License

MIT — This is an independent recreation, not affiliated with Blloc GmbH.
