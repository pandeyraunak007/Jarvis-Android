# Voxn AI — Google Play Store Readiness Assessment

**Date:** April 6, 2026
**Package:** `com.jarvis.app`
**Target SDK:** 35 | **Min SDK:** 28
**Verdict:** Not yet ready — 17 issues identified (5 critical blockers, 7 important, 5 minor)

---

## Critical Blockers

These must be fixed before submitting to Google Play.

### 1. No release signing configuration

The `build.gradle.kts` has no `signingConfigs` for release. Play Store requires a properly signed AAB (not debug APK).

**Claude Code command:**

```
Add a release signing config to app/build.gradle.kts. Create a signingConfigs block that reads keystore path, password, key alias, and key password from local.properties (do NOT hardcode secrets). Reference the signing config in the release buildType. Add a comment reminding me to generate a keystore with keytool.
```

---

### 2. R8/ProGuard disabled for release builds

`isMinifyEnabled = false` means no code shrinking, no obfuscation, and a bloated APK. Play Store expects optimized release builds.

**Claude Code command:**

```
In app/build.gradle.kts, enable isMinifyEnabled and isShrinkResources for the release build type. Add ProGuard rules in proguard-rules.pro for Room, Health Connect, Compose, and Kotlin serialization to prevent stripping needed classes.
```

---

### 3. Exact alarm permission not properly handled

`SCHEDULE_EXACT_ALARM` is used but `canScheduleExactAlarms()` is never checked. On Android 12+ this can crash; on Android 14+ the permission may be revoked by default.

**Claude Code command:**

```
In HabitManager.kt and NoteManager.kt, before calling alarmManager.setRepeating() or setExactAndAllowWhileIdle(), check if Build.VERSION.SDK_INT >= 31 and if so call alarmManager.canScheduleExactAlarms(). If false, fall back gracefully — either use setAndAllowWhileIdle() (inexact) or guide the user to open the exact alarm settings via Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM). Do not crash. Add the same check in a new BootReceiver that re-schedules alarms on RECEIVE_BOOT_COMPLETED.
```

---

### 4. Missing adaptive icon (required for API 26+)

The `mipmap-anydpi-v26/` directory is empty. Every device running Android 8.0+ will show a broken or white-square icon.

**Claude Code command:**

```
Create adaptive icon XML files in app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml and ic_launcher_round.xml. They should reference a foreground drawable and a background color. Create a simple vector foreground in app/src/main/res/drawable/ic_launcher_foreground.xml (a stylized "V" letter in electric blue #00D4FF on transparent background). Add ic_launcher_background color as #0A0A0F in colors.xml.
```

---

### 5. No privacy policy for Health Connect

Google requires a privacy policy URL for any app using Health Connect, and a separate Health Connect developer declaration form must be submitted.

**Claude Code command:**

```
Create a file PRIVACY_POLICY.md in the project root with a privacy policy for Voxn AI. State that the app collects no personal data, stores all data locally on-device, uses Health Connect in read-only mode for steps/calories/sleep/exercise, and shares no data with third parties. Include sections for: data collected, data storage, third-party sharing, data deletion, and contact information (use placeholder email). This will be hosted as a GitHub Pages site or similar for the Play Store listing.
```

---

## Important Issues

These won't immediately block review but will likely cause rejection or poor user experience.

### 6. Notification icon is a system default (renders as white square)

`NotificationReceiver` uses `android.R.drawable.ic_dialog_info` which renders as a solid white square on Android 5.0+.

**Claude Code command:**

```
Create a notification small icon as a monochrome vector drawable at app/src/main/res/drawable/ic_notification.xml — a simple "V" shape, white on transparent, following Android notification icon guidelines (must be flat, single color, no background). Then update NotificationReceiver.kt to use R.drawable.ic_notification instead of android.R.drawable.ic_dialog_info. Also set the notification color to the electric blue (#00D4FF) using setColor().
```

---

### 7. No BOOT_COMPLETED receiver (alarms lost on reboot)

The permission is declared but no receiver exists. All habit/note reminders are silently lost when the device restarts.

**Claude Code command:**

```
Create a new BroadcastReceiver at app/src/main/java/com/jarvis/app/util/BootReceiver.kt. Register it in AndroidManifest.xml with intent-filter for RECEIVE_BOOT_COMPLETED (exported=false). On receive, it should: get all habits with reminders enabled from the database (using runBlocking or a coroutine scope), re-schedule their alarms via HabitManager, and get all notes with future reminderDate and re-schedule them via NoteManager. Extract the alarm scheduling logic from both managers into reusable methods.
```

---

### 8. Silent exception swallowing in HealthConnectManager

`catch (_: Exception) { }` hides all errors from the user.

**Claude Code command:**

```
In HealthConnectManager.kt, replace the empty catch block in fetchAllData() with proper error handling. Add a MutableStateFlow<String?> called errorMessage. In the catch block, set errorMessage to a user-friendly string (e.g., "Unable to read health data. Check Health Connect permissions."). Expose it as a StateFlow. In HealthScreen.kt, collect this state and show a Snackbar or inline error message when non-null.
```

---

### 9. Room database has no migration strategy

`exportSchema = false` means no way to migrate data when the schema changes in future versions.

**Claude Code command:**

```
In JarvisDatabase.kt, change exportSchema to true. Add a ksp argument in app/build.gradle.kts: ksp { arg("room.schemaLocation", "$projectDir/schemas") }. Create the schemas directory. Add a .gitignore entry to track schema JSON files. Add a comment documenting that future schema changes require adding a Migration object to the databaseBuilder.
```

---

### 10. Package ID does not match app branding

Package is `com.jarvis.app` but user-facing name is "Voxn AI". This should be fixed before first Play Store release since `applicationId` cannot be changed after publishing.

**Claude Code command:**

```
Rename the applicationId in app/build.gradle.kts from "com.jarvis.app" to "com.voxn.ai". Update the namespace to "com.voxn.ai". Rename the package directories under app/src/main/java/ from com/jarvis/app to com/voxn/ai. Update all Kotlin file package declarations. Update AndroidManifest.xml android:name to ".VoxnApp". Rename JarvisApp.kt to VoxnApp.kt, JarvisDatabase to VoxnDatabase, JarvisColors to VoxnColors, JarvisFont to VoxnFont, JarvisTheme to VoxnTheme. Update all references across the codebase.
```

---

### 11. No Data Safety form information prepared

Play Store requires you to fill out a Data Safety form declaring what data you collect.

**Claude Code command:**

```
Add a section to PRIVACY_POLICY.md titled "Google Play Data Safety" with the following declarations ready to copy into the Play Console form:
- Data collected: None shared with third parties
- Data types accessed on-device: Health data (read-only via Health Connect), financial data (user-entered expenses, stored locally only), notes/tasks (stored locally only)
- Data encryption: SQLite database on device (encrypted at rest by Android's FDE/FBE)
- Data deletion: User can clear app data via Android settings; uninstalling removes all data
- No analytics, no crash reporting, no advertising SDKs
```

---

### 12. Notification channel created redundantly

`NotificationReceiver` re-creates the channel on every notification fire. This is wasteful and the channel should only be created in `JarvisApp.onCreate()`.

**Claude Code command:**

```
Remove the NotificationChannel creation code from NotificationReceiver.kt (lines 17-18 where it creates a new channel). The channel is already created in JarvisApp.onCreate() which runs before any notification can fire. Keep only the NotificationCompat.Builder and manager.notify() call in the receiver.
```

---

## Minor Issues

### 13. No unit or instrumented tests

**Claude Code command:**

```
Create basic unit tests for the core business logic. Add test files under app/src/test/java/com/jarvis/app/: ExpenseParserTest.kt (test parseNotification with various bank SMS formats, test categorize with known merchants), HabitEntityTest.kt (test currentStreak calculation, isCompletedToday), NoteEntityTest.kt (test isOverdue, hasUpcomingReminder). Use JUnit 4. Add testImplementation("junit:junit:4.13.2") to build.gradle.kts if not present.
```

---

### 14. Currency hardcoded to INR

**Claude Code command:**

```
This is informational — no code change needed now. For the Play Store listing, make sure the app description clearly states this is an INR-based expense tracker designed for Indian users. Consider adding a "Target audience" note in the store listing. Future enhancement: add a currency selector in a settings screen.
```

---

### 15. Debug APK committed to repository

`Jarvis-debug.apk` (64MB) is tracked in git despite `*.apk` being in `.gitignore`.

**Claude Code command:**

```
Remove the debug APK from git tracking: run "git rm --cached Jarvis-debug.apk" and commit with message "Remove debug APK from version control". The .gitignore already has *.apk so it won't be re-added.
```

---

### 16. HealthViewModel exposes manager as public val

**Claude Code command:**

```
In HealthViewModel.kt, change "val manager" to "private val manager". The permissions set is already accessible via the existing val permissions property.
```

---

### 17. No strings.xml for user-facing text

All user-visible strings are hardcoded in Compose code. This blocks localization and is a Play Store best practice concern.

**Claude Code command:**

```
This is informational for now. For future localization support, extract all hardcoded user-facing strings from Compose screens into app/src/main/res/values/strings.xml and reference them with stringResource(R.string.xxx). Priority strings: tab labels, section headers, empty states, button text, dialog titles.
```

---

## Pre-Submission Checklist

- [ ] Generate a release keystore and configure signing
- [ ] Enable R8 minification and add ProGuard rules
- [ ] Fix exact alarm permission handling
- [ ] Create adaptive icon
- [ ] Write and host a privacy policy
- [ ] Apply for Health Connect access via Play Console
- [ ] Fix notification icon
- [ ] Implement BootReceiver for alarm persistence
- [ ] Rename package to `com.voxn.ai` (before first publish!)
- [ ] Fill out Data Safety form in Play Console
- [ ] Set `exportSchema = true` and configure Room schema export
- [ ] Add error handling for Health Connect failures
- [ ] Remove debug APK from git
- [ ] Create Play Store assets (screenshots, feature graphic, description)
- [ ] Test on multiple API levels (28, 31, 33, 35)
- [ ] Generate signed AAB with `./gradlew bundleRelease`

---

*Generated from full codebase review — April 6, 2026*
