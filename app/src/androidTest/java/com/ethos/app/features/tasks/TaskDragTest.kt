package com.ethos.app.features.tasks

import com.ethos.app.core.ui.components.ContentFilter
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.ui.theme.EthosTheme
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TaskDragTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val day = LocalDate.of(2026, 11, 10)
    private var snapshot = TaskSnapshot()
    private val committed = mutableListOf<TaskAction>()
    private var opened = 0

    private fun show(count: Int = 3) {
        repeat(count) { index ->
            snapshot = TaskRules.apply(snapshot, TaskAction.Create("t$index", TaskFields(title = "Task $index"),
                RepeatRule(start = day)), day, 1)
        }
        compose.setContent {
            var saved by remember { mutableStateOf(snapshot) }
            EthosTheme {
                TasksContent(TasksUiState(saved, day, loading = false), TaskPage.LIST,
                    ContentFilter.ALL, { opened++ },
                    { action ->
                        committed.add(action)
                        snapshot = TaskRules.apply(snapshot, action, day, 1)
                        saved = snapshot
                    }, {}, {}, {})
            }
        }
    }

    @Test fun longPressDragPersistsOneOrderWithoutOpeningOrCompleting() {
        show()
        compose.onNodeWithText("Mover para cima").assertDoesNotExist()
        compose.onNodeWithText("Mover para baixo").assertDoesNotExist()
        val first = compose.onNodeWithTag("task:t0:1").fetchSemanticsNode().boundsInRoot
        val last = compose.onNodeWithTag("task:t2:1").fetchSemanticsNode().boundsInRoot
        compose.onRoot().performTouchInput {
            down(first.center)
            advanceEventTime(700)
            moveTo(Offset(first.center.x, last.center.y + last.height / 4), 500)
            up()
        }
        compose.waitForIdle()
        assertEquals(listOf("t1:1", "t2:1", "t0:1"),
            snapshot.activeOccurrences.sortedBy { it.position }.map { it.id })
        assertEquals(1, committed.size)
        assertTrue(committed.single() is TaskAction.Order)
        assertEquals(0, opened)
        assertTrue(snapshot.occurrences.all { it.status == TaskStatus.OPEN })
    }

    @Test fun cancelledDragLeavesStoredOrderAndTapStillOpensTask() {
        show()
        val first = compose.onNodeWithTag("task:t0:1").fetchSemanticsNode().boundsInRoot
        val last = compose.onNodeWithTag("task:t2:1").fetchSemanticsNode().boundsInRoot
        compose.onRoot().performTouchInput {
            down(first.center)
            advanceEventTime(700)
            moveTo(last.center, 500)
            cancel()
        }
        compose.waitForIdle()
        assertTrue(committed.isEmpty())
        assertEquals(listOf("t0:1", "t1:1", "t2:1"),
            snapshot.activeOccurrences.sortedBy { it.position }.map { it.id })
        compose.onNodeWithText("Task 0").performClick()
        assertEquals(1, opened)
    }
}
