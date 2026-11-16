# Quickstart: validate spec 021

## Automated

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Expected: build ok; JVM tests include `TaskPostponeTest` and `ReminderPostponeTest` passing; no new lint warnings.

## Manual (device/emulator, only with authorization)

1. Task due today 18:00 → tap card → popup shows title, summary, Concluir ▾ / Adiar, five icons, no history,
   no Fechar. Tap outside → closes.
2. Adiar → Amanhã → popup closes, snackbar "Adiada para amanhã"; task gone from Hoje; Calendário tomorrow shows it
   at 18:00. Desfazer → back on today.
3. Adiar → Escolher data → past days and today's deadline disabled; pick a date → moved.
4. Overdue task (Revisar pendências) → Adiar → sheet shows missed-deadline note → Amanhã → leaves Review, no
   coin change in Histórico de moedas.
5. Recurring task → Adiar one occurrence → next occurrence keeps its date; sheet note shows it.
6. Task with a fixed-date reminder at 09:00 on its day → postpone 2 days → reminder screen shows 09:00 two days
   later; a "1 h antes" reminder is 1 h before the new deadline.
7. ▾ → Concluí em outro dia… → pick yesterday → task done with that date.
8. Lista → choose another list → task moves. Excluir → existing delete dialog.
9. Done task → status strip + Desfazer; no Adiar.
10. Font 200% and TalkBack: all icon labels readable, actions announced.
