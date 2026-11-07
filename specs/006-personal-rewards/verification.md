# Verification: Personal rewards
**Date**: 2026-11-07
**Status**: Implemented;18/18 tasks complete. Final build,71 JVM and34 Android tests passed; coverage limits below.

## Scope and workflow
User explicitly authorized specify using TODO/specify_recompensas_app.md, then plan/tasks/implement.
Current authorization overrides saved prompt's older specify-only instruction. Analyze performed before code.
Spec checklist16/16.18 tasks cover18 FR and6 SC. Analysis found0 constitutional conflicts,0 blockers,
0 uncovered requirements or unmapped tasks. Plan/research/model/contracts/quickstart/tasks created first.
One read-only research agent was required by local plan skill; no implementation delegation.
Optional context hooks after specify/plan disclosed; managed plan reference updated directly per plan skill.
No before/after tasks/analyze/implement hooks registered. No commits/push/publication/version changes.
Pre-existing untracked planning/prompt files retained.

## Implemented design
Three schema4 tables: reward definitions, immutable purchase snapshots and durable confirmation intents.
Schema3→4 creates tables/index only; schema1/2/3 retained. Existing Room transactions and ledger reused.
PURCHASE entries have stable intent key,full negative coin cost,zero XP and unchanged totalXP. No earning
entitlement or second wallet. Reconciliation preserves unrelated purchase entries. No debt from purchases.
Soft-delete keeps snapshots/history; per-reward quota counts immutable purchases within local civil periods.
Edits count existing records; unlimited items have no invented quota. Positive integer checked input.
One persisted UUID per confirmation; receipt checked first on retry, before changed/deleted definition.
Compare disclosed expected preview with persisted intent AND current database state; stale refresh requires
new valid confirmation, preventing rapid second taps using old rendered price/balance/allowance.
Pending/committed unacknowledged intent recovers on cold reopen. Cancel/ack closes intent,never refunds.
Editor draft retained through SavedStateHandle; write errors retain draft/intent and allow safe retry.
Existing Recompensas route and coin history extended; Portuguese resources/current theme/navigation.

## Intermediate checks
First domain/build JVM run passed before final UI and expanded tests (not final evidence).
An intermediate full build overlapped a deliberate confirm contract refinement and saw old main signature
against new tests; failed compilation, not counted as final verification. Consolidated rerun pending.

## Final evidence
Final build/JVM/lint/R8/instrumentation and visual evidence follows; intermediate failures are retained separately.

## Coverage limits
API24 runtime,physical hardware and auditory TalkBack not yet validated. Existing feature evidence is not
proof of this feature. No exhaustive large-history performance benchmark claimed.
Emulator is Pixel_5/API36,temporary read-only,headless,no snapshots,software graphics,two cores; temporary
540x1170,density220. No personal data used. Build required escalation to access installed JDK/SDK/cache;
authorized execution was accepted. No source or feature scope affected by that environment restriction.

## Consolidated static/build evidence
Production source passed debug/JVM/lint/unsigned R8 release and instrumentation packaging:
```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:assembleDebugAndroidTest --console=plain --max-workers=2 '-Pkotlin.compiler.execution.strategy=in-process'
```
BUILD SUCCESSFUL in9m19s;148 actionable tasks(66 executed,82 up-to-date).
71 JVM tests,0 failures/errors. Lint0 errors/16 pre-existing warnings,0 new warnings,0 suppressions.
No production source changes after this successful build. Tests were expanded afterward (habit correction,
real purchase UI and correct existing Voltar resource), so final Android/lint evidence follows separately.
Reports: app/build/reports/tests/testDebugUnitTest/ and app/build/reports/lint-results-debug.html.
Initial manual capture was blocked by Bluetooth crash and System UI ANR in the AVD. Disabled Bluetooth in
this temporary read-only instance and chose Wait for System UI. Subsequent real store opened successfully.
[Empty store, light](evidence/store-empty-light.png) and matching XML verify actual zero balance,no samples,
existing navigation and reachable create action. These host incidents are not counted as app failures or a
clean initial launch. Mobile-data disable reported missing phone service; Wi-Fi disable succeeded.

| Artifact | SHA256 |
| --- | --- |
| app/build/outputs/apk/debug/app-debug.apk | 3478000E86D7420CEF096BBBFAB2C516D627561CA236D2F8BB8A69ED3022F935 |
| app/build/outputs/apk/release/app-release-unsigned.apk | 525A111D7F9FC5AA5A66DA89FFB7726580D47F4784655F0E823712E757E6F255 |
| app/schemas/com.ethos.app.core.tasks.data.TaskDatabase/4.json | 495234658375FC218E65FFE22B38E73D6C470210ABB1514917D9E809294EBB1F |

## Intermediate Android run
First consolidated Android run reached the new habit-correction regression fixture and rejected a weekly
practice created on Saturday without an explicit partial-period target (existing HabitProblem.PARTIAL_REQUIRED).
This was invalid test setup,not a rewards production defect. Fixture changed to Monday2026-11-02; no
production source changed. Final full rerun required before reporting Android success.
The first Android attempt was interrupted at test33 when the AVD became unresponsive,including adb read
commands.32 completed test results existed; habit fixture was the one product-independent failure, and the
running pre-existing task UI test was aborted by stopping only this session's temporary AVD process.
This is not a complete Android pass. Restarted read-only AVD with1536MB and Vulkan disabled; final rerun below.

## Final Android and lint result
```powershell
.\gradlew.bat :app:connectedDebugAndroidTest :app:lintDebug --console=plain --max-workers=2 '-Pkotlin.compiler.execution.strategy=in-process'
```
BUILD SUCCESSFUL in6m33s;86 actionable tasks(10 executed,76 up-to-date).
34/34 Android tests passed,0 failures/errors/skips on Pixel_5/API36. Includes9 reward database tests
(atomicity,concurrency,quotas,3→4 preservation,cold intent/receipt recovery,task/habit correction),4 reward
UI tests (error/retry,200% confirmation,cancel/receipt,real repository purchase),and21 existing regressions.
Existing migration fixtures now validate preserving1→4 and2→4 chains.71 JVM tests remain passing.
Final lint0 errors/16 pre-existing warnings,0 new warnings. No production or test source changes followed.
Reports: app/build/reports/androidTests/connected/debug/ and app/build/reports/lint-results-debug.html.
Retry AVD boot44s with Vulkan disabled/1536MB; all tests including previously interrupted task UI passed.
Temporary emulator resource/display changes do not modify persistent AVD configuration.

## Manual final visual/runtime matrix
Final debug APK reinstalled after connected-test uninstall; all sample values were entered through normal
UI in the disposable AVD. No production seeds or direct balance/database manipulation. A System UI Wait
prompt appeared again before manual inspection; dismissed. Subsequent app interactions/restart succeeded.
- [Real light confirmation](evidence/purchase-confirmation-light.png): fictitious task earned10 coins;
  reward Pausa ficticia cost5 with daily limit2. Preview disclosed resulting5 and allowance AFTER=1.
- [Dark200% confirmation](evidence/purchase-confirmation-dark-200.png): same persisted intent survived
  font/theme changes,scrollable explanation and reachable Confirmar compra/Voltar. Confirmed once.
- [Dark200% receipt](evidence/purchase-receipt-dark-200.png): immediate use,stable reference and acknowledgement;
  no cancel/refund/use-later actions. Store shows5 coins and current usage1/limit2.
- [Dark200% store](evidence/store-dark-200.png): card/price/quota and buy visible; edit/order/delete reachable
  by scrolling. [Editor portrait](evidence/editor-dark-200.png) and
  [editor landscape](evidence/editor-dark-landscape-200.png): input area scrolls,Save/Back reachable; dismissed
  without editing. This is not an exhaustive every-field/long-title/keyboard landscape matrix.
- [Dark200% history](evidence/history-dark-200.png) and
  [scrolled reference](evidence/history-dark-200-scrolled.png): original name,actual date/time,-5 coins,XP0,
  resulting balance5,icon and durable reference. [Light landscape200%](evidence/history-light-landscape-200.png)
  preserves theme/readability and scrolling.
- [After force-stop/relaunch,light landscape200%](evidence/persisted-light-landscape-200.png): retained wallet;
  Room cold-reopen tests separately validate original purchase snapshot,pending intent and unacknowledged receipt.
Matching XML accompanies each screenshot. Some initial dumps during recreation returned null root and stale
XML; affected final captures were taken again after stable render. No personal data in captures.

## Final limits and completion
API24 runtime,physical device and auditory TalkBack remain unverified. Actions have textual labels and
scroll/enlarged-font checks above; no claim of complete screen-reader or exhaustive font/landscape coverage.
No exhaustive history performance benchmark or OS kill at every instruction boundary; rollback,retry and
cold reopen tested. No new permissions/dependencies/version/publication/Git mutations. Concurrent unrelated
TODO planning notes were left untouched.18/18 tasks complete; spec/plan/tasks/analyze/implement completed.
Final review: git diff --check passed;18/18 task boxes complete; AGENTS.md outside SPECKIT block unchanged.
Temporary AVD shut down after final captures. No source/test changes after final passing checks.
