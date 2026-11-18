package com.ethos.app.core.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ethos.app.R
import com.ethos.app.core.icons.AppIcon
import com.ethos.app.core.icons.IconCategory
import kotlinx.coroutines.launch

// Fixed height so the sheet does not jump while the search narrows the grid.
private const val SHEET_HEIGHT_FRACTION = 0.9f

/** Whole icon catalog with search and category shortcuts; choosing an icon closes it, dismissing changes nothing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IconCatalogSheet(selected: AppIcon, tint: Color, onDismiss: () -> Unit, onChoose: (AppIcon) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val iconNames = AppIcon.entries.associateWith { stringResource(it.option.name) }
    val categoryNames = IconCategory.entries.associateWith { stringResource(it.label) }
    val catalog = iconCatalog(query, iconNames::getValue, categoryNames::getValue)
    val grid = rememberLazyGridState()
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(SHEET_HEIGHT_FRACTION)) {
            CatalogHeader(onClose = onDismiss)
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true, placeholder = { Text(stringResource(R.string.icon_search)) },
                leadingIcon = { Icon(painterResource(R.drawable.ic_search), contentDescription = null) })
            if (showsCategoryShortcuts(query)) {
                CategoryShortcuts(catalog.sections.map { it.category }, categoryNames::getValue) { category ->
                    scope.launch { grid.animateScrollToItem(catalog.headerPositions.getValue(category)) }
                }
            }
            if (catalog.isEmpty) {
                Text(stringResource(R.string.icon_search_empty, query), Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                CatalogGrid(catalog.sections, selected, tint, grid, categoryNames::getValue, onChoose)
            }
        }
    }
}

@Composable
private fun CatalogHeader(onClose: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.icon_catalog_title), Modifier.weight(1f).semantics { heading() },
            style = MaterialTheme.typography.titleLarge)
        IconButton(onClose) {
            Icon(painterResource(R.drawable.ic_close), contentDescription = stringResource(R.string.close))
        }
    }
}

@Composable
private fun CategoryShortcuts(
    categories: List<IconCategory>, categoryName: (IconCategory) -> String, onOpen: (IconCategory) -> Unit,
) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { category ->
            AssistChip(onClick = { onOpen(category) }, label = { Text(categoryName(category)) })
        }
    }
}

@Composable
private fun CatalogGrid(
    sections: List<IconSection>, selected: AppIcon, tint: Color, state: LazyGridState,
    categoryName: (IconCategory) -> String, onChoose: (AppIcon) -> Unit,
) {
    LazyVerticalGrid(GridCells.Adaptive(52.dp), Modifier.selectableGroup(), state = state,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        sections.forEach { section ->
            item(key = "category:${section.category.name}", span = { GridItemSpan(maxLineSpan) }) {
                Text(categoryName(section.category), Modifier.padding(top = 16.dp, bottom = 6.dp)
                    .semantics { heading() }, style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(section.icons, key = { "icon:${it.name}" }) { icon ->
                Box(contentAlignment = Alignment.Center) {
                    IconOptionButton(icon.option, isSelected = icon == selected, enabled = true, tint = tint,
                        onClick = { onChoose(icon) })
                }
            }
        }
    }
}
