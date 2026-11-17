package com.ethos.app.features.habits

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.SavedStateHandle
import com.ethos.app.core.habits.domain.*
import com.ethos.app.core.tasks.domain.TaskClock
import com.ethos.app.core.ui.theme.EthosTheme
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class HabitsContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val clock = object : TaskClock {
        override fun today(): LocalDate = LocalDate.of(2026, 11, 6)
        override fun nowMillis(): Long = System.nanoTime()
    }
    @Test fun createPreviewSaveRecordAndCorrect() {
        val repo = FakeRepository()
        compose.setContent {
            val vm = remember { HabitsViewModel(repo, clock, SavedStateHandle()) }
            EthosTheme {
                HabitsScreen(vm) { state ->
                    Column {
                        Button({ vm.newHabit(null) }) { Text("Novo hábito") }
                        HabitsContent(state, HabitPage.LIST, vm::detail, vm::retry)
                    }
                    HabitDialogs(state, emptyList(), vm)
                }
            }
        }
        compose.onNodeWithText("Novo hábito").performClick()
        compose.onNodeWithText("Nome do hábito").performTextInput("Ler páginas")
        compose.onNodeWithText("Prévia do progresso recalculado").performClick()
        compose.onNodeWithText("Salvar").performClick()
        compose.waitUntil(15_000) { repo.snapshot.value.habits.size == 1 }
        compose.onNodeWithText("Ler páginas").performScrollTo().performClick()
        compose.onNodeWithText("Marcar hoje").performScrollTo().performClick()
        compose.waitUntil(15_000) { repo.snapshot.value.records.size == 1 }
        compose.onNodeWithText("Desmarcar").performScrollTo().performClick()
        compose.waitUntil(15_000) { repo.snapshot.value.records.single().deleted }
        assertEquals(1, repo.snapshot.value.audits.count { it.kind == "DELETE_RECORD" })
    }
    @Test fun excludedHistoryAndZeroEventReviewAreReal() {
        var s = HabitRules.apply(HabitSnapshot(), HabitAction.Save(Habit("h", clock.today().minusDays(1), "Evitar"),
            HabitRule("r", "h", clock.today().minusDays(1), HabitType.AVOID, HabitUnit.DAY, 0)),
            clock.today().minusDays(1), 1, emptySet())
        s = HabitRules.apply(s, HabitAction.Status("h", HabitStatus.EXCLUDED), clock.today(), 2, emptySet())
        val periods = HabitRules.periods(s, "h", clock.today())
        compose.setContent {
            EthosTheme { HabitsContent(HabitsUiState(snapshot = s, today = clock.today(), periods = periods,
                loading = false), HabitPage.EXCLUDED, {}, {}) }
        }
        compose.onNodeWithText("Evitar").assertExists()
        // Spec 014: habits never become pending, even right after exclusion.
        assertEquals(0, periods.count { it.outcome == PeriodOutcome.PENDING })
    }
    @Test fun todayKeepsCompletedHabitVisibleAndOnlyShowsToday() {
        val repo = FakeRepository()
        repo.snapshot.value = HabitRules.apply(HabitSnapshot(),
            HabitAction.Save(Habit("h", clock.today(), "Ler hoje"),
                HabitRule("r", "h", clock.today(), unit = HabitUnit.WEEK, target = 1), partialTarget = 1),
            clock.today(), 1, emptySet())
        compose.setContent {
            val vm = remember { HabitsViewModel(repo, clock, SavedStateHandle()) }
            EthosTheme { HabitsScreen(vm) { state ->
                LazyColumn { habitItems(state, HabitPage.TODAY, null, vm::detail, vm::retry, vm::quickRecord) }
            } }
        }
        compose.onNodeWithText("Marcar").performClick()
        compose.onNodeWithText("✓ Feito hoje").assertExists().performClick()
        compose.onNodeWithText("Marcar").assertExists()
        compose.onNodeWithText("Ler hoje").assertExists()
        compose.onAllNodes(hasText("Dias distintos:", substring = true)).assertCountEquals(0)
        compose.onAllNodes(hasContentDescription("Semana atual:", substring = true)).assertCountEquals(0)
        compose.onAllNodes(hasText("Metas cumpridas", substring = true)).assertCountEquals(0)
    }
    private inner class FakeRepository : HabitRepository {
        val snapshot = MutableStateFlow(HabitSnapshot())
        override fun observe() = snapshot
        override suspend fun execute(action: HabitAction) {
            snapshot.value = HabitRules.apply(snapshot.value, action, clock.today(), clock.nowMillis(), emptySet())
        }
        override suspend fun erase(habitId: String) {
            val s = snapshot.value
            snapshot.value = s.copy(habits = s.habits.filterNot { it.id == habitId },
                records = s.records.filterNot { it.habitId == habitId })
        }
    }
}
