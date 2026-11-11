package com.ethos.app.features.shell

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ethos.app.R
import kotlinx.coroutines.launch

private val ptBr = Locale.forLanguageTag("pt-BR")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EthosShell(
    presentation: ShellPresentation = ShellPresentation(),
    pendingCount: Int = presentation.pendingItems.size,
    drawerLists: (@Composable (() -> Unit) -> Unit)? = null,
    taskPage: (@Composable (ShellDestination, ContentFilter, (ContentFilter) -> Unit,
        () -> Unit, () -> Unit) -> Unit)? = null,
    historyContent: (@Composable () -> Unit)? = null,
    walletContent: (@Composable (() -> Unit) -> Unit)? = null,
    rewardsContent: (@Composable () -> Unit)? = null,
    onNewTask: (() -> Unit)? = null,
    onNewHabit: (() -> Unit)? = null,
    onBreakHabit: () -> Unit = {},
    taskCreationEnabled: Boolean = true,
    habitCreationEnabled: Boolean = true,
    snackbarContent: @Composable () -> Unit = {},
    settingsContent: @Composable () -> Unit,
) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val current = ShellDestination.entries.firstOrNull { it.route == entry?.destination?.route }
        ?: ShellDestination.TODAY
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val bottom = listOf(
        ShellDestination.TODAY, ShellDestination.TASKS, ShellDestination.HABITS, ShellDestination.CALENDAR,
    )
    val menu = listOf(
        ShellDestination.TODAY, ShellDestination.REVIEW, ShellDestination.REWARDS, ShellDestination.HISTORY,
    )
    fun navigate(destination: ShellDestination) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(stringResource(R.string.your_space), style = MaterialTheme.typography.labelMedium)
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                    menu.forEach { destination ->
                        NavigationDrawerItem(
                            label = { Text(stringResource(destination.title)) },
                            selected = current == destination,
                            badge = {
                                if (destination == ShellDestination.REVIEW && pendingCount > 0) {
                                    Text(pendingCount.toString())
                                }
                            },
                            onClick = { scope.launch { drawer.close(); navigate(destination) } },
                        )
                    }
                    HorizontalDivider()
                    Text(stringResource(R.string.your_lists), style = MaterialTheme.typography.titleSmall)
                    if (drawerLists != null) {
                        drawerLists { scope.launch { drawer.close(); navigate(ShellDestination.TASKS) } }
                    } else {
                        Text(stringResource(R.string.no_lists), style = MaterialTheme.typography.bodyMedium)
                        Text(stringResource(R.string.create_list_future), style = MaterialTheme.typography.labelLarge)
                    }
                    HorizontalDivider()
                    NavigationDrawerItem(
                        label = { Text(stringResource(R.string.settings)) },
                        selected = current == ShellDestination.SETTINGS,
                        onClick = { scope.launch { drawer.close(); navigate(ShellDestination.SETTINGS) } },
                    )
                }
            }
        },
    ) {
        Scaffold(
            snackbarHost = snackbarContent,
            floatingActionButton = {
                if (current == ShellDestination.TODAY && onNewTask != null && onNewHabit != null) {
                    TodayCreateButton(taskCreationEnabled, habitCreationEnabled, onNewTask, onNewHabit, onBreakHabit)
                }
            },
            topBar = {
                Row(
                    Modifier.fillMaxWidth()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
                        )
                        .padding(horizontal = 4.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LabeledSymbol("☰", R.string.open_menu) { scope.launch { drawer.open() } }
                    // Model B header: date eyebrow over a large title (spec 010).
                    Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        Text(
                            LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", ptBr))
                                .uppercase(ptBr),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            stringResource(current.title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    LabeledSymbol("⚙", R.string.open_settings, outlined = true) {
                        navigate(ShellDestination.SETTINGS)
                    }
                }
            },
            bottomBar = {
                NavigationBar {
                    bottom.forEach { destination ->
                        val description = stringResource(R.string.open_destination, stringResource(destination.title))
                        NavigationBarItem(
                            selected = current == destination,
                            onClick = { navigate(destination) },
                            icon = { Text(destination.symbol) },
                            label = { Text(stringResource(destination.title)) },
                            modifier = Modifier.semantics { contentDescription = description },
                        )
                    }
                }
            },
        ) { padding ->
            NavHost(
                navController,
                startDestination = ShellDestination.TODAY.route,
                modifier = Modifier.padding(padding),
            ) {
                ShellDestination.entries.forEach { destination ->
                    composable(destination.route) {
                        var filter by rememberSaveable { mutableStateOf(ContentFilter.ALL) }
                        if (taskPage != null && destination in listOf(ShellDestination.TODAY,
                                ShellDestination.TASKS, ShellDestination.REVIEW, ShellDestination.CALENDAR,
                                ShellDestination.HABITS)) {
                            taskPage(destination, filter, { filter = it },
                                { navigate(ShellDestination.REWARDS) }, { navigate(ShellDestination.REVIEW) })
                        } else if (destination == ShellDestination.HISTORY && historyContent != null) {
                            historyContent()
                        } else if (destination == ShellDestination.REWARDS && rewardsContent != null) {
                            rewardsContent()
                        } else ShellPage {
                            when (destination) {
                                ShellDestination.TODAY -> TodayContent(
                                    presentation, filter, { filter = it },
                                    { navigate(ShellDestination.REWARDS) }, { navigate(ShellDestination.REVIEW) },
                                )
                                ShellDestination.TASKS -> EmptyContent(
                                    R.string.tasks_empty, R.string.tasks_empty_detail,
                                )
                                ShellDestination.HABITS -> EmptyContent(
                                    R.string.habits_empty, R.string.habits_empty_detail,
                                )
                                ShellDestination.CALENDAR -> EmptyContent(
                                    R.string.calendar_empty, R.string.calendar_empty_detail,
                                )
                                ShellDestination.REVIEW -> ReviewContent(
                                    presentation.pendingItems, filter,
                                ) { filter = it }
                                ShellDestination.REWARDS -> {
                                    if (walletContent != null) walletContent { navigate(ShellDestination.REWARDS) }
                                    else BalanceTiles(presentation) { navigate(ShellDestination.REWARDS) }
                                    EmptyContent(R.string.rewards_empty, R.string.rewards_empty_detail)
                                }
                                ShellDestination.HISTORY -> EmptyContent(
                                    R.string.history_empty, R.string.history_empty_detail,
                                )
                                ShellDestination.SETTINGS -> settingsContent()
                            }
                        }
                    }
                }
            }
        }
    }
    // Installed after NavHost so this callback takes priority over its Back handler.
    BackHandler(enabled = drawer.isOpen) { scope.launch { drawer.close() } }
}

@Composable
fun ShellPage(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(PaddingValues(20.dp)),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}

@Composable
private fun LabeledSymbol(symbol: String, label: Int, outlined: Boolean = false, onClick: () -> Unit) {
    val description = stringResource(label)
    if (outlined) OutlinedIconButton(
        onClick = onClick,
        modifier = Modifier.padding(end = 8.dp).sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { contentDescription = description },
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) { Text(symbol, style = MaterialTheme.typography.titleMedium) }
    else IconButton(
        onClick = onClick,
        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
            .semantics { contentDescription = description },
    ) { Text(symbol, style = MaterialTheme.typography.titleLarge) }
}
