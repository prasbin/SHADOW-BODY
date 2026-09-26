# SHADOW BODY

Personal AI-assisted physical self-improvement Android system:
fitness, adaptive training, morning activation, nutrition, hydration,
progression, grooming, wardrobe, body tracking, and local-first coaching —
presented as a personal physical-development operating system.

**Current phase: Phase 2 — User Profile + Body Baseline** (profile create/edit,
baseline records + history, Room v2 with explicit migration).

## UI direction

SHADOW BODY uses an original dark futuristic interface inspired by the
fictional Solo Leveling "System" aesthetic. It does not copy copyrighted
characters, artwork, logos, screenshots, or proprietary assets.

## Architecture

Practical MVVM + repository-oriented structure, single `:app` module:

```
app/src/main/java/com/shadowbody/app/
├── MainActivity.kt            # Compose entry point
├── ShadowBodyApp.kt           # Application: owns DB + preferences
├── navigation/                # Routes (single source of truth) + NavHost
├── ui/theme/                  # Colors, type, spacing, ShadowBodyTheme
├── ui/components/             # SystemPanel, StatCard, SectionHeader
├── ui/dashboard/              # DashboardScreen + DashboardViewModel
├── ui/settings/               # SettingsScreen + SettingsViewModel
├── domain/model/              # SystemModule / ModuleState (pure Kotlin)
└── data/
    ├── local/                 # ShadowBodyDatabase, SchemaAnchor(+Dao)
    └── preferences/           # AppPreferences (DataStore wrapper)
```

- UI state via `StateFlow`; navigation via Navigation Compose.
- Room is the store for all future user data. DataStore holds **app-level
  preferences only** (theme), never profile/body data.
- Migrations are explicit: `fallbackToDestructiveMigration` is never used.
  Room schemas are exported to `app/schemas/` and committed.

## Technology

| Component | Version | Notes |
|---|---|---|
| Gradle | 8.7 | wrapper, cached dist |
| Android Gradle Plugin | 8.6.1 | |
| Kotlin | 2.0.21 | + Compose compiler plugin 2.0.21 |
| JDK for builds | 21.0.9 (Android Studio JBR) | project-local via `org.gradle.java.home`; global `JAVA_HOME` untouched |
| compileSdk / targetSdk | 36 | platform android-36 + build-tools 36.x installed |
| minSdk | 26 | |
| applicationId | `com.shadowbody.app` | version `0.1.0-phase1` |
| Compose BOM | 2024.09.00 | Material3, Navigation 2.7.7 |
| Room | 2.6.1 | DB v2, KSP, explicit MIGRATION_1_2 |
| DataStore | 1.1.1 | Preferences |
| Robolectric | 4.13 | local JVM tests for Room/DataStore |

## Build

Prerequisites: Android SDK with platform android-36, build-tools 36.x,
and a JDK 17/21 (Android Studio's bundled JBR works; the project points
Gradle at it via `gradle.properties`).

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
.\gradlew.bat testDebugUnitTest    # local unit tests
.\gradlew.bat connectedDebugAndroidTest  # needs a booted emulator, e.g. CE_Test
```

Boot an emulator via its full SDK path, e.g.
`Sdk\emulator\emulator.exe -avd CE_Test`.

## Offline-first

Core functionality is local-first: Room + DataStore on-device, no backend,
no auth, no mandatory network. The manifest requests no network permission.
The app launches and works fully offline.

## Safety

SHADOW BODY is a fitness/wellness application. It does not diagnose disease,
injuries, deficiencies, or hormonal status; does not prescribe medication;
and does not claim medical certainty. Recommendations are assistance, not
medical authority — consult a qualified professional where appropriate.

## Testing status (Phase 2)

- Local unit tests: **42/42 pass** — routes (3), dashboard contract (4),
  dashboard profile state (1), Room incl. anchor round-trip (3), DataStore (2),
  repositories incl. reopen persistence (4), converters (6), profile
  validation (11), baseline validation (8).
- Instrumented tests: **4/4 pass** on emulator `CE_Test` — dashboard launch,
  settings navigation, migration v1→v2 (anchor preserved, new tables work),
  full profile+baseline UI flow (create → persist → baseline → history →
  edit → activity restart still persisted).
- Total: **46/46, 0 failures, 0 errors**.
- `Medium_Phone_API_36.1` AVD is unusable: its system image download is
  missing `system.img` (pre-existing environment issue, unrelated to the app).

## Roadmap

- [x] Phase 0 — Environment + read-only audit
- [x] Phase 1 — Android Foundation
- [x] Phase 2 — User Profile + Body Baseline (this build)
- [ ] Phase 2 — User Profile + Body Baseline
- [ ] Phase 3 — Workout Engine
- [ ] Phase 4 — Adaptive Workouts
- [ ] Phase 5 — Morning Activation
- [ ] Phase 6 — Nutrition MVP
- [ ] Phase 7 — Progression System MVP
- [ ] Phase 8 — Grooming MVP
- [ ] Phase 9 — Wardrobe + Outfit MVP
- [ ] Phase 10 — AI Body Coach MVP (local-first, provider abstraction)
- [ ] Phase 11 — Optimization + real-device release (Redmi Note 14 5G)

## Planning estimates (not guaranteed deadlines)

- ~6–12 weeks for a solid first complete version under consistent development.
- ~3–5 months if advanced AI, camera analysis, extensive real-device testing,
  optimization, polishing, and iteration are included.

## Phase 2 architecture

- `data/local/`: `UserProfile` (single row, id=1: age, height, weight,
  fitness level, equipment/goal/day sets, session minutes),
  `BaselineRecord` (timestamped snapshots, all measurements optional),
  `Converters` (CSV sets, unknown tokens dropped), DAOs, `Migrations`.
- `domain/validation/`: `ProfileValidator` (age 10–100, height 100–250 cm,
  weight 25–350 kg, NONE-exclusive equipment, required picks) and
  `BaselineValidator` (girths 20–250 cm, body fat 1–70%, ≥1 measurement).
- `data/repository/`: `ProfileRepository`, `BaselineRepository` (Flow).
- UI: `ProfileScreen` (empty state honest), `ProfileEditScreen` (chips +
  numeric fields), `BaselineHistoryScreen` (+ input dialog). Dashboard shows
  PROFILE NOT CONFIGURED vs ONLINE and opens the profile flow.
- Room v2: explicit `MIGRATION_1_2` creates both tables; anchor row verified
  intact post-migration. No destructive fallback, ever.

## Known limitations (Phase 2)

- Dashboard stats are honest placeholders; modules show SEALED until their phase.
- `org.gradle.java.home` in `gradle.properties` covers Gradle daemons, but the
  wrapper launcher still starts on the shell JDK — set `JAVA_HOME` to a
  JDK 17/21 for the build shell (see Build above).
- No release signing yet (debug only); release hardening is Phase 11.
