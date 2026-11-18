# Research: Growing editor sheet

## R1. Sheet container

- **Decision**: a custom `Dialog` that fills the window, with a bottom-aligned surface that animates its height
  between a wrapped (compact) and a full-screen (expanded) state.
- **Rationale**: the editor has to block an accidental swipe that would discard a draft. The task editor asks
  before discarding (`discardConfirmation`), and the habit editor cannot close while a save is running. A plain
  dialog sends back and outside taps to `onDismiss`, where the ViewModels already decide what happens. Keyboard
  insets (`imePadding`) and the expanded full screen are straightforward.
- **Alternatives**: `ModalBottomSheet` with partial and expanded anchors. Swipe-to-hide happens before the
  discard confirmation can be shown, and its height is tied to the drag anchors instead of the "Mais" action, so
  it was rejected for the container. It is still used for the short choice sheets, as in spec 028.

## R2. Choice sheets

- **Decision**: `ModalBottomSheet` with `skipPartiallyExpanded = true`, opened from the editor dialog. The editor
  keeps which sheet is open (`rememberSaveable`).
- **Rationale**: the same pattern already works inside the habit editor dialog (the icon catalog, spec 028), the
  task detail and the timer.

## R3. Where the rule mapping lives

- **Decision**: the habit frequency and limit mappings move from Composables to `HabitEditorPresentation.kt`, as
  pure functions over `HabitDraft`.
- **Rationale**: AGENTS.md forbids business decisions and `ordinal`/index contracts inside Composables. The
  current `PracticeFrequency` uses positions 0–3, and `BreakingRule` uses `HabitUnit.ordinal`. The values written
  stay identical.

## R4. Save button icon

- **Decision**: use the existing `ic_check` (with the content description "Salvar") on the round compact button.
- **Rationale**: this needs no new drawable. An upward arrow (the prototype) reads as "send", which suits chat
  apps, not an editor.

## R5. Accessibility

- **Decision**: chips merge their semantics into "Label: value" with `Role.Button`. Expanded rows reuse
  `SettingsLinkRow`, which reads the title and the value. The header buttons have content descriptions
  ("Recolher", "Fechar"), and the round Save button reads "Salvar".
