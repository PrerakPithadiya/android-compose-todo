# TaskFlow Design System Documentation & Audit

> Authoritative Design System Specification: Based on [TASKFLOW_DESIGN_SYSTEM.md](file:///f:/Todo-list-app/.agents/TASKFLOW_DESIGN_SYSTEM.md) and [extra_instructions.md](file:///f:/Todo-list-app/extra_instructions.md).
> Target: iOS HIG aesthetic built natively with Kotlin & Jetpack Compose for Android 7.0+ (API 24 to 35).

---

## 1. Token Inventory

### 1.1 Semantic Neutral Tokens
| Token | Light Value | Dark Value | Usage / Notes |
| :--- | :--- | :--- | :--- |
| `canvas` | `#F2F2F7` | `#000000` | Inset grouped screen canvas background |
| `card` | `#FFFFFF` | `#1C1C1E` | Inset group surface (elevation 1) |
| `cardRaised` | `#FFFFFF` | `#2C2C2E` | Sheets, popovers, elevated surfaces (elevation 2) |
| `cardSecondary` | `#F9F9FB` | `#2C2C2E` | Nested secondary fill inside cards |
| `fillControl` | `#767680` @ 12% | `#767680` @ 24% | Search field, segmented track fill |
| `fillSelected` | `#FFFFFF` | `#636366` | Segmented thumb, keypad key pressed fill |
| `labelPrimary` | `#000000` (100%) | `#FFFFFF` (100%) | Primary text, titles, headings |
| `labelSecondary` | `#3C3C43` @ **72%** | `#EBEBF5` @ 60% | Secondary metadata, subtitles, placeholders (Light adjusted to 72% for >=4.5:1 AA contrast) |
| `labelTertiary` | `#3C3C43` @ 30% | `#EBEBF5` @ 30% | Decorative/disabled only. Never for readable text |
| `separator` | `#3C3C43` @ 29% | `#545458` @ 65% | 1 physical pixel hairline dividers |
| `controlStroke` | `#8E8E93` | `#8E8E93` | Unchecked checkbox ring, switch off track (>=3:1) |
| `cardStroke` | `Transparent` | `#545458` @ 55% | **Dark only**: 1px physical border on cards to eliminate edge blending on OLED/LCD |
| `pressedOverlay` | `#000000` @ 6% | `#FFFFFF` @ 8% | Row touch overlay (no ripple) |
| `scrim` | `#000000` @ 32% | `#000000` @ 56% | Scrim behind modal bottom sheets and dialogs |

### 1.2 System Semantic Colors
| Role | Light Value | Dark Value | Purpose |
| :--- | :--- | :--- | :--- |
| `red` | `#FF3B30` | `#FF453A` | High Priority (P1 / Urgent), Destructive, Overdue |
| `orange` | `#FF9500` | `#FF9F0A` | Medium Priority (P2 / Warning), Time conflicts |
| `blue` | `#007AFF` | `#0A84FF` | Low Priority (P3 / Flexible), Primary accent base |
| `green` | `#34C759` | `#30D158` | Positive reinforcement, Completion badges |

### 1.3 Accent Role Solver (8 Curated Accents)
Each user-selected brand color is dynamically resolved at runtime via `AccentRoles` into:
- `accent`: Icons, ring strokes, selected tab tint, focus ring (>= 3:1 vs surface).
- `accentFill`: Background of filled buttons and center docked Plus button (label >= 4.5:1).
- `onAccent`: Text/icon on `accentFill` (White or Black depending on 4.5:1 contrast).
- `accentText`: Text or links smaller than 18sp bold (>= 4.5:1 vs `card` AND `canvas`).
- `accentContainer`: Tinted backgrounds for selected chips and badges.

8 Brand Accents supported:
1. `BLUE` (`#007AFF` / `#0A84FF`)
2. `PURPLE` (`#AF52DE` / `#BF5AF2`)
3. `PINK` (`#FF2D55` / `#FF375F`)
4. `ORANGE` (`#FF9500` / `#FF9F0A`)
5. `GREEN` (`#34C759` / `#30D158`)
6. `YELLOW` (`#FFCC00` / `#FFD60A`)
7. `INDIGO` (`#5856D6` / `#5E5CE6`)
8. `TEAL` (`#30B0C7` / `#40C8E0`)

### 1.4 Typography Tokens (Inter font family, Tabular Numerals, 200% Font Scale safe)
- `largeTitle`: `34sp` / `41sp`, Bold
- `title1`: `28sp` / `34sp`, Bold
- `title2`: `22sp` / `28sp`, SemiBold
- `title3`: `20sp` / `25sp`, SemiBold
- `headline`: `17sp` / `22sp`, SemiBold
- `body`: `17sp` / `22sp`, Regular
- `callout`: `16sp` / `21sp`, Regular
- `subheadline`: `15sp` / `20sp`, Regular
- `footnote`: `13sp` / `18sp`, Regular
- `caption`: `12sp` / `16sp`, Regular (min 11sp)

### 1.5 Spacing & Shape Tokens
- **Spacing Scale**: `xs = 4.dp`, `sm = 8.dp`, `md = 12.dp`, `lg = 16.dp`, `xl = 24.dp`, `xxl = 32.dp`
- **Hairline**: Exactly 1 physical pixel: `(1f / LocalDensity.current.density).dp`
- **Shapes**:
  - Inset Grouped Card: `12.dp`
  - Search Field: `10.dp`
  - Pill Chip: `20.dp` (Height `40.dp`)
  - Segmented Track / Thumb: `9.dp` / `7.dp`
  - Filled Button: `14.dp` (Height `50.dp`)
  - Bottom Sheet Top Corners: `28.dp`
  - Alert Dialog: `14.dp`

### 1.6 Spring Physics Tokens (`TFMotion`)
- `press`: damping `0.90`, stiffness `1500` (~0.16s response)
- `snappy`: damping `0.85`, stiffness `1290` (~0.35s response)
- `standard`: damping `0.825`, stiffness `630` (~0.50s response)
- `sheet`: damping `1.0` (never < 0.95), stiffness `400` (~0.63s response, zero overshoot)
- `bouncy`: damping `0.60`, stiffness `700` (~0.40s response)
- `LocalReducedMotion`: Disables spring overshoot and particle effects when system animations are turned off.

### 1.7 Haptics Mapping (`TFHaptics`)
- `Selection`: Segmented change, picker ticks, drag snap, chip select
- `Light`: Button press, tab change, sheet detent snap
- `Medium`: Task completion, toggle, long-press lift
- `Strong`: Destructive confirmation, PIN error shake, medal unlock

---

## 2. Component Inventory

| Component | Status in Codebase | Target Spec Compliance |
| :--- | :--- | :--- |
| `TFCardGroup` | Incomplete (uses basic `Surface`) | Inset grouped container, 12dp radius, 1px dark `cardStroke`, 56dp indented hairline |
| `TFTaskRow` | Needs refactor | 56dp min height, 48dp checkbox touch target, circular check, strikethrough, priority glyphs (`!`, `!!`, `!!!`), 900ms completion sequence |
| `TFButton` | Missing (uses M3 Button) | iOS style (Filled 50dp, Tinted, Plain, Destructive) with 0.98 press scale and no ripple |
| `TFSegmentedControl` | Basic implementation | 32dp tall, 9dp track, 7dp sliding thumb with `snappy` spring and `Selection` haptic |
| `TFSwitch` | Stock M3 Switch | 51x31dp iOS switch with `accentFill` track and smooth thumb spring |
| `TFChip` | Standard AssistChip | 40dp height, 20dp radius, priority glyphs, leading checkmark when selected |
| `TFSearchField` | Custom in HeaderBar | 36dp visual (48dp touch), 10dp radius, `fillControl`, sliding Cancel button on focus |
| `TFTabBar` | 4-tab bar + FAB | **5-slot tab bar (56dp + insets)** with **center-docked 52dp Plus button rising 8dp**; FAB removed |
| `TFLargeTitleNavBar` | Basic header | Collapsible 34sp `largeTitle` smoothly interpolating into 17sp centered title on scroll |
| `TFEmptyState` | Plain text | 96dp vector art, `title2` headline, `subheadline` body, filled CTA, up to 3 starter chips |
| `TFSkeleton` | Missing | Geometry-matched shimmer blocks with 1.4s sweep |
| `CreateTaskBottomSheet` | Monolithic scroll sheet | **2-Stage Progressive Disclosure**: Stage 1 Quick Capture (auto-height, NLP chips, toolbar) -> Stage 2 Inset Grouped Details |
| `IosKeypad` / `PinDotsView` | Functional | Restyle to 76dp circle keys, 400ms error shake + `Strong` haptic, rename "Face ID" to "Face Unlock" |

---

## 3. Ranked Violations of Design Spec & Remediation Strategy

| Rank | Severity | Violation | Affected Files | Remediation Plan |
| :---: | :---: | :--- | :--- | :--- |
| **1** | **Critical** | Floating FAB overlaps list content and clashes with bottom navigation bar | `HomeScreen.kt` | Remove floating FAB. Build `TFTabBar` with center-docked 52dp Plus button rising 8dp above the bar in slot 3. |
| **2** | **Critical** | Dark mode card edges blend into OLED black background (~1.2:1 contrast ratio) | `HomeScreen.kt`, `SettingsScreen.kt`, `Color.kt` | Introduce `cardStroke` (`#545458` @ 55%) 1px physical hairline border on all cards in dark theme. |
| **3** | **Critical** | Task completion instantly jumps to bottom without satisfaction or undo window | `HomeScreen.kt`, `CalendarScreen.kt` | Implement 900ms completion sequence: check draw -> strikethrough -> micro-confetti -> +50 XP capsule -> 900ms move with undo affordance. |
| **4** | **High** | Create/Edit Task Sheet causes excessive scrolling and gesture conflict | `CreateTaskBottomSheet.kt`, `EditTaskBottomSheet.kt` | Re-architect into 2-Stage Progressive Disclosure sheet (Stage 1 Quick Capture + Stage 2 Details). |
| **5** | **High** | Secondary text in light mode uses 60% alpha (`#3C3C43` @ 60% = ~3.4:1), failing WCAG AA (4.5:1) | `Color.kt`, all screens | Raise light theme `labelSecondary` to 72% alpha (~4.7:1 contrast). |
| **6** | **High** | Stock tween/linear animations instead of Apple spring physics | All screens & sheets | Implement `TFMotion` springs (`press`, `snappy`, `standard`, `sheet`, `bouncy`) across all animations. |
| **7** | **Medium** | Empty states lack engaging copy, artwork, and starter chips | `HomeScreen.kt`, `ListsScreen.kt`, `CalendarScreen.kt` | Replace plain empty messages with `TFEmptyState` component featuring 96dp art, CTA, and starter chips. |
| **8** | **Medium** | Medium priority uses Yellow, which fails contrast on white, and conveys meaning by color only | `Color.kt`, Task rows | Change Medium priority to System Orange (`#FF9500`) and add `!`, `!!`, `!!!` glyph markers. |
| **9** | **Medium** | Proprietary naming ("Face ID", "Memoji") on Android | `IosFaceIdIcon.kt`, `ProfileScreen.kt`, `SettingsScreen.kt` | Rename to Android-compliant terms: "Face Unlock" and "Avatar". |
| **10** | **Low** | Hairlines rendered with variable density approximations (blurry on some screens) | Theme, dividers | Implement physical 1-pixel hairline helper `(1f / LocalDensity.current.density).dp`. |
