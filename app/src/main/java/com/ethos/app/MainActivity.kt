package com.ethos.app

import android.os.Bundle
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.SideEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ethos.app.core.preferences.AppearanceMode
import com.ethos.app.core.ui.theme.EthosTheme
import com.ethos.app.features.settings.AppearanceScreen
import com.ethos.app.features.settings.AppearanceViewModel
import com.ethos.app.features.settings.SettingsContent
import com.ethos.app.features.shell.EthosShell
import com.ethos.app.features.shell.ShellDestination
import com.ethos.app.features.shell.ShellPresentation
import com.ethos.app.features.shell.BalanceTiles
import com.ethos.app.features.shell.FilterRow
import com.ethos.app.features.tasks.TasksViewModel
import com.ethos.app.features.tasks.TasksScreen
import com.ethos.app.features.tasks.TasksContent
import com.ethos.app.features.tasks.TaskDrawer
import com.ethos.app.features.tasks.TaskPage
import com.ethos.app.features.habits.*
import com.ethos.app.features.accounting.*
import com.ethos.app.features.rewards.*
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ethos.app.core.ui.accounting.LocalFinancialPrices
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import java.time.format.DateTimeFormatter
import java.util.Locale
import dagger.hilt.android.AndroidEntryPoint
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ethos.app.features.reminders.*
import com.ethos.app.core.ui.reminders.LocalOpenReminders
import com.ethos.app.core.reminders.domain.ReminderKind
import java.util.UUID
import androidx.core.net.toUri
import com.ethos.app.core.timers.domain.TimerKind
import com.ethos.app.core.timers.platform.TimerCoordinator
import com.ethos.app.core.ui.timers.LocalOpenTimer
import com.ethos.app.features.timers.*
import com.ethos.app.features.trackers.*
import com.ethos.app.features.calendar.CalendarScreen
import com.ethos.app.features.calendar.CalendarViewModel
import com.ethos.app.core.ui.trackers.LocalOpenTracker
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var timerCoordinator: TimerCoordinator
    private var reminderRequest by mutableStateOf<Pair<String, String>?>(null)
    /** Session id from a timer notification, resolved once task/habit/timer state has loaded. */
    private var timerSessionRequest by mutableStateOf<String?>(null)
    private var timerRequest by mutableStateOf<Pair<TimerKind, String>?>(null)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptReminder(intent)
    }
    override fun onStart() {
        super.onStart()
        // Foreground is the reliable moment to (re)attach the session service after kills or refused starts.
        timerCoordinator.request()
    }
    private fun acceptReminder(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "ethos-reminder" && data.authority == "local")
            reminderRequest = data.toString() to UUID.randomUUID().toString()
        if (data.scheme == "ethos-timer" && data.authority == "local" && data.pathSegments.firstOrNull() == "OPEN")
            timerSessionRequest = data.pathSegments.getOrNull(1)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) acceptReminder(intent)
        else reminderRequest = savedInstanceState.getString("reminderUri")?.let {
            it to savedInstanceState.getString("reminderToken").orEmpty() }
        enableEdgeToEdge()
        setContent {
            val appearanceViewModel: AppearanceViewModel = viewModel()
            val demoViewModel: com.ethos.app.features.settings.DemoViewModel = viewModel()
            AppearanceScreen(appearanceViewModel) { state, onSelect, onRetry ->
                val dark = when (state.mode) {
                    AppearanceMode.SYSTEM -> isSystemInDarkTheme()
                    AppearanceMode.LIGHT -> false
                    AppearanceMode.DARK -> true
                }
                SideEffect {
                    val bars = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                    enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
                }
                EthosTheme(darkTheme = dark) {
                    val tasksViewModel: TasksViewModel = viewModel()
                    val habitsViewModel: HabitsViewModel = viewModel()
                    val accountingViewModel: AccountingViewModel = viewModel()
                    val rewardsViewModel: RewardsViewModel = viewModel()
                    val reminderViewModel: ReminderViewModel = viewModel()
                    val reminderState by reminderViewModel.state.collectAsStateWithLifecycle()
                    val rewardsState by rewardsViewModel.state.collectAsStateWithLifecycle()
                    val timerViewModel: TimerViewModel = viewModel()
                    val timerState by timerViewModel.state.collectAsStateWithLifecycle()
                    TimerTicker(timerViewModel)
                    val trackerViewModel: TrackerViewModel = viewModel()
                    val trackerState by trackerViewModel.state.collectAsStateWithLifecycle()
                    val calendarViewModel: CalendarViewModel = viewModel()
                    TrackerTicker(trackerViewModel)
                    ReminderPermissionControls(reminderState, reminderViewModel::refresh) { requestPermission ->
                    CompositionLocalProvider(LocalOpenReminders provides reminderViewModel::open,
                        LocalOpenTimer provides { kind, id -> timerRequest = kind to id },
                        LocalOpenTracker provides trackerViewModel::open) {
                    AccountingScreen(accountingViewModel) { financial ->
                    CompositionLocalProvider(LocalFinancialPrices provides
                        financial.snapshot.prices.takeUnless { financial.loading || financial.error }) {
                    TasksScreen(tasksViewModel) { tasks ->
                      HabitsScreen(habitsViewModel) { habits ->
                        LaunchedEffect(reminderRequest, tasks.loading, habits.loading, trackerState.loading) {
                            val request = reminderRequest
                            if (request != null && !tasks.loading && !habits.loading && !trackerState.loading) {
                                val uri = request.first.toUri()
                                val action = uri.pathSegments.getOrNull(0).orEmpty()
                                val key = uri.pathSegments.getOrNull(1).orEmpty()
                                try {
                                    val row = reminderViewModel.request(key, action, request.second)
                                    if (row != null) {
                                        when (row.kind) {
                                            ReminderKind.TASK ->
                                                tasksViewModel.notification(row.sourceId, action == "COMPLETE")
                                            ReminderKind.HABIT -> habitsViewModel.detail(row.sourceId)
                                            // Opening the form never resolves the notice; only a saved entry does.
                                            ReminderKind.TRACKER -> trackerViewModel.fill(row.sourceId, row.slotDate)
                                        }
                                    }
                                    reminderRequest = null
                                } catch (e: kotlinx.coroutines.CancellationException) { throw e
                                } catch (_: Exception) {
                                    reminderViewModel.fallback(true)
                                    reminderRequest = null
                                }
                            }
                        }
                        val zone = timerViewModel.zone
                        LaunchedEffect(timerSessionRequest, timerState.loading, tasks.loading, habits.loading) {
                            val id = timerSessionRequest
                            if (id != null && !timerState.loading && !tasks.loading && !habits.loading) {
                                timerState.snapshot.sessions.firstOrNull { it.id == id }?.let {
                                    timerRequest = it.kind to it.sourceId }
                                timerSessionRequest = null
                            }
                        }
                        LaunchedEffect(timerRequest, timerState.loading, tasks.loading, habits.loading) {
                            val request = timerRequest
                            if (request != null && !timerState.loading && !tasks.loading && !habits.loading) {
                                timerViewModel.open(timerTarget(request.first, request.second, tasks.snapshot,
                                    habits.snapshot, timerState.snapshot, tasks.today, zone))
                                timerRequest = null
                            }
                        }
                        LaunchedEffect(timerState.completion) {
                            val completion = timerState.completion ?: return@LaunchedEffect
                            // Existing flows keep checklist, financial preview and single-award rules.
                            when (completion.kind) {
                                TimerKind.TASK -> tasksViewModel.notification(completion.sourceId, true)
                                TimerKind.HABIT -> habitsViewModel.recordDuration(completion.sourceId, completion.date)
                                TimerKind.REWARD -> Unit
                            }
                            timerViewModel.consumeCompletion(completion.token)
                        }
                        val timerCards = TimerCardActions(open = { timerRequest = it.kind to it.sourceId },
                            pause = timerViewModel::pause, resume = { session ->
                                timerTarget(session.kind, session.sourceId, tasks.snapshot, habits.snapshot,
                                    timerState.snapshot, tasks.today, zone)?.let { timerViewModel.start(it.start) }
                            })
                        val sessions = visibleSessions(tasks.snapshot, habits.snapshot, timerState, zone)
                        LaunchedEffect(tasks.snapshot.activeLists) {
                            habitsViewModel.listIds(tasks.snapshot.activeLists.map { it.id }.toSet())
                        }
                        HabitDialogs(habits, tasks.snapshot.activeLists, habitsViewModel)
                        // Revisão counts tasks only: habits never become pending (spec 014).
                        val pending = tasks.snapshot.activeOccurrences.count { it.needsReview(tasks.today) }
                        EthosShell(
                            snackbarContent = {
                                HabitPracticeFeedback(habits, habitsViewModel::undoPractice,
                                    habitsViewModel::dismissPracticeFeedback)
                            },
                            pendingCount = pending,
                            onNewTask = tasksViewModel::newTask,
                            onNewHabit = { habitsViewModel.newHabit(tasks.selectedListId) },
                            onBreakHabit = { habitsViewModel.newHabit(tasks.selectedListId, breaking = true) },
                            taskCreationEnabled = !tasks.loading && !tasks.busy,
                            habitCreationEnabled = !habits.loading && !habits.busy,
                            drawerLists = { navigate ->
                                TaskDrawer(tasks, { tasksViewModel.selectList(it); navigate() },
                                    tasksViewModel::newList, { tasksViewModel.manageLists(true) })
                            },
                            taskPage = { destination, filter, onFilter, onRewards, onReview ->
                              if (destination == ShellDestination.CALENDAR) {
                                CalendarScreen(calendarViewModel, tasksViewModel::detail, habitsViewModel::detail,
                                    trackerViewModel::fill, trackerViewModel::editEntry)
                              } else if (destination == ShellDestination.HABITS) {
                                HabitsContent(habits, HabitPage.ALL, { habitsViewModel.newHabit(null) },
                                    habitsViewModel::detail, habitsViewModel::retry, habitsViewModel::quickRecord,
                                    { habitsViewModel.newHabit(null, breaking = true) })
                              } else {
                                val page = when (destination) {
                                    ShellDestination.TODAY -> TaskPage.TODAY
                                    ShellDestination.REVIEW -> TaskPage.REVIEW
                                    else -> TaskPage.LIST
                                }
                                TasksContent(tasks, page, filter.ordinal, tasksViewModel::newTask,
                                    tasksViewModel::detail, tasksViewModel::action,
                                    { tasksViewModel.manageLists(true) }, tasksViewModel::retry,
                                    tasksViewModel::dismissError,
                                    onComplete = tasksViewModel::quickCheck,
                                    extraItems = {
                                        habitItems(habits, when (page) {
                                            TaskPage.TODAY -> HabitPage.TODAY
                                            TaskPage.REVIEW -> HabitPage.REVIEW
                                            else -> HabitPage.LIST
                                        }, tasks.selectedListId,
                                            { habitsViewModel.newHabit(tasks.selectedListId) },
                                            habitsViewModel::detail, habitsViewModel::retry,
                                            habitsViewModel::quickRecord)
                                    }) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        if (page == TaskPage.TODAY) {
                                            // The shell header already shows the date (spec 010).
                                            Text(stringResource(R.string.one_step),
                                                style = MaterialTheme.typography.bodyLarge,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            WalletTiles(financial, onRewards, accountingViewModel::retry)
                                            TimerSessionCards(timerState, sessions, zone, timerCards)
                                            TrackerPendingCard(trackerState.pending.size) {
                                                trackerState.pending.firstOrNull()?.let {
                                                    trackerViewModel.fill(it.id, trackerState.today) }
                                            }
                                            if (pending > 0) com.ethos.app.core.ui.components.ActionBanner(
                                                pluralStringResource(R.plurals.pending_count, pending, pending), "◷",
                                                onReview, urgent = true)
                                        }
                                        if (page == TaskPage.REVIEW) Text(stringResource(R.string.review_hint))
                                        if (page == TaskPage.TODAY) FilterRow(filter, onFilter)
                                    }
                                }
                              }
                            },
                            historyContent = { AccountingHistory(financial, accountingViewModel::retry,
                                rewardsState.snapshot.purchases) },
                            rewardsContent = { RewardsScreen(rewardsViewModel) {
                                val rewardSessions = sessions.filter { it.kind == TimerKind.REWARD }
                                if (rewardSessions.isNotEmpty()) Text(stringResource(R.string.timer_reward_sessions),
                                    style = MaterialTheme.typography.titleMedium)
                                TimerSessionCards(timerState, rewardSessions, zone, timerCards)
                            } },
                            walletContent = { rewards -> WalletTiles(financial, rewards, accountingViewModel::retry) },
                            settingsContent = {
                                SettingsContent(state, onSelect, onRetry)
                                if (BuildConfig.DEBUG) com.ethos.app.features.settings.DemoSettingsScreen(demoViewModel)
                                AccountingSettings(financial, accountingViewModel::edit, accountingViewModel::draft,
                                    accountingViewModel::save, accountingViewModel::retry)
                                ReminderSettingsContent(reminderState, reminderViewModel, requestPermission)
                            },
                        )
                        TrackerDialogs(trackerState, trackerViewModel, requestPermission)
                        TimerDialogs(timerState, timerViewModel, { kind, id ->
                            if (kind == TimerKind.TASK) tasksViewModel.detail(id) else habitsViewModel.detail(id)
                        }, requestPermission)
                      }
                    }
                    }
                    }
                    ReminderDialogs(reminderState, reminderViewModel, requestPermission)
                    }
                    }
                }
            }
        }
    }
    override fun onSaveInstanceState(outState: Bundle) {
        reminderRequest?.let { outState.putString("reminderUri", it.first)
            outState.putString("reminderToken", it.second) }
        super.onSaveInstanceState(outState)
    }
}
