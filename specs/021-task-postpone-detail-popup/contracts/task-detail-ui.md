# UI contract: task detail popup and postpone sheet

Texts are pt-BR resources; examples assume today = Monday 16 Nov 2026.

## Popup (open task)

| Order | Element | Content / behaviour |
|-------|---------|---------------------|
| 1 | Title | Task title (titleLarge) |
| 2 | Summary line | `◷ Hoje · 18:00` (warning colour + "· vencida" when overdue) · `<list icon> <list>` · `↻ <repeat label>` (recurring) · `🔔 N` (N reminders) · `▶ <minutes> min` chip (timed; opens timer) |
| 3 | Description | max 2 lines; "Ver tudo" expands; omitted when empty |
| 4 | Checklist | checkbox rows; toggles `TaskAction.Check` |
| 5 | Main buttons | `Concluir` (filled) + attached `▾` → menu "Concluí em outro dia…"; `Adiar` (tonal) only when postponable |
| 6 | Icon row | `Não será feita`, `Editar`, `Lembretes`, `Lista`, `Excluir`, each icon + label below |

- No confirm/dismiss buttons; closes on outside tap / back.
- Resolved task: row 5 becomes a status strip `Feita <dia> às <hh:mm>` / `Marcada como não será feita` + `Desfazer`;
  `Não será feita` and `Adiar` hidden; `Lembretes` disabled.

## Postpone sheet

- Title `Adiar “<título>”`; subtitle `Prazo atual: Hoje · 18:00` (+ ` · o horário se mantém` when timed).
- Option `Amanhã` — subtitle `ter, 17 nov` (+ ` · 18:00`).
- Option `Escolher data` — subtitle `Abre o calendário`; picker min = today, current deadline not selectable.
- Note (recurring): `Só esta vez. A série continua: próxima em <qua, 18 nov>.` (next open/future occurrence date;
  omitted when unknown).
- Note (overdue): `O prazo de <ontem/15 nov> fica registrado como perdido, sem penalidade.`

## Feedback

- Snackbar `Adiada para amanhã` / `Adiada para ter, 17 nov` with actions `Desfazer` and `Fechar`.

## Lista choice

- Dialog `Mover para` listing Caixa de entrada + active lists with icon; current marked; selecting applies
  `TaskAction.Move` and closes.
