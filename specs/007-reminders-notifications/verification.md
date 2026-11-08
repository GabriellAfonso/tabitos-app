# Verification: Lembretes e notificações

**Date**: 2026-11-07
**Status**: Implemented and verified (20/20 tasks); limits below.

## Workflow and scope

Current user authorized specify using TODO/specify_lembretes_notificacoes_app.md, then plan/tasks/implement.
This supersedes specify-only language in the saved prompt. Constitution and existing task/habit/accounting/
reward behavior inspected. spec checklist16/16;20 tasks cover15 FR and7 SC. Analyze before implementation:
0 constitutional conflicts,0 critical/high findings,0 uncovered requirements,0 unmapped tasks.
Coverage recorded in tasks.md. Pre/post-design five constitutional gates pass.
Read-only research agent used because local plan skill explicitly requires research agents; no code delegation.
Optional agent-context hooks after specify/plan disclosed; plan reference updated directly per plan skill.
No before hooks or after tasks/analyze/implement hooks registered. No branch/commit/push/publication/version changes.
Unrelated existing and concurrently created TODO notes preserved.

## Design implemented

Optional multiple task/habit reminders, scoped series overrides, chosen/relative/weekday forms, persisted state,
local one-shot alarms, notification actions opening existing Activity, replacement snooze and daily summary.
Schema5 adds definitions, deliveries and structured scheduling settings; schemas1–4 retained.
Existing task generator reused for bounded pure projection and explicit future materialization. No domain outcome
from alert delivery/dismissal/recovery. Concluir revalidates and calls existing checklist/financial UI.
Notifications private with generic public version. Direct immutable Activity PendingIntent; private receivers.
Contextual permission after successful configuration; status/channel/settings/exact access explanation.
Midnight/boot/update/time changes/foreground/source changes reconcile; forced stop requires user reopening.
Approximate delivery fallback when exact access unavailable; system/device restrictions still apply with access.
DST gap forward/overlap earlier offset; snooze absolute instant. Recovery uses stable notification identity,
terminal slot records and no replay of dismissed backlog. OS display and Room are not one transaction.

## Intermediate evidence

- First core build failed because string resources had not yet been added; not a final verification.
- Debug + JVM intermediate pass in1m49s after UI/resources integration.
- Full debug/JVM/lint/release R8/androidTest packaging pass in8m05s,148 tasks (68 executed).
 82 JVM tests at that intermediate point. Later scope/compatibility/tests refinements require consolidated rerun.
- Intermediate lint exposed API26 settings constant on24, plurals and KTX suggestions; fixed rather than suppressed.
- Second complete debug/JVM/lint/R8/androidTest-packaging run passed in5m14s:83 JVM tests,0 failures;
  lint0 errors and16 pre-existing warnings,0 new warnings. Final read-failure/financial-confirmation regression
  additions and guarded action read require one last consolidated run; no UI or schema change followed.
- First temporary AVD startup competed with build memory and had not reached window service. Stopped only verified
  Pixel_5/read-only processes; no persistent AVD change. Auto-review initially rejected stopping one unverified PID;
  both process arguments subsequently verified and safe termination approved. No outstanding approval blocker.

## Final evidence

Final production build passed in6m20s,148 tasks (40 executed/108 up-to-date): debug,85 JVM tests0 failures,
lint0 errors/16 pre-existing warnings/0 new warnings, unsigned release R8 and instrumentation packaging.
Command: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
:app:assembleDebugAndroidTest --console=plain --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process`.
Reports: app/build/reports/tests/testDebugUnitTest/ and app/build/reports/lint-results-debug.html.
No production changes after this pass. Two existing migration fixtures received line-wrap-only formatting
for120-column compliance afterward; connected validation will rebuild them. Device validation follows.

## Limits

API24 runtime, physical hardware and auditory TalkBack not yet verified. Manufacturer restrictions and force-stop
cannot be bypassed. No physical delivery guarantee; background idle throttling can consolidate delayed reminders.
No exhaustive large-history performance benchmark. Evidence from previous features is not proof of this feature.

## Intermediate Android run

First full connected run:42/43 passed, one test-preparation failure in5m48s. The real timed AlarmManager receiver
test passed (three task actions, private/public version, persisted replacement snooze and cancellation after
completion); four migration/persistence tests, three reminder UI tests and existing regressions also passed.
The blocked-recovery test attempted `appops POST_NOTIFICATION ignore`, but Android16 continued to report granted
runtime permission/app notifications; its initial blocked-capability assertion failed before recovery was tested.
Replaced that preparation with actual channel settings Switch interaction via UiAutomation; no production code
change or weakened assertion. Final full rerun required. This run is not reported as a complete Android pass.

Second full run:42/43 passed again in4m22s. Native settings automation was obstructed by a SystemUI ANR;
manual hierarchy also established the channel Switch is not clickable itself (its parent row is).
A targeted intermediate run compiled before the final parent-click adjustment and failed on that selector.
Test helper now dismisses only the known Wait/Aguardar ANR button and clicks the actionable parent.
Final isolated blocked-channel/recovery test passed in56s, including no false POSTED, one consolidated recovery,
three idempotent repeats, dismissed summary staying dismissed, unchanged task and cancellation after resolve.
No production changes. Test-only isChecked deprecation warning retained for older Android accessibility API
compatibility; no lint suppression or product warning introduced.

Consolidated run then passed43/43 Android tests and lint in3m31s. Manual final app inspection found a separate
dialog stacking regression when changing theme/font: task detail could cover the reminder editor after recreation.
Moved global reminder dialogs after all task/habit dialogs in MainActivity. Added ReminderActivityTest exercising
the actual ViewModel/detail/manager/editor flow, recreating Activity and checking the native active window.
All previous pass evidence precedes this one-line production correction; final build/44-test suite follows.
The initial dark200 screenshot captured the regression, not a passing editor; replaced by the final capture below.

## Final consolidated verification (after dialog-order correction)

Continued in a Claude Code session after the Codex session hit its limit; no production code changed in this step.
Command: `.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease
:app:connectedDebugAndroidTest --console=plain --max-workers=2 -Pkotlin.compiler.execution.strategy=in-process`.
BUILD SUCCESSFUL in4m58s,149 tasks (14 executed/135 up-to-date) on Pixel_5 AVD API36 (Android16).
85 JVM tests0 failures; 44/44 Android tests passed, including ReminderActivityTest (editor stays above task detail
after Activity recreation), ReminderUiTest (200% font, weekdays, no habit completion, storage-failure retry),
ReminderRuntimeTest and ReminderIntegrationTest. Lint0 errors,16 pre-existing warnings,0 new. Release R8 built.

Manual capture: evidence/reminder-editor-dark-200.png/.xml, system dark mode, font scale2.0, fictitious task,
captured after a font-scale change forced Activity recreation with the editor open: editor remains topmost.
Observation: at200% the first glyph of the scope text button is slightly clipped at its left edge; text remains
readable and actionable. Recorded as a cosmetic follow-up, not fixed here. Emulator font/theme restored, test app
uninstalled; temporary emulator stdout logs removed from the spec directory.
