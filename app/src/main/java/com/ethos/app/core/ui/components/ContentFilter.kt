package com.ethos.app.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ethos.app.R

/** Content filter of Hoje; shared by the shell and the task/habit pages. */
enum class ContentFilter(@param:StringRes val title: Int) {
    ALL(R.string.all), TASKS(R.string.tasks), HABITS(R.string.habits);

    val showsTasks: Boolean get() = this != HABITS
    val showsHabits: Boolean get() = this != TASKS
}

@Composable
fun FilterRow(filter: ContentFilter, onFilter: (ContentFilter) -> Unit) {
    SegmentedControl(ContentFilter.entries, filter, { stringResource(it.title) }, onFilter,
        Modifier.sizeIn(minHeight = 48.dp), tag = { "filter_${it.name}" })
}
