# Verification: Lists, tasks and recurrence

**Date**: 2026-11-06
**Status**: Implemented;20/20 tasks complete. Final checks passed with coverage limitations below.

## Workflow and scope
Specify from TODO/specify_listas_tarefas_app.md; three clarification groups explicitly approved;
16/16 quality checklist; technical plan/research/model/contracts/quickstart;20 tasks; read-only
analysis before implementation.23 functional requirements mapped to tasks, no critical findings.
No branch change, commit, push, version change, publication or production signing.
Existing user TODO changes (including concurrently added planning documents) were preserved.
Optional context hook was surfaced. Context script failed accessing python3.exe; only AGENTS.md
SPECKIT managed block was updated directly. No before/after tasks/analyze/implement hooks registered.

## Implemented architecture and boundaries
Room2.8.5 normalized lists/definitions/occurrences/checklists/audit records, exported schema1,
transactional mutations and stable root+ordinal occurrence identities. No destructive fallback.
No prior task database existed; no migration from a fictitious previous schema is claimed.
Future schema changes require migration and preservation tests. Appearance DataStore retained.
Pure civil-date/recurrence/correction rules, TaskRepository contract, Hilt ViewModel StateFlow,
lifecycle-aware Screen/content split and foreground refresh. java.time desugaring2.1.5 supports
configured API24/25, but minimum-version runtime is unverified.

All task operations are offline. No accounts, new runtime permissions, personal logs, habit engine,
wallet/XP values, balancing formulas, notification/background delivery or timers were introduced.
Soft deletion retains identity/audit provenance; no financial reversals or rewards are fabricated.
Aggregation inside transactions is an owner-scale simplicity tradeoff, not a high-volume benchmark.
Recurrences materialize through today plus a next occurrence; calendar only displays materialized
work and explicit dated tasks. No complete future-calendar projection is claimed.

## Intermediate checks and corrected failures
- Initial JVM/domain round:16 tests passed.
- Expanded suite exposed a future-series switch issue: older unresolved fixed occurrences blocked
  after-completion successors. Frontier-aware generation/resolution corrected it and its regression passed.
-26 JVM tests passed after correction, including settings/preferences regression coverage.
-10 Android tests passed on API36:2 Room persistence/rollback,2 task Compose flows and6 shell/theme tests.
- An earlier full debug/JVM/lint/R8/test-APK build passed in9m10s; later source changes require final rerun.
- Lint after resource cleanup:0 errors,16 warnings, no new warning suppressions.
- Regression cases also cover bounded lookahead, single exceptions preserving intervening dates,
  finite deleted slots, scoped future edits, monthly leap dates and dependent undo/correction identity.

## Runtime environment and limitations
Existing Pixel_5 API36 AVD started headless, read-only, snapshots disabled, software rendering/two cores.
First am start-W timed out under parallel build load. Screenshot initial-launch.png shows a System UI
ANR, not a clean launch. This is recorded as a host/emulator limitation, not silently counted as pass.
Chose Wait, reduced temporary resolution1080x2340->540x1170 and density440->220 to retain phone layout
while reducing rendering work; disabled animations on this disposable AVD. Changes disappear with
read-only session termination. after-emulator-adjustment.png shows stable Hoje; editor-light.png
shows readable editor and reachable actions. These intermediate screenshots precede final source.
No API24 image or physical-device execution available. No auditory TalkBack verification.
Controlled JVM dates verify boundaries without modifying device clock.

## Final results
Final source passed the combined command:
```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:connectedDebugAndroidTest --max-workers=2 '-Pkotlin.compiler.execution.strategy=in-process' --console=plain
```
BUILD SUCCESSFUL in5m38s;149 actionable tasks(58 executed,91 up-to-date).
-28/28 JVM tests,0 failures/errors:12 recurrence,6 organization/completion/date rules,5 task
 ViewModel failure/confirmation/retry tests and5 existing preference/settings tests.
-10/10 Android tests,0 failures/errors/skips,63.51s on API36:2 Room,2 task Compose and6 shell/theme.
-Debug assembly, exported Room schema1, lintDebug, release R8/resource shrinking and unsigned release passed.
-Lint:0 errors,16 warnings:13 existing pinned-version notices, Portuguese spelling false-positive,
 existing icon and backup-rule warnings. Newly introduced plural/unused-resource/state-boxing findings fixed.
-No Kotlin/build source changes followed this successful combined run; subsequent edits are documentation/evidence.

### Final manual checks
After the connected runner removed its packages, reinstalled the final debug APK on the disposable AVD.
Offline cold launch:Status ok,5564ms. Force-stop/relaunch with saved work:Status ok,3792ms.
Dark/200% cold launch:Status ok,4464ms. Timings describe this loaded emulator, not device benchmarks.
Airplane mode was confirmed as1 at the end.

Created a fictitious list/task through real UI/Room; typed task title was inspected before Save.
The first ADB text-injection sequence omitted a character in the list label; the helper's expected-label
lookup stopped, inspection found the actual label, and continuation used that observed label rather
than claiming the failed lookup passed. No duplicate list was created.
After force-stop, the undated task stayed out of Hoje, remained in its list with identical title,
and moved to Inbox successfully. Completed it, used Desfazer registro, moved it back into the list,
then confirmed list/content deletion. Both task/list disappeared; no fake wallet value was introduced.

Final visual evidence inspected:
- [Hoje normal/light](evidence/today-final-light.png).
- [Saved task after process restart](evidence/task-after-process-restart.png).
- [Hoje dark200%](evidence/today-dark-200.png).
- Editor200% [dark portrait](evidence/editor-dark-200.png),
 [dark landscape](evidence/editor-dark-landscape-200.png),
 [light portrait](evidence/editor-light-200.png),
 [light landscape](evidence/editor-light-landscape-200.png).
- [Lower editor controls after scrolling](evidence/editor-light-landscape-200-scrolled.png):
 checklist/repetition reached while Salvar/Voltar remain visible. Rotation retained the open draft.
- Corresponding XML hierarchies retain labels/bounds; this is not an auditory TalkBack check.

Visual/manual matrix focuses on Hoje/editor and actual list/task workflow, not every possible long
user label or every screen combination. API24, physical hardware, auditory TalkBack and Android Studio
interactive preview rendering remain unverified. No complete future calendar projection or financial
engine is claimed. Initial System UI ANR/launch timeout remain recorded above.

### Final artifacts
| Artifact | SHA256 |
|---|---|
| app/build/outputs/apk/debug/app-debug.apk | 97581AE07C4C86872E96D4529BF32FCA6DBC6C97F408589E02D189ADC93A5E70 |
| app/build/outputs/apk/release/app-release-unsigned.apk | 6C82F98EACA08A4462EDE52A4D84DC0A755906A6884C1292747F22F5713F8439 |

Schema:app/schemas/com.ethos.app.core.tasks.data.TaskDatabase/1.json,identity11941dfc54dccc89df0ffbac4cf34b89.
Reports:app/build/reports/tests/testDebugUnitTest/,app/build/reports/androidTests/connected/debug/,
app/build/reports/lint-results-debug.html;R8 mapping in app/build/outputs/mapping/release/.
The optimized release is intentionally unsigned; the debug APK is installable.
