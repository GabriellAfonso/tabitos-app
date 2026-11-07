package com.ethos.app.features.habits

import androidx.activity.ComponentActivity
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
                    HabitsContent(state, HabitPage.ALL, { vm.newHabit(null) }, vm::detail, vm::retry)
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
        compose.onNodeWithText("Registrar ocorrência/prática").performScrollTo().performClick()
        compose.waitUntil(15_000) { repo.snapshot.value.records.size == 1 }
        compose.onNodeWithText("Excluir registro incorreto").performScrollTo().performClick()
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
                loading = false), HabitPage.ALL, {}, {}, {}) }
        }
        compose.onNodeWithText("Hábitos excluídos").assertExists()
        compose.onNodeWithText("Evitar").assertExists()
        assertEquals(1, periods.count { it.outcome == PeriodOutcome.PENDING })
    }
    private inner class FakeRepository : HabitRepository {
        val snapshot = MutableStateFlow(HabitSnapshot())
        override fun observe() = snapshot
        override suspend fun execute(action: HabitAction) {
            snapshot.value = HabitRules.apply(snapshot.value, action, clock.today(), clock.nowMillis(), emptySet())
        }
    }
}
