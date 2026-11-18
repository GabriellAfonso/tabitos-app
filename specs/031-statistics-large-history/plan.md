# Implementation Plan: Statistics stay fast with a large history

**Branch**: no branch (work stays on `master`) | **Date**: 2026-11-18 | **Spec**: [spec.md](spec.md)

## Summary

Split the statistics computation into a data-only preparation (once per data change) and a cheap per-control
derivation; drop timer checkpoint-only changes before the computation; measure both on a synthetic 5-year dataset
on the JVM; prove identical content. No schema change. Details in [research.md](research.md).

## Technical Context

**Language/Version**: Kotlin (AGP 9 built-in), minSdk 24

**Primary Dependencies**: Coroutines/Flow; no new dependency

**Storage**: Room schema 7, unchanged

**Testing**: JUnit on the JVM (measurement + equality tests)

**Target Platform**: Android phone

**Project Type**: mobile-app, single `:app` module

**Performance Goals**: open < 1 s, control change < 100 ms on the JVM with 5 years of synthetic data

**Constraints**: identical content; read-only; no personal data

**Scale/Scope**: `features/statistics` only

## Constitution Check

- I: spec 031 with decisions and criteria; user asked for plan → tasks → implement. PASS
- II: no schema or stored-data change. PASS
- III: pure functions in `features/statistics`; ViewModel keeps `viewModelScope`; no new layer. PASS
- IV: UI unchanged. PASS
- V: equality tests and measurement on the JVM; phone timing recorded as not executed. PASS

After design: unchanged.

## Design

1. `StatisticsComputation.kt`: `StatisticsData` (the four snapshots), `StatisticsControls`,
   `PreparedStatistics(data, today, zone, gameMode)` with first day, credited time, habit periods and all-time
   history items (all kinds and coins-only) in display order; `content(controls)` derives the page.
2. `StatisticsViewModel`: `prepared = combine(sources, today, gameMode).map(prepare)` then
   `combine(prepared, controls).map(content)`; timer source mapped to `StatisticsTimers` (sessions + closed
   segments) with `distinctUntilChanged`.
3. `historyItems` gains a prepared variant: build once, then filter.
4. Tests: `StatisticsLargeHistoryTest` (synthetic 5 years: timing for prepare and content; equality between
   `computeStatistics` and prepared derivations across period kinds, filters, items, page sizes) and a checkpoint
   test for the timer mapping.

## Project Structure

```text
app/src/main/java/com/ethos/app/features/statistics/
├── StatisticsComputation.kt   # new: prepare + content
├── StatisticsHistory.kt       # prepared history items
└── StatisticsViewModel.kt     # flows split
app/src/test/java/com/ethos/app/features/statistics/
├── StatisticsDataset.kt       # synthetic 5-year data
└── StatisticsLargeHistoryTest.kt
```

## Complexity Tracking

No violations.
