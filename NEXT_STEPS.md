# Voxn AI — Next Steps for Google Play Launch

**Last updated:** April 7, 2026

---

## Pre-Launch (Manual Steps)

### 1. Enable GitHub Pages for Privacy Policy
- Go to: https://github.com/pandeyraunak007/Jarvis-Android/settings/pages
- Set **Source** to `Deploy from a branch`
- Set **Branch** to `main`, folder to `/docs`
- Save
- Privacy policy URL: `https://pandeyraunak007.github.io/Jarvis-Android/`

### 2. Take App Screenshots
- Run the app on a physical device or emulator
- Capture 4-8 screenshots (1080x1920 or 1440x2560 recommended)
- Screens to capture:
  - Dashboard (HUD) with data populated
  - Spending view with transactions and charts
  - Habits view with streaks and completions
  - Health view with arc gauges
  - Notes view with some notes
- Optional: tablet screenshots (7" and 10")

### 3. Export Feature Graphic
- Open `docs/feature-graphic.html` in a browser
- Screenshot the 1024x500 graphic
- Save as PNG for Play Store upload

### 4. Create App in Google Play Console
- Go to: https://play.google.com/console
- Click **Create app**
- App name: `Voxn AI`
- Default language: English
- App type: App
- Free/Paid: Free
- Accept declarations

### 5. Fill Store Listing
- Copy content from `PLAYSTORE_LISTING.md`
- **Short description:** Your personal life HUD — track spending, habits, health & notes in one app.
- **Full description:** See PLAYSTORE_LISTING.md
- **Category:** Productivity
- Upload feature graphic and screenshots

### 6. Upload Release AAB
- Go to: Production > Create new release
- Upload: `app/build/outputs/bundle/release/app-release.aab` (5.1 MB)
- Release name: `1.0`

### 7. Fill Data Safety Form
- Data collected: None shared externally
- On-device data: Health (read-only), expenses (user-entered), notes (user-entered)
- Encryption: Yes (Android FDE/FBE)
- Deletion: Clear app data or uninstall
- Analytics: None
- Ads: None
- See `PRIVACY_POLICY.md` for full details

### 8. Apply for Health Connect Access
- Play Console requires a separate declaration for Health Connect apps
- Fill the Health Connect developer declaration form
- Reference the privacy policy URL

### 9. Content Rating (IARC)
- Complete the rating questionnaire in Play Console
- Expected rating: **Everyone**

### 10. Set Pricing & Distribution
- Price: Free
- Countries: All (or select specific markets)

### 11. Review & Submit
- Ensure all sections show green checkmarks in Play Console
- Submit for review
- Review typically takes 1-3 days for new apps

---

## Post-Launch Improvements

### High Priority
- [ ] Onboarding flow (3-step welcome for new users)
- [ ] Data export to CSV
- [ ] Income/Expense toggle in spending
- [ ] Weekly/Multi-per-day habit frequencies
- [ ] Smart daily brief on dashboard
- [ ] Smart alerts (budget exceeded, unusual spending, inactivity)
- [ ] Recurring bills manager
- [ ] Rich notifications (Done/Snooze actions for habits)

### Medium Priority
- [ ] Android widgets (Glance API) — Spend, Habit, Dashboard
- [ ] Monthly budget & savings goals
- [ ] Spending insights charts (14-day bar, category pie)
- [ ] Calendar integration (Google Calendar events on dashboard)
- [ ] Notification SMS parser for auto-logging expenses
- [ ] User profile screen
- [ ] Launch animation

### Low Priority
- [ ] Global search across all modules
- [ ] Monthly reports
- [ ] Custom notification sounds
- [ ] Dark/Light theme toggle
- [ ] Localization (Hindi, etc.)
- [ ] Biometric lock
- [ ] AI assistant (natural language input)

### Tech Debt
- [ ] Unit tests for ViewModels and Managers (beyond current entity tests)
- [ ] Instrumented tests for Room database
- [ ] Error handling for database failures
- [ ] Migrate strings to `strings.xml` for localization
- [ ] Update deprecated Compose icons (AutoMirrored variants)

---

## Key Files Reference

| File | Purpose |
|------|---------|
| `app-release.aab` | Signed release bundle (in `app/build/outputs/bundle/release/`) |
| `release.keystore` | Release signing key (**back this up securely!**) |
| `local.properties` | Keystore credentials (gitignored) |
| `PRIVACY_POLICY.md` | Privacy policy source |
| `docs/index.html` | Privacy policy web page (GitHub Pages) |
| `docs/feature-graphic.html` | Feature graphic template |
| `PLAYSTORE_LISTING.md` | Store listing text |
| `PLAYSTORE_READINESS.md` | Original readiness assessment |

---

## Important Reminders

- **Back up `release.keystore`** — if lost, you cannot update the app on Play Store
- **Password:** stored in `local.properties` (never commit this file)
- **applicationId is `com.voxn.ai`** — this cannot be changed after first Play Store publish
- **Privacy policy URL must be live** before submitting for review

---

*Generated April 7, 2026*
