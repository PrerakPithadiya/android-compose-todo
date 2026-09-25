# TaskFlow - iOS-Inspired Android Task Manager

TaskFlow is a modern, high-performance task management application for Android, carefully crafted to deliver an authentic **Apple iOS (Human Interface Guidelines)** look and feel. Built with **Kotlin** and **Jetpack Compose**, it offers extensive features, smooth animations, and premium security controls.

---

## 📱 Key Features

### 1. 🔐 Account Auto-Detection, Phone OTP Registration & Multi-Account Switcher
* **Automatic Account Detection**: Automatically detects whether an account exists on launch; dynamically routes first-time users to the **Phone Registration** workflow, logged-out users to the **Login Screen**, and authenticated users directly to their workspace.
* **Multi-Account Profile Switcher**: Apple-styled multi-account selector allowing users to easily browse saved accounts on the device, view avatars and handles, unlock via password or 1-tap **Face ID / Biometrics**, and securely manage or remove inactive local profiles.
* **6-Digit Phone OTP Verification & 30s Countdown**: Clean phone number input with international country code selection, automated 6-digit verification code dispatch, and an Apple-styled **Interactive Push Notification Banner** featuring a live 30-second countdown timer pill, automatic 30s code expiration/dismissal, and 1-tap autofill directly into the 6-digit destination input cells with automatic keyboard management.
* **Account Setup & Password Security**: Streamlined profile initialization (Full Name, Username `@handle`, and 4-rule strict password validation checklist: 8+ chars, 1+ number, uppercase/symbol, match confirmation) with a 4-segment live strength meter and SHA-256 random salting.
* **Login & Recovery**: Username/Phone and Password authentication with visibility toggles, biometric instant login shortcut, "Forgot Password" SMS OTP recovery flow, and intuitive Log Out controls in Settings & Profile.


### 2. 🎨 TaskFlow Apple (iOS HIG) Design System & Primitives
* **Semantic Design Tokens**: Standardized token architecture across colors (`TFColor.kt`), typography (`TFType.kt`), corner squircles (`TFShape.kt`), 4dp-grid layout dimensions (`TFSpacing.kt`), motion springs (`TFMotion.kt`), and tactile haptics (`TFHaptics.kt`).
* **Accent Role Runtime Contrast Solver (`AccentRoles.kt`)**: Dynamically derives 5 accessible color roles per accent (Primary, OnPrimary, Container, OnContainer, Subtle) ensuring strict WCAG 2.1 AA/AAA compliance across all 8 accent palettes.
* **Physics-Based Real Spring Animations (`TFMotion.kt`)**: Real iOS spring physics curves (`press`, `snappy`, `standard`, `sheet`, `bouncy`) replacing linear easing curves across interactions and sheet transitions.
* **Primitive Component Suite (`ui/components/primitives/`)**:
  * **Inset Grouped Containers (`TFCardGroup`)**: `12dp` rounded grouped cards with `1 physical pixel` dividers indented at `56dp`.
  * **Interactive Task Rows (`TFTaskRow`)**: Interactive task items with haptic feedback, spring scale transitions, priority badges, category chips, and strike-through animations.
  * **Custom UI Primitives**: `TFButton` (Primary, Secondary, Plain, Destructive), `TFSegmentedControl` (sliding thumb), `TFChip`, `TFSwitch`, `TFSkeleton` shimmer placeholders, and `TFEmptyState` illustrations.
* **Unified Navigation Chrome (`ui/components/navigation/`)**:
  * **Large Title Navigation Bar (`TFLargeTitleNavBar`)**: Collapsing Large Title header with blur effect.
  * **5-Slot Docked Tab Bar (`TFTabBar`)**: 56dp height + gesture insets with center-docked 52dp Plus button rising 8dp above the bar without floating FAB clutter.
* **Progressive-Disclosure Create & Edit Sheets**: 2-stage progressive disclosure where Stage 1 is Quick Capture (auto-height, immediate keyboard focus, live on-device NLP schedule token extraction with removable chips, docked toolbar) and Stage 2 expands on drag-up to a full Inset Grouped form with inline calendar and clock accordions.
* **900ms Task Completion Sequence**: Choreographed animation sequence featuring 180ms check draw + ring fill, title strikethrough ease, radial micro-confetti burst, `+50 XP` floating capsule, and placement animation to completed group with instant cancel/undo affordance.
* **Dark Mode 1px Hairline Card Separation**: 1px physical hairline `cardStroke` (`#545458` @ 55%) border over `#1C1C1E` dark cards against pure black `#000000` canvas to prevent card blending; flat white on `#F2F2F7` without borders or shadows in light mode.


### 3. 📅 Interactive Calendar Screen
* **Multi-View Interface**: Toggle seamlessly between **Month**, **Week**, and **Agenda** modes.
* **Smart Sorting**: Automatically groups and ranks tasks chronologically by time (AM/PM parsed values) with completed tasks grouped cleanly at the bottom.
* **Filters**: Quick filtering options by category list.

### 4. 🔒 Passcode, Pattern & Multi-Tier Biometric Security (Face ID & Fingerprint)
* **Direct Front-Camera Face Lock (Google ML Kit + CameraX)**: Implements on-device face detection powered by CameraX and Google ML Kit Fast Face Detection. When Face Lock is enabled, the front camera initiates automatically upon opening or resuming the application, performing real-time facial recognition inside an authentic Apple Face ID rounded squircle viewfinder with animated scanning brackets and laser beam.
* **3-Tier Authentication Priority Hierarchy**:
  1. **Priority 1 - Face Lock**: Automatically triggers front-camera face detection immediately upon app launch; upon face verification, triggers haptic success feedback and smoothly unlocks.
  2. **Priority 2 - Fingerprint**: Graceful biometric fallback via Android OS `BiometricPrompt` if face detection is not recognized within a 6-second timeout, if the user declines, or if the user taps "Use Fingerprint".
  3. **Priority 3 - Manual PIN / Passcode**: Safe fallback to 4-Digit/6-Digit PIN keypad, Pattern grid, or Alphanumeric Password with persistent "Scan Face ID" and "Fingerprint" shortcut pills for instant re-authentication.
* **Hardware Modality Detection**: Automatically inspects device hardware (`PackageManager.FEATURE_CAMERA_FRONT`, `PackageManager.FEATURE_FINGERPRINT`) and `BiometricManager` capability to dynamically adapt the security experience.
* **Apple Face ID Integration**: Features an authentic Apple iOS Face ID vector icon (`IosFaceIdIcon`), adaptive keypad buttons, contextual subtitles ("Position your face directly in the frame"), and dynamic status badges conforming to Apple Human Interface Guidelines.
* **App Lock Options**: Configure Passcode (4-digit & 6-digit using custom iOS-style numeric keypads), Pattern Lock (via custom drawn pattern canvases), Alphanumeric Password, Face Lock, or Fingerprint unlock.
* **Smart Background Locking**: Monitors lifecycle changes using `FragmentActivity` to automatically enforce authentication whenever the app is backgrounded or resumed.

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
* **Natural Language Schedule Parsing & Smart Task Input (`TaskScheduleParser`)**:
  * On-the-fly offline natural language parser detecting dates (*"today"*, *"tomorrow"*, *"next monday"*), times (*"5pm"*, *"9:30 am"*), priority keywords (*"urgent"*, *"high priority"*), and tags (*"#work"*).
  * Live Apple Intelligence suggestion badge pill in `CreateTaskBottomSheet.kt` and `EditTaskBottomSheet.kt` with 1-tap auto-population into date/time pickers and priority chips.
* **Apple (iOS HIG) UI/UX Integration**:
  * Top Navigation Bar glowing sparkle action (`✦`) with Apple Intelligence gradient.
  * Home Screen "TaskFlow Intelligence" insight bento card with real-time pending task counter.
  * Authentic Apple modal bottom sheet (`28dp` radius) with 2 streamlined segmented tabs: **Ask AI** (direct grounded conversational assistant with natural language task creation) and **Priorities** (Eisenhower matrix classification with grounded rationale). Schedule Plan tab removed to maximize sheet responsiveness and direct chatbot access.
  * Modular `CreateTaskBottomSheet` and `EditTaskBottomSheet` components with inline priority chips, time pickers, and category selectors.
  * Dedicated "Apple Intelligence & LLM" section in Settings with API key management, live connection test diagnostics, model selection, and floating assistant customization.

### 🔮 12. Floating AI Assistant & Quick-Access Shortcut (Edge-Docked System Overlay)
* **System-Wide Floating Shortcut Button (`SYSTEM_ALERT_WINDOW`)**:
  * Persistent background service (`FloatingAiOverlayService`) rendering a customizable Apple HIG gradient bubble with a glowing translucent border, elevation drop shadow, and crisp vector icon.
  * Floats seamlessly over any running app (such as WhatsApp, Chrome, YouTube, or Home Screen), allowing users to immediately trigger the TaskFlow AI assistant anytime from anywhere on their phone.
* **Physical Spring-Back Border Snapping & Zero Center Resting**:
  * Draggable across 2D touch space with touch slop disambiguation and tactile haptic feedback.
  * Mathematical edge-docking solver (`FloatingAiButtonManager.calculateDockedTargetX`): when released, if the button center is left of the screen midpoint, it animates to the left border (`x = 0`); if right of the midpoint, it animates to the right border (`x = screenWidth - buttonWidth`).
  * **Zero Center Resting**: The button strictly never remains in the center of the display and always snaps back to the phone's border with Apple-styled spring overshoot physics.
* **Strict Navigation Bar Clearance (No Visual Overlaps)**:
  * Strict mathematical boundary clamping (`FloatingAiButtonManager.clampYPosition`) restricts the floating button to move exclusively within the clear space between the top navigation bar and bottom navigation bar.
  * **Top Navigation Boundary (160dp)**: Keeps the button strictly below the status bar, Large Title navigation bar, and search query field.
  * **Bottom Navigation Boundary (115dp)**: Keeps the button strictly above the 56dp 5-slot tab bar, the 8dp elevated center-docked Plus button, and gesture navigation insets.
  * Guaranteed across both system overlay mode (`FloatingAiOverlayService`) and in-app Compose mode (`InAppFloatingAiButton`).
* **Full Appearance Customization (8 Apple Color Themes & 6 AI Glyphs)**:
  * **8 Curated Apple (iOS HIG) Color Palettes**:
    * **Royal Purple** (`#7C3AED` -> `#A855F7`)
    * **System Blue** (`#007AFF` -> `#0A84FF`)
    * **Sunset Coral** (`#FF5E3A` -> `#FF9500`)
    * **Emerald Mint** (`#34C759` -> `#30D158`)
    * **Neon Cyan** (`#06B6D4` -> `#3B82F6`)
    * **Dark Minimal / Obsidian** (`#374151` -> `#1F2937`)
    * **Champagne Gold** (`#F59E0B` -> `#D97706`)
    * **System Rose** (`#EC4899` -> `#F43F5E`)
  * **6 Feature-Relevant AI Assistant Glyphs**:
    * **AI Sparkle** (`AutoAwesome` / `ic_floating_ai_sparkle`): Apple Intelligence / Generative AI stars.
    * **Assistant Chat** (`Chat` / `ic_floating_ai_chat`): Direct conversational chatbot messaging.
    * **Smart Agent** (`SmartToy` / `ic_floating_ai_bot`): Dedicated on-device AI bot assistant.
    * **Quick Capture** (`Bolt` / `ic_floating_ai_bolt`): Instant natural language task logging.
    * **Smart Tasks** (`CheckCircle` / `ic_floating_ai_check`): TaskFlow schedule manager & checklist.
    * **Intelligence** (`Psychology` / `ic_floating_ai_brain`): Deep cognitive schedule & priority logic.
  * **Interactive Customization Sheet (`FloatingAiCustomizeSheet`)**:
    * Dedicated Apple HIG modal sheet with a live interactive button preview card, 8 gradient swatches with haptic selection rings, and 6 glyph cards with descriptions.
    * Dedicated access point under **TaskFlow Intelligence** (beside the Floating AI toggle) via the "Button Appearance" setting row.
    * Dynamic real-time styling broadcast via `ACTION_UPDATE_STYLE` to instantaneously update the active overlay service and in-app Compose button without restarting the app.
* **Direct Chatbot Shortcut & Intent Routing**:
  * Tapping the floating button immediately brings TaskFlow to the foreground with `EXTRA_OPEN_AI_CHAT`, automatically popping open `TaskFlowIntelligenceSheet` pre-selected to the **Ask AI** chatbot tab.
* **Conversational Natural Language Task Creation**:
  * Users can message the integrated AI using everyday natural language to instantly add tasks (e.g. *"Add task: Team standup tomorrow at 9am #work"*, *"Remind me to buy groceries tonight 6pm"*, *"New task: Finish quarterly report on Friday high priority"*).
  * `TaskScheduleParser` detects creation intents, strips command prefixes, and extracts titles, dates, 12-hour AM/PM times, category tags, and Eisenhower priority markers.
  * Directly inserts the task into the Room SQLite database, schedules exact alarm reminders with `TaskNotificationScheduler`, awards `+50 XP`, triggers success haptics, and posts an Apple-styled markdown confirmation card in the chat stream.
* **In-App Compose Fallback**:
  * An in-app draggable Compose edge button (`InAppFloatingAiButton`) rendered with `TFMotion` springs when the feature is enabled but system overlay permissions have not yet been granted.
* **Settings Toggle & Onboarding Dialog**:
  * Dedicated "Floating AI Assistant" switch under the Apple Intelligence card in Settings.
  * Integrated Apple HIG dialog with 1-tap deep link to Android's `ACTION_MANAGE_OVERLAY_PERMISSION` screen and graceful in-app fallback.

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

