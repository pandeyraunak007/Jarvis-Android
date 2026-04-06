# Voxn AI — Technical Documentation
### V.O.X.N. — Your Intelligent Life Assistant
**Version 1.0 | Last Updated: April 2026**

---

## 1. Overview

Voxn AI is a cross-platform personal life management app available on **iOS** and **Android**. It consolidates health tracking, habit formation, expense management, task management, and calendar integration into a single dark-themed HUD interface.

| | iOS | Android |
|---|---|---|
| **Language** | Swift 5 / SwiftUI | Kotlin / Jetpack Compose |
| **Min OS** | iOS 17.0+ | Android 9 (API 28)+ |
| **Architecture** | MVVM + Singleton Managers | MVVM + Manager Layer |
| **Database** | CoreData (programmatic) | Room (KSP) |
| **Health** | HealthKit | Health Connect |
| **Calendar** | EventKit | — (planned) |
| **Notifications** | UNUserNotificationCenter | AlarmManager + BroadcastReceiver |
| **Widgets** | WidgetKit (3 widgets) | — (planned) |
| **Bundle ID** | `Raunak.Jarvis` | `com.jarvis.app` |

---

## 2. Architecture

```
┌─────────────────────────────────────────────────┐
│                   UI Layer                       │
│  Dashboard │ Health │ Habits │ Spending │ Notes  │
│  (SwiftUI Views / Jetpack Compose Screens)       │
├─────────────────────────────────────────────────┤
│                ViewModel Layer                   │
│  DashboardVM │ HealthVM │ HabitVM │ SpendVM │    │
│  NoteVM — State management, business logic       │
├─────────────────────────────────────────────────┤
│                Manager Layer                     │
│  HealthKit/HealthConnect │ HabitManager │         │
│  ExpenseParser │ NoteManager │ CalendarManager   │
│  NotificationManager │ WidgetDataManager         │
├─────────────────────────────────────────────────┤
│                Data Layer                        │
│  CoreData / Room Database                        │
│  UserDefaults / SharedPreferences                │
│  App Groups (iOS widget data sharing)            │
└─────────────────────────────────────────────────┘
```

### Data Flow
- **iOS**: CoreData `@FetchRequest` → `@Published` properties → SwiftUI views auto-update
- **Android**: Room DAO `Flow<List<T>>` → `StateFlow` in ViewModel → `collectAsStateWithLifecycle()` in Compose

---

## 3. Database Schema

### 3.1 Expense / ExpenseEntity

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID / String | Primary key |
| `amount` | Double | Transaction amount (INR) |
| `merchant` | String | Merchant/payee name |
| `categoryRaw` | String | Enum: Grocery, Food, Entertainment, Investment, Travel, Bills, Others |
| `paymentMethodRaw` | String | Enum: Credit Card, Debit Card, UPI, Cash, Wallet, Other |
| `date` | Date / Long | Transaction timestamp |
| `note` | String? | Optional user note |
| `typeRaw` | String | "Expense" or "Income" (iOS only) |

### 3.2 Habit / HabitEntity

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID / String | Primary key |
| `name` | String | Habit name |
| `createdAt` | Date / Long | Creation timestamp |
| `reminderEnabled` / `hasReminder` | Bool | Whether daily reminder is set |
| `reminderHour` | Int | 0-23 |
| `reminderMinute` | Int | 0-59 |
| `frequencyRaw` | String | Daily, Weekly, Multiple/Day (iOS only) |
| `targetCount` | Int | For multi-per-day habits (iOS only) |
| `weeklyDaysRaw` | String | Comma-separated day numbers (iOS only) |

### 3.3 HabitCompletion / HabitCompletionEntity

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID / String | Primary key |
| `habitId` | String | FK → Habit (cascade delete) |
| `date` | Date / Long | Completion timestamp |

### 3.4 NoteItem / NoteEntity

| Field | Type | Description |
|-------|------|-------------|
| `id` | UUID / String | Primary key |
| `title` | String | Note title |
| `body` | String | Note content |
| `categoryRaw` | String | Personal / Office |
| `priorityRaw` | String | High / Medium / Low |
| `dueDate` | Date? / Long? | Optional due date |
| `reminderDate` | Date? / Long? | Optional reminder timestamp |
| `isPinned` | Bool | Pin state |
| `isCompleted` | Bool | Completion state |
| `createdAt` | Date / Long | Creation timestamp |

### 3.5 Non-persisted: HealthData

| Field | Type | Description |
|-------|------|-------------|
| `steps` | Double | Today's steps (goal: 10,000) |
| `caloriesBurned` | Double | Active calories (goal: 500 kcal) |
| `sleepHours` / `sleepMinutes` | Double | Last night's sleep (goal: 8h) |
| `workoutMinutes` | Double | Exercise time (goal: 60 min) |
| Weekly averages | Double | 7-day rolling averages for all metrics |

---

## 4. Module Details

### 4.1 Dashboard (HUD)

**Purpose**: Unified overview of all life systems.

**Sections**:
| Section | iOS | Android |
|---------|-----|---------|
| Greeting + Date | Time-of-day greeting with user name | Time-of-day greeting |
| Daily Brief | AI-generated multi-line summary (tasks, spend, health, habits, calendar) | — |
| Calendar | Today's events from EventKit with detail view | Week strip + due today/overdue notes |
| Health Gauges | 4 arc gauges (steps, kcal, sleep, workout) | 4 arc gauges |
| Habit Progress | Progress ring + streak | Progress ring + streak |
| Spending Summary | Today / Week / Month + category bars | Today / Week / Month + category bars |
| Notes Summary | Active / Overdue / Upcoming counts | Active / Overdue / Upcoming counts |
| Smart Alerts | Budget warnings, unusual spend, inactivity | — |
| Action Suggestions | Context-aware CTAs | — |

### 4.2 Health

**Data Sources**: HealthKit (iOS) / Health Connect (Android)

| Metric | Goal | Display |
|--------|------|---------|
| Steps | 10,000 | Arc gauge + bar chart |
| Calories | 500 kcal | Arc gauge + bar chart |
| Sleep | 8 hours | Arc gauge + bar chart |
| Workout | 60 min | Arc gauge + bar chart |

**Features**:
- Today's metrics with animated gauges
- 7-day weekly trend bar chart
- Stats card: Average, Best, Lowest, Today (iOS) / Detailed readouts (Android)
- Permission handling with fallback UI

### 4.3 Habits

**Frequency Types** (iOS has all three, Android has daily only):

| Type | Description |
|------|-------------|
| Daily | Once per day, every day |
| Weekly | Select specific days (S M T W T F S) |
| Multiple/Day | Target count (2-20x), e.g., "Drink water 8x" |

**Features**:
- Checkbox (daily) / progress ring (multi-count) toggle
- Streak tracking with fire badge
- Monthly completion calendar grid
- Reminders via local notifications (daily recurring)
- Material3 TimePicker (Android) / native time picker (iOS)

### 4.4 Spending

**Transaction Flow**:
1. Tap FAB → Add Expense/Income sheet
2. Enter amount, category, notes, date
3. Transaction saved to DB
4. Aggregations update in real-time

**Time Range Filtering**:
- Chips: This Month, Last Month, 3 Months, 6 Months, Custom
- Custom range: graphical date picker (FROM / TO)

**Statement View**:
- Total spend + transaction count for selected period
- Today / This Week quick stats (always current)
- Transactions grouped by date (TODAY, YESTERDAY, 05 APR 2026)
- Daily totals per group

**Analytics**:
- 14-day daily spend bar chart
- Week-over-week comparison (%)
- Average daily spend
- Category pie chart + bar breakdown
- Recurring bills management

**Notification Parser** (iOS):
- Paste bank SMS → auto-detect amount, merchant, payment method
- 70+ merchant auto-categorization

### 4.5 Notes & Reminders

**Features**:
- Title, body, category (Personal/Office), priority (High/Medium/Low)
- Optional due date with DatePicker
- Optional reminder with DatePicker + TimePicker
- Pin / Complete / Edit / Delete actions
- Search + filter by category & priority
- Overdue detection (red highlight)

**Notifications**:
- iOS: Rich notifications with action buttons (Complete / Snooze 15m / Snooze 30m)
- Android: Standard notifications via AlarmManager + BroadcastReceiver

---

## 5. User Flows

### 5.1 First Launch
```
App Opens
  ↓
Launch Animation (iOS: 3.2s boot sequence / Android: instant)
  ↓
Dashboard loads
  ↓
Permission prompts (in order):
  1. Notifications (Android 13+)
  2. HealthKit / Health Connect
  3. Calendar (iOS only)
  ↓
Dashboard shows with empty state
```

### 5.2 Adding an Expense
```
Spending Tab → FAB (+)
  ↓
Amount → Category → Notes → Date
  ↓
Save → Transaction appears in statement
  ↓
Aggregations update (totals, charts, category breakdown)
  ↓
Widget data refreshed (iOS)
```

### 5.3 Creating a Habit
```
Habits Tab → FAB (+)
  ↓
Name → Frequency (iOS: Daily/Weekly/Multi) → Reminder toggle
  ↓
If reminder: TimePicker → Set time
  ↓
Save → Habit appears in daily objectives
  ↓
Daily reminder scheduled via AlarmManager/UNNotification
```

### 5.4 Setting a Note Reminder
```
Notes Tab → FAB (+) or Edit existing
  ↓
Title → Body → Category → Priority
  ↓
Toggle Due Date → DatePicker
Toggle Reminder → DatePicker + TimePicker
  ↓
Save → Note appears with due date badge
  ↓
Reminder alarm scheduled → Notification fires at set time
  ↓
User taps notification → App opens (iOS: action buttons available)
```

### 5.5 Checking Daily Brief (iOS)
```
Dashboard opens
  ↓
DashboardViewModel generates contextual brief:
  - Pending tasks count + priorities
  - Today's spending vs. budget
  - Health goal progress
  - Incomplete habits
  - Next calendar event
  - Time-of-day recommendation
  ↓
Smart alerts if:
  - Budget exceeded (>100%)
  - Unusual spending (>2.5x daily avg)
  - No expenses in 3+ days
  - Overdue tasks exist
```

---

## 6. Notification System

### iOS
| Category | Trigger | Actions |
|----------|---------|---------|
| Habit Reminder | UNCalendarNotificationTrigger (repeating) | Mark Done, Snooze 15m, Snooze 30m |
| Note Reminder | UNCalendarNotificationTrigger (one-time) | Complete, Snooze 15m, Snooze 30m |

- Foreground display enabled (banner + sound + badge)
- Snooze creates new one-time trigger
- "Done"/"Complete" directly updates CoreData from notification

### Android
| Category | Trigger | Actions |
|----------|---------|---------|
| Habit Reminder | AlarmManager.setRepeating (daily) | Tap to open app |
| Note Reminder | AlarmManager.setExactAndAllowWhileIdle | Tap to open app |

- Channel: "jarvis_channel" (HIGH importance)
- Runtime permission request on Android 13+
- Auto-cancel enabled

---

## 7. Widget System (iOS Only)

| Widget | Sizes | Content |
|--------|-------|---------|
| Spend Tracker | Small, Medium | Today's spend, monthly total, budget bar, top category |
| Habit Tracker | Small | Progress ring, completed/due count, streak |
| Voxn AI Dashboard | Medium, Large | Spend + habits + health stats + next event + greeting |

**Data Sharing**: App Group (`group.Raunak.Jarvis`) via shared UserDefaults
**Refresh**: Every 30 minutes + on data change via `WidgetCenter.shared.reloadAllTimelines()`

---

## 8. Design System

### Color Palette
| Token | Hex | Usage |
|-------|-----|-------|
| Electric Blue | `#00D4FF` | Primary accent, selected states |
| Cyan | `#00F0FF` | Secondary info, notes |
| Neon Green | `#39FF14` | Success, health, income, habits |
| Warning Orange | `#FF6B35` | Spending, alerts |
| Alert Red | `#FF3B30` | Danger, overdue, errors |
| Purple | `#BF5AF2` | Calendar events |
| Background | `#0A0A0F` | App background |
| Card BG | white @ 5% | Card surfaces |

### Typography
| Style | Font | Size | Weight |
|-------|------|------|--------|
| Hero Number | Monospaced | 42 | Bold |
| Data Readout | Monospaced | 24 | Semibold |
| Section Title | System | 20 | Bold |
| Card Title | System | 16 | Semibold |
| Card Body | System | 14 | Regular |
| Caption | System | 12 | Regular |
| Data Label | Monospaced | 12 | Medium |

### Components
- **GlassCard**: Frosted glass card with colored border + glow shadow
- **ArcGauge**: 270-degree animated circular gauge
- **ProgressRing**: Full circle progress indicator with center content
- **HUDSectionHeader**: Section title with icon + gradient line
- **GlowText**: Text with multi-layer neon shadow
- **PulsingGlow**: Animated breathing glow effect

---

## 9. Permissions & Entitlements

### iOS
| Permission | Purpose |
|------------|---------|
| HealthKit | Steps, calories, sleep, workout metrics |
| Calendar (EventKit) | Today's events on dashboard |
| Notifications | Habit & note reminders |
| App Groups | Widget data sharing |

### Android
| Permission | Purpose |
|------------|---------|
| `POST_NOTIFICATIONS` | Reminder notifications (Android 13+) |
| `SCHEDULE_EXACT_ALARM` | Precise reminder scheduling |
| `RECEIVE_BOOT_COMPLETED` | (Declared, not yet implemented) |
| `health.READ_STEPS` | Health Connect integration |
| `health.READ_ACTIVE_CALORIES_BURNED` | Health Connect |
| `health.READ_SLEEP` | Health Connect |
| `health.READ_EXERCISE` | Health Connect |

---

## 10. Build & Deploy

### iOS
```bash
# Requirements: Xcode 16+, iPhone iOS 17+, free Apple ID
open Jarvis.xcodeproj
# Set signing team → Connect device → Cmd+R
# Free cert expires in 7 days; re-run to reinstall
```

### Android
```bash
# Requirements: Android Studio / JDK 17, Android 9+ device
export JAVA_HOME="/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home"
./gradlew assembleDebug
# APK at app/build/outputs/apk/debug/app-debug.apk
```

---

## 11. Next Action Items

### High Priority

| # | Feature | Platform | Description |
|---|---------|----------|-------------|
| 1 | **Calendar Integration** | Android | Add EventKit-equivalent using Android CalendarProvider to show today's events on dashboard |
| 2 | **Daily Brief** | Android | Port the AI-generated daily summary from iOS (spending alerts, health insights, task priorities, time-based recommendations) |
| 3 | **Income/Expense Toggle** | Android | Add TransactionType (Expense/Income) support — currently Android only tracks expenses |
| 4 | **Recurring Bills** | Android | Port recurring expense auto-logging from iOS |
| 5 | **Smart Alerts** | Android | Budget exceeded, unusual spending, inactivity detection |
| 6 | **Habit Frequencies** | Android | Add Weekly (select days) and Multiple/Day frequency options — currently only Daily |
| 7 | **Rich Notifications** | Android | Add action buttons (Done/Snooze) to notification, matching iOS behavior |
| 8 | **Boot Receiver** | Android | Re-schedule alarms after device reboot (permission declared but not implemented) |

### Medium Priority

| # | Feature | Platform | Description |
|---|---------|----------|-------------|
| 9 | **Onboarding Flow** | Both | 3-step welcome: name setup, permission grants, feature overview |
| 10 | **Spending Insights Chart** | Android | Port 14-day daily spend bar chart and week-over-week comparison |
| 11 | **Notification Parser** | Android | Auto-parse bank SMS notifications to log expenses (merchant detection, amount extraction) |
| 12 | **Budget & Savings Goals** | Android | Monthly budget with progress bar, savings goal with progress ring |
| 13 | **Widgets** | Android | Home screen widgets (Spend, Habit, Dashboard) using Glance API |
| 14 | **User Profile** | Android | User name setup for personalized greeting |
| 15 | **Data Export** | Both | CSV export for spending data |
| 16 | **Launch Animation** | Android | Port the arc reactor boot sequence from iOS |

### Low Priority / Future

| # | Feature | Platform | Description |
|---|---------|----------|-------------|
| 17 | **Global Search** | Both | Search across all modules (expenses, habits, notes) |
| 18 | **Monthly Reports** | Both | Auto-generated monthly summary with charts |
| 19 | **Custom Notification Sounds** | Both | Themed notification tones |
| 20 | **Siri Shortcuts** | iOS | Quick actions (add expense, mark habit) |
| 21 | **iCloud Sync** | iOS | Requires paid developer account |
| 22 | **Dark/Light Theme** | Both | Currently dark-only; add optional light mode |
| 23 | **Localization** | Both | Multi-currency support, language translations |
| 24 | **Biometric Lock** | Both | FaceID/TouchID/Fingerprint to secure app |
| 25 | **AI Assistant** | Both | Natural language input ("I spent 500 on food at Swiggy") |

### Tech Debt

| # | Item | Platform | Description |
|---|------|----------|-------------|
| T1 | **Package rename** | Android | Rename `com.jarvis.app` → `com.voxn.ai` |
| T2 | **Internal naming** | Both | Rename internal classes (JarvisColors → VoxnColors, etc.) |
| T3 | **App Group ID** | iOS | Rename `group.Raunak.Jarvis` → `group.Raunak.VoxnAI` |
| T4 | **Bundle ID** | iOS | Rename `Raunak.Jarvis` → `Raunak.VoxnAI` |
| T5 | **Unit Tests** | Both | Add test coverage for ViewModels and Managers |
| T6 | **Error Handling** | Both | Graceful error states for DB failures, permission denials |

---

## 12. Repository Links

- **iOS**: https://github.com/pandeyraunak007/Jarvis
- **Android**: https://github.com/pandeyraunak007/Jarvis-Android

---

*Built with SwiftUI + Jetpack Compose | 100% offline, no analytics, no cloud*
