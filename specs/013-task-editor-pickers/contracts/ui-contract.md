# UI Contract: Task Editor

## Editor layout (top to bottom)

1. Header: "Nova tarefa" / "Editar tarefa", Cancel and Save actions.
2. Error card (existing `TaskError`).
3. Title field (focused on new task).
4. Deadline chips: Sem data · Dia específico · Até domingo · Até sexta (dias úteis); weekly chips show the
   current week range as supporting text. Recurring drafts hide "Sem data" (as today).
5. Date field (when deadline ≠ none and as today's visibility rule) → date picker.
6. Time field (only for specific day) → time picker, with clear action.
7. List chips.
8. "Mais opções" toggle (expanded by rule FR-002):
   - Description
   - Checklist items: text field + move up/down + remove per item; "Adicionar item"
   - Difficulty, importance, coin estimate
   - Repetition: Não repete · Todo dia · Toda semana · Todo mês · A cada X dias, with sub-options
   - Start date picker (new recurring), ending (Nunca / Até data → picker / Após N vezes)
9. Scope choice (editing recurring series).
10. Review explanation + checkbox (only per FR-007).

## Callbacks (content ← screen)

- `onChange(TaskDraft)`, `onDifficulty(Int)`, `onRepeat(RepeatKind)`, `onSave()`, `onClose()`
  (`onClose` routes to ViewModel `closeDraft()` which may request discard confirmation).

## Semantics

- Date/time fields: role button, description "<label>, <formatted value or 'não definido'>".
- Checklist controls: "Remover <item>", "Mover <item> para cima/baixo".
- Pickers: Confirm "OK", Cancel "Cancelar"; time picker also "Remover horário" when a time exists.

## Task detail

- Due date shown with long format; completion date field replaced by a date picker field (no future days).
