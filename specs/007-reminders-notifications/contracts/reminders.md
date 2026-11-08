# UI/platform contracts

UI → ReminderViewModel → ReminderRepository; sources via existing core TaskRepository/HabitRepository.
Manager in item details: create/edit/remove, modes/days/time/reference, ONLY_THIS/THIS_AND_FUTURE.
Failed saves retain draft, expose retry; drafts survive recreation. Portuguese resources, scroll and previews.
Reminder dialogs are composed after all existing task/habit dialogs so the editor stays above its origin
after Activity recreation, theme or font changes. Verify native active window, not only isolated content.
Settings: actual app/channel capability, precision explanation/access, chosen summary time/disable.

Notification request URI contains persisted slot token/action OPEN/COMPLETE/SNOOZE. MainActivity validates
identity and current source before action; arbitrary public extras never authorize mutations.
COMPLETE calls existing TasksViewModel.detail/complete after source load; preserves checklist/financial UI.
HABIT never supports COMPLETE. Removed target shows fallback. Consumed request cannot replay on rotation.
Snooze10/30/60/custom future replaces same delivery; never changes deadline or domain state.
Nonexported alarm receiver and protected allowlisted recovery broadcasts only reconcile.
Blocked channel/permission never records delivery as posted.
