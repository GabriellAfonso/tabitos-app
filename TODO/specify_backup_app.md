# Backup completo da rotina para a mudança de Ethos para Tabitos

**Estado** — pronto para especificar.

Em outro chat, execute `$speckit-specify` com o corpo após a linha `---`. Leia primeiro este arquivo,
`AGENTS.md`, `.specify/memory/constitution.md` e as referências abaixo. Consulte a conversa de origem
se estiver acessível. Pergunte apenas sobre decisões de produto ainda pendentes.

**Origem** — conversa de design no Codex, ID `01a1f4c5-8d56-7ef3-966e-b584d70e1707`.
As decisões necessárias estão registradas neste arquivo.

**Referências no projeto**

- `app/build.gradle.kts` e `app/src/main/AndroidManifest.xml` identificam a instalação atual.
- `app/schemas/com.ethos.app.core.tasks.data.TaskDatabase/7.json` e
  `app/src/main/java/com/ethos/app/core/preferences/` cobrem os dados persistidos e as preferências.
- `specs/025-settings-grouped-list/spec.md` descreve Configurações → Dados.
- `specs/007-reminders-notifications/spec.md` e `specs/033-default-task-reminders/spec.md` descrevem os lembretes.
- `specs/008-timers-duration-goals/spec.md` e `specs/016-timed-tasks-habits/spec.md` descrevem os cronômetros.
- `specs/022-statistics-history/spec.md` e `specs/034-tracker-page/spec.md` descrevem históricos e acompanhamentos.

---

## Goal and release sequence

Specify a manual, offline backup that lets a person move their entire routine from Ethos to Tabitos.
The user already relies on a real routine in the installed app. First they will update that installation
while its Android application ID is still `com.ethos.app`, then export a file to a location they control.
Later they will install Tabitos with a different application ID and import that same file. Both installations
can coexist while the user checks the result. The backup must work across that boundary, including when
encrypted. The new ID string is unnecessary for this feature.

## What the copy contains

The current project uses Room schema 7 for structured records and DataStore for preferences. Android system
backup is disabled in the manifest. Inventory the actual persisted user data at specification time so that
features added since this discussion are covered. Capture one consistent point in time across the stores.

- Preserve lists and ordering, task definitions and occurrences, checklists, dates and deadlines, recurrence
  rules and exceptions, completions, reviews and audits.
- Preserve habits, rules and their history, intervals, overrides, logs and outcomes. Include tracker definitions,
  form versions, questions, options, dated entries and answers with their original relationships.
- Preserve accounting settings, prices, entitlements, ledger history, coins, XP and levels, rewards and purchases.
  Preserve the records behind statistics, including historical and archived items as they exist at export time.
- Include timer goals, sessions, segments, corrections and audits, including timers belonging to purchases.
  Include reminder rules, delivery history, snoozes and daily summary settings.
- Include user preferences such as theme, game mode, timer alarm and default task reminders.
  Preserve identities, links, dates and ordering so that the restored records behave like the originals.

## Export and optional encryption

Add **Exportar backup** and **Importar backup** under **Configurações → Dados**, with Portuguese user-facing copy.
Export creates one file in a location chosen with the Android document picker, available to the new installation.
Offer **Proteger com senha** on each export. Use the local export date and 24-hour time in the suggested filename,
for example `Tabitos-backup-2026-11-19_14-30.zip` or `Tabitos-backup-2026-11-19_14-30.zip.enc`.
Use the Tabitos name even when exporting from Ethos.

Explain the choice at export. Anyone with the plain file can read the routine. The encrypted file requires its
password to read or restore the data on another installation. Losing the password prevents recovery from that file.
Recognize the format and encryption from the content even after the file is renamed. Ask for a password on import
only for a protected file. Report export success only when the complete file has been written.

## Import and replacement

Import is the restore operation. After file selection and validation, show the backup date and counts of lists,
tasks and habits so the user can recognize the copy. A fresh installation can proceed with **Importar**.
If the destination contains personal data, explain that importing replaces its current routine, including changes
made since the backup. Let the person cancel, export their current routine first, or confirm **Substituir e importar**.
Built-in initial records alone should allow the fresh-install flow. Account for personal history or changed
preferences even when there are no visible tasks.

Replace the complete destination state as one user-visible operation. A cancellation, wrong password, damaged
or incomplete file, unsupported format, or interruption must leave a usable, complete routine. The app must
never report a partial import as successful or leave Room and preferences from different copies.
Repeated import of the same file must not duplicate entries or replay domain actions such as granting coins.

## Timers and reminders

Exporting while a timer is running leaves it counting in the source app. The copy captures the elapsed time
through the instant of export, including the current segment. Import restores that session paused and ready to
resume. Example: export at 12 minutes and import the next day. The timer resumes from 12 minutes on user action.
The time between installations is excluded. Transfer alone cannot complete a task, log a habit, trigger a timer
alarm, grant XP, charge for a reward again, or change the user's balance.

Reconcile reminders against the restored routine and permissions available in the destination installation.
Schedule future reminders, preserve the existing handling of missed deliveries, and avoid a burst of old
notifications. Android permission grants belong to each installation. The file carries the user's reminder settings.

## Acceptance scenarios

1. Using fictitious data, export from the original ID and import under a different ID. Exercise both plain and
   encrypted files. Verify recurrence, history, tracker answers against their original form version, timer links,
   purchases, balances, statistics and preferences as well as counts in the preview.
2. Rename each kind of file and import it. The encrypted file asks for its password before revealing personal
   data. A wrong password or modified encrypted content cannot replace destination data.
3. Add a task on Tuesday after a Monday export. Import Monday's file into the populated installation. Cancelling
   preserves Tuesday's task. Confirming restores Monday's state. Importing that file twice produces the same routine.
4. Exercise the 12-minute timer example and a pending future reminder. Verify the source timer continues,
   the restored session is paused at the captured time, and only eligible reminders are scheduled.
5. Try a truncated file, an unsupported format and an interrupted import into an existing routine. Verify the
   person can reopen a complete routine and that a failed import preserves prior data and preferences.

## Scope and open questions

The feature covers manual export and full replacement. Merging two routines, scheduled backups, accounts,
backend sync, uninstalling Ethos, and changing the app name or application ID belong outside this specification.
Use synthetic data for verification and respect the project's device and build authorization rules.

No product decision is outstanding from the design conversation. Leave the archive layout, format versioning,
cryptographic implementation and recovery mechanics to planning, subject to these portability, privacy and
data-preservation requirements.
