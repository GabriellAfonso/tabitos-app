package com.ethos.app
import com.ethos.app.core.tasks.domain.TaskAction

import androidx.activity.compose.ReportDrawnWhen
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ethos.app.core.preferences.AppearanceMode
import com.ethos.app.core.reminders.domain.ReminderKind
import com.ethos.app.core.startup.DeferredSource
import com.ethos.app.core.timers.domain.TimerKind
import com.ethos.app.core.timers.domain.TimerRules
import com.ethos.app.core.timers.domain.TimerState
import com.ethos.app.core.ui.timers.TimerChip
import com.ethos.app.features.timers.clockText
import com.ethos.app.core.ui.accounting.LocalFinancialPrices
import com.ethos.app.core.ui.components.ActionBanner
import com.ethos.app.core.ui.components.ContentFilter
import com.ethos.app.core.ui.components.FilterRow
import com.ethos.app.core.ui.components.SettingsLinkRow
import com.ethos.app.core.ui.components.SettingsSectionHeader
import com.ethos.app.core.ui.reminders.LocalOpenReminders
import com.ethos.app.core.ui.theme.EthosTheme
import com.ethos.app.core.ui.timers.LocalOpenTimer
import com.ethos.app.core.ui.trackers.LocalOpenTracker
import com.ethos.app.features.accounting.AccountingSettings
import com.ethos.app.features.accounting.EarningsSettingRow
import com.ethos.app.features.accounting.AccountingUiState
import com.ethos.app.features.accounting.AccountingViewModel
import com.ethos.app.features.accounting.WalletTiles
import com.ethos.app.features.calendar.CalendarScreen
import com.ethos.app.features.calendar.CalendarViewModel
import com.ethos.app.features.habits.HabitDialogs
import com.ethos.app.features.habits.HabitPage
import com.ethos.app.features.habits.HabitsContent
import com.ethos.app.features.habits.HabitsRefreshEffect
import com.ethos.app.features.habits.HabitsUiState
import com.ethos.app.features.habits.HabitsViewModel
import com.ethos.app.features.habits.habitItems
import com.ethos.app.features.reminders.ReminderDialogs
import com.ethos.app.features.reminders.NotificationSettings
import com.ethos.app.features.reminders.NotificationSystemSettings
import com.ethos.app.features.reminders.notificationSettingsActions
import com.ethos.app.features.reminders.ReminderUiState
import com.ethos.app.features.reminders.ReminderViewModel
import com.ethos.app.features.reminders.rememberNotificationPermissionRequest
import com.ethos.app.features.rewards.RewardsScreen
import com.ethos.app.features.rewards.RewardsViewModel
import com.ethos.app.features.settings.AppearanceActions
import com.ethos.app.features.settings.AppearanceUiState
import com.ethos.app.features.settings.AppearanceViewModel
import com.ethos.app.features.settings.GameModeSetting
import com.ethos.app.features.settings.GameModeUiState
import com.ethos.app.features.settings.GameModeViewModel
import com.ethos.app.features.settings.ThemeSetting
import com.ethos.app.features.settings.TimerAlarmSetting
import com.ethos.app.features.shell.CreateActions
import com.ethos.app.features.shell.EthosShell
import com.ethos.app.features.shell.ShellDestination
import com.ethos.app.features.shell.StartupViewModel
import com.ethos.app.features.statistics.StatisticsItem
import com.ethos.app.features.statistics.StatisticsNavigation
import com.ethos.app.features.statistics.StatisticsScreen
import com.ethos.app.features.statistics.StatisticsViewModel
import com.ethos.app.features.tasks.DrawerListCallbacks
import com.ethos.app.features.tasks.RescheduledSnackbar
import com.ethos.app.features.tasks.TaskDialogs
import com.ethos.app.features.tasks.TaskDrawer
import com.ethos.app.features.tasks.TaskPage
import com.ethos.app.features.tasks.TasksContent
import com.ethos.app.features.tasks.TasksRefreshEffect
import com.ethos.app.features.tasks.TasksUiState
import com.ethos.app.features.tasks.TasksViewModel
import com.ethos.app.features.timers.TimerCardActions
import com.ethos.app.features.timers.TimerDialogs
import com.ethos.app.features.timers.TimerSessionCards
import com.ethos.app.features.timers.TimerTicker
import com.ethos.app.features.timers.TimerUiState
import com.ethos.app.features.timers.TimerViewModel
import com.ethos.app.features.timers.timerTarget
import com.ethos.app.features.timers.visibleSessions
import com.ethos.app.features.trackers.TrackerDialogs
import com.ethos.app.features.trackers.TrackerPendingCard
import com.ethos.app.features.trackers.TrackerTicker
import com.ethos.app.features.trackers.TrackerUiState
import com.ethos.app.features.trackers.TrackerViewModel
import kotlinx.coroutines.CancellationException

/** Requests from notification Intents and popup links; each is consumed once the needed state has loaded. */
@Stable
class AppRequests {
    /** Reminder deep link and a token that makes its handling idempotent across recreation. */
    var reminder by mutableStateOf<Pair<String, String>?>(null)
    /** Session id from a timer notification, resolved to its source after timers load. */
    var timerSession by mutableStateOf<String?>(null)
    var timer by mutableStateOf<Pair<TimerKind, String>?>(null)
    /** Page asked for by a popup link, such as "Ver histórico" (spec 024); cleared once the shell navigates. */
    var destination by mutableStateOf<ShellDestination?>(null)

    /** A notification is being handled; it must never wait for the preload queue (spec 027). */
    val handlingNotification: Boolean get() = reminder != null || timerSession != null || timer != null
}

/** Activity-scoped ViewModels: they are shared by several destinations, dialogs and notifications. */
@Stable
private class AppViewModels(
    val tasks: TasksViewModel, val habits: HabitsViewModel, val accounting: AccountingViewModel,
    val rewards: RewardsViewModel, val reminders: ReminderViewModel, val timers: TimerViewModel,
    val trackers: TrackerViewModel, val calendar: CalendarViewModel, val gameMode: GameModeViewModel,
    val statistics: StatisticsViewModel, val startup: StartupViewModel,
)

/** One frame of collected state, so page functions receive values instead of collecting again. */
private class AppState(
    val tasks: TasksUiState, val habits: HabitsUiState, val financial: AccountingUiState,
    val reminders: ReminderUiState, val timers: TimerUiState, val trackers: TrackerUiState,
    val requestPermission: () -> Unit, val timerCards: TimerCardActions, val gameMode: GameModeUiState,
) {
    /** Coins, XP, rewards and their screens are shown only with game mode on (spec 017). */
    val showsGame: Boolean = gameMode.enabled
    /** Revisão counts tasks only: habits never become pending (spec 014). */
    val pendingReviews: Int = tasks.snapshot.activeOccurrences.count { it.needsReview(tasks.today) }
    val todayReady: Boolean = TodaySources(tasks, habits, timers, trackers, financial, gameMode).isReady()
}

@Composable
private fun appViewModels() = AppViewModels(
    viewModel(), viewModel(), viewModel(), viewModel(), viewModel(), viewModel(), viewModel(), viewModel(), viewModel(),
    viewModel(), viewModel(),
)

/** Composition root of the app; [onDarkTheme] lets the Activity style the system bars. */
@Composable
fun EthosApp(requests: AppRequests, onDarkTheme: (Boolean) -> Unit) {
    val appearanceViewModel: AppearanceViewModel = viewModel()
    val appearance by appearanceViewModel.state.collectAsStateWithLifecycle()
    val dark = when (appearance.mode) {
        AppearanceMode.SYSTEM -> isSystemInDarkTheme()
        AppearanceMode.LIGHT -> false
        AppearanceMode.DARK -> true
    }
    SideEffect { onDarkTheme(dark) }
    EthosTheme(darkTheme = dark) {
        val vm = appViewModels()
        val tasks by vm.tasks.state.collectAsStateWithLifecycle()
        val habits by vm.habits.state.collectAsStateWithLifecycle()
        val financial by vm.accounting.state.collectAsStateWithLifecycle()
        val reminders by vm.reminders.state.collectAsStateWithLifecycle()
        val timers by vm.timers.state.collectAsStateWithLifecycle()
        val trackers by vm.trackers.state.collectAsStateWithLifecycle()
        val gameMode by vm.gameMode.state.collectAsStateWithLifecycle()
        TasksRefreshEffect(vm.tasks)
        HabitsRefreshEffect(vm.habits)
        TimerTicker(vm.timers)
        TrackerTicker(vm.trackers)
        val requestPermission = rememberNotificationPermissionRequest(vm.reminders::refresh)
        val timerCards = TimerCardActions(
            open = { requests.timer = it.kind to it.sourceId },
            pause = vm.timers::pause,
            resume = { session ->
                timerTarget(session.kind, session.sourceId, tasks.snapshot, habits.snapshot, timers.snapshot,
                    tasks.today, vm.timers.zone)?.let { vm.timers.start(it.start) }
            },
            discard = vm.timers::requestDiscard,
        )
        val app = AppState(tasks, habits, financial, reminders, timers, trackers, requestPermission, timerCards,
            gameMode)
        StartupOrder(vm.startup, app.todayReady, requests.handlingNotification)
        NotificationRouting(requests, vm, app)
        LaunchedEffect(tasks.snapshot.activeLists) {
            vm.habits.listIds(tasks.snapshot.activeLists.map { it.id }.toSet())
        }
        CompositionLocalProvider(
            LocalOpenReminders provides vm.reminders::open,
            LocalOpenTimer provides { kind, id -> requests.timer = kind to id },
            LocalOpenTracker provides vm.trackers::open,
            LocalFinancialPrices provides financial.snapshot.prices.takeUnless { financial.loading || financial.error },
        ) {
            // Order matters: later dialogs stack above earlier ones.
            HabitDialogs(habits, tasks.snapshot.activeLists, vm.habits) { habitId ->
                vm.habits.detail(null)
                openStatistics(vm, requests, StatisticsItem.Habit(habitId))
            }
            AppShell(vm, app, requests, appearance, AppearanceActions(appearanceViewModel::select,
                appearanceViewModel::retry, appearanceViewModel::setTimerAlarm))
            TrackerDialogs(trackers, vm.trackers, requestPermission)
            TimerDialogs(timers, vm.timers, { kind, id ->
                if (kind == TimerKind.TASK) vm.tasks.detail(id) else vm.habits.detail(id)
            }, requestPermission)
            TaskDialogs(tasks, vm.tasks) { taskId ->
                vm.tasks.detail(null)
                openStatistics(vm, requests, StatisticsItem.TaskSeries(taskId))
            }
            ReminderDialogs(reminders, vm.reminders, requestPermission)
        }
    }
}

/** "Ver histórico" of a task series or habit: Estatísticas › Histórico filtered by it (spec 024). */
private fun openStatistics(vm: AppViewModels, requests: AppRequests, item: StatisticsItem) {
    vm.statistics.focus(item)
    requests.destination = ShellDestination.STATISTICS
}

@Composable
private fun AppShell(
    vm: AppViewModels, app: AppState, requests: AppRequests, appearance: AppearanceUiState,
    appearanceActions: AppearanceActions,
) {
    val tasks = app.tasks
    val habits = app.habits
    EthosShell(
        today = tasks.today,
        pendingCount = app.pendingReviews,
        gameMode = app.showsGame,
        createActions = CreateActions(
            taskEnabled = !tasks.loading && !tasks.busy,
            habitEnabled = !habits.loading && !habits.busy,
            onNewTask = vm.tasks::newTask,
            onNewHabit = { vm.habits.newHabit(tasks.selectedListId) },
            onBreakHabit = { vm.habits.newHabit(tasks.selectedListId, breaking = true) },
        ),
        drawerLists = { drawer ->
            TaskDrawer(tasks, drawer.tasksOpen, DrawerListCallbacks(
                onSelect = { vm.tasks.selectList(it); drawer.openTasks() },
                onNew = { drawer.close(); vm.tasks.newList() },
                onEdit = { drawer.close(); vm.tasks.listDraft(it) },
                onDelete = { drawer.close(); vm.tasks.requestDeleteList(it.id) },
                onReorder = { lists -> vm.tasks.action(TaskAction.OrderLists(lists.map { it.id })) },
            ))
        },
        snackbar = {
            tasks.rescheduled?.let {
                RescheduledSnackbar(it, tasks.today, undo = vm.tasks::undoReschedule,
                    dismiss = vm.tasks::dismissRescheduled)
            }
        },
        requested = requests.destination,
        onRequestHandled = { requests.destination = null },
    ) { destination, navigate ->
        LaunchedEffect(destination) { vm.startup.request(destination.deferredSources()) }
        when (destination) {
            ShellDestination.TODAY -> TaskDestination(vm, app, TaskPage.TODAY, navigate)
            ShellDestination.TASKS -> TaskDestination(vm, app, TaskPage.LIST, navigate)
            ShellDestination.REVIEW -> TaskDestination(vm, app, TaskPage.REVIEW, navigate)
            ShellDestination.CALENDAR -> CalendarScreen(vm.calendar, vm.tasks::detail, vm.habits::detail,
                vm.trackers::fill, vm.trackers::editEntry)
            ShellDestination.REWARDS -> RewardsScreen(vm.rewards) { RewardSessions(vm, app) }
            ShellDestination.STATISTICS -> StatisticsScreen(tasks.today, vm.timers.zone, StatisticsNavigation(
                openTask = vm.tasks::detail, openHabit = vm.habits::detail,
                openRewards = { navigate(ShellDestination.REWARDS) },
            ), vm.statistics)
            ShellDestination.EXCLUDED_HABITS -> HabitsContent(habits, HabitPage.EXCLUDED, vm.habits::detail,
                vm.habits::retry)
            ShellDestination.SETTINGS -> SettingsDestination(vm, app, appearance, appearanceActions, navigate)
            ShellDestination.EARNINGS -> SettingsSubPage {
                AccountingSettings(app.financial, vm.accounting::edit, vm.accounting::draft, vm.accounting::save,
                    vm.accounting::retry)
            }
        }
    }
}

/** Hoje, list pages and Revisão share the task list; Hoje and list pages also show habits. */
@Composable
private fun TaskDestination(
    vm: AppViewModels, app: AppState, page: TaskPage, navigate: (ShellDestination) -> Unit,
) {
    if (page == TaskPage.TODAY && !app.todayReady) {
        TodayLoading()
        return
    }
    val tasks = app.tasks
    var filter by rememberSaveable { mutableStateOf(ContentFilter.ALL) }
    val habitPage = when (page) {
        TaskPage.TODAY -> HabitPage.TODAY
        TaskPage.REVIEW -> HabitPage.REVIEW
        TaskPage.LIST -> HabitPage.LIST
    }
    TasksContent(
        state = tasks, page = page, filter = filter,
        onOpen = vm.tasks::detail, onAction = vm.tasks::action,
        onRetry = vm.tasks::retry,
        onDismissError = vm.tasks::dismissError, onComplete = vm.tasks::quickCheck,
        onNotDone = { vm.tasks.action(TaskAction.Resolve(it, false, tasks.today)) },
        timerChip = { app.timerChip(vm, TimerKind.TASK, it.id, tasks.durations[it.taskId]) },
        extraItems = {
            habitItems(app.habits, habitPage, tasks.selectedListId, vm.habits::detail, vm.habits::retry,
                vm.habits::quickRecord,
                timerChip = { app.timerChip(vm, TimerKind.HABIT, it.id, app.habits.durations[it.id]) })
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (page) {
                TaskPage.TODAY -> {
                    TodayHeader(vm, app, { navigate(ShellDestination.REWARDS) }, { navigate(ShellDestination.REVIEW) })
                    FilterRow(filter) { filter = it }
                }
                TaskPage.REVIEW, TaskPage.LIST -> Unit
            }
        }
    }
}

/** Hoje appears complete, so no card shows a missing price or a wrong timer state while sources load. */
@Composable
private fun TodayLoading() {
    Text(stringResource(R.string.loading_tasks), Modifier.padding(20.dp))
}

/**
 * Hoje's sources load first; deferred sources start after Hoje is shown, or at once while a notification is
 * handled. "Fully drawn" is logged with the time from launch to Hoje's content, never with any content (spec 027).
 */
@Composable
private fun StartupOrder(startup: StartupViewModel, todayReady: Boolean, handlingNotification: Boolean) {
    ReportDrawnWhen { todayReady }
    LaunchedEffect(todayReady) { if (todayReady) startup.todayShown() }
    LaunchedEffect(handlingNotification) {
        if (handlingNotification) startup.request(DeferredSource.entries.toSet())
    }
}

/** Wallet, running timers, pending trackers and the review banner above Hoje's list. */
@Composable
private fun TodayHeader(vm: AppViewModels, app: AppState, openRewards: () -> Unit, openReview: () -> Unit) {
    // The shell header already shows the date (spec 010).
    Text(stringResource(R.string.one_step), style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (app.showsGame) WalletTiles(app.financial, openRewards, vm.accounting::retry)
    TimerSessionCards(app.timers, app.visibleTimerSessions(vm), vm.timers.zone, app.timerCards)
    TrackerPendingCard(app.trackers.pending.size) {
        app.trackers.pending.firstOrNull()?.let { vm.trackers.fill(it.id, app.trackers.today) }
    }
    val pending = app.pendingReviews
    if (pending > 0) {
        ActionBanner(pluralStringResource(R.plurals.pending_count, pending, pending), R.drawable.ic_pending_actions,
            openReview, urgent = true)
    }
}

@Composable
private fun RewardSessions(vm: AppViewModels, app: AppState) {
    val sessions = app.visibleTimerSessions(vm).filter { it.kind == TimerKind.REWARD }
    if (sessions.isNotEmpty()) {
        Text(stringResource(R.string.timer_reward_sessions), style = MaterialTheme.typography.titleMedium)
    }
    TimerSessionCards(app.timers, sessions, vm.timers.zone, app.timerCards)
}

/** Countdown on a timed task/habit card (spec 016); null for the normal variant. */
private fun AppState.timerChip(vm: AppViewModels, kind: TimerKind, sourceId: String, minutes: Int?): TimerChip? {
    if (minutes == null) return null
    val zone = vm.timers.zone
    val target = timerTarget(kind, sourceId, tasks.snapshot, habits.snapshot, timers.snapshot, tasks.today, zone)
        ?: return null
    val session = timers.snapshot.open(kind, sourceId)
    val progress = TimerRules.progress(timers.snapshot, kind, sourceId, target.start.period, tasks.today,
        timers.now, zone)
    val remaining = session?.let { TimerRules.remaining(timers.snapshot, it, timers.now, zone) }
        ?: (minutes * MILLIS_PER_MINUTE - progress).coerceAtLeast(0)
    val running = session?.state == TimerState.RUNNING
    return TimerChip(clockText(remaining), running) {
        if (running && session != null) vm.timers.pause(session.id) else vm.timers.start(target.start)
    }
}

private const val MILLIS_PER_MINUTE = 60_000L

private fun AppState.visibleTimerSessions(vm: AppViewModels) =
    visibleSessions(tasks.snapshot, habits.snapshot, timers, vm.timers.zone)
        .filter { showsGame || it.kind != TimerKind.REWARD }

/** Settings as a grouped list (spec 025): Aparência, Modo jogo, Notificações e alarmes, Dados. */
@Composable
private fun SettingsDestination(
    vm: AppViewModels, app: AppState, appearance: AppearanceUiState, appearanceActions: AppearanceActions,
    navigate: (ShellDestination) -> Unit,
) {
    val notifications = notificationSettingsActions(vm.reminders, app.requestPermission)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
        SettingsSectionHeader(stringResource(R.string.appearance))
        ThemeSetting(appearance, appearanceActions.select, appearanceActions.retry)
        SettingsSectionHeader(stringResource(R.string.game_mode_setting))
        GameModeSetting(app.gameMode, vm.gameMode::request, vm.gameMode::confirmOff)
        if (app.showsGame) EarningsSettingRow(app.financial) { navigate(ShellDestination.EARNINGS) }
        SettingsSectionHeader(stringResource(R.string.settings_notifications))
        NotificationSettings(app.reminders, notifications)
        TimerAlarmSetting(appearance.timerAlarm, appearanceActions.setTimerAlarm)
        NotificationSystemSettings(app.reminders, notifications)
        SettingsSectionHeader(stringResource(R.string.settings_data))
        SettingsLinkRow(stringResource(R.string.habit_excluded), ShellDestination.EXCLUDED_HABITS.icon,
            onClick = { navigate(ShellDestination.EXCLUDED_HABITS) })
    }
}

@Composable
private fun SettingsSubPage(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
}

/**
 * Routes notification taps once tasks, habits, trackers and timers have loaded. Opening a tracker form never
 * resolves its notice; only a saved entry does. Timer completions go through the existing completion flows so
 * checklist, financial preview and single-award rules still apply.
 */
@Composable
private fun NotificationRouting(requests: AppRequests, vm: AppViewModels, app: AppState) {
    val tasks = app.tasks
    val habits = app.habits
    val timers = app.timers
    val ready = !tasks.loading && !habits.loading
    LaunchedEffect(requests.reminder, ready, app.trackers.loading) {
        val request = requests.reminder ?: return@LaunchedEffect
        if (!ready || app.trackers.loading) return@LaunchedEffect
        val uri = request.first.toUri()
        val action = uri.pathSegments.getOrNull(0).orEmpty()
        val key = uri.pathSegments.getOrNull(1).orEmpty()
        try {
            vm.reminders.request(key, action, request.second)?.let { row ->
                when (row.kind) {
                    ReminderKind.TASK -> vm.tasks.notification(row.sourceId, action == "COMPLETE")
                    ReminderKind.HABIT -> vm.habits.detail(row.sourceId)
                    ReminderKind.TRACKER -> vm.trackers.fill(row.sourceId, row.slotDate)
                }
            }
        } catch (e: CancellationException) { throw e
        } catch (_: Exception) { vm.reminders.fallback(true) }
        requests.reminder = null
    }
    LaunchedEffect(requests.timerSession, timers.loading, ready) {
        val id = requests.timerSession ?: return@LaunchedEffect
        if (timers.loading || !ready) return@LaunchedEffect
        timers.snapshot.sessions.firstOrNull { it.id == id }?.let { requests.timer = it.kind to it.sourceId }
        requests.timerSession = null
    }
    LaunchedEffect(requests.timer, timers.loading, ready) {
        val request = requests.timer ?: return@LaunchedEffect
        if (timers.loading || !ready) return@LaunchedEffect
        if (!app.showsGame && request.first == TimerKind.REWARD) { requests.timer = null; return@LaunchedEffect }
        vm.timers.open(timerTarget(request.first, request.second, tasks.snapshot, habits.snapshot, timers.snapshot,
            tasks.today, vm.timers.zone))
        requests.timer = null
    }
    LaunchedEffect(timers.completion) {
        val completion = timers.completion ?: return@LaunchedEffect
        when (completion.kind) {
            TimerKind.TASK -> vm.tasks.timerCompleted(completion.sourceId, completion.date)
            TimerKind.HABIT -> vm.habits.timerCompleted(completion.sourceId, completion.date)
            TimerKind.REWARD -> Unit
        }
        vm.timers.consumeCompletion(completion.token)
    }
}
