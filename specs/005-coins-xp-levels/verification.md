# Verification: Coins, XP and levels
**Date**: 2026-11-06
**Status**: Implemented;20/20 tasks complete; final results and coverage limits below.

## Scope and authorization
User requested specify using TODO/specify_moedas_xp_niveis_app.md, then plan/tasks/implement, and approved
clarification groups1A/2A/3A. Spec checklist16/16. Current branch retained; user later explicitly authorized git-commit and PC shutdown after completion.
No push/publication,
version increase, new dependency or runtime permission. Pre-existing TODO changes preserved.

## Workflow and analysis
Plan/research/data-model/UI contract/quickstart and20 tasks generated before production changes.
Read-only planning research agent was required by local speckit-plan skill. No implementation delegation.
Analysis:22 requirements/20 tasks,100% requirement coverage,0 blocking ambiguities,0 critical issues,
0 constitutional conflicts,0 unmapped tasks. Mapping is in tasks.md. SC001–007 map to validation.
No before/after tasks/analyze/implement hooks registered. Optional context hooks after specify/plan were
handled with the local context skill; script failed accessing python3.exe, so only the SPECKIT managed
block was updated directly. Nothing in hooks authorized Git publication.

## Implemented design
TaskDatabase3 adds activation, price, entitlement and immutable ledger tables with explicit migration2→3;
1→2 retained. Source and financial changes share existing Room transactions. Stable outcome entitlements,
first-entry price, linked original/effective reversals, no hidden debt, zero-impact incidents retained.
Baseline existing habit record IDs prevent moving/reclassifying old records into retroactive earnings or
charges; pre-existing outcomes stay unpriced. New retroactive records and old open tasks remain eligible.
Practice reward uses distinct days, with domain cap after target. Avoid/limit reuse existing civil periods
and deterministic date/entered/id ordering. Confirmed periods do not duplicate event penalties.
Unique practice-day date correction transfers original price/effective reversal lineage into the new day.
MISSED_DEADLINE remains a financial-zero audit; only explicit task failure charges.
Long checked arithmetic; BigInteger exact threshold comparisons and integer search without a level cap.
UI: real adaptive coin/level tiles, history surviving source deletion, nine-field prospective settings,
retained settings draft, task/habit estimates and explicit previews before financial mutations.
Recompensas remains unavailable for spending. Themes/navigation preserved; Portuguese resources.

## Intermediate checks and fixes
- First debug/JVM run passed before the final UI/expanded tests; not final coverage.
- Expanded intermediate JVM report:61 tests,0 failures.
- Lint found BigInteger.longValueExact requires API31. Replaced with toLong only after binary search
  proves the threshold is within the nonnegative Long XP total; no suppression/minSdk change.
- Reworded six newly introduced plural-candidate strings using explicit count labels.
- An intermediate compile overlapped ongoing date-correction edits and saw mismatched main/test signatures;
  not counted as a final code failure/pass. Subsequent builds run only on consolidated source.

## Final checks
Final debug/JVM/lint/R8 release and all21 Android tests passed; details follow.

## Coverage limits
API24 runtime, physical hardware and auditory TalkBack remain unverified. No exhaustive long-history
performance benchmark claimed. Emulator checks use existing Pixel_5 API36 AVD, temporary read-only,
headless/no snapshots/software graphics/two cores, temporary540x1170/density220.

Intermediate consolidated source: debug/JVM/lint/unsigned R8 release and Android test APK built successfully
(5m02s;148 actionable tasks;64 JVM tests). Lint0 errors/17 warnings; final plural label is corrected next.
First Android run:19/21 passed,2 UI test fixture failures (wrong cancel label and missing loading wait).
All5 new financial database/migration/rollback tests and all14 existing Android tests passed in that run.
UI fixtures corrected to use the actual cancel resource and wait for loaded settings before interaction.
Final source also appends a zero-delta linked entry when a penalized event changes actual date; refund
continues to reference the original effective charge. New regression test covers correction then refund.

## Final static/build result
Consolidated final production source passed:
```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:assembleDebugAndroidTest --console=plain --max-workers=2 '-Pkotlin.compiler.execution.strategy=in-process'
```
BUILD SUCCESSFUL in4m14s;148 actionable tasks(60 executed,88 up-to-date).
65 JVM tests,0 failures; lint0 errors/16 pre-existing warnings,0 new warnings and no suppressions.
Debug, unsigned R8/resource-shrunk release and instrumentation APK packaged. No production change afterward.
Reports: app/build/reports/tests/testDebugUnitTest/, app/build/reports/lint-results-debug.html.

| Artifact | SHA256 |
| --- | --- |
| app/build/outputs/apk/debug/app-debug.apk | 274076D3EBBC0DFFB7790745EDCBB4384760313B24267667EAAE06C35C599372 |
| app/build/outputs/apk/release/app-release-unsigned.apk | FC4472D7EF6619A508454019A3F25C5A34B77AF2FF790997130B0E8DCA7BAACA |
| app/schemas/com.ethos.app.core.tasks.data.TaskDatabase/3.json | 7712E2CB4AF24789DA63A6FD7EFA1DE2B00A93E9F32597535448010D75FECF60 |
## Final Android result
```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain --max-workers=2 '-Pkotlin.compiler.execution.strategy=in-process'
```
BUILD SUCCESSFUL in2m41s;77 actionable tasks(1 executed,76 up-to-date).
21/21 tests passed,0 failures/skips on API36. Includes5 accounting database/migration/rollback tests,
2 real-repository financial UI tests and14 existing task/habit/navigation/appearance regressions.
Reports: app/build/reports/androidTests/connected/debug/.
No production or test source changed after these final passing checks.

## Manual visual/runtime matrix
The connected-test driver uninstalled the app on completion. Installed the final debug APK again for
manual inspection. Initial launch encountered Pixel Launcher and System UI ANR dialogs in the AVD;
dismissed each with Wait. These host/emulator incidents are not counted as a clean first launch.
Subsequent app launch/force-stop/relaunch worked. Device date was2026-11-07 (API36), not altered;
domain/integration tests use controlled dates. Wi-Fi/mobile data were disabled during manual checks.

- [Initial wallet, light](evidence/today-light.png): zero coins, zero XP and level1.
- Entered fictitious Conferir carteira through the real task form with default Média/Média; no date,
  so it correctly remained in Inbox rather than Hoje. Editor showed current+10 coins/+20 XP estimate.
- [Explicit financial confirmation](evidence/confirmation.png): original reason/date, actual+10/+20,
  resulting10 coins/20 XP visible before confirming. Saved through the normal UI, with no synthetic ledger.
- [Persisted after offline process restart](evidence/persisted-light.png):10 coins,20 XP,level1,
 20/100 within-level progress. The approved side-by-side compact tiles remain at normal fonts.
- [Wallet, dark200%](evidence/today-dark-200.png): adaptive stacked cards with legible values/progress.
- [History, dark portrait200%](evidence/history-dark-200.png): source, actual date, delta, balance and entry time.
- [History, dark landscape200%](evidence/history-dark-landscape-200.png),
  [scrolled to amounts/time](evidence/history-dark-landscape-200-scrolled.png) and
  [light landscape200%](evidence/history-light-landscape-200.png): content reachable by internal scrolling.
- [Table fields, light200%](evidence/settings-light-200.png),
  [final fields/actions, light200%](evidence/settings-light-200-bottom.png),
  [final actions, dark landscape200%](evidence/settings-dark-landscape-200-bottom.png): all category fields
  and Save/Back reachable by scrolling; values remain intact after theme/rotation changes. No table edits
  were committed during manual inspection. Automated UI tests separately verified invalid input and saving0.

XML captures accompany each image and correspond to stable app screens, replacing initial ANR/launcher
captures. Screenshots contain only newly entered fictitious data in the temporary read-only AVD.
This matrix covers wallet/history/table and normal-font task confirmation; it is not an exhaustive
large-font dialog/long-title matrix and is not auditory TalkBack coverage.

## Completion
65 JVM and21 Android tests passed; debug/lint/unsigned release passed, schema3 exported and preserving
1→2→3 migration validated.20/20 tasks complete. No source changes followed final validation.
Commits were explicitly requested after implementation; push/publication/version bumps remain unauthorized.
The temporary read-only emulator is shut down after evidence capture. No persistent AVD preference changes.