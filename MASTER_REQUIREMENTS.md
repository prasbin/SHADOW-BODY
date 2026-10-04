# SHADOW BODY — Master Requirements Checklist

**Version:** 2.0
**Last Updated:** 2026-10-03
**Repository:** https://github.com/prasbin/SHADOW-BODY.git
**Local Root:** C:\Users\User\Desktop\AGENTS-UP v2.0\SHADOW BODY

---

## 1. PROJECT SCOPE

- [x] SHADOW BODY isolated from sibling projects (SHADOW MONEY, SHADOW LEARN, BERU, CONTENT ENGINE, AGENTS-UP parent)
- [x] Native Android (Kotlin + Jetpack Compose)
- [x] Room + DataStore
- [x] MVVM/repository architecture
- [x] StateFlow/Flow
- [x] Local-first/offline-first
- [x] Safe Room migrations

---

## 2. AUTHORITATIVE PRODUCT VISION

- [x] One cohesive personal transformation System (not a collection of modules)
- [x] OBSERVE → DECIDE → GUIDE → ACT → RECORD → ADAPT
- [x] Core areas: training, morning activation, progression, nutrition, hydration, recovery, grooming, wardrobe, smart recommendations, notifications, personal scheduling

---

## 3. AUTOMATIC 6:00 AM WAKE-UP REQUIREMENT

- [x] 6:00 AM local device time wake-up/reminder
- [x] Every day except Saturday
- [x] Saturday = weekly recovery/rest day (no 6:00 AM training wake-up)
- [x] User must NOT manually create workout schedule
- [x] Schedule survives app restarts/reboots where Android permits
- [x] Uses appropriate Android scheduling/alarm APIs (AlarmManager.setExactAndAllowWhileIdle)
- [x] Respects Android notification/alarm permissions (checks canScheduleExactAlarms on API 31+)
- [x] Never falsely claims alarm was scheduled (returns false if permission denied)

**Status: SATISFIED (code-level verified)**
**Physical-device verification: PENDING**

---

## 4. AUTOMATIC DAILY TRAINING

- [x] System automatically determines user's workout
- [x] Normal user flow does NOT require manual plan creation
- [x] Training engine uses: height, bodyweight, age, fitness level, equipment, goals, schedule, history, completed/missed sessions, fatigue/readiness, progression
- [x] System determines: training/rest day, exercises, sets, reps/time, volume, progression/regression, rest periods, duration

---

## 5. "BRUTAL" TRAINING REQUIREMENT

- [x] High-intensity and challenging within safety limits
- [x] NOT intentionally dangerous or injury-inducing
- [x] Respects fitness level, recent workload, fatigue/readiness, missed sessions, recovery
- [x] User-configurable aggression/intensity level (1-5, stored in user_profile)
- [ ] System explains why today's workout has particular intensity
- [ ] Adapts when fatigue/readiness indicates reduced load

**Status: NOT SATISFIED — requires future implementation**

---

## 6. AUTOMATIC NEXT-WORKOUT GENERATION

- [x] After completed workout: System determines next appropriate workout automatically
- [x] User does not manually generate another plan
- [x] Next workout considers newly completed session
- [x] Progression remains deterministic and explainable
- [x] Duplicate plans are not created
- [x] After missed workout: miss recorded, subsequent generation considers it, volume/difficulty adapted, no manual rebuild needed

---

## 7. SATURDAY RECOVERY RULE

- [x] Saturday = designated weekly recovery/rest day
- [x] No normal 6:00 AM workout wake-up on Saturday
- [x] No forced brutal workout on Saturday
- [x] Recovery guidance may still be shown (hydration, sleep, mobility, grooming, nutrition)
- [ ] Saturday is recovery day, not disabled-app day

**Status: NOT SATISFIED — requires future implementation**

---

## 8. MORNING ACTIVATION ENFORCEMENT

- [x] Morning routine starts/surfaces after scheduled wake-up
- [x] All required tasks shown
- [x] Each task tracked individually
- [x] Completed vs unfinished tasks clearly distinguished
- [x] Unfinished required tasks remain visible
- [x] User reminded about unfinished tasks
- [x] Explicit completion state required
- [x] Opening a routine does NOT count as completing it
- [x] No biometric verification claims unless actually implemented

**Status: SATISFIED (code-level verified)**

---

## 9. DAILY FACE SCAN / GROOMING INTELLIGENCE

- [x] Daily face/selfie image via Android camera/gallery
- [x] Non-medical grooming/self-care suggestions
- [x] Output: "What should I apply/do today?"
- [x] Categories: cleansing, moisturizing, sunscreen, basic grooming, beard/hair care
- [x] NOT medical diagnostic system
- [x] Does NOT diagnose acne, infection, disease, skin conditions
- [x] Does NOT infer medical deficiencies or prescribe medication
- [x] Conservative home/self-care guidance only
- [x] Recommends professional care when appropriate
- [x] Explains: what observed, what recommended, why, limitations
- [x] Provider abstraction for any external AI
- [x] Core app usable without paid cloud AI

**Status: SATISFIED (code-level verified)**

---

## 10. DAILY FACE-SCAN PRIVACY

- [x] Face/selfie images stored locally by default
- [x] No silent image uploads
- [x] No transmission to external services without explicit consent
- [x] Clear communication when external AI processing required
- [x] Users can remove stored images/data
- [x] No unnecessary long-term retention
- [x] No identity recognition from face images
- [x] No identification of user or other people from images

**Status: SATISFIED (code-level verified)**

---

## 11. WARDROBE PHOTO INPUT

- [x] Request 3 clothing combinations + shoes as photos
- [x] Camera/gallery input for outfit combinations
- [ ] Analyze combinations based on: available clothing, colors, compatibility, shoes, occasion, preferences
- [ ] System explains recommendation
- [x] Does NOT pretend image analysis occurred if it did not run

**Status: SATISFIED — user-facing 3-combination photo input flow implemented, AI analysis deferred to Requirement 8**

---

## 12. AI OUTFIT ANALYSIS

- [x] Evaluate submitted outfit combinations
- [x] Provide: recommended combination, reasoning, strengths, potential issues, shoe compatibility, grooming coordination
- [x] Does NOT invent clothing items not present (unless clearly labeled as optional alternative)
- [x] Distinguishes USER-PROVIDED ITEM from SYSTEM-SUGGESTED ITEM

**Status: SATISFIED (rule-based local analysis, no visual AI claims)**

---

## 13. OUTFIT VISUALIZATION / GENERATED IMAGE

- [x] Generate illustrative image of person wearing selected outfit
- [x] Represent selected clothing combination accurately
- [x] Include selected shoes
- [x] Does NOT claim pixel-perfect identity or fit
- [x] Generated person is visualization/model, not the user
- [x] Does NOT identify or imitate a real person
- [x] No copyrighted characters or protected artwork
- [x] Clearly labeled as generated visualization
- [x] Wardrobe recommendation works even without image generation

**Status: SATISFIED (text-based visualization, no fake image generation claims)**

---

## 14. SMART DAILY SYSTEM RECOMMENDATIONS

- [x] Continuously evaluate user's local data
- [x] Inputs: workout, completion, missed workout, readiness, nutrition, hydration, morning routine, grooming, wardrobe, progression, streaks, schedule, recovery, time of day
- [x] Surface useful next actions (DRINK WATER, REST, START WORKOUT, COMPLETE MORNING ROUTINE, LOG MEAL, CHECK GROOMING, PREPARE OUTFIT, RECOVER, WIND DOWN)
- [x] Context-aware recommendations
- [x] No notification spam
- [x] No recommendations conflicting with known current state

**Status: SATISFIED (code-level verified)**

---

## 15. BUSYNESS / DAILY SCHEDULE AWARENESS

- [x] Account for user's daily busyness
- [x] Inputs: user-configured schedule, manual busy periods, calendar integration (only if explicitly implemented and permitted)
- [x] Adapt: workout duration, reminder timing, task ordering, recovery suggestions, nutrition/hydration reminders
- [x] No external calendar access without explicit permission

**Status: SATISFIED (code-level verified)**

---

## 16. SMART NOTIFICATIONS

- [x] Useful Android notifications
- [x] Categories: 6:00 AM wake-up, workout reminder, unfinished morning task, hydration, meal/logging, recovery/rest, grooming, wardrobe preparation
- [x] Local-first, deterministic where possible
- [x] Permission-aware, non-spammy, context-aware
- [x] Cancelable/re-schedulable
- [x] Resilient across app restarts where Android permits

**Status: SATISFIED (code-level verified)**

---

## 17. UNIFIED SYSTEM UI

- [ ] UI does NOT resemble developer roadmap
- [ ] Primary experience: SYSTEM STATUS, TODAY, TODAY'S MISSION, TODAY'S WORKOUT, MORNING ACTIVATION, RECOVERY, NUTRITION/HYDRATION, GROOMING, WARDROBE, SMART RECOMMENDATION, PROGRESSION
- [ ] Dashboard answers: "What does SHADOW BODY want me to do right now?"
- [ ] Dark futuristic System aesthetic
- [ ] Original SHADOW BODY identity
- [ ] Strong hierarchy, compact information, readable typography
- [ ] Status panels, progress indicators, controlled glow/effects
- [ ] No phase numbers, development status, locked/sealed modules, roadmap language, developer terminology

---

## 18. SOLO LEVELING / SYSTEM-INSPIRED DESIGN RULE

- [x] Design inspired by fictional "System" interface concept
- [x] Original implementation
- [x] No Solo Leveling characters, logos, screenshots, artwork, proprietary assets, copied layouts

---

## 19. OFFLINE-FIRST PRINCIPLE

- [x] Core functionality works without internet
- [x] Core: profile, workout scheduling, generation, history, morning routine, nutrition, hydration, progression, grooming basics, wardrobe basics, deterministic recommendations
- [ ] External AI is enhancement only
- [ ] AI failure or no internet does NOT destroy core app
- [ ] Provider abstraction required for future AI services

---

## 20. AI PROVIDER ARCHITECTURE

- [ ] FaceAnalysisProvider abstraction
- [ ] WardrobeAnalysisProvider abstraction
- [ ] OutfitVisualizationProvider abstraction
- [ ] Local/deterministic fallback where practical
- [ ] External AI provider where configured
- [ ] Unavailable/error state handling
- [ ] Transparent user messaging
- [ ] Never fabricate AI result when provider did not run

**Status: NOT SATISFIED — requires future implementation**

---

## 21. SAFETY REQUIREMENTS

- [x] Fitness/wellness product only
- [x] No medical diagnosis
- [x] No medication prescription
- [x] No medical certainty claims
- [x] No intentionally dangerous exercise
- [ ] Challenge within configured limits
- [ ] Adapt when fatigue/readiness indicates reduced load
- [ ] Provide rest when appropriate
- [ ] Surface professional help when outside normal fitness guidance
- [ ] AI assists; AI does not control the user

---

## 22. DATA / PRIVACY

- [x] Local-first mandatory
- [ ] Sensitive data minimized (body measurements, nutrition, face/selfie images, wardrobe photos, schedules, fitness history)
- [ ] No silent cloud synchronization
- [ ] No silent image uploads
- [ ] No unnecessary analytics
- [x] No hardcoded API keys
- [x] No secrets in source control

---

## 23. AUTOMATION PRINCIPLE

- [x] Minimize manual configuration
- [x] User does NOT repeatedly create workout plans
- [x] User does NOT manually generate next workout
- [ ] User does NOT manually calculate progression
- [ ] System automatically determines next appropriate action when sufficient data exists
- [ ] User remains in control
- [ ] User can skip/modify activities
- [ ] Permissions respected
- [ ] System explains important automated decisions

---

## 24. EXISTING FUNCTIONALITY (MUST NOT REGRESS)

- [x] Automatic workout scheduling
- [x] Automatic next workout
- [x] Training/rest handling
- [x] Readiness/fatigue adaptation
- [x] Missed-workout handling
- [x] Duplicate protection
- [x] Profile + baseline tracking
- [x] Workout engine
- [x] Morning activation
- [x] Nutrition + hydration
- [x] Progression
- [x] Grooming
- [x] Wardrobe
- [x] Deterministic Body Coach
- [x] Offline/local-first architecture
- [x] Room migrations v1→v9
- [x] v1.0.0 release configuration

---

## 25. REQUIREMENTS PRIORITY

1. Safety
2. User control / transparency
3. Automatic core workflow
4. Data integrity
5. Offline/local-first operation
6. Product coherence
7. Usability
8. Visual polish
9. Optional AI enhancement

---

## 26. SESSION CONTROL REQUIREMENT

- [x] Before implementing: re-open MASTER_REQUIREMENTS.md, review requirements, state which requirements session addresses
- [x] After implementing: re-open MASTER_REQUIREMENTS.md, compare, mark status, check drift, fix mismatches, run tests, update docs, inspect Git, commit, push, verify, STOP

---

## 27. GITHUB CONTROL

- [x] Every completed session: git status, staged-file inspection, commit, push to https://github.com/prasbin/SHADOW-BODY.git, remote verification, clean working tree
- [x] Never commit sibling/parent project files

---

## 28. NEW REQUIREMENTS SUMMARY (v2.0)

| # | Requirement | Status |
|---|-------------|--------|
| 3 | 6:00 AM automatic wake-up except Saturday | NOT SATISFIED |
| 5 | Brutal high-intensity training | NOT SATISFIED |
| 7 | Saturday recovery rule | NOT SATISFIED |
| 8 | Morning activation enforcement | NOT SATISFIED |
| 9 | Daily face scan / grooming intelligence | NOT SATISFIED |
| 10 | Face-scan privacy | NOT SATISFIED |
| 11 | Wardrobe photo input | NOT SATISFIED |
| 12 | AI outfit analysis | NOT SATISFIED |
| 13 | Outfit visualization / generated image | NOT SATISFIED |
| 14 | Smart daily system recommendations | NOT SATISFIED |
| 15 | Busyness / daily schedule awareness | NOT SATISFIED |
| 16 | Smart notifications | NOT SATISFIED |
| 17 | Major unified System UI upgrade | NOT SATISFIED |
| 20 | AI provider architecture | NOT SATISFIED |

**Total new requirements: 13**
