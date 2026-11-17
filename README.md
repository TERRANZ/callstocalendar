# Calls To Calendar Sync

[![License](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/platform-Android-3DDC84)](https://developer.android.com/)
[![Min SDK](https://img.shields.io/badge/min%20sdk-21+-3DDC84)](https://developer.android.com/studio/releases/platforms#5.0)

An Android app that automatically syncs your phone's call log into your Google Calendar. Every call — incoming, outgoing, or missed — becomes a calendar event with full details.

## How It Works

1. On startup, the app queries your call log for calls made since the last sync
2. For each call, it fetches the contact name associated with the phone number
3. It creates a calendar event with the call's date, duration, type, and caller info
4. An hourly background alarm keeps the sync running even after the app is closed
5. A boot receiver re-sets the alarm after device restart

## Features

- **Automatic sync** — calls are synced to your calendar every hour
- **Call types** — distinguishes between incoming, outgoing, and missed calls
- **Contact lookup** — resolves phone numbers to contact names (when available)
- **Deduplication** — prevents duplicate events by tracking calls via a unique hash
- **Calendar selection** — choose which calendar to sync to
- **Persistent** — alarm survives device reboots

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                      MainActivity                         │
│  - Request permissions on launch                         │
│  - Load available calendars into a map                    │
│  - UI: button to select target calendar                   │
└──────────────────────────────────────────────────────────┘
                           │
         ┌─────────────────┼─────────────────┐
         │                 │                 │
         ▼                 ▼                 ▼
┌────────────────┐ ┌──────────────┐ ┌──────────────────┐
│  CallsUtil     │ │ CalendarUtil │ │   PhoneCall      │
│  - getCallLogs │ │ - setupSync  │ │  Entity:         │
│  - doSync      │ │   Alarm      │ │  missed,         │
│  - filter by   │ │ - getContact │ │  incoming,       │
│    last_sync   │ │   Name       │ │  startDate,      │
│  - convert to  │ │ - saveCall   │ │  length,         │
│    PhoneCall   │ │   Event      │ │  number          │
└────────────────┘ └──────────────┘ └──────────────────┘
         ▲                                      │
         │                                      ▼
┌──────────────────────────────────────────────────────────┐
│  SyncReceiver (BroadcastReceiver)                         │
│  - Fired by AlarmManager hourly                           │
│  - Triggers CallsUtil.doSync()                            │
│                                                         │
│  BootReceiver (BroadcastReceiver)                         │
│  - Fired on BOOT_COMPLETED                                │
│  - Re-sets the hourly sync alarm                          │
└──────────────────────────────────────────────────────────┘
```

## Permissions

The app requires the following permissions at runtime:

| Permission | Purpose |
|---|---|
| `READ_CALL_LOG` | Read phone call history |
| `READ_CALENDAR` | Query existing calendar events for deduplication |
| `WRITE_CALENDAR` | Insert call events into your calendar |
| `READ_CONTACTS` | Resolve phone numbers to contact names |
| `INTERNET` | Required by Android for Calendar API access |
| `RECEIVE_BOOT_COMPLETED` | Re-activate alarm after device reboot |

## Build & Run

### Prerequisites

- Android Studio (latest version)
- JDK 8+
- An Android device or emulator (API 21+)

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
2. Connect an Android device or start an emulator (API 21+)
3. Click **Run** or press `Shift+F10`

## Technical Details

| Setting | Value |
|---|---|
| Min SDK | 21 (Android 5.0) |
| Target SDK | 30 (Android 11) |
| Compile SDK | 30 |
| Language | Kotlin 1.3.72 |
| Sync interval | 1 hour (inexact) |
| Dedup key | `CalendarContract.Events.UID_2445` |

## Note

The app's UI and string resources are currently in Russian. The app is designed to be minimal — a single button to select your target calendar, with all sync logic running in the background.

## License

This project is open source. See the [LICENSE](LICENSE) file for details.
