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

### 5. 🗂️ Custom Lists & Categories
* **CRUD Categories**: Create, customize, and delete lists with specific colors (Teal, Indigo, Purple, Orange, Red, Pink, etc.) and custom symbols.
* **Safe Deletions**: Prompt-based migration system that safely reassigns tasks to other lists when a list is deleted.

### ⚡ 6. Performance & Tactile Enhancements
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
