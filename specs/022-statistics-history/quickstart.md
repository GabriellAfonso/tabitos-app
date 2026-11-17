# Quickstart: validating spec 022

## Automated (Windows)
```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```
Expected: build OK; new JVM tests `StatisticsPeriodTest`, `StatisticsSummaryTest`, `StatisticsHistoryTest`,
`TaskEraseTest` pass with the existing suite; no new lint warnings.

Instrumented (only with explicit authorization): `HistoryErasureTest` in `connectedDebugAndroidTest`.

## Manual scenarios (debug build, never on the personal routine without consent)
1. ☰ → Estatísticas: opens on Resumo, "Esta semana". Configurações has no "Histórico de moedas".
2. Complete tasks on/after deadline, postpone one: Concluídas, % no prazo, Adiadas and the bar of today match.
3. Compare each habit's % and sequência with its card (same period rules).
4. Run a task timer for a few minutes and pause: "Tempo com cronômetro" shows it; a timed reward does not.
5. Change Semana/Mês/Ano/Tudo and arrows; Histórico follows; force-stop and reopen → "Esta semana".
6. Histórico filter Moedas, period Tudo: same entries as the former coin history.
7. Game mode off: no coins/XP/purchases/Moedas filter anywhere on the page.
8. Delete a task with the box unchecked → its items stay, marked deleted; tap → "Apagar histórico" → items gone;
   balance and level unchanged, coin entries titled "Tarefa apagada".
9. Delete a habit with the box checked → not in Hábitos excluídos, items gone, balance unchanged.
