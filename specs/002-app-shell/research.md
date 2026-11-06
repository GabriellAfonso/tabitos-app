# Research: application shell

## Navigation
Decision: existing Navigation Compose with string routes, singleTop, popUpTo(start) saveState and
restoreState. Rationale: only eight routes; avoids serialization plugin and repeated root stacks.
Alternatives: manually tracking current screen would lose navigation lifecycle/restoration guarantees;
Navigation3 would change stack without need.
Source: https://developer.android.com/develop/ui/compose/navigation
Source: https://developer.android.com/guide/navigation/backstack/multi-back-stacks

## Preferences
Decision: singleton Preferences DataStore, explicit enum string and application-context Hilt binding.
Rationale: one preference needs atomic persistence, not Room. Unknown mode defaults SYSTEM;
IOException must reach durable error state, retry restarts collection/edit. No corruption reset.
Alternative: SharedPreferences offers no benefit and conflicts with project preference choice.
Source: https://developer.android.com/topic/libraries/architecture/datastore

## UI and state
Decision: fixed approved sage color schemes, Material3 drawer/scaffold/cards, scrollable content,
lifecycle-aware StateFlow and saveable filters. Runtime models empty; samples in preview-only functions.
Rationale: preserve design without fake business functionality. Settings content supplied as shell slot
keeps features independent. Larger fonts can stack tiles/use horizontal scroll for filter buttons.
Source: https://developer.android.com/develop/ui/compose/state-saving
Source: https://developer.android.com/develop/ui/compose/accessibility

## Verification
Decision: real temporary-file JVM DataStore tests plus fake failure ViewModel tests; Compose instrumented
navigation tests and available device checks. No unit tests for static resource echoing.
Dependency versions remain centrally pinned and will be recorded after resolution/build.

## Resolved dependency versions
Navigation Compose 2.9.8, Lifecycle Compose 2.10.0, DataStore Preferences 1.2.1.
Lifecycle2.11 requires AGP9.2, so it is incompatible with the retained AGP9.0.1.
These conservative pins still require actual build verification, not a claim of vendor certification.
https://developer.android.com/jetpack/androidx/releases/navigation
https://developer.android.com/jetpack/androidx/releases/lifecycle
https://developer.android.com/jetpack/androidx/releases/datastore
