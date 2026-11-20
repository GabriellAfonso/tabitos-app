package com.tabitos.app.features.habits

import com.tabitos.app.core.habits.domain.Habit
import com.tabitos.app.core.habits.domain.HabitAction
import com.tabitos.app.core.habits.domain.HabitRule
import com.tabitos.app.core.habits.domain.HabitRules
import com.tabitos.app.core.habits.domain.HabitSnapshot
import com.tabitos.app.core.habits.domain.HabitType
import com.tabitos.app.core.habits.domain.HabitUnit
import com.tabitos.app.core.ui.components.ContentFilter
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class VisibleHabitsTest {
    private val today = LocalDate.of(2026, 11, 18)
    private val state = run {
        val snapshot = listOf(
            habit("read", "Ler", HabitType.PRACTICE, target = 1),
            habit("smoke", "Fumar", HabitType.AVOID, target = 0),
            habit("phone", "Celular", HabitType.LIMIT, target = 2),
        ).fold(HabitSnapshot()) { s, save -> HabitRules.apply(s, save, today, 1, emptySet()) }
        HabitsUiState(snapshot = snapshot, today = today, loading = false,
            periods = snapshot.habits.flatMap { HabitRules.periods(snapshot, it.id, today) })
    }

    @Test fun everythingOnHojeHidesBadHabits() {
        assertEquals(listOf("read"), visibleIds(ContentFilter.ALL))
    }

    @Test fun habitsFilterShowsGoodAndBadHabits() {
        assertEquals(listOf("read", "smoke", "phone"), visibleIds(ContentFilter.HABITS))
    }

    @Test fun listPageKeepsBadHabits() {
        val ids = visibleHabits(state, HabitPage.LIST, null, showsBadHabits = false).map { it.id }
        assertEquals(listOf("read", "smoke", "phone"), ids)
    }

    private fun visibleIds(filter: ContentFilter) =
        visibleHabits(state, HabitPage.TODAY, null, filter.showsBadHabits).map { it.id }

    private fun habit(id: String, title: String, type: HabitType, target: Int) = HabitAction.Save(
        Habit(id, today, title), HabitRule("rule-$id", id, today, type, HabitUnit.DAY, target))
}
