package com.ethos.app.features.shell

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ethos.app.R
import com.ethos.app.core.ui.theme.EthosTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val headerDate = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"))

/** What the drawer's lists section can do: open a list page or just close the drawer before a dialog. */
data class DrawerListActions(val openTasks: () -> Unit, val close: () -> Unit, val tasksOpen: Boolean)

/**
 * App frame: header, drawer and navigation. Each destination's content comes from [page];
 * the shell owns no feature state. [today] comes from the injected clock, never from the system clock.
 */
@Composable
fun EthosShell(
    today: LocalDate,
    pendingCount: Int,
    gameMode: Boolean,
    createActions: CreateActions,
    drawerLists: @Composable (lists: DrawerListActions) -> Unit,
    snackbar: @Composable () -> Unit = {},
    /** Page asked for from outside the shell (a popup link); [onRequestHandled] clears it once navigated. */
    requested: ShellDestination? = null,
    onRequestHandled: () -> Unit = {},
    page: @Composable (destination: ShellDestination, navigate: (ShellDestination) -> Unit) -> Unit,
) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val current = ShellDestination.entries.firstOrNull { it.route == entry?.destination?.route }
        ?: ShellDestination.TODAY
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    /** Sub-pages stack above their parent so Back returns to it; main pages replace each other. */
    fun navigate(destination: ShellDestination) {
        if (destination.parent != null) {
            navController.navigate(destination.route) { launchSingleTop = true }
            return
        }
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }
    fun navigateFromDrawer(destination: ShellDestination) = scope.launch { drawer.close(); navigate(destination) }
    LaunchedEffect(requested) { requested?.let { navigate(it); onRequestHandled() } }
    val leavesGameDestination = !gameMode && current.isGameOnly
    LaunchedEffect(leavesGameDestination) { if (leavesGameDestination) navigate(ShellDestination.TODAY) }
    ModalNavigationDrawer(drawerState = drawer, drawerContent = {
        ShellDrawer(current, pendingCount, ShellDestination.drawerFor(gameMode), ::navigateFromDrawer) {
            drawerLists(DrawerListActions(
                openTasks = { navigateFromDrawer(ShellDestination.TASKS) },
                close = { scope.launch { drawer.close() } },
                tasksOpen = current == ShellDestination.TASKS,
            ))
        }
    }) {
        Scaffold(
            snackbarHost = snackbar,
            floatingActionButton = {
                if (current == ShellDestination.TODAY || current == ShellDestination.TASKS) {
                    TodayCreateButton(createActions)
                }
            },
            topBar = {
                ShellHeader(today, current, HeaderActions(
                    openMenu = { scope.launch { drawer.open() } },
                    back = { navController.popBackStack() },
                    openSettings = { navigate(ShellDestination.SETTINGS) },
                ))
            },
        ) { padding ->
            NavHost(navController, startDestination = ShellDestination.TODAY.route,
                modifier = Modifier.padding(padding)) {
                ShellDestination.entries.forEach { destination ->
                    composable(destination.route) { page(destination, ::navigate) }
                }
            }
        }
    }
    // Installed after NavHost so this callback takes priority over its Back handler.
    BackHandler(enabled = drawer.isOpen) { scope.launch { drawer.close() } }
}

@Composable
private fun ShellDrawer(
    current: ShellDestination, pendingCount: Int, destinations: List<ShellDestination>,
    navigate: (ShellDestination) -> Unit, lists: @Composable () -> Unit,
) {
    ModalDrawerSheet {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.your_space), style = MaterialTheme.typography.labelMedium)
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
            destinations.forEach { destination ->
                NavigationDrawerItem(
                    label = { Text(stringResource(destination.title)) },
                    icon = { Icon(painterResource(destination.icon), contentDescription = null) },
                    selected = current == destination,
                    badge = {
                        if (destination == ShellDestination.REVIEW && pendingCount > 0) Text(pendingCount.toString())
                    },
                    onClick = { navigate(destination) },
                )
            }
            HorizontalDivider()
            Text(stringResource(R.string.your_lists), style = MaterialTheme.typography.titleSmall)
            lists()
            HorizontalDivider()
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.settings)) },
                icon = { Icon(painterResource(ShellDestination.SETTINGS.icon), contentDescription = null) },
                selected = current == ShellDestination.SETTINGS,
                onClick = { navigate(ShellDestination.SETTINGS) },
            )
        }
    }
}

private class HeaderActions(val openMenu: () -> Unit, val back: () -> Unit, val openSettings: () -> Unit)

/** Model B header: date eyebrow over a large title (spec 010). Sub-pages show Back and no settings button. */
@Composable
private fun ShellHeader(today: LocalDate, current: ShellDestination, actions: HeaderActions) {
    val isSubPage = current.parent != null
    Row(
        Modifier.fillMaxWidth()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSubPage) HeaderButton(R.drawable.ic_arrow_back, R.string.back, onClick = actions.back)
        else HeaderButton(R.drawable.ic_menu, R.string.open_menu, onClick = actions.openMenu)
        Column(Modifier.weight(1f).padding(horizontal = 4.dp)) {
            Text(today.format(headerDate).uppercase(Locale.forLanguageTag("pt-BR")),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(current.title), style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
        }
        if (!isSubPage) {
            HeaderButton(ShellDestination.SETTINGS.icon, R.string.open_settings, outlined = true,
                onClick = actions.openSettings)
        }
    }
}

@Composable
private fun HeaderButton(
    @DrawableRes icon: Int, @StringRes label: Int, outlined: Boolean = false, onClick: () -> Unit,
) {
    val description = stringResource(label)
    val modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = description }
    if (outlined) OutlinedIconButton(
        onClick = onClick,
        modifier = Modifier.padding(end = 8.dp).then(modifier),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = IconButtonDefaults.outlinedIconButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
    ) { Icon(painterResource(icon), contentDescription = null) }
    else IconButton(onClick = onClick, modifier = modifier) {
        Icon(painterResource(icon), contentDescription = null)
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun EthosShellPreview() {
    EthosTheme {
        EthosShell(
            today = LocalDate.of(2026, 11, 11),
            pendingCount = 2,
            gameMode = true,
            createActions = CreateActions(taskEnabled = true, habitEnabled = true, {}, {}, {}),
            drawerLists = { Text(stringResource(R.string.inbox)) },
        ) { destination, _ -> Text(stringResource(destination.title), Modifier.padding(20.dp)) }
    }
}
