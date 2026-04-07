# Voxn AI — Privacy Policy

**Effective Date:** April 7, 2026
**App Name:** Voxn AI
**Developer:** Raunak Pandey
**Contact:** raunak@voxn.ai

---

## Data Collected

Voxn AI collects and stores the following data **locally on your device only**:

- **Health data** — Steps, calories burned, sleep duration, and exercise minutes read from Health Connect (read-only).
- **Financial data** — User-entered expenses and income, including amount, merchant, category, and payment method.
- **Habits** — User-created habit names, reminder times, and completion history.
- **Notes & Reminders** — User-created notes with titles, body text, categories, priorities, due dates, and reminder dates.
- **User preferences** — Display name and app settings (e.g., monthly budget, savings goal).

No personal information (name, email, phone number, location) is collected beyond what the user voluntarily enters.

---

## Data Storage

All data is stored **locally on your device** using an encrypted SQLite database (protected by Android's file-based encryption). No data is transmitted to any server, cloud service, or external system.

---

## Third-Party Sharing

Voxn AI does **not** share any data with third parties. There are:

- No analytics SDKs
- No crash reporting services
- No advertising networks
- No server-side data collection
- No data brokers or resellers

---

## Health Connect Usage

Voxn AI uses the Health Connect API in **read-only** mode to display health metrics within the app. Specifically:

- `READ_STEPS` — Daily and weekly step counts
- `READ_ACTIVE_CALORIES_BURNED` — Daily and weekly calorie expenditure
- `READ_SLEEP` — Sleep session duration
- `READ_EXERCISE` — Exercise session duration

Health data is fetched on-demand when the app is open and is **never** stored persistently, cached, or transmitted off-device. The app does not write any data to Health Connect.

---

## Data Deletion

You can delete all app data at any time by:

1. **Clearing app data:** Go to Android Settings > Apps > Voxn AI > Storage > Clear Data.
2. **Uninstalling the app:** Removing Voxn AI deletes all locally stored data permanently.
3. **Revoking Health Connect access:** Go to Settings > Health Connect > Voxn AI > Disconnect.

There is no server-side data to request deletion of.

---

## Children's Privacy

Voxn AI is not directed at children under 13 and does not knowingly collect data from children.

---

## Changes to This Policy

We may update this privacy policy from time to time. Changes will be reflected by updating the "Effective Date" at the top of this document.

---

## Google Play Data Safety

The following declarations apply to the Google Play Data Safety form:

| Question | Answer |
|----------|--------|
| Does your app collect or share any user data? | No data is shared with third parties. Data is collected locally only. |
| Data types accessed on-device | Health data (read-only via Health Connect), financial data (user-entered expenses), notes/tasks (user-entered) |
| Is data encrypted? | Yes — SQLite database on device, encrypted at rest by Android's FDE/FBE |
| Can users request data deletion? | Yes — clear app data or uninstall |
| Analytics SDKs | None |
| Crash reporting | None |
| Advertising SDKs | None |

---

## Contact

If you have questions about this privacy policy, contact us at:
**Email:** raunak@voxn.ai
