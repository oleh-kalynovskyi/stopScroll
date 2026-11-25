# FocusGuard (ScrollStop)

**FocusGuard** is a lightweight, privacy-first, open-source Android app built with **Kotlin** and **Jetpack Compose (Material 3)** that helps you break the habit of mindless Instagram scrolling using flexible blocking schedules, mindful friction, and a grayscale pass-through overlay during temporary unlocks.

---

## ✨ Features

- **Flexible Blocking Schedules**
  - **All Day (24/7):** Block selected apps around the clock.
  - **Custom Time Periods:** Add any number of custom focus intervals (including overnight ranges that cross midnight, such as `22:00 – 02:00`). Adjacent or overlapping intervals are automatically merged when evaluating active focus windows.
  - **Session Limit Mode (Allowance + Cooldown):** Use selected apps freely in full color (no gray screen, no upfront lock screen) for a shared allowance of **5, 10, or 15 minutes** of active screen-on time. A non-intrusive top pill banner warns 2 minutes before the limit; once reached, apps are hard-blocked for **1 hour**. Staying away from guarded apps for 1 hour automatically resets any partial session usage.
- **Automatic 2-Hour Rule & Hourly Quota**
  - **Short Focus Periods (`<= 2 hours`):** Automatically enforced as a **Hard Block** — no temporary unlock button is shown on the lock screen.
  - **Long Focus Periods (`> 2 hours`) or All Day Mode:** Allows up to **15 minutes of temporary unlock per clock hour** (when Hard Block is unchecked).
- **Hard Block Mode**
  - Optional checkbox available in both **All Day** and **Custom Periods** modes that disables all temporary unlocks completely. The only way to lift a Hard Block early is to open FocusGuard and change your settings.
- **10-Second Mindful Friction Pause**
  - Before you can spend your hourly unlock quota, the lock screen displays a 10-second breathing countdown so impulsive opens can be abandoned with a single tap on **Exit to Home Screen**.
- **Desaturated / Gray Screen Pass-Through Overlay**
  - While a temporary unlock is active and Instagram is in the foreground, FocusGuard renders a non-intrusive translucent dark-gray overlay (`TYPE_APPLICATION_OVERLAY`) over the screen so the feed looks dull and less stimulating, while letting all touches and gestures pass straight through.
  - Leaving Instagram hides the overlay immediately; returning while unlock time remains re-applies it; when the timer expires, Instagram is locked again right away.
- **Bilingual UI (English & Українська)**
  - Automatically follows your phone's system language by default, with an instant language switcher (`Auto / EN / UA`) in the top app bar.
- **100% Offline & Private**
  - No internet permission, no analytics, no ads, and `canRetrieveWindowContent="false"` in the Accessibility Service configuration (screen content is never read).

---

## 🛠️ Tech Stack

| Layer | Technology |
| :--- | :--- |
| **Language** | 100% Kotlin |
| **UI** | Jetpack Compose + Material 3 (Dark Theme) |
| **System Integration** | `AccessibilityService` (`TYPE_WINDOW_STATE_CHANGED`), `WindowManager` (`TYPE_APPLICATION_OVERLAY`) |
| **Persistence** | `SharedPreferences` + `Gson` |
| **SDK Levels** | `minSdk = 26` (Android 8.0) · `targetSdk = 35` (Android 15) |

---

## 📂 Project Structure

```text
app/src/main/
├── AndroidManifest.xml
├── java/com/example/scrollstop/
│   ├── AppBlockerService.kt     # AccessibilityService + pass-through gray WindowManager overlay
│   ├── AppStrings.kt            # English & Ukrainian localization + locale helpers
│   ├── LockScreenActivity.kt    # Full-screen Compose lock screen, 10s friction timer & quota slider
│   ├── MainActivity.kt          # Permissions dashboard, master toggle, schedule & TimePicker UI
│   ├── SettingsManager.kt       # SharedPreferences + Gson persistence, schedule & hourly quota engine
│   └── ui/theme/
│       └── Theme.kt             # Minimalist dark Material 3 color scheme
└── res/
    ├── values/strings.xml       # Default (English) system strings
    ├── values-uk/strings.xml    # Ukrainian system strings
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
   git clone https://github.com/<your-username>/ScrollStop.git
   cd ScrollStop
   ```

2. **Open in Android Studio** and press **Run (`▶`)**, or build/install from the terminal:
   ```bash
   # Build debug APK
   ./gradlew :app:assembleDebug

   # Install directly onto a connected device
   ./gradlew :app:installDebug
   ```
   The debug APK will be generated at `app/build/outputs/apk/debug/app-debug.apk`.

---

## 🔐 First-Launch Permissions Setup

FocusGuard requires two Android system permissions shown at the top of the home screen in the **Permissions Dashboard**:

1. **Display Over Other Apps (`SYSTEM_ALERT_WINDOW`)**
   - Used to render the pass-through gray tint overlay during temporary unlocks.
2. **Accessibility Service (`FocusGuard Blocker`)**
   - Used strictly to detect when `com.instagram.android` enters the foreground.
   - Configured with `android:canRetrieveWindowContent="false"` — **FocusGuard cannot read your messages, passwords, or screen text**.

> **Note for Android 13+ (Sideloaded APKs):**
> If Android shows *"Restricted setting"* when you try to enable **FocusGuard Blocker** in Accessibility Settings:
> 1. Open **System Settings → Apps → FocusGuard**.
> 2. Tap the **⋮** menu in the top-right corner and select **Allow restricted settings**.
> 3. Return to **Accessibility Settings** and enable **FocusGuard Blocker**.

---

## 🤝 Contributing

Contributions, issues, and feature requests are welcome! Some ideas for future enhancements:
- Multi-app package selection (TikTok, YouTube Shorts, X/Twitter, etc.)
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
