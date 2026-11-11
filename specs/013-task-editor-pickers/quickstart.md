# Quickstart: validate spec 013

## Prerequisites

- JDK and Android SDK as for earlier specs; API 36 emulator running for instrumented tests.

## Automated

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
.\gradlew.bat :app:connectedDebugAndroidTest
```

Expected: all JVM tests pass, including `EthosDatesTest`, `TaskEditorPresentationTest` (8 recurrence kinds
round-trip, review visibility, advanced-expanded rule) and `TasksViewModelTest` discard cases; instrumented
`TasksContentTest` and `TaskEditorTest` pass; no new lint warnings.

## Manual (emulator, pt-BR)

1. Hoje → "+" → Tarefa: type a title, save → task appears for today (SC-001).
2. New task: tap date → pick a day next month → field shows "10 dez"; tap time → 14:30 → row shows
   "14:30 · 10 dez".
3. Expand "Mais opções": add 3 checklist items, remove the middle, move last up, save, reopen.
4. Repetition: create one task per option/sub-option; reopen each; the same option is selected.
5. Edit a non-overdue task: no review checkbox. Use demo data with an overdue task: change its date →
   review checkbox appears; save without it → review-required message.
6. Open task detail → "Data em que foi feita": future days disabled.
7. Habit detail calendar title reads "novembro de 2026".
8. Settings → font 200% and TalkBack: editor scrolls; date fields announce label and value.
9. Back with unsaved changes → discard confirmation; without changes → closes.
