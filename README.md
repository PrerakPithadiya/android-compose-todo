# TaskFlow - iOS-Inspired Android Task Manager

TaskFlow is a modern, high-performance task management application for Android, carefully crafted to deliver an authentic **Apple iOS (Human Interface Guidelines)** look and feel. Built with **Kotlin** and **Jetpack Compose**, it offers extensive features, smooth animations, and premium security controls.

---

## 📱 Key Features

### 1. 🎨 Apple (iOS HIG) Design System
* **Authentic Styling**: Uses iOS system color tokens (`SystemBlue`, `SystemGroupedBackground`, `SystemSurface`) and divider styles.
* **Inset Grouped Layouts**: Task lists and action cards are grouped within `12dp` rounded container structures.
* **Apple Typographic Hierarchy**: Formatted using SF-styled font sizes, weights, and letter-spacings.

### 2. 📅 Interactive Calendar Screen
* **Multi-View Interface**: Toggle seamlessly between **Month**, **Week**, and **Agenda** modes.
* **Smart Sorting**: Automatically groups and ranks tasks chronologically by time (AM/PM parsed values) with completed tasks grouped cleanly at the bottom.
* **Filters**: Quick filtering options by category list.

### 3. 🔒 Passcode, Pattern & Biometric Security
* **App Lock Options**: Configure Passcode (using custom iOS-style numeric keypads), Pattern Lock (via custom drawn pattern canvases), or Biometrics (fingerprint/face unlock).
* **Smart Background Locking**: Monitors lifecycle changes using `FragmentActivity` to automatically prompt auth whenever the app is backgrounded or resumed.

### 4. 🔔 Smart Local Reminders
* **Scheduled Alarms**: Direct integration with Android's `AlarmManager` to broadcast scheduled reminders.
* **Precise Timing**: Uses `SCHEDULE_EXACT_ALARM` and `POST_NOTIFICATIONS` to prompt exact-time alarms.

### 5. 👤 Dedicated Apple (iOS HIG) Profile Page & Photo Editor
* **Apple "Move and Scale" Photo Editor**: Immersive full-screen avatar editor with multi-touch pinch-to-zoom (`1.0x` to `5.0x`), 2D pan gestures, circular crop mask, and dynamic rule-of-thirds grid.
* **Transformations & 7 Apple Filters**: 90° clockwise rotation, horizontal flipping, reset controls, and 7 curated Apple iOS filters (`Original`, `Vivid`, `Warm`, `Cool`, `Noir`, `Silvertone`, `Dramatic`) plus brightness & contrast adjustments.
* **Strict Confirmation Workflow**: Staging architecture ensuring photos are only applied and saved to private app storage once explicitly finalized and confirmed by the user.
* **Hero Identity Card**: High-resolution avatar with live photo gallery picker / 8 curated Memoji presets, verified Pro badge, editable Bio, and interactive Focus Status selector (`🎯 Deep Work`, `⚡ In the Flow`, `🚀 Shipping Code`, `☕ Coffee Break`, `🏖️ On Holiday`).
* **Productivity Level & XP System**: Real-time calculated Level & XP progress bar based on daily task completions, early bird achievements, and streaks.
* **Apple Watch / Fitness Achievement Medals**: 8 collectible 3D glowing medals (`🌟 First Step`, `🔥 Week on Fire`, `⚡ Hyper Focus`, `🌅 Early Bird`, `📚 Master Organizer`, `🛡️ Fort Knox`, `🎯 Perfectionist`, `👑 Century Club`) with interactive inspection sheets.
* **Productivity Bento Analytics**: 4-cell Bento grid showcasing Total Completed Tasks, Active Streak flame counter, Focus Time saved, On-Time Efficiency %, and 7-day consistency bar visualizer.
* **TaskFlow Pro & iCloud Sync Hub**: Apple Card-styled metallic card with real-time iCloud sync status, manual "Sync Now" trigger with rotating animation, and Pro perks overview.
* **Digital Productivity Pass & QR Sharing**: Apple Wallet-styled shareable digital pass with QR code and Android System Share Sheet integration.

### 6. 🗂️ Custom Lists & Categories
* **CRUD Categories**: Create, customize, and delete lists with specific colors (Teal, Indigo, Purple, Orange, Red, Pink, etc.) and custom symbols.
* **Safe Deletions**: Prompt-based migration system that safely reassigns tasks to other lists when a list is deleted.

### ⚡ 7. Performance & Tactile Enhancements
* **120Hz Refresh Rate**: Integrates `HighRefreshRateManager` to lock display refresh rates to maximum levels (120Hz/144Hz) and prevent Variable Refresh Rate (VRR) throttling.
* **Rich Haptic Engine**: Incorporates customizable vibrations (Light, Medium, Strong intensities) for keypad entries, pattern drawing, and checklist actions.

---

## 🛠️ Technology Stack & Dependencies

* **Language**: Kotlin
* **UI Framework**: Jetpack Compose & Material 3
* **Image Loading**: Coil
* **Authentication**: AndroidX Biometric library
* **Notifications & Timing**: Android AlarmManager & BroadcastReceivers
* **Build System**: Kotlin Gradle DSL (with Version Catalogs `libs.versions.toml`)

---

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/PrerakPithadiya/android-compose-todo.git
   ```
2. Open the project in **Android Studio (Ladybug or newer)**.
3. Sync Gradle and run the application on a device running Android 7.0 (API 24) or higher.
