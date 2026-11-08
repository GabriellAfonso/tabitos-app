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

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private var reminderRequest by mutableStateOf<Pair<String, String>?>(null)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        acceptReminder(intent)
    }
    private fun acceptReminder(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "ethos-reminder" && data.authority == "local")
            reminderRequest = data.toString() to UUID.randomUUID().toString()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) acceptReminder(intent)
        else reminderRequest = savedInstanceState.getString("reminderUri")?.let {
            it to savedInstanceState.getString("reminderToken").orEmpty() }
        enableEdgeToEdge()
        setContent {
            val appearanceViewModel: AppearanceViewModel = viewModel()
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
                    ReminderPermissionControls(reminderState, reminderViewModel::refresh) { requestPermission ->
                    CompositionLocalProvider(LocalOpenReminders provides reminderViewModel::open) {
                    AccountingScreen(accountingViewModel) { financial ->
                    CompositionLocalProvider(LocalFinancialPrices provides
                        financial.snapshot.prices.takeUnless { financial.loading || financial.error }) {
                    TasksScreen(tasksViewModel) { tasks ->
                      HabitsScreen(habitsViewModel) { habits ->
                        LaunchedEffect(reminderRequest, tasks.loading, habits.loading) {
                            val request = reminderRequest
                            if (request != null && !tasks.loading && !habits.loading) {
                                val uri = request.first.toUri()
                                val action = uri.pathSegments.getOrNull(0).orEmpty()
                                val key = uri.pathSegments.getOrNull(1).orEmpty()
                                try {
                                    val row = reminderViewModel.request(key, action, request.second)
                                    if (row != null) {
                                        if (row.kind == ReminderKind.TASK) {
                                            tasksViewModel.notification(row.sourceId, action == "COMPLETE")
                                        } else habitsViewModel.detail(row.sourceId)
                                    }
                                    reminderRequest = null
                                } catch (e: kotlinx.coroutines.CancellationException) { throw e
                                } catch (_: Exception) {
                                    reminderViewModel.fallback(true)
                                    reminderRequest = null
                                }
                            }
                        }
                        LaunchedEffect(tasks.snapshot.activeLists) {
                            habitsViewModel.listIds(tasks.snapshot.activeLists.map { it.id }.toSet())
                        }
                        HabitDialogs(habits, tasks.snapshot.activeLists, habitsViewModel)
                        val pending = tasks.snapshot.activeOccurrences.count { it.needsReview(tasks.today) } +
                            habits.pendingCount
                        EthosShell(
                            pendingCount = pending,
                            drawerLists = { navigate ->
                                TaskDrawer(tasks, { tasksViewModel.selectList(it); navigate() },
                                    tasksViewModel::newList, { tasksViewModel.manageLists(true) })
                            },
                            taskPage = { destination, filter, onFilter, onRewards, onReview ->
                              if (destination == ShellDestination.HABITS) {
                                HabitsContent(habits, HabitPage.ALL, { habitsViewModel.newHabit(null) },
                                    habitsViewModel::detail, habitsViewModel::retry)
                              } else {
                                val page = when (destination) {
                                    ShellDestination.TODAY -> TaskPage.TODAY
                                    ShellDestination.REVIEW -> TaskPage.REVIEW
                                    ShellDestination.CALENDAR -> TaskPage.CALENDAR
                                    else -> TaskPage.LIST
                                }
                                TasksContent(tasks, page, filter.ordinal, tasksViewModel::newTask,
                                    tasksViewModel::detail, tasksViewModel::action,
                                    { tasksViewModel.manageLists(true) }, tasksViewModel::retry,
                                    tasksViewModel::dismissError,
                                    extraItems = {
                                        habitItems(habits, when (page) {
                                            TaskPage.TODAY -> HabitPage.TODAY
                                            TaskPage.REVIEW -> HabitPage.REVIEW
                                            else -> HabitPage.LIST
                                        }, tasks.selectedListId,
                                            { habitsViewModel.newHabit(tasks.selectedListId) },
                                            habitsViewModel::detail, habitsViewModel::retry)
                                    }) {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        if (page == TaskPage.TODAY) {
                                            Text(tasks.today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM",
                                                Locale.forLanguageTag("pt-BR"))))
                                            Text(stringResource(R.string.one_step),
                                                style = MaterialTheme.typography.headlineSmall)
                                            WalletTiles(financial, onRewards, accountingViewModel::retry)
                                            if (pending > 0) Card(onClick = onReview) {
                                                Text(pluralStringResource(R.plurals.pending_count, pending, pending),
                                                    Modifier.padding(16.dp))
                                            }
                                        }
                                        if (page == TaskPage.REVIEW) Text(stringResource(R.string.review_hint))
                                        if (page == TaskPage.TODAY || page == TaskPage.REVIEW) {
                                            FilterRow(filter, onFilter)
                                        }
                                    }
                                }
                              }
                            },
                            historyContent = { AccountingHistory(financial, accountingViewModel::retry,
                                rewardsState.snapshot.purchases) },
                            rewardsContent = { RewardsScreen(rewardsViewModel) },
                            walletContent = { rewards -> WalletTiles(financial, rewards, accountingViewModel::retry) },
                            settingsContent = {
                                SettingsContent(state, onSelect, onRetry)
                                AccountingSettings(financial, accountingViewModel::edit, accountingViewModel::draft,
                                    accountingViewModel::save, accountingViewModel::retry)
                                ReminderSettingsContent(reminderState, reminderViewModel, requestPermission)
                            },
                        )
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
