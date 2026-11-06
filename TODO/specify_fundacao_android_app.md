# Specify — Ponto 1: fundação Android do Ethos

**Estado:** pronto para executar a etapa specify em outro chat.
**Data:** 2026-11-06.

## Como usar

No outro chat, aberto na raiz `C:/Users/gabri/Projetos/ethos`, peça para executar
`$speckit-specify` usando o corpo em inglês após o separador `---` deste arquivo.
Isso autoriza apenas a etapa specify; plan, tasks e implement serão solicitados separadamente.

## Referências e origem

- `AGENTS.md`: instruções operacionais e fluxo de prompts salvos.
- `.specify/memory/constitution.md`: constituição v1.0.0.
- `PLANO_BASE.md`: contexto inicial; este prompt detalha e delimita o ponto 1.
- Referência de convenções: `C:/Users/gabri/Projetos/Ipb_castelo_branco/app`.
- Conversa de origem: conversa de planejamento do Ethos em 2026-11-06.
  ID, link e caminho de transcript não estão disponíveis neste contexto.
  As decisões necessárias estão registradas integralmente abaixo.
- Não há arquivo de backend correspondente: backend não faz parte desta entrega.

---

Create a feature specification for the Android project foundation of Ethos.
Execute only the Spec Kit specify stage. Do not implement the application, generate a technical
implementation plan or task list, commit changes, publish artifacts, or advance to another stage.

## Verified starting state

The repository is C:/Users/gabri/Projetos/ethos. At the time this prompt was saved it contains
AGENTS.md, PLANO_BASE.md, local Spec Kit skills under .agents/skills/, and Spec Kit infrastructure
under .specify/. Constitution version 1.0.0 is adopted. There is no Android application module,
Gradle build configuration, Gradle Wrapper, application source code, or implemented feature.
An earlier premature Android scaffold was removed at the user's request. Reinspect the current
repository before writing the specification; do not assume that previously removed code exists.

The user wants to design each feature in one conversation and hand a self-contained specify prompt
to another chat. Creating this specification must not be interpreted as permission to implement it.
Read AGENTS.md and the constitution first. Use this file as the record of settled decisions and
avoid asking the user to repeat them.

## Product intent and delivery sequence

Ethos will be a personal Android task, habit, and routine organizer inspired by TickTick.
The immediate priority is to make it useful for its owner. Distribution will initially be by APK;
Google Play publication is a future possibility once the app is mature enough.

The agreed base is divided into three separate specifications:
1. Android project foundation: this feature.
2. Visual structure, navigation, and configurable appearance persisted with DataStore: later.
3. Local task persistence using Room, exercised through a minimal real task flow: later.

These boundaries supersede the broader early foundation wording in PLANO_BASE.md. Do not merge
all three into this specification. This first delivery is intentionally an engineering foundation,
not an already usable task manager.

## Settled identity and platform decisions

- Display name and project name: Ethos. The user explicitly kept this name.
- Native Android application written in Kotlin.
- Minimum Android SDK: 24 (Android 7.0), explicitly accepted to retain older-device compatibility.
- Initial application namespace/base application ID: com.ethos.app, the documented initial choice.
  No personal domain was supplied. Do not block specification on requesting one.
- Start with a single :app application module. Additional modules require a concrete later need.
- Initial visible delivery: launch the app and show a simple screen displaying "Ethos".
  The user accepted this as the proof that the project builds and starts.
- Initial distribution is an installable APK for personal use. No Play Store release work now.

## Foundation scope

Specify a minimal reproducible Android project that can be opened in Android Studio, built through
the checked-in Gradle Wrapper, installed, and launched on a supported Android device or emulator.
The main screen must render the Ethos name using Compose and Material 3 without network access,
login, onboarding, permissions, or external service credentials.

The chosen foundation tools are Kotlin, Gradle Kotlin DSL, a centralized version catalog,
Jetpack Compose, Material 3, Hilt, and KSP. Use one launcher Activity and an application entry point
compatible with Hilt. Hilt/KSP must be genuinely integrated with the application entry points;
do not create artificial business models, repositories, or services merely to demonstrate injection.

Use AGP 9 with built-in Kotlin as required by the project guidance. The later plan stage must
validate exact compatible versions of AGP, Gradle, Kotlin/Compose tooling, Hilt, KSP, Java, and SDKs.
Do not present unverified versions or compatibility escape flags copied from the reference as
settled requirements. compileSdk and targetSdk are technical planning decisions, not open product
questions that must block this specify stage.

Establish a source layout that can grow into feature-oriented organization with shared core
infrastructure. Create only files needed for this foundation; do not require empty layer trees,
placeholder ViewModels, empty navigation graphs, or speculative domain abstractions.

Keep debug and release build configurations separate and enable R8 for release. Provide a debug
APK that can be installed for the owner's initial checks. Release APK signing must use a local
key outside version control; never use the debug signing key as the release identity. Do not
generate a personal release key or require the owner to supply signing credentials just to sync,
build debug, or specify the project. Document the local signing/build workflow when implemented;
do not invent a signing automation or credential storage scheme at the specification stage.

Provide appropriate ignore rules for machine-specific files, SDK paths, build output, and signing
material. Document Android Studio/JDK/SDK prerequisites, wrapper commands, expected artifact
locations, and basic installation/launch verification. Do not copy local.properties, caches,
personal configuration, credentials, app branding, or church business rules from the IPB app.

The foundation must use Android string resources for visible text and follow the project's
Portuguese UI/English identifier convention. Its minimal screen must remain readable with larger
font settings and must not require a full design system or a custom branded icon at this stage.
A minimal Material theme needed to render the screen is in scope; theme selection and persisted
appearance are not.

## Accepted future tools, explicitly deferred integration

Room, DataStore, Coroutines/Flow, ViewModel/StateFlow, and Navigation Compose are agreed tools for
the wider app. This feature must not create a Room database/schema, DataStore preference storage,
task model, state management layer, or multi-destination navigation just to preinstall future work.
Introduce dependencies when this feature actually requires them; do not treat an unused dependency
list as an acceptance criterion. The next features will introduce their real behavior.

## Out of scope

- Task CRUD, completion, habit tracking, routines, recurrence rules, reminders, and scheduling.
- Today/Habits/Calendar/Settings destinations, bottom navigation, and calendar UI.
- Persisted preferences, theme selector, a complete design system, and polished visual branding.
- Room entities, DAOs, repositories, migrations, or dummy databases.
- Backend, API clients, Retrofit, authentication, Google login, Firebase, push notifications,
  analytics, crash-reporting services, and cross-device synchronization.
- Network or notification permissions without a concrete feature that needs them.
- Play Console setup, store listing, application bundles as a distribution requirement,
  publishing, CI/CD infrastructure, automatic releases, version bump automation, or Git commits.

## Acceptance and verification expectations

Express acceptance criteria that can later be demonstrated, including:
- A fresh checkout can sync and build using documented prerequisites and the included Wrapper.
  Dependency download during development is allowed; offline runtime does not imply offline setup.
- A debug APK is generated and can be installed on a supported device/emulator.
- Launch shows the Ethos name without a crash or a network/authentication prerequisite.
- minSdk 24 is configured; device coverage and any unavailable runtime checks are reported honestly.
- Hilt/KSP application integration compiles; release shrinking configuration is valid and release
  build validation does not silently depend on personal signing material or a debug release key.
- No secrets, signing keys, local SDK paths, or build caches are checked into version control.
- Relevant build and lint checks run when implementation is eventually requested. Validate minimal
  screen readability and launch. Add tests only for meaningful behavior; do not fabricate unit tests
  or pretend that NO-SOURCE test tasks prove behavior.
- Document commands executed, artifacts produced, and any checks not run due to environment limits.

## Decision status

There are no blocking product questions left for this foundation. Exact dependency/tool versions,
compile/target SDKs, temporary launcher styling, and compatible build details belong to the later
plan stage. This prompt records the scope and constraints; it does not authorize that stage.
