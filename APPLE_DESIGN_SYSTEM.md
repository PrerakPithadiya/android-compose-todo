# Apple (iOS HIG) Design System Specification

## Overview
This document records the exact design specification for the **TaskFlow Android Application** to provide users with an authentic, premium **Apple (iOS HIG) user experience**.

---

## 1. Color Palette & Tokens

| Token Name | Hex / Value | Purpose / Usage |
| :--- | :--- | :--- |
| `SystemBlue` | `#007AFF` | Primary iOS Accent Color (Buttons, Active Tabs, Highlights) |
| `SystemBlueLight` | `#E5F1FF` | Soft Blue Tint for Selected Chips / Secondary Badges |
| `SystemBlueDark` | `#0051A8` | Pressed Accent & High-Contrast Typography |
| `SystemGroupedBackground` | `#F2F2F7` | iOS Inset Grouped Page Canvas Background |
| `SystemSurface` | `#FFFFFF` | iOS Card / Grouped Section Surface Fill |
| `SystemSurfaceSecondary` | `#F9F9FB` | Secondary Card Container Fill |
| `SystemLabelPrimary` | `#000000` | Primary Headings & Task Titles (100% Opacity) |
| `SystemLabelSecondary` | `#3C3C43` (60% alpha) | Secondary Subtitles, Dates, & Metadata |
| `SystemLabelTertiary` | `#3C3C43` (30% alpha) | Dimmed / Strikethrough Text for Completed Tasks |
| `SystemGray` | `#8E8E93` | Inactive Tab Icons, Default Icons |
| `SystemGray2` | `#AEAEB2` | Outlines & Unselected Circular Checkbox Strokes |
| `SystemGray5` | `#E5E5EA` | Secondary Avatar / Input Container Fill |
| `SearchInputBackground` | `#767680` (12% alpha) | iOS Integrated Search Bar Background Fill |
| `SystemDivider` | `#3C3C43` (29% alpha) | Thin Inset Grouped Dividers (`0.5.dp` thickness) |
| `SystemRed` | `#FF3B30` | Destructive Actions / Error Alerts |
| `SystemGreen` | `#34C759` | Completion Badges & Positive Reinforcement |

---

## 2. Typography Hierarchy (SF / Inter Font Family)

- **Large Title**: `34sp`, Bold (`FontWeight.Bold`), Letter Spacing `-0.5sp` (e.g., Main Screen Header "Tasks")
- **Headline / Screen Title**: `24sp` - `28sp`, Bold (`FontWeight.Bold`), Letter Spacing `-0.25sp`
- **Title 2 / Section Header**: `20sp`, Bold (`FontWeight.Bold`)
- **Card Title / Task Title**: `17sp`, Medium (`FontWeight.Medium`), Line Height `22sp`
- **Body / Date Subtitle**: `15sp`, Regular (`FontWeight.Normal`)
- **Subhead / Tag Label**: `13sp`, Medium (`FontWeight.Medium`)
- **Caption / Tab Label**: `10sp` - `11sp`, Medium (`FontWeight.Medium`)

---

## 3. Component Design & Layout Guidelines

### 3.1 Header Navigation Bar
- **Style**: Sticky top bar with `#F2F2F7` background or translucent backdrop blur.
- **Large Title**: "Tasks" (34sp Bold text).
- **Actions**: `#007AFF` circular `+` button and user profile avatar (`40dp` circular clip).
- **Integrated Search Field**: Inset search bar with `10dp` corner radius, `#767680` 12% fill, left magnifying glass icon, placeholder "Search", and clear `X` button when active.

### 3.2 Inset Grouped Task Cards & Containers
- **Container Radius**: `12dp` rounded corners (`RoundedCornerShape(12.dp)`).
- **Surface Fill**: Solid `#FFFFFF` card background with subtle `0.5.dp` outline or low-elevation shadow (`1.dp`).
- **Circular Checkboxes**: `24dp` circular toggle. Uncompleted state: `#AEAEB2` border ring; Completed state: `#007AFF` solid circle fill with white checkmark (`Material Symbols / Outlined Check`).
- **Inset Dividers**: `0.5.dp` horizontal divider starting after the checkbox (`left = 56.dp`) to separate items cleanly within a grouped card.
- **Strikethrough Animation**: Completed tasks transition to `30%` opacity with strike-through text.

### 3.3 Progress Banner Card
- **Style**: Inset rounded card (`12.dp` radius) displaying "$N$ tasks left for today", current date subtitle, circular progress arc (`#007AFF` stroke), and percentage badge.

### 3.4 iOS Translucent Tab Bar
- **Style**: Fixed bottom bar with translucent background (`SystemSurface`), `0.5.dp` top divider (`SystemDivider`), height `72dp`.
- **Tab Items**: 4 tabs (`Home`, `Calendar`, `Lists`, `Settings`).
- **Active State**: Active icon & text tinted with `#007AFF` System Blue.
- **Inactive State**: Inactive icon & text tinted with `#8E8E93` System Gray.

### 3.5 Modal Bottom Sheets
- **Top Corner Radius**: `28dp` top-start and top-end corners.
- **Header**: Sheet title in `24sp` Bold, subtitle in `13sp`, close `X` button.
- **Inputs**: Outlined input fields with `12dp` - `16dp` rounded corners, `#007AFF` focused stroke.
- **Segmented / Filter Chips**: Rounded `20dp` pill chips with `#007AFF` active fill and `#FFFFFF` text.

---

## 4. Architectural Integration & Commit Mandate
All future Android Compose screens, components, themes, and views must consume these Apple HIG tokens and guidelines to preserve a seamless iOS experience across the entire app.
