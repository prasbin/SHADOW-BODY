# SHADOW BODY

Personal AI-assisted physical self-improvement Android system:
fitness, adaptive training, morning activation, nutrition, hydration,
progression, grooming, wardrobe, body tracking, and local-first coaching —
presented as a personal physical-development operating system.

**Release: v1.0.0 — MVP** (complete Phase 1-10 feature set, offline-first, deterministic local logic).

## UI direction

SHADOW BODY uses an original dark futuristic interface inspired by the
fictional Solo Leveling "System" aesthetic. It does not copy copyrighted
characters, artwork, logos, screenshots, or proprietary assets.

The app presents as a personal transformation system: SYSTEM → STATUS → TODAY → OBJECTIVES → ACTIONS → PROGRESS. Development phases are internal project-management information and are not exposed in the user-facing UI.

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
| Room | 2.6.1 | DB v7, KSP, explicit MIGRATION_1_2 + 2_3 + 3_4 + 4_5 + 5_6 + 6_7 |
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

## Testing status (Phase 10)

- Local unit tests: **284/284 pass** — routes, dashboard contract, Room, migrations, Phase 5 Morning Activation, Phase 6 Nutrition, Phase 7 Progression, Phase 8 Grooming, Phase 9 Wardrobe/Outfit, Phase 10 Coach engine.
- Instrumented tests: **Passing** — MorningFlowTest, Migration4To5Test, Migration5To6Test, Migration6To7Test, Migration7To8Test, Migration8To9Test, NutritionFlowTest, ProgressionFlowTest, and full UI flows.
- Total: **284/284 unit tests, 0 failures, 0 errors**.
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
- [x] Phase 5 — Morning Activation
- [x] Phase 6 — Nutrition MVP
- [x] Phase 7 — Progression System MVP
- [x] Phase 8 — Grooming MVP
- [x] Phase 9 — Wardrobe + Outfit MVP
- [x] Phase 10 — AI Body Coach MVP (this build)
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

## Phase 5 architecture

- `data/local/`: `MorningRoutine`, `MorningRoutineStep`, `MorningRoutineLog`, `MorningRoutineStepLog` — daily routine runs with per-step outcomes (COMPLETED/SKIPPED). `MorningRoutineSeeds` seeds the built-in routine with `INSERT OR IGNORE` on unique `seedKey` and `(routineId, position)`.
- `domain/morning/`: `MorningCompletionRule` (day is COMPLETED if all steps are dealt with, ABANDONED if only skipped, NOT_STARTED otherwise), `MorningDayKey` (ISO local date), `MorningTimer` (pause/resume/extend countdown).
- `domain/validation/MorningRoutineValidator`: step title/instructions required, duration/reps bounds, category from enum.
- `data/repository/`: `MorningRoutineRepository` (routines + steps), `MorningActivationRepository` (run lifecycle, logs, day state).
- UI: `MorningActivationScreen` (routine list, day status, start/resume, history), `MorningRunScreen` (step-by-step with complete/skip/timer), `MorningRoutineEditorScreen` (create/edit routines, reorder steps).
- Room v5: explicit additive `MIGRATION_4_5` creates the four morning tables and seeds the built-in routine. Preserves all Phase 1-4 data.

## Phase 6 architecture

- `data/local/`: `NutritionGoal` (single-row daily targets: calories, protein, carbs, fat, hydration), `FoodLog` (per-meal: name, macros, calories, serving, notes, dayKey, timestamp), `HydrationLog` (per-drink: amountMl, dayKey, timestamp). Indexes on `dayKey` and `loggedAt` for fast daily aggregation and history.
- `domain/validation/NutritionValidator`: food log (name required, calories ≥0, macros ≥0, serving required), goal (calories 500–10000, macros 0–1000, hydration 0–10000). Non-medical bounds only.
- `data/repository/NutritionRepository`: combines goals, food logs, hydration logs into `DailyNutritionSummary` with deterministic totals (calories, protein, carbs, fat, hydration) and remaining-vs-target where goal exists. `getAllLoggedDays()` for history navigation.
- UI: `NutritionScreen` (dashboard module: today's totals, macro cards, food list with delete, hydration list with delete, FABs for add food/water, goal edit dialog), `NutritionViewModel` (dialog state, validation, repository actions).
- Dashboard integration: Phase 6 "Nutrition" module is OPEN with navigation to `NutritionScreen`.
- Room v6: explicit additive `MIGRATION_5_6` creates the three nutrition tables. Preserves all Phase 1-5 data. No seeding — user configures their own goals.

## Phase 7 architecture

- `data/local/`: `XpTransaction` (immutable XP awards with source, sourceRef for idempotency, dayKey, timestamp), `Attribute` (Strength, Endurance, Discipline, Recovery, Nutrition — single row), `Streak` (current/longest streak, lastActiveDayKey), `Achievement` (definitions + unlock state). Unique index on `XpTransaction(source, sourceRef)` prevents duplicate awards.
- `domain/progression/`: `ProgressionEngine` — pure deterministic logic for level (floor(totalXp/100)+1), attribute increments per source (WORKOUT→Str/End, MORNING→Disc/Rec, MEAL→Nut, HYD→Rec/Nut), streak calculation (calendar days, one per day max), achievement evaluation (7 built-in: First Step, Workout Initiate, Morning Awakened, Nutrition Logged, Hydration Habit, Week Warrior, XP 100).
- `data/repository/ProgressionRepository`: XP award with duplicate protection via unique sourceRef, `processProgression()` recomputes attributes/streak/achievements from transaction log.
- Integration: SessionRepository.finish() → +50 XP (WORKOUT), MorningActivationRepository.finish() → +30 XP (MORNING_ACTIVATION), NutritionRepository.addFood() → +10 XP (MEAL), NutritionRepository.addHydration() → +5 XP (HYDRATION).
- UI: `ProgressionScreen` (dashboard module: level/XP progress bar, attribute cards, streak cards, achievement list with unlock state, recent XP transaction history), `ProgressionViewModel` (Flow-based summary, refresh trigger).
- Dashboard integration: Phase 7 "Progression" module is OPEN with navigation to `ProgressionScreen`.
- Room v7: explicit additive `MIGRATION_6_7` creates the four progression tables and seeds achievement definitions. Preserves all Phase 1-6 data.

## Phase 8 architecture

- `data/local/`: `GroomingPreferences` (singleton row: frequency, preferred routine), `GroomingRoutine` (seedKey, name, description, isActive, sortOrder), `GroomingRoutineStep` (routineId, title, instructions, category, targetDurationSec, position, isEnabled, isSeeded), `GroomingLog` (routineId, routineName, dayKey, attempt, startedAt, completedAt, status, completedSteps, skippedSteps, totalSteps, notes), `GroomingStepLog` (logId, stepId, position, title, category, targetDurationSec, outcome, elapsedSec, recordedAt). Unique indexes on `(routineId, position)` and `(routineId, dayKey, attempt)`.
- `domain/grooming/`: `GroomingDayKey` (ISO date utility), `GroomingStepCategory` (HAIR, SKIN, ORAL_CARE, FACE, BODY, NAILS, HYGIENE, OTHER), `GroomingCompletionRule` (pure logic for run state transitions).
- `data/repository/GroomingRepository`: `startRun()` creates a log + step logs from the active routine, `recordStep()` updates outcome/elapsed, `finish()` marks complete, `abandon()` marks abandoned, `ensureSeeded()` inserts the built-in "Daily Essentials" routine on first launch.
- UI: `GroomingScreen` (dashboard module: day status card, routine list, recent history), `GroomingRunScreen` (step-by-step run with complete/skip buttons), `GroomingRoutineEditorScreen` (create/edit routines with steps), `GroomingViewModel` + `GroomingRunViewModel` + `GroomingRoutineEditorViewModel`.
- Dashboard integration: Phase 8 "Grooming" module is OPEN with navigation to `GroomingScreen`.
- Room v8: explicit additive `MIGRATION_7_8` creates the five grooming tables and seeds the built-in routine. Preserves all Phase 1-7 data.

## Phase 9 architecture

- `data/local/`: `WardrobeItem` (name, category, clothingType, color, secondaryColor, style, season, occasion, fit, isEnabled, notes, photoPath, timestamps), `OutfitRecord` (name, topItemId, bottomItemId, footwearItemId, accessoryItemId, occasion, season, explanation, createdAt). Indexes on `category`, `isEnabled`, and `createdAt`.
- `domain/wardrobe/`: `OutfitGenerator` — deterministic outfit selection by category, occasion, and season. Selects first matching item from each category (TOP, BOTTOM, FOOTWEAR, ACCESSORY). Reports missing categories when inventory is incomplete.
- `domain/model/`: `WardrobeCategory` (TOP, BOTTOM, FOOTWEAR, ACCESSORY), `WardrobeSeason` (SPRING, SUMMER, AUTUMN, WINTER, ALL_SEASON), `WardrobeOccasion` (CASUAL, WORK, SPORT, FORMAL, PARTY, OUTDOOR).
- `data/repository/WardrobeRepository`: CRUD operations, filtering by category, search by name/type/color, enable/disable items.
- `data/repository/OutfitRepository`: Save/delete outfit records, observe recent history.
- UI: `WardrobeScreen` (inventory list with search/filter), `WardrobeItemEditorScreen` (add/edit items), `OutfitGeneratorScreen` (parameter selection, generation, save), `WardrobeViewModel` + `OutfitViewModel`.
- Dashboard integration: Phase 9 "Wardrobe" module is OPEN with navigation to `WardrobeScreen`.
- Room v9: explicit additive `MIGRATION_8_9` creates the two wardrobe tables. Preserves all Phase 1-8 data.
- Photo support: optional `photoPath` field stores a local file path only. No image analysis, computer vision, or AI recognition.

## Phase 10 architecture

- `domain/coach/`: `CoachEngine` interface (provider abstraction), `LocalDeterministicCoachEngine` (deterministic local implementation), `CoachProfile` (input data), `CoachRecommendation` (output with reason, category, priority, action), `CoachSummary` (sorted recommendations).
- `ui/coach/`: `CoachScreen` (System-style dashboard with top priority, all recommendations, tap-to-navigate), `CoachViewModel` (combines data from all existing repositories into CoachProfile, calls engine).
- Deterministic rules: profile setup, high fatigue/soreness → recovery, training day without workout → workout, low hydration → hydration, morning/grooming not started → reminders, wardrobe items → outfit suggestion, missed workouts → consistency, streak → encouragement.
- No database migration required — Coach operates entirely from existing Phase 1-9 data.
- No external AI, cloud, API keys, or internet required.
- Safety: no medical diagnosis, no injury diagnosis, no medication prescriptions, no mental health claims. Conservative recommendations when data indicates limitations.

## Known limitations (Phase 10)

- No social/competitive features — purely single-player progression.
- No dynamic XP scaling — fixed rewards per activity type.
- Achievements are predefined; no custom/user-created achievements.
- Streak uses calendar day keys (ISO date) — assumes device timezone is consistent.
- Attribute values are abstract game stats, not biomechanical measurements.
- No prestige/rebirth system or level cap.
- XP transaction history is read-only — no manual adjustment UI.

## Release v1.0.0 — MVP

### Feature Summary

| Module | Status | Description |
|--------|--------|-------------|
| Foundation | Complete | App launch, navigation, dark theme, offline-first |
| Profile + Baseline | Complete | User profile, body measurements, fitness level |
| Workout Engine | Complete | Exercise library, plans, sessions, sets, rest timer |
| Adaptive Workouts | Complete | Readiness-based recommendations, exercise adaptation |
| Morning Activation | Complete | Daily routines, step tracking, history |
| Nutrition | Complete | Food/hydration logging, daily goals, history |
| Progression | Complete | XP, levels, attributes, streaks, achievements |
| Grooming | Complete | Routines, steps, daily logs, history |
| Wardrobe + Outfits | Complete | Clothing inventory, deterministic outfit generation |
| Body Coach | Complete | Deterministic recommendations, transparent reasoning |

### Build

```bash
./gradlew.bat assembleDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

### Testing

```bash
./gradlew.bat testDebugUnitTest
```

### Installation

1. Enable USB debugging on device
2. `adb install app/build/outputs/apk/debug/app-debug.apk`
3. Launch SHADOW BODY

### Release Checklist

- [x] App launches successfully
- [x] Navigation works across all modules
- [x] Dark theme consistent
- [x] Profile + baseline functional
- [x] Workout engine functional
- [x] Adaptive workouts functional
- [x] Morning activation functional
- [x] Nutrition functional
- [x] Progression functional
- [x] Grooming functional
- [x] Wardrobe + outfits functional
- [x] Body Coach functional
- [x] 284/284 tests passing
- [x] All migrations verified
- [x] Build successful
- [x] Offline operation verified
- [x] No unnecessary permissions
- [x] No debug logging
- [x] No secrets or API keys
- [x] GitHub backup verified

### Technology Stack

- Kotlin 2.0.21
- Jetpack Compose (BOM 2024.09.00)
- Room 2.6.1 (schema v9)
- Navigation Compose 2.7.7
- DataStore Preferences 1.1.1
- Material 3
- compileSdk 36, minSdk 26, targetSdk 36

### Safety Boundaries

- No medical diagnosis or treatment
- No injury diagnosis
- No medication prescriptions
- No mental health claims
- No biometric data collection
- No cloud synchronization
- No account/login required
- No network permissions requested

### Known Limitations

- Real-device verification pending (Redmi Note 14 5G)
- No cloud backup or sync
- No social features
- No advanced AI/cloud agents
- No computer vision or image analysis
- No release signing configuration (debug only)
- `org.gradle.java.home` in `gradle.properties` covers Gradle daemons, but the
  wrapper launcher still starts on the shell JDK — set `JAVA_HOME` to a
  JDK 17/21 for the build shell (see Build above).
- No release signing yet (debug only); release hardening is Phase 11.
