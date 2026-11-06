# Specify — Ponto 2: interface, navegação e aparência do Ethos

**Estado:** pronto para executar a etapa specify em outro chat.
**Data:** 2026-11-06.

## Como usar

Na raiz `C:/Users/gabri/Projetos/ethos`, executar `$speckit-specify` com o corpo em inglês
após o separador `---`. Gerar apenas a especificação; plan, tasks e implement exigem solicitação
separada. Este arquivo é um prompt de passagem de contexto, não uma spec já executada.

## Referências e origem

- `AGENTS.md` e `.specify/memory/constitution.md` v1.0.0.
- `specs/001-android-foundation/spec.md`, `plan.md` e `verification.md`.
- `prototypes/hoje/index.html`: referência visual aprovada, rodada 02, modelo B.
- `TODO/planejamento_ponto_2.md`: decisões funcionais futuras e questões ainda abertas.
- `PLANO_BASE.md`: contexto inicial, refinado por este prompt.
- Origem: conversa de planejamento do Ethos em 2026-11-06. ID, link e caminho de transcript
  não estão disponíveis; o contexto necessário está registrado neste prompt e nas referências.
- Não existe contraparte de backend nesta entrega.

---

Create the feature specification for Ethos's visual application shell, navigation, and persisted
appearance preferences. This is point 2 of the agreed delivery sequence. Execute only the specify
stage, not plan, tasks, analysis, implementation, commits, or publication.

## Inspect the current repository first

The working repository is C:/Users/gabri/Projetos/ethos. Read AGENTS.md, constitution v1.0.0,
the Android foundation specification/plan/verification, and the current Kotlin source before
specifying changes. The foundation has already been implemented in another chat: Kotlin with
AGP 9 built-in Kotlin, Compose Material 3, Hilt/KSP, a single :app module, MainActivity,
EthosApplication, and a simple centered Ethos launch screen. The app targets native Android,
with minimum SDK 24 and base identity com.ethos.app. See actual build files for tool versions.
Do not regenerate the foundation or claim current verification coverage beyond its recorded
evidence. The existing managed agent context reports API 36 launch checks and no API 24 runtime
coverage; recheck the evidence if it has changed.

The user plans and discusses features in this chat, then hands saved specify prompts to another
chat. Do not ask again about settled choices. This prompt does not authorize implementing them.

## Purpose and scope boundary

Turn the single-screen foundation into a coherent, navigable Android shell for a personal task,
habit, and routine organizer. Provide the approved visual layout, real navigation, useful empty
states, and working light/dark/system appearance persisted locally with DataStore.

The owner will initially use APKs, with Google Play considered later. No publishing work belongs
to this feature. The app must remain usable offline without login, services, or new permissions.

This delivery is the visual shell. Task/list persistence and recurrence, habits and goals,
the currency/XP engine, and reward management/redemption are separate future specifications.
Do not expand this shell into those domains because the visual prototype simulates their actions.

## Approved visual reference

Use prototypes/hoje/index.html as the concrete visual reference. The user chose option B,
"Um passo de cada vez" (compact lists with filters within Hoje), then explicitly liked round 02
showing balance, level, rewards, and pending review. Preserve that direction instead of reopening
the A/B/C design choice.

The HTML shows three independent phone mockups representing destinations in the SAME app:
Hoje, Recompensas, and Revisar pendências. They are not three layouts to implement side by side.
The surrounding laboratory page, phone frames, browser theme controls, notes, fictitious status
bar, and simulated system gesture strip are presentation tools and must not become Android UI.

Follow the approved compact hierarchy, rounded cards, subtle boundaries, green/sage accents,
light and dark treatments, and generous enough touch areas. Translate the visual intent to
Material 3 rather than embedding the HTML or copying browser dimensions literally. Support
system insets, font enlargement, and constrained/landscape layouts without clipping controls.
The user views local prototypes in Brave, personal profile; that is not an app runtime dependency.

## Navigation and destinations

Start on Hoje. The approved prototype has four primary bottom destinations:
- Hoje
- Tarefas
- Hábitos
- Calendário

Provide a top-left menu control and a settings entry as in the reference. The side menu includes:
- Hoje
- Revisar pendências, with a pending counter when there are actual pending items
- Recompensas
- Histórico de moedas
- Minhas listas, eventually user-defined names such as Rotina, Trabalho, and Pessoal
- Criar lista, as a future domain capability
- Configurações

Revisar pendências is accessed from the side menu and a discreet Hoje notice. Do not add it as
a fifth bottom destination. Opening a drawer destination must close the drawer. Back behavior
must be predictable, with no duplicate top-level destinations accumulated by repeated taps.
The later plan must choose a simple consistent navigation policy and state restoration approach.

Lists will contain separate Tarefas and Hábitos tabs. A project may be represented by a named
list for now; distinct project management capabilities have not been specified. The shell may
provide reusable list-detail UI in previews, but must not create persistent lists, pretend a
list was saved, or create a new Project domain as part of this feature.

## Hoje

Keep the approved model B compact, with:
- Header and date.
- Two small progress tiles: coin balance leading to rewards, and XP/level information.
- A discreet entry such as "Você tem 3 pendências para revisar" when real pending data exists.
- Filters Tudo / Tarefas / Hábitos within Hoje.
- A compact content area mixing the day's tasks and habits according to the selected filter.
- A quick-add affordance consistent with the prototype, without pretending to save domain data.

The production shell has no task/habit/currency domain yet, so its initial content must be
honest: meaningful empty states, no fictitious tasks, no fabricated pending counter, and no
seeded balance or earned level. Pending notices must be absent at zero. Keep the balance/level
layout, with a truthful neutral or unavailable presentation rather than inventing a starting XP
curve or wallet balance. The numeric progression rules are deferred.

Demonstrate populated reference states using explicit sample data in Compose previews or another
development-only presentation, isolated from production. This preserves the approved visual
reference without shipping mock data as the owner's actual progress.

## Additional screen structures

- Tarefas and Hábitos: identifiable destinations with relevant empty states; no CRUD yet.
- Calendário: destination and appropriate empty/placeholder presentation; do not port the
  deliberately inaccurate sample HTML date grid or invent real calendar filtering behavior.
- Revisar pendências: Tudo / Tarefas / Hábitos filters, pending-item presentation and empty states.
  Confirm/correct actions belong to future domain implementations, not fake successful operations.
- Recompensas: balance context, reward-card visual structure in previews, honest empty state at
  runtime. No creation, cost deduction, redemption, or stored reward history yet.
- Histórico de moedas: empty transaction-history destination, with preview rows for visual
  validation. No accounting ledger in this feature.
- Configurações: fully working appearance selection and an entry for explaining earnings and
  penalties. Do not populate a final balancing table from illustrative prototype values.
- Creation/edition forms and difficulty/importance calculators are future work. A present shell
  affordance for an unavailable capability must be omitted, clearly disabled, or explain its
  status; it must never report a save, confirmation, or redemption that did not happen.

The later plan should use the smallest UI models needed to render these states. Do not require
empty repositories, Room databases, artificial business services, or future domain interfaces
just to render empty screens. Keep domain logic separate from presentation when it is introduced.

## Appearance preferences

Offer Claro / Escuro / Seguir o sistema in settings. Persist the selection with Preferences
DataStore. When system mode is selected, follow the device theme; explicit light or dark selection
overrides it. Apply changes across all destinations without manually restarting the Activity,
and retain the choice after reopening the app.

Use Android string resources in Portuguese, English code identifiers, lifecycle-aware state
collection, and the existing Hilt architecture. Keep the setting usable with screen readers and
large fonts. Handle preference read/write failures honestly and recoverably; do not silently
claim a preference was saved when persistence failed. Transient navigation/filter state is not
task data and does not justify a Room database.

## Settled future rules that explain the UI, not implementation scope

Carry these into the specification's dependencies/deferred scope so later chats do not reopen
them, but do not implement them in this shell:

- Today combines tasks and habits from user lists. Each list has Tarefas and Hábitos tabs.
- Tasks can repeat daily, weekdays, selected weekdays, monthly, and every X days. Weeks run Monday
  through Sunday; "this week, weekdays" ends Friday without holiday calculations. Weekly tasks
  may have no fixed day. Missing monthly dates fall on the last day, preserving the original
  day for future months. Every-X-days supports fixed-calendar and completion-relative modes.
- Overdue tasks are reviewed, not automatically penalized. They can be completed after a forgotten
  record or marked Não será feita. That action penalizes only the selected recurring occurrence.
  Rescheduling overdue work requires reviewing its original deadline. Deletion of work that no
  longer makes sense is not a failed-task action and does not itself incur a penalty.
- Habits may build a behavior, avoid it, or impose a maximum frequency. Negative events can occur
  multiple times per day. Each confirmed prohibited event, or event over a maximum, incurs its own
  penalty. Deadline/goal failures require review; do not infer failure merely from missing records.
- One pending-review destination combines tasks and habits with filters, a side-menu counter,
  and a contextual Hoje notice. It allows correction of forgotten records before confirmation.
- Difficulty is Fácil/Média/Difícil and importance is Baixa/Média/Alta. Higher difficulty rewards
  effort more; higher importance raises failure consequences. Selecting Difícil suggests Alta,
  but must not overwrite importance already explicitly adjusted by the user.
- Gains in coins/XP and penalties in coins must be visible in future creation/editing forms,
  with access to the explanatory table there and in settings. Formula/amounts remain undecided.
- Earn coins and XP, progress through levels, and spend coins on user-defined personal rewards.
  No energy system. Failures do not remove XP or levels.
- Penalties may bring coins below zero, but never below −300. Confirmed incidents remain in
  history even at the floor. Reward redemption requires the full cost to be available and
  cannot create debt.

The prototype's +10 coins/+15 XP/−5 coins, initial 120 balance, level 4, progress bar proportions,
and reward prices are illustrative, not agreed balancing rules. The prototype's sample date,
tasks, lists, and messages are not seed data or requirements to hard-code.

## Acceptance expectations

Specify measurable, observable acceptance scenarios covering:
1. Offline launch into the approved Hoje shell and navigation across primary/menu destinations.
2. Working Hoje and review filters, sensible initial empty states, and no fictitious live records.
3. Menu open/close, back navigation, no duplicate root stacking, and visible destination identity.
4. Access from the Hoje balance tile to rewards and from the menu to rewards/history/review.
5. Pending notice/counter behavior for zero and sample populated preview states.
6. Light/dark/system switching, propagation to all screens, persistence after relaunch, and
   recoverable DataStore failures.
7. Readable Portuguese content, labeled actionable icons, suitable touch targets, and usable
   screens at enlarged font settings and with system insets.
8. Clear separation of previews/sample data from production and honest unavailable actions.
9. Relevant build, lint, preference/state checks and navigation/UI validation when implementation
   is later authorized. Report actual verification and device coverage, not assumed success.
10. Foundation/build/signing behavior is preserved; no network services, permissions, Room
    schema, release publication, or currency/task/habit engines are introduced by this shell.

## Boundaries and remaining decisions

There are no blocking product questions for this visual shell. Use the approved HTML and the
recorded rules rather than asking the user to choose B again. Minor Compose/navigation technical
choices belong to planning. Undecided task fields, recurrence edge cases, habit targets,
currency/XP balancing, ledger reversals, reward editing, and full calendar behavior are matters
for later feature specifications. Do not silently decide them while specifying this feature.
