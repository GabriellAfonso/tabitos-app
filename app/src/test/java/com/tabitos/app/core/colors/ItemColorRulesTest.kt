package com.tabitos.app.core.colors

import com.tabitos.app.core.habits.domain.Habit
import com.tabitos.app.core.habits.domain.HabitAction
import com.tabitos.app.core.habits.domain.HabitProblem
import com.tabitos.app.core.habits.domain.HabitRule
import com.tabitos.app.core.habits.domain.HabitRuleException
import com.tabitos.app.core.habits.domain.HabitRules
import com.tabitos.app.core.habits.domain.HabitSnapshot
import com.tabitos.app.core.tasks.domain.TaskAction
import com.tabitos.app.core.tasks.domain.TaskList
import com.tabitos.app.core.tasks.domain.TaskProblem
import com.tabitos.app.core.tasks.domain.TaskRuleException
import com.tabitos.app.core.tasks.domain.TaskRules
import com.tabitos.app.core.tasks.domain.TaskSnapshot
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ItemColorRulesTest {
    private val day = LocalDate.of(2026, 11, 20)
    private val teal = ItemColors.custom(0x1E7F86)

    private fun saveList(color: Int) =
        TaskRules.apply(TaskSnapshot(), TaskAction.SaveList(TaskList("l", "Casa", color = color)), day, 1L)

    private fun saveHabit(color: Int) = HabitRules.apply(HabitSnapshot(),
        HabitAction.Save(Habit("h", day, "Meditar", color = color), HabitRule("r", "h", day)), day, 1, emptySet())

    @Test fun listKeepsCustomAndLegacyColors() {
        assertEquals(teal, saveList(teal).lists.single().color)
        assertEquals(PaletteColor.GRAY.position, saveList(PaletteColor.GRAY.position).lists.single().color)
    }

    @Test fun listRefusesValuesThatAreNotColors() {
        listOf(7, 0x1E7F86, 0x801E7F86.toInt()).forEach { color ->
            try {
                saveList(color)
                fail("accepted $color")
            } catch (e: TaskRuleException) { assertEquals(TaskProblem.INVALID_INPUT, e.problem) }
        }
    }

    @Test fun habitKeepsCustomColorAndRefusesInvalid() {
        assertEquals(teal, saveHabit(teal).habits.single().color)
        try {
            saveHabit(7)
            fail("accepted 7")
        } catch (e: HabitRuleException) { assertEquals(HabitProblem.INPUT, e.reason) }
    }
}
