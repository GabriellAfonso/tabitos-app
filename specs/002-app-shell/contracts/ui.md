# UI and preference contracts
- Eight routes start on today. Four bottom entries only. Drawer closes on selection and Back;
  repeated root taps add no entries. Other roots return to today on Back.
- Today balance opens rewards. Header settings opens settings. Review only drawer/positive notice.
- Today/review filters independently select type; zero records means no numeric pending badge/notice.
- Production passes empty models; previews pass explicit samples. Missing balance/level is unavailable,
  never invented zero earned progress. All future operation affordances explain status or are omitted.
- Settings slot receives AppearanceUiState and onSelect/onRetry callbacks; global theme uses saved mode.
- AppearanceRepository: modes Flow<AppearanceMode>; suspend setMode(mode). Failures propagate;
  cancellation propagates. Single writer UI disables options while saving/loading/read-error.
- Errors remain in UiState until explicit retry or successful recovery; write errors retain saved mode.
- Screen receives ViewModel, collects lifecycle-aware; content has only state/callbacks.
- All text is Portuguese resource data. Icon-only actions have labels; 48dp targets and scroll/insets.
