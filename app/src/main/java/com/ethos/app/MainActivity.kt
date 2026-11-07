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
import androidx.compose.runtime.LaunchedEffect
import java.time.format.DateTimeFormatter
import java.util.Locale
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                    TasksScreen(tasksViewModel) { tasks ->
                      HabitsScreen(habitsViewModel) { habits ->
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
                                            BalanceTiles(ShellPresentation(), onRewards)
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
                            settingsContent = { SettingsContent(state, onSelect, onRetry) },
                        )
                      }
                    }
                }
            }
        }
    }
}
