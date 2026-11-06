# Quickstart validation

Use existing JDK21/SDK36 and Wrapper. Run:
`./gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleRelease`
Then `./gradlew.bat :app:connectedDebugAndroidTest` with a booted device.

1. Offline capture title-only in Inbox; create/edit/reorder lists, move/reorder tasks and relaunch.
2. Add two checklist items; completion asks remaining count, cancel retains draft; confirm and undo.
3. Try completion before creation/future date; reject both. Record legitimate forgotten completion.
4. Verify day time does not cause early review; Friday/Sunday boundaries move weekly work into review.
5. Review/reschedule with explicit missed-deadline history; Não será feita and undo remain available.
6. Test each recurrence/end mode, month31/leap years, missed daily occurrences and independent checks.
7. Edit/delete each scope and verify finite counts/cadence/history; block dependent undo with later actions.
8. Delete populated list only after confirmation. No fake balance mutation or habit creation.
9. Test light/dark/system, 200% fonts, landscape, accessibility labels and recoverable persistence errors.
10. Run Room reopen/rollback tests; inspect exported schema1. Report unavailable API24/TalkBack honestly.
