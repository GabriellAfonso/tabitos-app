# Research: Habits and goals

## Shared persistence
Decision: extend TaskDatabase and register migration1->2; keep task_lists identity.
Rationale: verified RoomTaskRepository uses withTransaction and soft-deleted lists. Atomic list/habit
exclusion prevents inconsistent active content. Alternatives: second database rejected for atomicity;
JSON snapshots rejected by constitution.

## Historical rules
Decision: effective-dated rule rows, active intervals, per-period explicit overrides and stable records.
Rationale: future-only type changes and corrections require historical interpretation. Alternative:
mutating a single current rule loses prior meaning; a general event framework is unnecessary.

## Integration
Decision: MainActivity assembles task/habit feature slots, one lazy list per destination.
Rationale: existing shell taskPage excludes HABITS/HISTORY; TasksContent list habits tab is placeholder.
Alternative: nested unbounded LazyColumns or feature implementation imports violate existing structure.

## Dependencies and evidence
Retain pinned existing libraries, no dependency selection required. Source and schema inspection plus
research agent confirmed paths and patterns above. Migration must be tested from exported v1 with real
saved task/checklist/audit rows. Existing API36 checks do not establish API24/TalkBack coverage.
All technical unknowns resolved; no new external service/API integration.
