# UI contract: Estatísticas (spec 022)

## Navigation
- `ShellDestination.STATISTICS` (route `statistics`, title "Estatísticas", icon `ic_bar_chart`) in the drawer after
  Revisar pendências/Recompensas; not game-only. `ShellDestination.HISTORY` and its Settings link are removed.

## StatisticsContent(state, actions)
- Period row: `‹` (IconButton, "Período anterior", disabled when `!hasPrevious`), label button with `▾`
  (opens DropdownMenu Semana/Mês/Ano/Tudo, current checked), `›` ("Próximo período", disabled when `!hasNext`).
  Arrows absent for Tudo; Tudo shows a second line "desde <data>".
- Tabs Resumo | Histórico (Resumo selected on open).
- Resumo sections (cards): Tarefas (Concluídas, % no prazo or —, Adiadas, bar chart), Hábitos (geral %, one row per
  habit: icon, name, %, sequence; red for bad habits), Tempo com cronômetro (total, rows), Moedas e nível (game mode).
  Empty section → short message.
- Histórico: FilterChips row; LazyColumn with month header (Ano/Tudo), day header, rows; adjustments with
  `bodySmall` + `onSurfaceVariant`; "Mostrar mais" when more items; empty message.
- Row tap: existing task → `openTask(occurrenceId)`; existing habit → `openHabit(habitId)`; reward → `openRewards()`;
  deleted subject → deleted dialog with "Apagar histórico" → irreversible confirmation → `erase(subject)`.
- States: loading (indicator), error (message + Tentar novamente), content.

## Actions
`StatisticsActions(selectKind, previous, next, selectTab, selectFilter, showMore, openTask, openHabit, openRewards,
requestErase, confirmErase, dismissErase, retry)`.

## Task delete dialog
- Adds Checkbox row "Apagar também o histórico desta tarefa" (unchecked). Confirm sends
  `TaskAction.Delete(id, scope, eraseHistory)`.

## Habit exclude dialog / Hábitos excluídos
- Exclude confirmation adds Checkbox "Apagar também o histórico deste hábito"; checked → `HabitsViewModel.erase`.
- Excluded habit details add "Apagar por completo" → confirmation "Não é possível desfazer" → erase.
