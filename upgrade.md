# Upgrade Plan — Calls To Calendar Sync

> **Date:** 2026-11-17
> **Goal:** Bring the project from its 2021-era toolchain (AGP 4.0 / Gradle 6.1 / Kotlin 1.3 / SDK 30) up to the current stable Android stack: **compile/target SDK 36 (Android 16)**, **AGP 9.5**, **Gradle 9.6+**, **Kotlin 2.x**, with code fixes required by platform behavior changes on Android 11–16.
>
> **Why now:** Google Play requires new apps and updates to target **Android 16 (API 36) or higher starting 2026-08-31**. The current app targets API 30 and can no longer be updated on Play.

---

## 1. Baseline vs. Target

### Current state

| Item | Version |
|---|---|
| Gradle (wrapper) | 6.1.1 |
| Android Gradle Plugin | 4.0.0 |
| Kotlin | 1.3.72 |
| JDK | 8 (implicit) |
| compileSdk / targetSdk | 30 / 30 |
| minSdk | 21 |
| Repositories | google() + jcenter() (jcenter is retired) |
| Kotlin plugin | kotlin-android + kotlin-android-extensions |

### Target state

| Item | Version | Notes |
|---|---|---|
| Gradle (wrapper) | 9.6.0+ | Minimum for AGP 9.5; `-all` distribution |
| Android Gradle Plugin | 9.5.0 | Latest stable (Oct 2026), supports up to API 37 |
| Kotlin | 2.2.10+ | AGP 9 has **built-in Kotlin** (KGP 2.2.10 default); no `kotlin-android` plugin needed |
| JDK | 17 | Required by AGP 9.x (17+; 21 recommended for Android Studio Koala+) |
| compileSdk / targetSdk | 36 / 36 | Android 16 ("Baklava") |
| minSdk | 21 (keep) or 24 (optional) | Play's rule is targetSdk-only; keeping 21 is safe, 24 simplifies API-level branching |
| Build Tools | 36.0.0 | AGP 9.5 default |
| Repositories | google() + mavenCentral() | jcenter() is dead |

---

## 2. Phase 1 — Build system (toolchain)

### 2.1 JDK

- Install/switch to **JDK 17+** (JDK 21 recommended). AGP 9.x hard-requires 17.
- In Android Studio: **Settings → Build → Gradle → Gradle JDK → 17+**.
- Command-line builds: ensure `org.gradle.java.home` or `JAVA_HOME` points at 17+.

### 2.2 Gradle wrapper

- Bump `gradle/wrapper/gradle-wrapper.properties`:
  ```
  distributionUrl=https\://services.gradle.org/distributions/gradle-9.6.0-all.zip
  ```
- Update the wrapper JAR/scripts: run `gradle wrapper --gradle-version 9.6.0` from a machine that has a local Gradle install, or let Android Studio re-sync (it will offer to update the wrapper).

### 2.3 Root `build.gradle`

Replace `jcenter()` with `mavenCentral()` and upgrade AGP + Kotlin.

**Option A (minimal diff, Groovy DSL):**

```groovy
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath "com.android.tools.build:gradle:9.5.0"
        // Optional: pin a KGP newer than AGP 9's built-in default (2.2.10)
        // classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.10"
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
```

**Option B (recommended, modern): version catalog + plugins DSL**

Create `gradle/libs.versions.toml`:

```toml
[versions]
agp = "9.5.0"
kotlin = "2.2.10"
coreKtx = "1.19.1"
appcompat = "1.8.0"
constraintlayout = "2.2.2"
junit = "4.13.2"
junitExt = "1.3.0"
espresso = "3.7.0"

[libraries]
core-ktx = { group = "androidx.core", name = "core-ktx", version.ref = "coreKtx" }
appcompat = { group = "androidx.appcompat", name = "appcompat", version.ref = "appcompat" }
constraintlayout = { group = "androidx.constraintlayout", name = "constraintlayout", version.ref = "constraintlayout" }
junit = { group = "junit", name = "junit", version.ref = "junit" }
junit-ext = { group = "androidx.test.ext", name = "junit", version.ref = "junitExt" }
espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espresso" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
```

`settings.gradle` becomes:

```groovy
pluginManagement {
    repositories {
        google { content { includeGroupByRegex "com\\.android.*"; includeGroupByRegex "com\\.google.*"; includeGroupByRegex "androidx.*" } }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "Calls To Calendar Sync"
include ':app'
```

`build.gradle` (root) becomes nearly empty:

```groovy
plugins {
    alias(libs.plugins.android.application) apply false
}
```

> Option B is the current Android Studio default and keeps the repo future-proof. Either option satisfies the upgrade goal; B is recommended.

### 2.4 `app/build.gradle`

Key changes:

- **Remove `apply plugin: 'kotlin-android'`** — AGP 9 has built-in Kotlin by default.
- **Remove `apply plugin: 'kotlin-android-extensions'`** — the plugin was removed in Kotlin 2.0. (This app has no `by lazy` / `findViewById` bindings, so nothing else is needed. If any had existed, they'd be replaced with `findViewById` or ViewBinding.)
- **Replace deprecated SDK accessors** with the modern property DSL: `compileSdk 36`, `targetSdk 36`, `minSdk 21`.
- **Drop `buildToolsVersion`** — AGP picks the default (36.0.0) unless you have a reason to pin.
- **Fix the proguard file**: the project uses `getDefaultProguardFile('proguard-android-optimize.txt')` but only `proguard-rules.pro` exists — since AGP 8 the file must exist or the build fails. Use `getDefaultProguardFile('proguard-android.txt')` or add a `proguard-android-optimize.txt` copy.

Result (Option A style):

```groovy
plugins {
    id 'com.android.application'
}

android {
    namespace 'nu.mine.terranz.callstocalendarsync'
    compileSdk 36

    defaultConfig {
        applicationId "nu.mine.terranz.callstocalendarsync"
        minSdk 21
        targetSdk 36
        versionCode 1
        versionName "1.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
        }
    }
}

dependencies {
    implementation fileTree(dir: "libs", include: ["*.jar"])
    implementation libs.core.ktx
    implementation libs.appcompat
    implementation libs.constraintlayout
    testImplementation libs.junit
    androidTestImplementation libs.junit.ext
    androidTestImplementation libs.espresso.core
}
```

> **Kotlin stdlib:** no longer needed as an explicit dependency — AGP 9's built-in Kotlin adds `kotlin-stdlib` automatically.

### 2.5 `gradle.properties`

- Keep `android.useAndroidX=true` (AGP 9 defaults to true; harmless to keep).
- **Remove `android.enableJetifier=true`** — jetifier is deprecated and unnecessary once all dependencies are AndroidX (they are).
- Optional (AGP 9 defaults, explicit for clarity):
  ```
  android.newDsl=true
  android.builtInKotlin=true
  ```

### 2.6 Namespace

AGP 8+ requires `namespace` in the module's `build.gradle` and removes the `package` attribute from `AndroidManifest.xml`.

- `app/build.gradle`: add `namespace 'nu.mine.terranz.callstocalendarsync'`.
- `AndroidManifest.xml`: delete the `package="nu.mine.terranz.callstocalendarsync"` attribute.

---

## 3. Phase 2 — SDK levels & manifest

### 3.1 SDK levels

- `compileSdk 36`, `targetSdk 36` (see Phase 1).
- `minSdk 21`: keep for maximum device coverage. The Play Store rule is **targetSdk-only**; a low minSdk is allowed. Raising to 24 is an optional simplification (fewer API-level branches in code).

### 3.2 Manifest — components must declare `android:exported` (required since targetSdk 31)

AGP enforces this when targeting 31+.

- `MainActivity` has a LAUNCHER intent-filter → `android:exported="true"`.
- `BootReceiver` has an intent-filter (BOOT_COMPLETED) → `android:exported="true"`.
- `SyncReceiver` has no intent-filter; it is only fired by an explicit PendingIntent → `android:exported="false"`.

### 3.3 Manifest — backup rules (required on API 31+)

`android:allowBackup="true"` without backup rules triggers Play Console warnings on API 31+ and is being phased out. Add extraction rules:

```xml
<application
    android:allowBackup="true"
    android:dataExtractionRules="@xml/data_extraction_rules"
    android:fullBackupContent="@xml/backup_rules"
    ...>
```

Create `app/src/main/res/xml/data_extraction_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <include domain="sharedpref" path="."/>
    </cloud-backup>
    <device-transfer>
        <include domain="sharedpref" path="."/>
    </device-transfer>
</data-extraction-rules>
```

Create `app/src/main/res/xml/backup_rules.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <include domain="sharedpref" path="."/>
</full-backup-content>
```

> The app's only persisted state is the `cal_id` shared preference (target calendar) — backing up sharedprefs is exactly right.

### 3.4 PendingIntent flags (required on API 31+)

`CalendarUtil.setupSyncAlarm()` uses `PendingIntent.getBroadcast(context, 0, intent, 0)`. On API 31+ a mutable/immutable flag **must** be specified or the app crashes with `IllegalArgumentException`.

Fix (the alarm intent carries no extras → immutable is correct):

```kotlin
val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
val alarmIntent = PendingIntent.getBroadcast(context, 0, intent, flags)
```

---

## 4. Phase 3 — Runtime permission model (Android 11+ / API 30+)

### 4.1 READ_CALL_LOG / READ_CONTACTS split (critical for this app)

Since targeting API 30, the system grants `READ_CALL_LOG` and `READ_CONTACTS` **separately**. A user can grant only one. This app needs **both** (call log + contact-name lookup), so:

1. Request `READ_CALL_LOG` first (the app is useless without it).
2. After grant, request `READ_CONTACTS` (contact names are an enhancement, not core).
3. `getContactName()` must already tolerate a denied contact permission — it does (empty result, no crash).

### 4.2 Fix bogus runtime permission requests

`MainActivity.checkAndRequestPermissions()` currently requests `INTERNET` and `RECEIVE_BOOT_COMPLETED` at runtime. Both are **normal** (install-time) permissions — requesting them does nothing and produces a confusing system dialog. Remove them from the runtime request; they stay in the manifest.

> Note: `INTERNET` is actually unnecessary for this app (no network I/O happens — calendar access is via content providers). It can be dropped entirely, but keeping it is harmless.

### 4.3 Permission UX

Consider the `ActivityResultContracts.RequestPermission` / `RequestMultiplePermissions` APIs instead of the deprecated `ActivityCompat.requestPermissions` callback flow, and add rationale text via `shouldShowRequestPermissionRationale()` (the `READ_CALL_LOG` prompt is unusual and benefits from explanation).

---

## 5. Phase 4 — Background scheduling (Android 8.0+ / API 26+)

### 5.1 AlarmManager reliability on modern Android

`setInexactRepeating` is heavily batched by Doze/App Standby on Android 6+, and on Android 12+ inexact repeating alarms are further constrained. The app's sync may silently stop. The **recommended replacement is `WorkManager`**:

- `androidx.work:work-ktx:2.11.2` (verify current version at implementation time).
- Replace the alarm + `SyncReceiver` with a `PeriodicWorkRequest` (e.g., 1-hour interval — WorkManager's minimum is 15 minutes, so 1h is fine):

```kotlin
val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.HOURS)
    .setConstraints(Constraints.Builder().setRequiresBatteryNotLow(false).build())
    .build()
WorkManager.getInstance(context)
    .enqueueUniquePeriodicWork("call-sync", ExistingPeriodicWorkPolicy.KEEP, syncRequest)
```

- `SyncWorker` (a `CoroutineWorker` or `Worker`) calls `CallsUtil().doSync(context)`.
- `BootReceiver` can be removed entirely (WorkManager reschedules after reboot automatically).
- The `RECEIVE_BOOT_COMPLETED` permission becomes unnecessary and can be removed from the manifest.

**Alternative (lower-effort):** keep `AlarmManager` and use `setAndAllowWhileIdle` with `AlarmManager.RTC_WAKEUP` — still less reliable than WorkManager on OEM-modified Android.

### 5.2 `FLAG_INCLUDE_STOPPED_PACKAGES`

Currently added to the alarm intent so the receiver fires even when the app is stopped. With WorkManager this flag is unnecessary. With AlarmManager it still works but is fragile; prefer WorkManager.

---

## 6. Phase 5 — Deprecated APIs & code cleanup

| Location | Deprecated | Replacement |
|---|---|---|
| `MainActivity.kt`, `CalendarUtil.kt`, `CallsUtil.kt` | `android.preference.PreferenceManager` (removed from platform docs; deprecated since API 29) | `androidx.preference.PreferenceManager` — add `androidx.preference:preference-ktx:1.2.1` |
| `MainActivity.kt` | `AlertDialog.Builder` from `android.app` | `androidx.appcompat.app.AlertDialog.Builder` (theme consistency) |
| `CalendarUtil.kt` | `Contacts.People.NAME` | `ContactsContract.Contacts.DISPLAY_NAME` |
| `CalendarUtil.kt` | Reflection into `ContactsContract$PhoneLookup` | Direct import of `ContactsContract.PhoneLookup.CONTENT_FILTER_URI` (public since API 11) |
| `CallsUtil.kt` | `android.provider.CallLog.Calls` | `android.provider.CallLog.Calls` is the same class — no change; only the `android.preference.*` imports need fixing |
| `MainActivity.kt` | `checkSelfPermission` + `ActivityCompat.requestPermissions` | `ContextCompat.checkSelfPermission` + `ActivityResultContracts` (preferred, see 4.3) |

> `android.provider.CallLog` remains the correct provider class for call log access; the deprecated one is `android.provider.Contacts.People`, not `CallLog`.

---

## 7. Phase 6 — Android 15/16 behavior changes (targetSdk 35/36)

Relevant items after checking the official behavior-changes docs:

1. **Edge-to-edge (forced since targetSdk 35):** the single-screen UI must not be hidden by system bars. Fix with `WindowCompat.setDecorFitsSystemWindows(window, false)` + `ViewCompat.setOnApplyWindowInsetsListener` padding, or use `EdgeToEdge` from `androidx.core`.
2. **Predictive back (targetSdk 36):** `onBackPressed` is no longer called. This app doesn't intercept back navigation → **no action needed**.
3. **Adaptive layouts (targetSdk 36):** orientation/resizability restrictions ignored on large screens. This app has none set → **no action needed**.
4. **Safer Intents, Local Network Permission, Health permissions:** not applicable to this app.
5. **16 KB memory page size (Android 15+):** only affects apps with native libraries. This app has none → **no action needed**.

---

## 8. Phase 7 — Verification & release

### 8.1 Build checks

```bash
./gradlew clean build          # assembles all variants, runs lint + unit tests
./gradlew lintDebug            # lint must pass without new errors
./gradlew testDebugUnitTest
```

- Confirm the wrapper resolves **Gradle 9.6+** and AGP **9.5.0** downloads from google()/mavenCentral() without jcenter.
- Lint will surface the `exported`, `PendingIntent`, and backup-rules issues if any were missed.

### 8.2 Device verification matrix

| Area | Test |
|---|---|
| Permissions | Fresh install → grant `READ_CALL_LOG`, then `READ_CONTACTS`; verify dialog order |
| Sync | Make calls, wait for next sync window; verify calendar event appears with correct contact name |
| WorkManager | Force-stop app; verify periodic sync still runs; verify reboot reschedules |
| Edge-to-edge | Verify UI content not obscured by status/navigation bars on API 35/36 devices |
| Backup | `adb shell bmgr allow_backup <pkg>`, `adb shell bmgr run_backup <pkg>`; verify prefs survive restore |
| Old devices | Smoke-test on an API 21 emulator if keeping minSdk 21 |

### 8.3 Release

- Bump `versionCode`/`versionName` (e.g., 2.0).
- Configure Play App Signing (if not already) — required for Play.
- Upload AAB (Play requires app bundles, not APKs).
- Play Console: target API 36 satisfies the 2026-08-31 requirement.

---

## 9. Risk & rollback notes

- **jcenter retirement:** already broken in the current state for any new dependency; this upgrade fixes it.
- **kotlin-android-extensions removal:** the plugin is gone in Kotlin 2.0; AGP 9 + built-in Kotlin makes this unavoidable. Code impact here is zero (no synthetic view bindings in the codebase).
- **WorkManager vs AlarmManager:** behavioral change (sync timing may shift slightly). Mitigation: keep the "last_sync" timestamp logic unchanged — it already makes sync idempotent regardless of timing.
- **READ_CALL_LOG / READ_CONTACTS split:** if a user grants only one, the app works with reduced functionality instead of crashing. Verify with the permission matrix above.
- **Rollback:** each phase is a self-contained commit; revert per-phase if a build or device test fails.

---

## 10. Suggested commit sequence

1. `build: upgrade Gradle wrapper to 9.6.0, AGP to 9.5.0, Kotlin to 2.2.10 (built-in), replace jcenter with mavenCentral`
2. `build: add namespace, migrate to version catalog, modernize SDK DSL (compileSdk/targetSdk 36), remove jetifier`
3. `manifest: add android:exported to components, add backup/data-extraction rules, remove package attribute`
4. `fix: specify PendingIntent FLAG_IMMUTABLE for sync alarm (API 31+ crash fix)`
5. `fix: separate READ_CALL_LOG and READ_CONTACTS runtime requests; stop requesting normal permissions at runtime`
6. `refactor: replace AlarmManager hourly alarm with WorkManager PeriodicWorkRequest; remove BootReceiver`
7. `refactor: migrate to androidx.preference, androidx AlertDialog, ContactsContract.PhoneLookup; remove reflection`
8. `ui: support edge-to-edge on targetSdk 36`
9. `release: bump version to 2.0`

Each commit should build cleanly (`./gradlew assembleDebug`) before the next begins.
