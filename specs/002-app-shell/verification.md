# Verification: Interface, navigation and appearance

**Date**: 2026-11-06
**Status**: Implemented; 15/15 tasks complete, final checks passed with limitations below.

## Scope and workflow
Specification from TODO/specify_interface_navegacao_app.md, then plan/research/design, 15 tasks,
read-only consistency analysis and implementation. 14 functional requirements mapped to tasks;
16/16 specification checklist items pass, no blocking clarification or constitutional findings.
Current user request supersedes the saved prompt's specify-only restriction.
Existing master and user changes in TODO/prototype were preserved; no commit/publication/version change.
The optional context extension failed detecting Python; its intended managed-block update was done directly.

## Build and automated checks
JDK21, SDK36, existing Wrapper/AGP9 built-in Kotlin:
```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:assembleDebugAndroidTest --max-workers=2 '-Pkotlin.compiler.execution.strategy=in-process' --console=plain
```
Final combined build/test: BUILD SUCCESSFUL in 3m29s, 145 tasks (40 executed); details below.
- Debug compilation/assembly passed with Navigation2.9.8, Lifecycle2.10.0, DataStore1.2.1.
- 5 JVM tests passed: persisted mode survives a new DataStore instance; unknown stored value falls
  back without overwriting; read-error recovery; write-error recovery preserves saved choice;
  selection blocked until read recovery.
- lintDebug: 0 errors, 16 warnings. Primarily retained version-update notices, existing missing icon/
  backup-rule warnings and a Portuguese spelling false-positive.
  No warning suppression. New dependency versions deliberately avoid Lifecycle2.11's AGP9.2 minimum.
- R8 minification/resource shrinking and intentionally unsigned release assembly passed.
- APK of Compose instrumentation tests compiled successfully.
- Initial instrumented-test compilation failed on an unnecessary import of an assertion member;
  corrected and rebuilt. No failed production compilation remains.

## Static checks
- Single launcher MainActivity, minSdk24, target36, no network/dangerous runtime permissions.
  AndroidX adds only its internal signature receiver permission.
- No Room/schema, domain persistence, backend, login, currency engine or signing material introduced.
- Appearance is the only DataStore preference; repository errors reach durable UiState and retry.
- Production root passes empty presentation; explicit fictitious samples are referenced only by previews/tests.
- All visible text in Portuguese resources, date localized pt-BR, pending count uses plural resources.
- Screen/content split, lifecycle-aware StateFlow, Hilt and serialized preference selection.
- Only the SPECKIT managed block of AGENTS.md was changed.

## Device checks
Pixel_5 API36 AVD launched headless, read-only, snapshots disabled; no persistent AVD changes.
First attempt with default GPU had System UI ANRs and later disconnected during large-font rotation.
First am start -W timed out, although subsequent screenshot showed Hoje; do not count that launch as
clean performance evidence. Retried with software rendering/two cores. These are host/emulator
limitations, not assumed passing scenarios or a claim of zero ANRs.
Connected Compose command: `:app:connectedDebugAndroidTest`.
Final runtime outcomes and evidence are recorded below.

## Limitations
No API24 image/physical device available; configured minimum SDK is not runtime coverage.
No auditory TalkBack validation. Semantics/labels can be inspected separately from actual spoken output.
No numeric currency/progression balancing, full task/habit/calendar behavior or domain CRUD is claimed.
Preview source is inspectable; Android Studio interactive preview rendering is not claimed without execution.

## Final automated result
The complete final source passed in one combined run:
```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease :app:connectedDebugAndroidTest --max-workers=2 '-Pkotlin.compiler.execution.strategy=in-process' --console=plain
```
BUILD SUCCESSFUL in 3m29s; 145 actionable tasks (40 executed, 105 up-to-date).
5/5 JVM tests and 6/6 connected Compose tests passed, zero failures/errors/skips.
Connected suite executed in 36.198s on API36. Final lint: 0 errors, 16 warnings
(13 pinned-version notices, existing icon/backup warnings and a Portuguese spelling false-positive).
The unused string was removed; numeric pending notices now have singular/plural resources.

Connected cases: all three appearances with Activity recreation/navigation; balance opens honest
rewards; ten repeated root taps and Back; populated type filtering/pending count; drawer Back/close
and review; independent filters restored across destinations. The initial drawer test assumed removal
from composition; Material retains hidden drawer content. Corrected assertion verifies invisibility,
passed isolated retry, then all six passed together on final code.

## Final manual and visual result
- All eight destinations checked through their distinct content: Hoje/Tarefas/Hábitos/Calendário,
  review/rewards/history/settings. No mock production progress or fake operation success.
- Offline fresh debug installation/launch passed: Status ok, COLD, TotalTime3898ms. Later explicit
  dark-mode force-stop/relaunch passed: Status ok, COLD, TotalTime7400ms. These timings describe this
  loaded emulator only and are not representative device benchmarks.
- System mode followed `cmd uimode night yes/no`: see [dark](evidence/system-dark.png) and
  [light](evidence/system-light.png), both showing system selection. Explicit dark survived reopening:
  [dark relaunch](evidence/dark-relaunch.png), with the device still light.
- Visual inspection found dark status icons on an explicit dark theme/device-light combination.
  Fixed Activity SystemBarStyle to follow effective app appearance; white icons now remain visible.
- Header now measures its content with safe top/horizontal insets rather than fixed height.
- Font200% settings passed visual inspection in [dark portrait](evidence/settings-dark-200.png),
  [dark landscape](evidence/settings-dark-landscape-200.png), [light portrait](evidence/settings-light-200.png)
  and [light landscape](evidence/settings-light-landscape-200.png). Landscape content scrolls to reach
  choices/information: [scrolled controls](evidence/settings-light-landscape-200-scrolled.png).
  Corresponding .txt evidence records exposed app labels and bounds, not a TalkBack audio check.
- Explicit preview source covers populated Hoje, review, rewards, history and list task/habit tabs;
  sample filter/banner behavior was executed by instrumentation. Studio preview rendering itself
  remains unverified, as stated above.
- UIAutomator sometimes captured a stale frame/null hierarchy during Android transitions; intermediate
  misleading captures were discarded and final retained images visually inspected. Repeated System UI
  ANRs/timeouts during early attempts remain recorded as environment limitations, not hidden failures.

## Final artifacts
| APK | Bytes | SHA256 |
|---|---:|---|
| app/build/outputs/apk/debug/app-debug.apk | 13828505 | C33B2ECD06D42BB3089A6809D698885EF960801785C557A08AD019038A29392F |
| app/build/outputs/apk/release/app-release-unsigned.apk | 1222301 | 23B6E87604B0A9CBEA35BD4A30F680F54BC101F9FECF83F0C38982705FCEB666 |

Reports: app/build/reports/tests/testDebugUnitTest/, app/build/reports/androidTests/connected/debug/,
app/build/reports/lint-results-debug.html; R8 mapping in app/build/outputs/mapping/release/.
No source changes followed the final successful combined build/test run.

Review's Tarefas empty filter manually passed; [zero review](evidence/review-empty.png).
Long history title remains fully readable at font200%: [history](evidence/history-light-200.png).
The read-only emulator was shut down after verification. All 15 tasks are marked complete.
