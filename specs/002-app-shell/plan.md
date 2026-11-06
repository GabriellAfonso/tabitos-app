# Implementation Plan: Interface, navigation and appearance
**Branch**: master (existing) | **Date**: 2026-11-06 | **Spec**: [spec.md](spec.md)

## Summary
Replace foundation screen with the approved native visual shell. Navigation Compose supplies saved
navigation state; a single appearance ViewModel uses an injected repository backed by Preferences
DataStore. Empty screens need no repositories, Room, domain engines or extra modules.

## Technical Context
**Language/Version**: existing Kotlin 2.3.10 built into AGP 9.0.1, Gradle 9.3.1, JVM17/JDK21.
**Primary Dependencies**: existing Compose BOM 2026.02.00, Material3, Hilt2.59.2/KSP2.3.4;
Navigation Compose, Lifecycle runtime/viewmodel Compose, Preferences DataStore; JUnit/coroutines-test,
Compose instrumentation tests for real UI navigation and temporary-file DataStore tests.
**Storage**: Preferences DataStore only; appearance string SYSTEM/LIGHT/DARK, no Room/schema.
**Testing**: JVM repository/ViewModel failure and persistence tests, Compose navigation tests,
build/debug lint/unsigned R8 release, available emulator UI checks.
**Target Platform**: min24, compile/target36; native Android single :app, com.ethos.app.
**Performance Goals**: direct offline navigation within two actions; no background domain work.
**Constraints**: no permissions/network services/login, secrets or signing changes; Portuguese resources.
**Scale/Scope**: 8 destinations, 2 independently saved filters, 3 persisted appearances, one owner.

## Constitution Check
Before research and after design all gates PASS:
- I: validated spec, current request authorizes plan/tasks/implementation; analyze precedes code.
- II: offline Preferences DataStore, explicit retry and no false saved state; no structured records.
- III: Screen/content split, StateFlow lifecycle collection, Hilt ViewModel -> repository interface ->
  DataStore; no Android in domain contract. Empty screens have only presentation models.
- IV: sage light/dark tokens, strings resources, 48dp actions, scroll/insets/font checks; no personal logs.
- V: meaningful preference failure/persistence and navigation tests; evidence with device limitations.

## Project Structure
Documentation: spec.md, plan.md, research.md, data-model.md, contracts/ui.md, quickstart.md,
tasks.md, checklists/requirements.md and verification.md in specs/002-app-shell/.
Source (under app/src/main/java/com/ethos/app/):
- MainActivity.kt connects injected AppearanceViewModel through AppearanceScreen.
- core/preferences/{AppearanceRepository,DataStoreAppearanceRepository,PreferencesModule}.kt
- core/ui/theme/EthosTheme.kt: approved palette, no dynamic color override.
- features/settings/{AppearanceViewModel,AppearanceScreen,SettingsContent}.kt
- features/shell/{EthosShell,ShellContent,ShellModels,ShellPreviews}.kt
- res/values/strings.xml for all runtime/preview Portuguese texts.
Tests: app/src/test/.../{core/preferences,features/settings}; app/src/androidTest/.../features/shell.
Existing foundation screen may be removed after replacement; do not keep unused launch UI.

**Structure Decision**: one shell feature owns empty destinations; settings owns appearance controls,
shared preference contract is in core. Shell imports settings content but not its implementation/data;
move shared settings content into shell if necessary to avoid cross-feature implementation imports.
Concretely settings exposes appearance Screen/content to MainActivity; shell renders settings through
an injected composable slot so features do not import each other.

## Navigation and state policy
String routes, NavHost starts today. Navigation always popUpTo(today) with saveState, launchSingleTop,
restoreState. Thus at most Hoje + one other top-level destination is active; Back returns Hoje.
Drawer Back takes priority via BackHandler; selecting closes drawer. Bottom visible across shell,
only four entries, none selected at secondary destinations. rememberSaveable filters scoped to entries.
Restored process keeps entry/filter, a fresh launch starts today. Information is an expandable settings
section rather than an extra root destination.

## Preference failure policy
Repository emits mode or propagates failures; one singleton DataStore. ViewModel explicitly subscribes
and retries failed collection. Read failure shows system fallback and durable inline retry; writes are
serialized by disabling controls until completion. Update visible saved selection only after successful
edit; retain previous selection on failure with attempted choice for retry. Cancellation is rethrown.
Do not swallow corruption/read failures into an apparent successful emptyPreferences result.

## Verification strategy
JVM temporary-file DataStore integration tests verify reopening/unknown values. Fake repositories test
read failure, write failure, retry and saved selection. Compose tests cover repeated taps/Back/drawer,
empty states and filters. Preview samples exercise positive counts and matching records. Run debug,
unit tests, lint and release; connected tests and visual landscape/font/theme/relaunch checks if device
available. Record unavailable API24/TalkBack rather than treating configured minSdk as runtime coverage.

## Complexity Tracking
No violations, new modules, use cases, Room, empty domain repositories or backend.

## Execution notes
- Existing working branch master preserved. No new branch/commit requested or created.
- Context update script failed locating python3.exe; only the managed AGENTS.md block was updated directly.
- All implementation phases complete; validation evidence and limitations are in verification.md.

Final UI refinement: header uses a content-sized Material Row with safe top/horizontal insets, so
long titles grow rather than clip at enlarged fonts. Activity SystemBarStyle follows the effective
app appearance, including explicit dark override while the device is light. MainActivity obtains its
Hilt-backed ViewModel via Lifecycle Compose's default Activity factory.
