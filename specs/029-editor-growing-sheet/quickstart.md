# Quickstart: Growing editor sheet

## Build and tests (Windows)

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

Instrumented tests (`connectedDebugAndroidTest`) only run with explicit authorization.

## Manual checks

1. Hoje › "+" › Criar tarefa: the sheet opens over Hoje with the keyboard. Type "Comprar pão" and tap ✓: the task
   is saved in Caixa de entrada with no date.
2. Criar tarefa › "Sem data" chip › Dia específico › Amanhã › 09:00: the chip reads "Amanhã 09:00".
3. Criar tarefa › "Mais": the sheet fills the screen, the keyboard closes, and the rows show Descrição, Checklist,
   Importância (and Dificuldade with game mode on). The collapse arrow returns with the values kept.
4. Criar bom hábito › frequency chip › Semanal: a "3 vezes por semana" chip appears. The timer chip turns it into
   "Com cronômetro" plus a minutes chip.
5. Quebrar mau hábito: the tile is red with the block icon. Limitar › limit chip › − / + and period.
6. Edit a recurring task from its popup: the sheet opens expanded, with no keyboard, and shows "Só esta / Esta e as
   próximas" above Save.
7. Edit an overdue task and change its date: the review confirmation appears above Save.
8. Game mode off: no Dificuldade row and no estimate.
9. Font size at 200% and dark theme: the chips wrap and every control can be reached by scrolling.
