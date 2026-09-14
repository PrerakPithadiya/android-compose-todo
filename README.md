# TaskFlow - iOS-Inspired Android Task Manager

TaskFlow is a modern, high-performance task management application for Android, carefully crafted to deliver an authentic **Apple iOS (Human Interface Guidelines)** look and feel. Built with **Kotlin** and **Jetpack Compose**, it offers extensive features, smooth animations, and premium security controls.

---

## 📱 Key Features

### 1. 🔐 Account Auto-Detection, Phone OTP Registration & Login
* **Automatic Account Detection**: Automatically detects whether an account exists on launch; dynamically routes first-time users to the **Phone Registration** workflow, logged-out users to the **Login Screen**, and authenticated users directly to their workspace.
* **6-Digit Phone OTP Verification & 30s Countdown**: Clean phone number input with international country code selection, automated 6-digit verification code dispatch, and an Apple-styled **Interactive Push Notification Banner** featuring a live 30-second countdown timer pill, automatic 30s code expiration/dismissal, and 1-tap autofill directly into the 6-digit destination input cells with automatic keyboard management.
* **Account Setup & Password Security**: Streamlined profile initialization (Full Name, Username `@handle`, and 4-rule strict password validation checklist: 8+ chars, 1+ number, uppercase/symbol, match confirmation) with a 4-segment live strength meter and SHA-256 random salting.
* **Login & Recovery**: Username/Phone and Password authentication with visibility toggles, biometric instant login shortcut, "Forgot Password" SMS OTP recovery flow, and intuitive Log Out controls in Settings & Profile.

### 2. 🎨 Apple (iOS HIG) Design System
* **Authentic Styling**: Uses iOS system color tokens (`SystemBlue`, `SystemGroupedBackground`, `SystemSurface`) and divider styles.
* **Inset Grouped Layouts**: Task lists and action cards are grouped within `12dp` rounded container structures.
* **Apple Typographic Hierarchy**: Formatted using SF-styled font sizes, weights, and letter-spacings.

### 3. 📅 Interactive Calendar Screen
* **Multi-View Interface**: Toggle seamlessly between **Month**, **Week**, and **Agenda** modes.
* **Smart Sorting**: Automatically groups and ranks tasks chronologically by time (AM/PM parsed values) with completed tasks grouped cleanly at the bottom.
* **Filters**: Quick filtering options by category list.

### 4. 🔒 Passcode, Pattern & Biometric Security (Face ID & Fingerprint)
* **Hardware Modality Detection**: Automatically inspects device hardware (`PackageManager.FEATURE_FACE`, `PackageManager.FEATURE_FINGERPRINT`) and `BiometricManager` capability to dynamically adapt the security experience.
* **Apple Face ID Integration**: Features an authentic Apple iOS Face ID vector icon (`IosFaceIdIcon`), adaptive keypad buttons, contextual subtitles ("Look at your device to continue"), and dynamic status badges conforming to Apple Human Interface Guidelines.
* **App Lock Options**: Configure Passcode (4-digit & 6-digit using custom iOS-style numeric keypads), Pattern Lock (via custom drawn pattern canvases), Alphanumeric Password, or Biometrics (Face ID / Fingerprint unlock).
* **Multi-Modal Biometric Support**: Seamless support for Class 3 and Class 2 biometrics (Google Pixel 4/7/8/9, Samsung Galaxy One UI Face Recognition, OnePlus, Xiaomi) with zero battery drain, secure TEE authentication, and graceful passcode fallbacks.
* **Smart Background Locking**: Monitors lifecycle changes using `FragmentActivity` to automatically prompt auth whenever the app is backgrounded or resumed.

### 5. 🔔 Smart Local Reminders & Dynamic Notification Branding
* **Scheduled Alarms**: Direct integration with Android's `AlarmManager` to broadcast scheduled reminders.
* **Precise Timing**: Uses `SCHEDULE_EXACT_ALARM` and `POST_NOTIFICATIONS` to prompt exact-time alarms.
* **Dynamic Active Icon Branding**: Reminders dynamically query `AppIconManager.getActiveIcon()` and render the user's currently selected app icon into a high-resolution, anti-aliased Apple squircle bitmap via `.setLargeIcon(iconBitmap)`.
* **Monochrome Status Bar Vector**: Uses a dedicated, compliant circular checkmark vector (`ic_notification_check`) for `.setSmallIcon()` tinted with the active icon's accent color, completely eliminating legacy default Android robot fallbacks.

### 6. 👤 Dedicated Apple (iOS HIG) Profile Page & Photo Editor
* **Apple "Move and Scale" Photo Editor**: Immersive full-screen avatar editor with multi-touch pinch-to-zoom (`1.0x` to `5.0x`), 2D pan gestures, circular crop mask, and dynamic rule-of-thirds grid.
* **Transformations & 7 Apple Filters**: 90° clockwise rotation, horizontal flipping, reset controls, and 7 curated Apple iOS filters (`Original`, `Vivid`, `Warm`, `Cool`, `Noir`, `Silvertone`, `Dramatic`) plus brightness & contrast adjustments.
* **Strict Confirmation Workflow**: Staging architecture ensuring photos are only applied and saved to private app storage once explicitly finalized and confirmed by the user.
* **Hero Identity Card**: High-resolution avatar with live photo gallery picker / 8 curated Memoji presets, verified Pro badge, editable Bio, and interactive Focus Status selector (`🎯 Deep Work`, `⚡ In the Flow`, `🚀 Shipping Code`, `☕ Coffee Break`, `🏖️ On Holiday`).
* **Productivity Level & XP System**: Real-time calculated Level & XP progress bar based on daily task completions, early bird achievements, and streaks.
* **Apple Watch / Fitness Achievement Medals**: 8 collectible 3D glowing medals (`🌟 First Step`, `🔥 Week on Fire`, `⚡ Hyper Focus`, `🌅 Early Bird`, `📚 Master Organizer`, `🛡️ Fort Knox`, `🎯 Perfectionist`, `👑 Century Club`) with detailed unlock criteria, actionable profile tips, interactive inspection sheets, and quick filter tabs ("All", "Achieved 🏆", "In Progress 🔒").
* **Productivity Bento Analytics**: 4-cell Bento grid showcasing Total Completed Tasks, Active Streak flame counter, Focus Time saved, On-Time Efficiency %, and 7-day consistency bar visualizer.
* **Multi-Layer Security Sheet**: 4-stage biometric + OTP verified password change flow and inline username editor.
* **Digital Productivity Pass & QR Sharing**: Apple Wallet-styled shareable digital pass with QR code and Android System Share Sheet integration.

### 7. 🗂️ Custom Lists & Categories
* **CRUD Categories**: Create, customize, and delete lists with specific colors (Teal, Indigo, Purple, Orange, Red, Pink, etc.) and custom symbols.
* **Safe Deletions**: Prompt-based migration system that safely reassigns tasks to other lists when a list is deleted.

### 8. 💾 Multi-User Android Jetpack Room (SQLite) Database Architecture
* **Dedicated `users` Table**: All registered users are stored in SQLite with full credentials (SHA-256 salted hashes), unique `@usernames`, phone numbers, profile information, and account creation timestamps.
* **Per-User Screen Locks in SQLite**: Each user's screen lock type (PIN, Pattern, Alphanumeric), salted passcode hash, biometric preferences, and lock timeout settings are stored directly in their SQLite user row.
* **Per-User Task Isolation**: Tasks are associated with individual user IDs (`userId` foreign reference). When User A logs in, they see User A's tasks; when User B logs in, they see User B's tasks.
* **Single Source of Truth**: Full local-first persistence for users, tasks, and categories using official Android Jetpack Room 2.8 with Google KSP.
* **Reactive Kotlin Flow Streaming**: UI automatically reacts and re-renders instantaneously upon any database insertion, update, or deletion with zero polling or latency.
* **Atomic Category Migration**: Category deletions with task migrations execute inside atomic SQLite transactions (`withTransaction`), preventing data loss or orphaned tasks.
* **Pre-population & Seeding**: Automatic database initialization callback seeds standard default categories (`Work`, `Personal`, `Health`, `Study`) and starter items for newly registered users on first launch.
* **Offline-First Resilience**: All operations work without internet connection, ensuring 100% data integrity and instant startup performance.

### ⚡ 9. Performance & Tactile Enhancements
* **120Hz Refresh Rate**: Integrates `HighRefreshRateManager` to lock display refresh rates to maximum levels (120Hz/144Hz) and prevent Variable Refresh Rate (VRR) throttling.
* **Rich Haptic Engine**: Incorporates customizable vibrations (Light, Medium, Strong intensities) for keypad entries, pattern drawing, and checklist actions.

### 🎨 10. Dynamic App Icon Customization (Android Activity-Alias)
* **Android `<activity-alias>` Architecture**: Full system-level launcher icon customization leveraging native Android `<activity-alias>` declarations pointing to `.MainActivity`, resolving the manifest launcher intent-filter properly without duplicate or missing launcher entries.
* **8 Apple (iOS HIG) Curated Icon Styles**:
  * **Classic iOS**: Apple System Blue gradient (`#007AFF` to `#0055D4`) with signature white circular checklist glyph.
  * **Dark Minimal**: Deep obsidian/graphite dark aesthetic (`#2C2C2E` to `#121214`) with sleek silver border.
  * **Neon Blue Glow**: Midnight deep navy background (`#0D1326` to `#050811`) with electric cyan radiant glow (`#00F0FF`).
  * **Glassmorphism**: Apple chromatic aurora gradient (`#6C5CE7` -> `#FD79A8` -> `#74B9FF`) with frosted glass refraction badge.
  * **Sunset Coral**: Warm California twilight gradient (`#FF5E3A` to `#FF2A68`) with clean circular checkmark.
  * **Emerald Mint**: Fresh natural Apple Health green gradient (`#34C759` to `#00A86B`) with clean circular checkmark.
  * **Royal Purple**: Regal iOS ultraviolet gradient (`#AF52DE` to `#5856D6`) with clean circular checkmark.
  * **Champagne Gold**: Luxe warm metallic champagne glow (`#F3A152` to `#E5832E` on dark graphite) with gold circular checkmark.
* **Zero-Downtime Icon Switching**: Executes component enablement with `PackageManager.DONT_KILL_APP` and enables target aliases before disabling inactive aliases to prevent launcher icon disappearance.
* **Reactive Singleton State**: Managed via `AppIconManager` with SharedPreferences persistence, automatic PackageManager synchronization, and an authentic Apple squircle preview bottom sheet in Settings with smooth vertical scrolling.

### 🧠 11. Small LLM Integration: TaskFlow Intelligence & Schedule Optimizer
* **Dual-Engine Hybrid Architecture**: Combines ultra-lightweight cloud intelligence (**Google Gemini 1.5 Flash / 2.0 Flash**) with an embedded **Deterministic On-Device Heuristic Engine** (`LocalScheduleOptimizer`) for instantaneous, 100% offline schedule planning with zero APK weight increase.
* **100% User Data Access & Zero-Hallucination Guarantee**:
  * **Dynamic Grounding Context (`TaskFlowContextBuilder`)**: Ingests and serializes the active user's real-time SQLite tasks, categories, profile identity, daily task goals, active focus status (`🎯 Deep Work`, `⚡ In the Flow`), morning digest settings, and regional timezone into a compact, structured JSON snapshot.
  * **Closed-World Directives & Determinism**: Enforces closed-world system directives with `temperature = 0.0`, `topK = 1`, and strict JSON Schema output mode to eliminate creative drift.
  * **Mathematical Cross-Validation Barrier (`TaskFlowCrossValidator`)**: Every suggested task ID and schedule returned by the LLM is cross-validated against the local Room SQLite database before reaching the UI. Non-existent or altered task IDs are automatically discarded, providing a mathematical guarantee against hallucinations.
  * **Human-In-The-Loop Confirmation**: Displays clear visual diffs (e.g. original time vs. suggested time, priority adjustments) requiring explicit user confirmation before modifying SQLite tasks or alarms.
* **Intelligent Schedule Planning & Time-Conflict Resolution**:
  * Automatically detects overlapping time collisions (e.g. two tasks scheduled at 02:00 PM).
  * Reorganizes tasks chronologically according to cognitive load and time of day.
  * Identifies productivity buffer gaps (>90 minutes) and suggests restorative breaks.
  * Recommends optimal Focus Status for the day.
* **Auto-Suggest Task Priorities (Eisenhower Matrix)**:
  * Automatically classifies tasks into authentic Apple HIG priority tiers: **🔴 High (P1 / Urgent & Important)**, **🟡 Medium (P2 / Important)**, and **🔵 Low (P3 / Flexible)**.
  * Provides a 1-sentence transparent rationale grounded strictly in task deadline, category, and daily goal progress.
  * 1-tap "Apply Priorities to All Tasks" writes priorities directly to SQLite database.
* **Grounded Conversational Schedule Assistant ("Ask AI")**:
  * Interactive Apple iOS chat sheet allowing natural language queries: "What should I focus on next?", "Do I have any schedule conflicts?", "How close am I to my daily goal?".
  * Instant quick-action prompt chips for frictionless 1-tap questions.
* **Apple (iOS HIG) UI/UX Integration**:
  * Top Navigation Bar glowing sparkle action (`✦`) with Apple Intelligence gradient.
  * Home Screen "TaskFlow Intelligence" insight bento card with real-time pending task counter.
  * Authentic Apple modal bottom sheet (`28dp` radius) with 3 segmented tabs: *Schedule Plan*, *Priorities*, and *Ask AI*.
  * Apple-styled priority pill badges on task cards across Home and Calendar screens.
  * Dedicated "Apple Intelligence & LLM" section in Settings with API key management, live connection test diagnostics, and model selection.

---

## 🛠️ Technology Stack & Dependencies

* **Language**: Kotlin 2.2.10
* **UI Framework**: Jetpack Compose & Material 3
* **Database & Persistence**: Android Jetpack Room 2.8.4 (SQLite ORM) with Kotlin Symbol Processing (KSP)
* **Reactive Streams**: Kotlin Coroutines & Flow
* **Image Loading**: Coil 2.7.0
* **Authentication**: AndroidX Biometric library
* **Notifications & Timing**: Android AlarmManager & BroadcastReceivers
* **Build System**: Gradle with Kotlin DSL & Version Catalog (`libs.versions.toml`)

---

## 🚀 Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/PrerakPithadiya/android-compose-todo.git
   ```
2. Open the project in **Android Studio (Ladybug or newer)**.
3. Sync Gradle and run the application on a device running Android 7.0 (API 24) or higher.

---

## 🔬 Engineering Log & Case Studies

### [Case Study #2: Resolving OTP Autofill Failure, Premature Notification Dismissal, and Adding 30-Second Countdown Expiry](https://github.com/PrerakPithadiya/android-compose-todo/issues/2)

* **Date**: September 2026
* **Issue Reference**: [GitHub Issue #2](https://github.com/PrerakPithadiya/android-compose-todo/issues/2) (`Fixes #2`)
* **Area**: Jetpack Compose Authentication Flow, Haptic Feedback, State Propagation, Notification Banner Lifecycle

#### 1. Background & Symptoms
During phone registration or password reset, users receive a simulated Apple iOS dynamic push notification banner displaying an incoming 6-digit OTP code. Tapping the "Autofill" button caused the notification banner to abruptly disappear without populating the 6-digit destination input cells, leaving the user on an empty input screen with the verification code gone from view. Additionally, the notification banner lacked a visible countdown timer and did not automatically disappear after the required 30-second security window.

#### 2. Root Cause Analysis
1. **Unwired Autofill Callback**: In `AuthScreen.kt`, `IosNotificationBanner`'s `onAutofillClick` callback contained a placeholder comment (`// Banner autofill action`) with only `showOtpBanner = false`. The dispatched OTP code was never propagated to `OtpVerificationScreen.kt`.
2. **Premature Dismissal on Click**: In `IosNotificationBanner.kt`, clicking either the banner body or the "Autofill" chip explicitly invoked `onDismiss()`, immediately hiding the banner regardless of destination field status.
3. **Missing Live Timer & Static 60-Second Delay**: The banner previously rendered static text (`"Valid for 60 seconds"`) with a background `delay(60000)` without an active ticking counter or visual countdown badge.
4. **Mismatched OTP Expiry Window**: `AuthManager.OTP_VALIDITY_SECONDS` was hardcoded to 60 seconds instead of the standardized 30 seconds.

#### 3. Architectural Solution
* **Reactive Autofill Code Propagation**: Introduced `autofillCode` state in `AuthScreen.kt` forwarded directly to `OtpVerificationScreen.kt` (and `ForgotPasswordSheet.kt` via `LoginScreen.kt`). Inside `OtpVerificationScreen.kt`, a reactive `LaunchedEffect(autofillCode)` auto-populates `enteredOtp = autofillCode.take(6)`, resets error states, dismisses the on-screen keyboard, and enables the "Verify & Continue" button.
* **Persistent Banner on Autofill**: Removed `onDismiss()` from the Autofill chip and banner click handlers, ensuring the OTP banner remains visible and stable when tapped.
* **Apple HIG 30-Second Countdown Pill & Auto-Dismissal**: Added a live ticking `countdownSeconds` loop (from 30 down to 0) in `IosNotificationBanner.kt` paired with an Apple-styled header timer badge (transitioning from System Blue to System Orange when `< 10s`) and auto-dismisses upon reaching 0. Added an explicit close button for optional manual dismissal.
* **Synchronized 30-Second Security Policy**: Reduced `AuthManager.OTP_VALIDITY_SECONDS` to 30 seconds, synchronizing banner auto-dismissal, screen validity badges, and server-side verification timeouts. Added unit tests in `AuthManagerTest.kt` verifying the 30s expiry window.

---

### [Case Study #1: Resolving Startup Process Termination, Database Deadlock, and Session Loss on Restart](https://github.com/PrerakPithadiya/android-compose-todo/issues/1)

* **Date**: September 2026
* **Issue Reference**: [GitHub Issue #1](https://github.com/PrerakPithadiya/android-compose-todo/issues/1) (`Fixes #1`)
* **Area**: Android Lifecycle, Room SQLite Architecture, PackageManager `<activity-alias>`, Session Persistence

#### 1. Background & Symptoms
Following account registration or cold startup, the application experienced immediate silent termination. Upon reopening, the application repeatedly prompted the user to log in again despite user records existing in the local SQLite database. Concurrently, Android OS posted a system alert from `RescueParty`: *"There is an issue with the application and that the cache needs to be cleared"*.

#### 2. Root Cause Analysis
1. **Active Launcher `<activity-alias>` Process Kill in `onCreate()`**:
   `AppIconManager.initialize()` executed `syncComponentStates()` during `MainActivity.onCreate()`. In Android (API 24–35), invoking `PackageManager.setComponentEnabledSetting()` on an active launcher component forces Android's `ActivityManagerService` to immediately kill the process to rebuild package manifest entry points. Repeated startup kills triggered Android's automated crash-loop detection (`RescueParty`).
2. **Recursive Database Initialization Deadlock**:
   In `AppDatabase.Callback.onCreate(db)`, an unhandled coroutine invoked `AppDatabase.getInstance(context)` and queried `categoryDao.getCategoryCount()` while SQLite's schema creation transaction was still active, causing recursive connection acquisition and SQLite lock deadlocks.
3. **Asynchronous Session Write Loss (`apply()` vs. `commit()`)**:
   `AuthManager.registerAccount()` and `login()` persisted the active user ID using asynchronous `SharedPreferences.apply()`. When the process was terminated immediately after registration, the in-memory preference write was discarded before syncing to disk, while Room SQLite committed the `UserEntity` synchronously. On restart, `savedUserId` was `null`, leaving `isLoggedIn = false` and forcing the user back to the login screen.
4. **Cross-Thread Compose State Mutation**:
   `isAccountCreated` was mutated directly on `Dispatchers.IO` instead of `Dispatchers.Main`.

#### 3. Architectural Solution
* **Zero-Termination App Icon Manager**: Removed runtime component setting mutations on cold launch. Component states are now strictly modified upon explicit user selection in Settings, with early return guards and synchronous `.commit()` storage.
* **Direct SQLite Seed Transaction**: Replaced recursive Room DAO calls in `Callback.onCreate(db)` with synchronous, direct `SupportSQLiteDatabase.execSQL(...)` statements, eliminating recursive connection acquisition.
* **Infallible Session Recovery & Synchronous Commits**: Upgraded `KEY_ACTIVE_USER_ID` writes to `.commit()`, tracked `KEY_EXPLICIT_LOGOUT`, and added a fallback query (`userDao.getMostRecentUser()`) in `AuthManager.initialize()` that automatically restores the active session if accounts exist in SQLite and the user did not explicitly log out.
* **Thread-Safe State Dispatch**: Dispatched all mutable Compose authentication states strictly to `Dispatchers.Main`.
* **Automated Test Suite**: Added `AuthManagerTest.kt` verifying OTP lifecycle and authentication fallback logic.

