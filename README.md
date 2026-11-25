# FocusGuard (ScrollStop)

**FocusGuard** is a lightweight, privacy-first, open-source Android app built with **Kotlin** and **Jetpack Compose (Material 3)** that helps you break the habit of mindless scrolling across **Instagram, TikTok, X (Twitter), Snapchat, and Threads** using flexible blocking schedules, session-based usage allowances with automatic cooldowns, mindful friction, and a pass-through gray overlay during temporary unlocks.

---

## ✨ Features

- **Multi-App Protection (Shared Schedule & Pool)**
  - Guard **Instagram**, **TikTok**, **X (Twitter)**, **Snapchat**, and **Threads** (pick any combination; at least one app remains selected).
  - All selected apps share a single schedule, hourly unlock quota, or session allowance so you can't bypass a limit by switching to another feed.
- **Three Flexible Modes**
  - **All Day (24/7):** Block selected apps around the clock.
  - **Custom Time Periods:** Add any number of custom focus intervals (including overnight ranges that cross midnight, such as `22:00 – 02:00`). Adjacent or overlapping intervals are automatically merged when evaluating active focus windows.
  - **Session Limit Mode (Hourly Allowance):** A "use first, block after" approach for intentional short visits:
    - **Full-Color Free Access:** Open guarded apps immediately with **no upfront lock screen** and **no gray screen overlay**.
    - **Configurable Session Presets:** Choose **5, 10, or 15 minutes** of shared active usage per clock hour via quick preset chips.
    - **Active Screen-On Tracking:** Time only ticks while a guarded app is in the foreground and the screen is on — leaving the app or locking your phone pauses the timer automatically.
    - **2-Minute Warning Banner:** When 2 minutes remain in the current hour, a non-intrusive, touch-pass-through pill banner appears at the top of the screen for ~5 seconds (no notification permission required).
    - **Clock-Hour Reset & Hard Block:** Once the limit is reached, selected apps are hard-blocked until the start of the next clock hour (`:00`), when both usage and blocks reset automatically.
    - **Live Status & Progress:** The home screen displays a real-time countdown, progress bar, and the exact time your allowance resets.
- **Automatic 2-Hour Rule & Hourly Quota** _(All Day & Custom Modes)_
  - **Short Focus Periods (`<= 2 hours`):** Automatically enforced as a **Hard Block** — no temporary unlock button is shown on the lock screen.
  - **Long Focus Periods (`> 2 hours`) or All Day Mode:** Allows up to **15 minutes of temporary unlock per clock hour** (when Hard Block is unchecked).
- **Hard Block Mode** _(All Day & Custom Modes)_
  - Optional checkbox that disables all temporary unlocks completely. The only way to lift a Hard Block early is to open FocusGuard and change your settings.
- **10-Second Mindful Friction Pause**
  - Before spending your hourly unlock quota in All Day / Custom mode, the lock screen displays a 10-second breathing countdown so impulsive opens can be abandoned with a single tap on **Exit to Home Screen**.
- **Desaturated / Gray Screen Pass-Through Overlay**
  - While a temporary unlock is active and a guarded app is in the foreground, FocusGuard renders a non-intrusive translucent dark-gray overlay (`TYPE_ACCESSIBILITY_OVERLAY`) over the screen so the feed looks dull and less stimulating, while letting all touches, back presses, and gestures pass straight through.
  - Leaving the guarded app hides the overlay immediately; returning while unlock time remains re-applies it; when the timer expires, the app is locked again right away.
- **8-Language Localization**
  - Supports **English, Українська, Deutsch, Español, Français, Português, Italiano, and Polski**.
  - Follows your phone's system language by default, with an instant in-app language switcher in the top app bar.
- **100% Offline & Private**
  - No internet permission, no analytics, no ads, and `canRetrieveWindowContent="false"` in the Accessibility Service configuration (screen content is never read).

---

## 🛠️ Tech Stack

| Layer                  | Technology                                                                                           |
| :--------------------- | :--------------------------------------------------------------------------------------------------- |
| **Language**           | 100% Kotlin                                                                                          |
| **UI**                 | Jetpack Compose + Material 3 (Dark Theme)                                                            |
| **System Integration** | `AccessibilityService` (`TYPE_WINDOW_STATE_CHANGED`), `WindowManager` (`TYPE_ACCESSIBILITY_OVERLAY`) |
| **Persistence**        | `SharedPreferences` + `Gson`                                                                         |
| **SDK Levels**         | `minSdk = 26` (Android 8.0) · `targetSdk = 35` (Android 15)                                          |

---

## 📂 Project Structure

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/example/scrollstop/
│   ├── AppBlockerService.kt       # AccessibilityService, session tracker, gray overlay & 2-min warning pill
│   ├── AppStrings.kt              # Localization contract, language enum & locale helpers
│   ├── LockScreenActivity.kt      # Isolated full-screen Compose lock screen, 10s friction timer & quota slider
│   ├── MainActivity.kt            # Entry point hosting FocusGuardScreen
│   ├── PermissionUtils.kt         # Overlay & Accessibility permission checks and settings intents
│   ├── SettingsManager.kt         # SharedPreferences + Gson persistence, schedule, quota & session limit engine
│   ├── i18n/                      # 8-language string tables (EN, UK, DE, ES, FR, PT, IT, PL)
│   └── ui/
│       ├── FocusGuardScreen.kt    # Main dashboard screen
│       ├── components/            # Stateless Compose cards (MasterToggle, Schedule, SessionLimit, TimePicker, etc.)
│       └── theme/                 # Minimalist dark Material 3 color scheme & typography
└── res/
    ├── values*/strings.xml        # Localized system strings (8 languages)
    └── xml/
        ├── accessibility_service_config.xml
        └── locales_config.xml
```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug or newer
- **JDK 11+**
- An Android device or emulator running **Android 8.0 (API 26)** or higher

### Build & Install

1. **Clone the repository:**

   ```bash
   git clone https://github.com/oleh-kalynovskyi/ScrollStop.git
   cd ScrollStop
   ```

2. **Open in Android Studio** and press **Run (`▶`)**, or build/install from the terminal:

   ```bash
   # Build debug APK
   ./gradlew :app:assembleDebug

   # Run unit tests
   ./gradlew :app:testDebugUnitTest

   # Install directly onto a connected device
   ./gradlew :app:installDebug
   ```

   The debug APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🔐 First-Launch Permissions Setup

FocusGuard requires two Android system permissions shown at the top of the home screen in the **Permissions Dashboard**:

1. **Display Over Other Apps (`SYSTEM_ALERT_WINDOW`)**
   - Used alongside the Accessibility overlay to keep overlays and lock transitions reliable across Android versions.
2. **Accessibility Service (`FocusGuard Blocker`)**
   - Used strictly to detect when a selected guarded app (Instagram, TikTok, X, Snapchat, or Threads) enters the foreground, track active session time, and display the pass-through gray overlay or 2-minute warning pill.
   - Configured with `android:canRetrieveWindowContent="false"` — **FocusGuard cannot read your messages, passwords, or screen text**.

> **Note for Android 13+ (Sideloaded APKs):**
> If Android shows _"Restricted setting"_ when you try to enable **FocusGuard Blocker** in Accessibility Settings:
>
> 1. Open **System Settings → Apps → FocusGuard**.
> 2. Tap the **⋮** menu in the top-right corner and select **Allow restricted settings**.
> 3. Return to **Accessibility Settings** and enable **FocusGuard Blocker**.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Some ideas for future enhancements:

- Additional guarded apps (e.g., YouTube Shorts, Reddit)
- Daily & weekly block/unlock statistics
- Optional strict-mode cooldown before disabling the Master Blocker

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes (`git commit -m 'Add my feature'`)
4. Push to the branch (`git push origin feature/my-feature`)
5. Open a Pull Request

---

## 📄 License

This project is open source and available under the [MIT License](https://github.com/oleh-kalynovskyi/stopScroll/blob/main/MIT%20License).
This project is open source and available under the [GNU General Public License v3.0](https://github.com/oleh-kalynovskyi/stopScroll/blob/main/GNU%20General%20Public%20License%20v3.0).
