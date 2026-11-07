# Verification: Habits and goals

**Date**: 2026-11-06
**Status**: Implemented; 21/21 tasks complete; final separated checks passed with the limits below.

## Scope and workflow
Specify used TODO/specify_habitos_metas_app.md; user approved future type/period changes,
active-date-only recording/corrections, explicit missing-list restore destination, daily avoid review
and reopening changed reviewed outcomes. Checklist16/16; plan/research/model/UI contract/quickstart;
21 dependency-ordered tasks and consistency analysis of16 FR and7 SC before implementation.
All requirements mapped, no blocking ambiguity or constitutional conflict. Research delegation was
explicitly requested by the local planning skill; implementation remains local and sequential.
No branch change, commit, push, publication, version bump, financial engine or new permission.
Pre-existing user TODO changes were preserved.

## Implemented design
Normalized Room habits/rules/intervals/overrides/records/reviews/audits extend TaskDatabase2;
exported schemas1/2 and explicit migration1->2. Same list identity and atomic list exclusion,
without erasing habit records. Pure civil-date evaluation; future-effective historical rules,
explicit partial targets, event versus distinct-day counts, reviewed corrections and streak uncertainty.
Immutable lifecycle-aware state, saved editor draft, retained write-retry action, Portuguese resources,
calendar/progress/detail, pause/resume/exclude/restore and real lazy Hoje/list/review integration.
Hábitos excluídos is in Hábitos; Histórico de moedas retains its approved honest unavailable state.
No seeded habit data, currency/XP/ledger, reminders, timers, backend or dependency/version changes.

## Intermediate executed checks
- First11 habit rule tests plus28 existing JVM tests passed.
- Added2 ViewModel failure/retry/manual-importance/draft tests; passed.
- Expanded rules to17 cases; total47 JVM tests passed before final integration validation.
- Initial debug assembly and lint passed; initial lint had2 new plural-candidate warnings, corrected
  by count-label phrasing without suppressions.
- First14 Android tests:13 passed,1 failed in the migration test fixture's JSON reader BEFORE migration.
  Exported schema omits notNull for nullable fields; reader now uses optBoolean(false).
- Android tests already passing in that first round included transactional list exclusion/rollback,
  habit capture/preview/record/correction, excluded history and existing task/shell regressions.
- Final source was refined after intermediate builds; only the final checks below establish final coverage.

## Final checks
Final debug/JVM/lint/release passed; final isolated Android retry passed. Visual evidence and exact results follow below.

## Coverage limits
API24, physical hardware and auditory TalkBack unverified. Android checks use the existing Pixel_5
API36 AVD, headless/read-only/no snapshots, software graphics/two cores and temporary540x1170/density220.
Controlled clocks exercise calendar boundaries without changing the device clock.
No throughput benchmark or exhaustive long-history/large-label matrix is claimed.

## Manual and visual evidence on final calendar source
- Offline install/cold start on the disposable AVD. Initial System UI ANR was dismissed with Wait;
  this is a host/emulator limitation, not counted as a clean first launch. Subsequent relaunches worked.
- Created fictitious Leitura teste through the real UI, inspected the explicit progress confirmation,
  saved, force-stopped/reopened and found the same persisted habit. Recorded practice and saw streak1.
  ADB input required a second preview tap after the keyboard finished closing; the first tap did not
  open confirmation and was not counted as success.
- [Habit area, light](evidence/habits-light.png), [editor, light](evidence/editor-light.png),
  [explicit preview](evidence/preview.png), [persisted habit after process restart](evidence/reopened.png).
- [Final calendar, dark200%](evidence/detail-dark-200.png) and
  [record controls after scrolling](evidence/detail-dark-200-scrolled.png).
  Initial200% inspection revealed wrapped day numbers and cramped month controls; final code scales
  calendar cell widths with font scale and stacks month controls. Horizontal scroll exposes all7 columns.
  The deliberately retained before-calendar-fix image documents the corrected issue.
- Editor200% inspected in [light portrait](evidence/editor-light-200.png),
  [dark portrait](evidence/editor-dark-200.png), [light landscape](evidence/editor-light-landscape-200.png),
  [dark landscape](evidence/editor-dark-landscape-200.png), and
  [lower fields after scrolling](evidence/editor-dark-200-scrolled.png).
  Back/preview remain visible; difficulty/importance/color/icon/list fields are reachable by scrolling.
  The editor remains open across theme/rotation changes.
- One UI hierarchy dump returned a null root during theme/font recreation. Repeated after recreation
  and replaced the stale capture; final XML files correspond to stable captures.
- Device-local date shown by this AVD was2026-11-07; no device clock change was made. Rule tests use
  explicit controlled dates. Screenshots contain test data only; nothing was seeded in production code.

This visual matrix covers main habit/editor/detail flows, not every goal/lifecycle dialog or arbitrary
long title. XML labels/bounds were inspected; this does not constitute auditory TalkBack coverage.
## Final validation environment interruption
The combined rerun after the calendar/font fix was interrupted by the instrumentation driver after
6 passing Android tests and0 assertion failures: ADB Connection reset/Connection refused, followed by
Netty cleanup errors in the host test driver. adb devices still listed the running AVD afterward.
This interrupted run is not counted as14 passing tests. Retrying build/release first and connected
checks separately avoids overlapping emulator instrumentation with release processing.
## Final static/build result
Final calendar source passed the separated command:
```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease --console=plain --max-workers=2 '-Pkotlin.compiler.execution.strategy=in-process'
```
BUILD SUCCESSFUL in37s;115 actionable tasks(7 executed,108 up-to-date).47 JVM tests,0 failures;
lint0 errors/16 pre-existing warnings, no suppressions; debug and unsigned R8/resource-shrunk release.
No source change followed this passing build. Connected retry result follows below.

| Final artifact | SHA256 |
|---|---|
| app/build/outputs/apk/debug/app-debug.apk | CBA401804C8F988B0415FE040D422D2D321C8A434AFE5D5F211B01CA49898BE3 |
| app/build/outputs/apk/release/app-release-unsigned.apk | 29A79DB6F0CADD5ACEC7C2FD6E4A088E3E0C573DDBCE031EE95E3613EA103BC3 |
| app/schemas/com.ethos.app.core.tasks.data.TaskDatabase/2.json | B716BC8F3A3C5CBF87D82A09984A0E84DC9FA13DD4CCB585104790F3E6B0850A |

Reports: app/build/reports/tests/testDebugUnitTest/, app/build/reports/lint-results-debug.html,
app/build/reports/androidTests/connected/debug/. Release is intentionally unsigned; debug is installable.
Optional context hooks exist after specify/plan; managed context updated directly within SPECKIT only.
No before/after tasks/analyze/implement hooks were registered; no hook authorized commits/publication.
## Final connected result
```powershell
.\gradlew.bat :app:connectedDebugAndroidTest --console=plain --max-workers=2 '-Pkotlin.compiler.execution.strategy=in-process'
```
BUILD SUCCESSFUL in2m43s;77 actionable tasks(1 executed,76 up-to-date).
14/14 Android tests,0 failures/errors/skips on API36. This isolated run used the final calendar source
and succeeded after the interrupted combined run. Coverage:2 habit database/migration/atomicity tests,
2 habit UI tests,2 existing task database tests,2 task UI tests and6 shell/appearance tests.

Final result:47 JVM +14 Android passed, debug/lint/unsigned release passed, schema2 exported and
migration validated. No code changes followed these checks.21/21 tasks complete. Only documentation
and the managed AGENTS.md SPECKIT block were finalized afterward. No commits/push/publication.
The temporary read-only AVD was shut down after validation; no persistent AVD preferences were changed.
