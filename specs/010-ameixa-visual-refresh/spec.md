# Feature Specification: Paleta Ameixa e modelo B de Hoje

> Atualização 2026-11-10: [spec 011](../011-habit-toggle-demo/spec.md) substitui a confirmação
> de prática rápida por marcar/desmarcar direto e retira barras/progresso semanal de Hoje.
> As demais decisões visuais e confirmações permanecem válidas.
> Spec 012 also replaces the task-card confirmation with direct marking/unmarking and a Concluídas section.

**Feature Branch**: existing working branch; no branch creation requested
**Created**: 2026-11-07
**Status**: Implemented 2026-11-07
**Input**: design rounds in `prototypes/tema/index.html` (artifact https://claude.ai/artifact/3XTcoJc6JLKx3Yv5EGW5ED),
decisions in `TODO/planejamento_tema.md`, visual base `prototypes/hoje/index.html` (model B by Codex). The user chose
palette B · Ameixa and asked to apply palette and model B together ("faz os dois juntos").

## Scope

Visual refresh only. No new domain rules, data, permissions or Room changes.

1. Theme: replace the sage green light/dark schemes with Ameixa (plum primary, lilac-tinted neutrals, warm deep
   dark). Coins use an amber tertiary role so money is distinct from plum highlights. Theme mode (system/light/dark)
   keeps working through DataStore.
2. Shell: top bar shows the current date as an uppercase eyebrow over a large title, menu as a plain icon and
   settings as a round outlined button. Bottom navigation keeps the four destinations.
3. Hoje: wallet tiles "SUAS MOEDAS" with the coin glyph ◈ in amber and "Ver recompensas ›", "SEU PROGRESSO" with
   level, XP within level and bar; review and tracker notices as tappable banners; Tudo/Tarefas/Hábitos as a
   segmented control (same filter semantics and test tags).
4. Task rows: card with a check box. Tapping the box on an open task starts the existing completion flow (checklist
   confirmation and financial preview); it never completes silently. Resolved tasks show a filled box; tapping it
   opens the detail (undo stays there). Recurring tasks show only a two-arrow circular repeat icon,
   with an accessible Tarefa recorrente description and no visible text or chip background.
   Use a thin 1.4-unit rounded stroke and muted onSurfaceVariant tint at 75% opacity.
5. Habit rows: colored icon square, progress line, current Monday–Sunday week bars for practice habits, and an action
   button. "Marcar" records a practice for today and "Registrar" records an occurrence for avoid/limit habits, both
   through the existing financial preview. Avoid/limit cards use the warm amber container. Paused or excluded
   habits have no quick action; a practice already recorded today shows "✓ Feito" and opens the detail.
6. Calendar and rewards adopt the same components (segmented views, dot indicator, coin glyph on prices/balance).

## Acceptance

- The default list icon and Tarefas navigation icon use ☰, matching the hamburger menu.
  Existing lists with the saved Pessoal icon (index 5, formerly a star) also render ☰ without rewriting data.

### Home progress and review refinement (2026-11-10)

- Level tiles show only within-level XP/required XP (e.g. 40/100), retaining the progress bar and full
  XP explanation for accessibility. No changes to level or XP calculation.
- The Hoje review notice uses a solid theme error-red container and contrasting onError text, with a
  drawn 40dp clock centered vertically. Count/navigation stay unchanged; tracker notices keep their style.

### Color selection correction (2026-11-10)

- List and habit editors display the existing six saved colors as circular swatches instead of text chips.
- Selection uses a ring and check mark, accessible color names/radio state, wrapping and 48dp touch targets.
- List colors tint their existing icons in the title, list management and drawer, with no extra color dots;
  habit colors remain
  visible behind their card icons. Existing color indices, palette and persistence remain unchanged.

- Light and dark render the Ameixa palette; contrast of text on backgrounds stays readable.
- Existing behavior and tests keep passing (filters, review, rewards purchase, reminders, timers, trackers).
- The quick check box and habit action never change coins/XP without the existing confirmation.
- Controls keep content descriptions, 48dp targets where interactive, and work at font scale 2.0.

## Out of scope

Drawer redesign, new icons set, changes to list/habit color choices, animations.

## Hoje creation control (approved 2026-11-10)

Task lists replace visible up/down task buttons with long-press drag ordering, live placement feedback,
edge autoscroll and a single persisted order on release. Cancel leaves stored order unchanged. Retain
screen-reader move actions without visible buttons. Reordering is scoped to open tasks in the selected
list; hidden future occurrences keep their relative slots. Hoje/review/calendar retain their existing order.

Task-card deadline labels use Hoje for the current local date, Amanhã for the next local date, and
dd/MM/yyyy for other dates. Undated tasks retain Sem data; time/status and scheduling rules are unchanged.

- Replace the inline Nova tarefa and Novo hábito buttons on Hoje with one bottom-end floating + button,
  above bottom navigation. Tapping it opens a menu with Nova tarefa and Novo hábito, independent of filter.
- Each option opens its existing editor and respects its own loading/busy state. Close the menu on selection,
  outside tap, Back or leaving Hoje. Preserve existing creation controls on other destinations.
- Give the button an accessible creation label and reserve bottom scroll space so it cannot cover the last row.

## Verification (2026-11-07, emulator Pixel_5 API 36 and owner's phone)

- `:app:testDebugUnitTest` 108/108, `:app:lintDebug` without new warnings, `:app:assembleRelease` success.
- `:app:connectedDebugAndroidTest` 58/58 on API 36. Two tests (`TasksContentTest`, `AccountingUiTest`) now tap the
  task title instead of the card center, because on the list page the center of the new card is the reorder row.
- Manual on emulator, light and dark: Hoje header with date eyebrow, wallet with amber ◈, segmented filter, task card
  with check box, habit card with "Marcar" and week bars. Tapping the box and "Marcar" opened "Confirmar efeito
  financeiro" before anything was written; after confirming, balance 20 ◈, 40 XP and "✓ Feito".
- Debug APK installed on the owner's phone (Android 13). Not validated: TalkBack audio, font 200% screenshots,
  API 24.
