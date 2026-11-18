# Calls To Calendar Sync

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-3DDC84)](https://developer.android.com/)
[![Min SDK](https://img.shields.io/badge/min%20sdk-23+-3DDC84)](https://developer.android.com/studio/releases/platforms#6.0)

An Android app that automatically syncs your phone's call log into your Google Calendar. Every call — incoming, outgoing, or missed — becomes a calendar event with full details.

## How It Works

1. On startup, the app queries your call log for calls made since the last sync
2. For each call, it fetches the contact name associated with the phone number
3. It creates a calendar event with the call's date, duration, type, and caller info
4. A WorkManager periodic task keeps the sync running even after the app is closed
5. WorkManager automatically reschedules the sync after a device restart

## Features

- **Automatic sync** — calls are synced to your calendar every hour
- **Call types** — distinguishes between incoming, outgoing, and missed calls
- **Contact lookup** — resolves phone numbers to contact names (when available)
- **Deduplication** — prevents duplicate events by tracking calls via a unique hash
- **Calendar selection** — choose which calendar to sync to
- **Persistent** — WorkManager reschedules sync automatically after reboot

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                      MainActivity                         │
│  - Request runtime permissions on launch                  │
│  - Kick off an immediate background sync                   │
│  - Load available calendars into a map                     │
│  - UI: button to select target calendar                    │
└──────────────────────────────────────────────────────────┘
                            │
          ┌─────────────────┼──────────────────┐
          ▼                 ▼                 ▼
┌────────────────┐ ┌──────────────┐ ┌──────────────────┐
│  CallsUtil     │ │ CalendarUtil │ │   PhoneCall      │
│  - getCallLogs │ │ - enqueueWork│ │  Entity:         │
│  - doSync      │ │ - getContact │ │  missed,         │
│  - filter by   │ │   Name       │ │  incoming,       │
│    last_sync   │ │ - saveCall   │ │  startDate,      │
│  - convert to  │ │   Event      │ │  length,         │
│    PhoneCall   │ │ - checkCall  │ │  number          │
└────────────────┘ └──────────────┘ └──────────────────┘
                            │
          ┌─────────────────┼──────────────────┐
          ▼                 ▼                 ▼
┌──────────────────┐ ┌──────────────────┐ ┌──────────────────┐
│ SyncWorker       │ │   PreferenceMgr  │ │    Manifest      │
│ (androidx.work)  │ │  - stores        │ │  - permissions   │
│ PeriodicWorkReq. │ │    cal_id /       │ │  - backup rules  │
│ CoroutineWorker  │ │    last_sync prefs│ │  - edge-to-edge  │
└──────────────────┘ └──────────────────┘ └──────────────────┘
```

**Background sync:** the hourly `AlarmManager` alarm plus the `SyncReceiver` /
`BootReceiver` broadcast receivers from the original design were replaced by
**WorkManager** (`androidx.work`). A 1-hour `PeriodicWorkRequest` (unconstrained
by battery level) runs `SyncWorker`, a `CoroutineWorker` that calls
`CallsUtil.doSync()`. WorkManager reschedules itself after reboot, so no boot
receiver is needed. See [`upgrade.md`](upgrade.md) for the full toolchain
migration notes.

## Permissions

The app requires the following permissions at runtime:

| Permission | Purpose |
|---|---|
| `READ_CALL_LOG` | Read phone call history |
| `READ_CALENDAR` | Query existing calendar events for deduplication |
| `WRITE_CALENDAR` | Insert call events into your calendar |
| `READ_CONTACTS` | Resolve phone numbers to contact names |
| `INTERNET` | Required by Android for Calendar API access |

## Build & Run

### Prerequisites

- Android Studio (latest version)
- JDK 17+ (JDK 21 recommended — AGP 9.x requires 17+)
- An Android device or emulator (API 23+)

### Building

```bash
# Clone the repo
git clone https://github.com/terranz/callstocalendar.git
cd callstocalendar

# Build with Gradle
./gradlew assembleDebug
```

### Running

1. Open the project in Android Studio
2. Connect an Android device or start an emulator (API 23+)
3. Click **Run** or press `Shift+F10`

## Technical Details

| Setting | Value |
|---|---|
| Min SDK | 23 (Android 6.0) |
| Target SDK | 36 (Android 12.1) |
| Compile SDK | 37 (Android 15 / UP-1) |
| Language | Kotlin 2.2.10 (built-in with AGP) |
| Build tooling | AGP 9.5.0 · Gradle 9.6.0 · version catalog |
| Sync interval | 1 hour (WorkManager PeriodicWorkRequest) |
| Dedup key | `CalendarContract.Events.UID_2445` |

## Note

The app's UI and string resources are currently in Russian. The app is designed to be minimal — a single button to select your target calendar, with all sync logic running in the background.

## License

This project is open source. See the [LICENSE](LICENSE) file for details.
