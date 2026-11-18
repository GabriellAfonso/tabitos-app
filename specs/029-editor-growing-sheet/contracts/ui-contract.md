# UI Contract: Growing editor sheet

Callers are unchanged:

- `TaskEditor(draft, lists, occurrence, today, busy, error, onChange, onDifficulty, onRepeat, onSave, onClose,
  onNewList)` from `TaskDialogs`.
- `HabitEditor(state, lists, HabitEditorActions(change, difficulty, save, retry))` from `HabitDialogs`.

## Shared component (`core/ui/components/GrowingSheet.kt`)

- `GrowingSheet(expanded, title, busy, onDismiss, onCollapse, onSave, name, content)`. The surface has the test tag
  `editor-sheet`. In the compact state it shows the round Save button (`editor-save`, "Salvar"). In the expanded
  state it shows the header (collapse "Recolher" with `onCollapse`, or close "Fechar" when `onCollapse` is null)
  and a text Save button (`editor-save`).
- `EditorChip(label, value, icon, onClick)`: semantics "label: value", Role.Button.
- `ChoiceSheet(title, onDismiss, content)`: has a "Pronto" button.
- `RadioOptions(options, selected, enabled, onSelect)`.

## Test tags

| Tag | Element |
|-----|---------|
| `editor-name` | name field |
| `editor-more` | "Mais" chip |
| `editor-save` | Save (compact or expanded) |
| `chip:<SHEET>` | quick chip opening that sheet (e.g. `chip:WHEN`) |
| `row:<SHEET>` | expanded row opening that sheet |
| `checklist:<index>` | checklist item field (unchanged) |
