# Feature Specification: Android foundation

**Feature Branch**: none (no Git repository)
**Created**: 2026-11-06
**Status**: Implemented; verification coverage and limitations in verification.md
**Input**: `TODO/specify_fundacao_android_app.md`; current request also authorizes plan, tasks and implement.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Build the foundation (Priority: P1)

The owner/developer can open the project and produce an installable development package.
**Why this priority**: Future features require a reproducible foundation.
**Independent Test**: Follow documented setup without personal signing credentials.
**Acceptance Scenarios**:
1. **Given** prerequisites, **When** building a fresh checkout, **Then** an installable debug package is produced.
2. **Given** no release key, **When** validating production configuration, **Then** an optimized unsigned package builds.

### User Story 2 - Launch offline (Priority: P1)

The owner can install and open Ethos on Android 7.0 or newer and see its name.
**Why this priority**: Proves that the foundation starts.
**Independent Test**: Install and launch without network access.
**Acceptance Scenarios**:
1. **Given** the installed app offline, **When** launched, **Then** one screen shows “Ethos” without crash,
   login, onboarding or permission requests.
2. **Given** enlarged fonts and light/dark appearance, **When** opened or rotated, **Then** the name remains readable.

### User Story 3 - Maintain safely (Priority: P2)

The developer can reproduce checks and prepare a locally signed production package without exposing private files.
**Why this priority**: Keeps future development reproducible and private.
**Independent Test**: Follow documentation and inspect source and verification evidence.
**Acceptance Scenarios**:
1. **Given** delivered source, **When** inspected, **Then** keys, secrets, SDK paths and caches are excluded.
2. **Given** verification evidence, **When** reviewed, **Then** executed and unavailable checks are distinguished.

### Edge Cases

- Missing prerequisites and unavailable downloads are setup failures, not runtime network requirements.
- Unavailable devices, including the minimum supported version, must be reported as unverified coverage.
- No production key: debug and unsigned production validation remain possible.
- Largest system fonts, landscape and dark appearance must preserve readability.

## Requirements *(mandatory)*

### Escopo e restrições do Ethos

Engineering foundation only, not a usable task manager. No personal data or permissions.
Exclude tasks, habits, routines, persistence, preference controls, navigation destinations, backend,
login, synchronization, analytics, publishing and CI/CD. Technical constraints from the source prompt
and constitution are resolved in the plan, not expanded into future features.

### Functional Requirements

- **FR-001**: Deliver one reproducibly buildable Android app named Ethos with documented setup and included build launcher.
- **FR-002**: Produce an installable debug package without personal credentials or production signing material.
- **FR-003**: Support Android 7.0 and newer with initial identity `com.ethos.app`.
- **FR-004**: Display “Ethos” on one launch screen offline without permission, login or onboarding prerequisites.
- **FR-005**: Keep visible text in Portuguese resources and readable with enlarged fonts and light/dark system appearance.
- **FR-006**: Separate development and optimized production builds; production must never use the debug signing identity.
- **FR-007**: Exclude secrets, signing material, machine SDK paths and caches from delivered source.
- **FR-008**: Document prerequisites, commands, artifacts, local signing and verification limitations.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Documented setup produces one installable development package without personal credentials.
- **SC-002**: Every executed launch scenario displays Ethos offline without crash; report minimum-version coverage separately.
- **SC-003**: Entire name is readable in portrait/landscape at largest system font setting in both appearances on tested devices.
- **SC-004**: Source inspection finds zero included secrets, signing keys, machine SDK paths or caches.
- **SC-005**: Evidence distinguishes every executed check and unavailable scenario; empty tests are not behavioral coverage.

## Assumptions

- Development setup may download dependencies; runtime remains offline.
- Owner supplies development prerequisites and any future production key locally.
- No blocking product questions remain; tool versions belong to planning.
- Minimal rendering theme only; persisted appearance and navigation are explicitly deferred by the prompt.
- Current user request supersedes the saved prompt's specify-only execution restriction.
