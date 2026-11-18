# Feature Specification: Settings as a grouped list

**Feature Branch**: `025-settings-grouped-list` (no branch created; work stays on the current branch)

**Created**: 2026-11-17

**Status**: Implemented (2026-11-17)

**Input**: Design conversation on 2026-11-17 with prototype `prototypes/configuracoes/index.html`; the user chose
option "A · Lista agrupada" and asked to apply it (short spec, no plan/tasks documents).

## Current state (verified 2026-11-17)

- `SettingsPage` in `EthosApp.kt` stacks, in a 20 dp padded column: the "Modo jogo" switch, "Aparência" (headline,
  hint, three radio rows), "Alarme ao terminar o cronômetro", "Como funcionam ganhos e penalidades" (two tables,
  facts and "Editar ganhos e penalidades", game mode only), the "Hábitos excluídos" link and "Lembretes" (three
  status sentences, "Resumo diário" with a time field, "Salvar horário e ativar resumo", "Desativar resumo" and
  four text buttons: notifications, two Android channels and precise alarms).
- Titles use five different text styles, there are no sections and the status text shows even when all is fine.
- The user's real routine is stored in the app; this feature changes no data, rule or schema (Room stays 7).

## Decisions

1. Settings is a Material list in four sections, each with a header: **Aparência**, **Modo jogo**,
   **Notificações e alarmes**, **Dados**. Rows are a shared component in `core/ui/components/SettingsRows.kt`
   (icon, title, current value, optional switch/chevron/button); the whole row is the touch target.
2. Aparência: one "Tema" row showing the current choice; tapping opens a dialog with the hint and the three
   options (Seguir o sistema, Claro, Escuro). Choosing closes the dialog and saves as before (DataStore, no
   Activity restart). Read/write errors keep the retry card.
3. Modo jogo: switch row (turning off still asks first; blocked with a counting reward timer, spec 017). With game
   mode on, a "Ganhos e penalidades" row shows "Fácil 5 ◈ · Médio 10 ◈ · Difícil 20 ◈" from the current prices and
   opens a sub-page (`ShellDestination.EARNINGS`, Back returns to Settings) with the existing tables, facts and
   editor. The sub-page is game-only: turning game mode off elsewhere sends it to Hoje.
4. Notificações e alarmes, in this order:
   - "Notificações bloqueadas" appears only when reminder or summary notifications are blocked, in the error color,
     with "Permitir": asks for the permission when it is missing, otherwise opens the app's Android notification
     settings. Text says which of reminders/summaries do not appear.
   - "Resumo diário": switch plus "Todo dia às HH:mm" or "Desligado". Tapping the row or turning the switch on opens
     the existing hour/minute wheel picker (no typing); OK saves and turns the summary on (permission asked in
     context, spec 007). Turning the switch off disables it. Off by default as before.
   - "Alarme ao terminar o cronômetro": switch (spec 016).
   - "Horário preciso": shown only when Android denies precise alarms, as "Aproximado. Toque para permitir…" in the
     error color; tapping opens Android's precise alarm access. When allowed, the row is hidden (revised
     2026-11-17: an informative row looked like a broken button).
   - "Sons e vibração no Android": opens a small dialog to choose the Lembretes or Resumos channel settings.
5. Dados: "Hábitos excluídos" with a chevron (unchanged destination).
6. Icons are Material Symbols Rounded vector drawables (spec 019), added for theme, coins, blocked notifications,
   summary, alarm, precise time, sound and "open outside".

## Out of scope

- No new settings, no count on "Hábitos excluídos", no change to reminder/accounting/game rules or stored data.
- Health banner and inline segmented theme (option B) and category hub (option C) were not chosen.

## Acceptance

- Settings shows the four sections in the order above; no status sentence appears when everything is allowed.
- Theme, game mode, summary, timer alarm and precise alarm behave as before through the new rows and dialogs.
- "Ganhos e penalidades" opens and Back returns to Settings; it is hidden with game mode off.
- Previews exist for the shared rows; strings are Portuguese in `strings.xml`; unused strings are removed.

## Verification (2026-11-17)

- `.\gradlew.bat :app:assembleDebug :app:compileDebugAndroidTestKotlin :app:testDebugUnitTest :app:lintDebug`
  passed: 229 JVM tests, no new lint warnings (only the existing plurals, launcher and dependency notices).
- New JVM test: `ReminderSettingsPresentationTest`; `ShellModelsTest` covers the Earnings sub-page.
- Instrumented tests updated but not run (no authorization this time): `AppearanceNavigationTest` (Tema dialog),
  `SettingsNavigationTest` (Ganhos e penalidades and Back), `ReminderUiTest` (summary picker, blocked warning).
- Not checked: real device, TalkBack audio, Android 12 or older (channel and precise-alarm screens).
