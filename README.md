# SHADOW BODY

Personal AI-assisted physical self-improvement Android system:
fitness, adaptive training, morning activation, nutrition, hydration,
progression, grooming, wardrobe, body tracking, and local-first coaching —
presented as a personal physical-development operating system.

**Current phase: Phase 5 — Morning Activation** (morning routine logs, step outcomes, custom routines, Room v5 with explicit migration).

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
├── ui/workout/                # Training hall, plan editor/detail, active session, result
├── ui/adaptive/               # AdaptiveViewModel + AdaptiveScreen
├── domain/model/              # SystemModule / ModuleState (pure Kotlin)
├── domain/validation/         # ProfileValidator, BaselineValidator, WorkoutValidator, AdaptiveValidator
├── domain/adaptive/           # WorkoutAdaptationEngine, WorkoutGenerator, AdaptationLimits, reasons
├── domain/workout/            # RestTimer (lifecycle-aware rest clock)
└── data/
    ├── local/                 # ShadowBodyDatabase (v4), entities, DAOs, Migrations
    ├── repository/            # Profile, Baseline, Exercise, Plan, Session, Readiness, Adaptation, Recommendation
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
| Room | 2.6.1 | DB v4, KSP, explicit MIGRATION_1_2 + 2_3 + 3_4 |
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

## Testing status (Phase 5)

- Local unit tests: **231/231 pass** — routes, dashboard contract, Room, migrations, Phase 5 Morning Activation domain/validation/repository/seeds/views.
- Instrumented tests: **Passing** — MorningFlowTest, Migration4To5Test, and full UI flows.
- Total: **231/231 unit tests, 0 failures, 0 errors**.
- `Medium_Phone_API_36.1` AVD is unusable: its system image download is
  missing `system.img` (pre-existing environment issue, unrelated to the app).
- Host RAM is tight (16 GB): the emulator must be stopped before Kotlin
  compilation and restarted before `connectedDebugAndroidTest`, otherwise the
  QEMU process is killed mid-run by memory pressure.

## Roadmap

- [x] Phase 0 — Environment + read-only audit
- [x] Phase 1 — Android Foundation
- [x] Phase 2 — User Profile + Body Baseline
- [x] Phase 3 — Workout Engine
- [x] Phase 4 — Adaptive Workouts
- [x] Phase 5 — Morning Activation (this build)
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

## Phase 3 architecture

- `data/local/`: `Exercise` (28 seeded bodyweight/gym movements, unique name,
  muscle group, equipment, difficulty, instructions), `WorkoutPlan` +
  `WorkoutPlanExercise` (ordered slots: sets, reps-or-duration, rest seconds),
  `WorkoutSession` + `SessionExercise` + `SessionSet` (recorded actuals).
  Deleting a plan keeps history (`planId` → `SET NULL`); exercise rows are
  protected by `RESTRICT`; `session_exercise`/`session_set` cascade from the
  session; `(sessionId, position)` and `(sessionExerciseId, setNumber)` are
  unique so ordering is deterministic.
- Starting a session is one `@Transaction`: plan slots are snapshotted into
  session rows, so later plan edits never rewrite recorded history.
- `data/repository/`: `ExerciseRepository` (duplicate-safe `INSERT OR IGNORE`
  seeding on both migration and first use), `PlanRepository` (create/update,
  reorder, delete), `SessionRepository` (start, log set, toggle exercise,
  finish, abandon, delete, history).
- `domain/validation/WorkoutValidator`: plan name/duration, per-slot
  sets/reps/time/rest bounds, and set logs. A set cannot be completed until at
  least one rep or second is recorded, so history never claims unearned work.
- `domain/workout/RestTimer`: coroutine-owned ticking with start, pause, resume,
  reset (stop/cancel) and +30 s, driven by `StateFlow` and cleared on dispose.
- UI: `WorkoutListScreen` (plans + recent history), `PlanEditorScreen`
  (add/reorder/remove slots, exercise search), `PlanDetailScreen` (targets,
  equipment warnings, start), `ActiveWorkoutScreen` (set logging, progress,
  rest panel), `WorkoutResultScreen` (real totals from stored rows).
- Room v3: explicit `MIGRATION_2_3` creates the six workout tables and seeds the
  exercise library while preserving v2 profile/baseline data. No destructive
  fallback, ever.

## Phase 4 architecture

- `data/local/`: `ReadinessReport` (append-only self check-in: fatigue 1–5,
  soreness 1–5, optional note), `ExerciseAdaptation` (per-exercise current
  target, progression state, last reason code + text), `AdaptationCheckpoint`
  (single row, the last applied completed session), `WorkoutRecommendation`
  (generated session header + status) and `RecommendedExercise` (frozen target
  and reason snapshot). History is protected: recommendations and misses
  `SET NULL` when their plan is deleted, `recommended_exercise` cascades with its
  recommendation, and `exercise_adaptation` is `RESTRICT`ed so a tracked
  exercise cannot vanish. `(exerciseId)` and `(recommendationId, position)` are
  unique, so tracking and ordering are deterministic.
- `domain/adaptive/WorkoutAdaptationEngine`: pure, deterministic, explainable.
  Progresses only after `SESSIONS_TO_PROGRESS = 2` clean sessions, regresses on
  completion ratio < 0.6 or rep ratio < 0.7, and never exceeds the safety
  envelope in `AdaptationLimits` (sets 1–6, reps 5–30 in steps of 2, duration
  15–600 s in 30 s steps, rest 30–180 s). Every decision carries a stable
  `AdaptationReason` code and a sentence the user can read.
- `domain/adaptive/WorkoutGenerator`: picks the plan, filters the library by
  profile equipment, applies adaptation state, plans rotation, and a weekly
  target, then trims to the requested session minutes (reps estimated at
  4 s/rep plus 30 s per exercise) with a hard 6-exercise cap. If no profile
  exists it falls back to the bodyweight library rather than inventing data.
- `AdaptationRepository`: folds completed sessions into adaptation state exactly
  once. Sessions are processed in order after the checkpoint, readiness is read
  at or before the session's end, and the session's own target snapshot is the
  source of truth — so a manual plan edit resets the streak instead of being
  overwritten by stale targets. The checkpoint is advanced with a
  compare-and-set, so re-running is idempotent. Completed sessions are read,
  never rewritten.
- `RecommendationRepository` + `AdaptiveWorkoutPlanner`: generation, adopt
  (snapshot → real `WorkoutPlan`), skip (explicit `MissedWorkout`), dismiss.
  A recommendation is immutable: later history never rewrites what the user was
  told to do.
- `domain/validation/AdaptiveValidator`: readiness ranges and note length, with
  per-field errors rendered under the offending control.
- UI: `AdaptiveScreen` (readiness check-in, generate, per-target explanation,
  adopt / skip / dismiss, back to dashboard) and `AdaptiveViewModel`; the
  dashboard module opens it and the ADAPTIVE module is unlocked.
- Room v4: explicit additive `MIGRATION_3_4` creates the five adaptive tables
  and preserves every Phase 3 row. No destructive fallback, ever.

## Known limitations (Phase 4)

- Adaptation is rep-based and schedule-based only. It reads completed-session
  reps, completion and the user's own readiness ratings; it does not infer
  anything from time under tension, RPE or biometrics, because the app does not
  collect them.
- A "missed workout" is only what the user explicitly marks as skipped. The app
  never decides on its own that a session was missed.
- Generated workouts come from the seeded bodyweight/gym library and the user's
  own plans. There is no exercise creation or editing yet.
- Dashboard stats are honest placeholders; modules still show SEALED until their
  phase, and the workout/adaptive modules show real counts only.
- Rest timing is manual: the rest panel offers 30/60/90 s presets and +30 s
  rather than auto-starting each exercise's configured rest value.
- The exercise browser is a searchable picker inside the plan editor; there is
  no standalone library screen yet.
- `org.gradle.java.home` in `gradle.properties` covers Gradle daemons, but the
  wrapper launcher still starts on the shell JDK — set `JAVA_HOME` to a
  JDK 17/21 for the build shell (see Build above).
- No release signing yet (debug only); release hardening is Phase 11.
