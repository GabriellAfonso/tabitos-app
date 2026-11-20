package com.tabitos.app.features.shell

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tabitos.app.R
import com.tabitos.app.core.ui.components.DrawerRow
import com.tabitos.app.core.ui.components.SectionLabel
import com.tabitos.app.core.ui.theme.TabitosTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

private val headerDate = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale.forLanguageTag("pt-BR"))

/** Open list shown in the header of the list page; [icon] is null for Caixa de entrada, which has none. */
data class ListTitle(val name: String, @param:DrawableRes val icon: Int?, val tint: Color?, val actions: ListTitleActions?)

/** Present only for user lists: Caixa de entrada cannot be edited or deleted. */
data class ListTitleActions(val edit: () -> Unit, val delete: () -> Unit)

/** What the drawer's lists section can do: open a list page or just close the drawer before a dialog. */
data class DrawerListActions(val openTasks: () -> Unit, val close: () -> Unit, val tasksOpen: Boolean)

/**
 * App frame: header, drawer and navigation. Each destination's content comes from [page];
 * the shell owns no feature state. [today] comes from the injected clock, never from the system clock.
 */
@Composable
fun TabitosShell(
    today: LocalDate,
    pendingCount: Int,
    gameMode: Boolean,
    createActions: CreateActions,
    drawerLists: @Composable (lists: DrawerListActions) -> Unit,
    /** Title of the list page; null keeps "Tarefas". */
    listTitle: ListTitle? = null,
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
                ShellHeader(today, current, listTitle, HeaderActions(
                    openMenu = { scope.launch { drawer.open() } },
                    back = { navController.popBackStack() },
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

/**
 * Compact menu (option C of prototypes/menu): app name, pages, "Minhas listas" and the lists scroll; Configurações
 * stays pinned at the bottom, since the header no longer has a shortcut to it.
 */
@Composable
private fun ShellDrawer(
    current: ShellDestination, pendingCount: Int, destinations: List<ShellDestination>,
    navigate: (ShellDestination) -> Unit, lists: @Composable () -> Unit,
) {
    ModalDrawerSheet {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 16.dp)) {
            Text(stringResource(R.string.app_name), Modifier.padding(start = 16.dp, top = 4.dp, bottom = 12.dp),
                style = MaterialTheme.typography.titleLarge)
            destinations.forEach { destination ->
                DestinationRow(destination, current, navigate,
                    badge = pendingCount.takeIf { destination == ShellDestination.REVIEW && it > 0 })
            }
            SectionLabel(stringResource(R.string.your_lists), Modifier.padding(start = 16.dp, top = 16.dp, bottom = 6.dp))
            lists()
        }
        HorizontalDivider()
        Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            DestinationRow(ShellDestination.SETTINGS, current, navigate)
        }
    }
}

@Composable
private fun DestinationRow(
    destination: ShellDestination, current: ShellDestination, navigate: (ShellDestination) -> Unit,
    badge: Int? = null,
) {
    val selected = current == destination
    DrawerRow(stringResource(destination.title), destination.icon, selected,
        Modifier.selectable(selected, role = Role.Tab) { navigate(destination) }, badge = badge)
}

private class HeaderActions(val openMenu: () -> Unit, val back: () -> Unit)

/**
 * Model B header: date eyebrow over a large title (spec 010). Sub-pages show Back instead of the menu.
 * Settings lives only in the drawer, so the header has no shortcut to it. A list page titles itself with
 * the open list ([listTitle]) instead of the generic "Tarefas".
 */
@Composable
private fun ShellHeader(today: LocalDate, current: ShellDestination, listTitle: ListTitle?, actions: HeaderActions) {
    val isSubPage = current.parent != null
    val title = listTitle.takeIf { current == ShellDestination.TASKS }
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                title?.icon?.let { icon ->
                    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(24.dp),
                        tint = title.tint ?: MaterialTheme.colorScheme.primary)
                }
                Text(title?.name ?: stringResource(current.title), style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        title?.actions?.let { ListMenu(title.name, it) }
    }
}

/** Editar/Excluir of the open list, the same choices as holding it in the drawer. */
@Composable
private fun ListMenu(name: String, actions: ListTitleActions) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Box(Modifier.padding(end = 4.dp)) {
        val description = stringResource(R.string.list_options, name)
        IconButton(onClick = { expanded = true },
            modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics {
                contentDescription = description
            }) { Icon(painterResource(R.drawable.ic_more_vert), contentDescription = null) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text(stringResource(R.string.edit)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_edit), contentDescription = null) },
                onClick = { expanded = false; actions.edit() })
            DropdownMenuItem(text = { Text(stringResource(R.string.delete)) },
                leadingIcon = {
                    Icon(painterResource(R.drawable.ic_close), contentDescription = null,
                        tint = MaterialTheme.colorScheme.error)
                },
                onClick = { expanded = false; actions.delete() })
        }
    }
}

@Composable
private fun HeaderButton(@DrawableRes icon: Int, @StringRes label: Int, onClick: () -> Unit) {
    val description = stringResource(label)
    IconButton(onClick = onClick,
        modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).semantics { contentDescription = description }) {
        Icon(painterResource(icon), contentDescription = null)
    }
}

@Preview(showBackground = true, widthDp = 380, heightDp = 820)
@Composable
private fun TabitosShellPreview() {
    TabitosTheme {
        TabitosShell(
            today = LocalDate.of(2026, 11, 11),
            pendingCount = 2,
            gameMode = true,
            createActions = CreateActions(taskEnabled = true, habitEnabled = true, {}, {}, {}),
            drawerLists = { Text(stringResource(R.string.inbox)) },
        ) { destination, _ -> Text(stringResource(destination.title), Modifier.padding(20.dp)) }
    }
}
