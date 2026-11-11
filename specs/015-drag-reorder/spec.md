# Feature Specification: Drag to Reorder Everywhere

**Created**: 2026-11-11 | **Status**: Implemented (interface; JVM-verified, instrumented not run)

**Input**: User request (2026-11-11): every "Mover para cima/baixo" must become drag; the task drag felt
stiff because the items that make room jumped instead of sliding.

## Scope

- Task list pages (existing long-press drag): neighbours now slide with a spring while the dragged card
  follows the finger; storage still changes only on release (`TaskAction.Order`).
- Lists manager, rewards store, task checklist items, tracker questions and tracker choice options: the
  up/down buttons are replaced by drag. Short lists inside dialogs/editors drag from a visible grip (⠿) so
  text fields keep their own gestures; the rewards store uses long-press like tasks.
- Accessibility: every reorderable item exposes TalkBack actions "Mover para cima" / "Mover para baixo";
  the grip is described as "Arrastar para reordenar" and has a 48 dp target.
- Persistence: lists and rewards are saved with their full new order in one write
  (`TaskAction.OrderLists`, `RewardRepository.order`); a stale order (item added/removed meanwhile) is
  rejected for lists and ignored for rewards. No Room schema change.

## Out of scope

- Auto-scroll while dragging inside dialog/editor columns (lists there are short).
- Reordering habits, reminders or calendar items (no manual order exists today).

## Acceptance

1. Dragging a task over others makes them slide smoothly into the free slot; releasing saves once.
2. No "Mover para cima/baixo" buttons remain in the app; TalkBack can still reorder every list.
3. A reordered list/reward order survives reopening the app.
