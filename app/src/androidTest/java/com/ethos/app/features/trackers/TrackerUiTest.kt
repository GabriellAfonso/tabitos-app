package com.ethos.app.features.trackers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.Density
import com.ethos.app.core.habits.domain.*
import com.ethos.app.core.trackers.domain.*
import com.ethos.app.features.calendar.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class TrackerUiTest {
    @get:Rule val compose = createComposeRule()
    private val today = LocalDate.of(2026, 11, 7)

    @Test fun entryFormScaleButtonsChoicesAndTextAtDoubleFont() {
        val version = TrackerVersion("t", 1, 0, listOf(
            TrackerQuestion("u", QuestionType.SCALE, "Intensidade da vontade", required = true, primary = true,
                minLabel = "Nenhuma", maxLabel = "Muito forte"),
            TrackerQuestion("c", QuestionType.CHOICE, "Quais gatilhos você percebeu?", multiple = true,
                options = listOf(TrackerOption("a", "Tédio", "Sem nada para fazer"), TrackerOption("b", "Cansaço")))))
        var draft by mutableStateOf(TrackerEntryDraft("e", "t", today, 1))
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, 2f)) {
                MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
                    TrackerEntryContent(version, draft, true, null) { a ->
                        draft = draft.copy(answers = draft.answers.filterNot { it.questionId == a.questionId } + a)
                    }
                } }
            }
        }
        compose.onNodeWithText("Intensidade da vontade (obrigatória)").assertExists()
        compose.onNodeWithText("0 = Nenhuma · 10 = Muito forte").assertExists()
        compose.onNodeWithContentDescription("Valor 7 na escala de 0 a 10").performScrollTo().performClick()
        assertEquals(7, draft.answer("u")?.scale)
        compose.onNodeWithText("Tédio").performScrollTo().performClick()
        compose.onNodeWithText("Cansaço").performScrollTo().performClick()
        assertEquals(listOf("a", "b"), draft.answer("c")?.choices)
        compose.onNodeWithText("Sem nada para fazer").assertExists()
    }

    @Test fun builderAddsTypesAndPrimaryIsExclusiveWithoutReordering() {
        var draft by mutableStateOf(TrackerFormDraft("t", "h"))
        var id = 0
        compose.setContent {
            MaterialTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
                TrackerFormContent(draft, true, { draft = it }, { type, multiple ->
                    draft = draft.copy(questions = draft.questions + TrackerQuestion("q${id++}", type, "",
                        multiple = multiple, options = if (type == QuestionType.CHOICE)
                            listOf(TrackerOption("o$id", "")) else emptyList()))
                }, { TrackerOption("o${id++}", "") })
            } }
        }
        compose.onNodeWithText("+ Texto livre").performScrollTo().performClick()
        compose.onNodeWithText("+ Escala").performScrollTo().performClick()
        compose.onNodeWithText("+ Escala").performScrollTo().performClick()
        compose.onNodeWithText("+ Escolha múltipla").performScrollTo().performClick()
        assertEquals(listOf(QuestionType.TEXT, QuestionType.SCALE, QuestionType.SCALE, QuestionType.CHOICE),
            draft.questions.map { it.type })
        assertTrue(draft.questions[3].multiple)
        assertEquals(0 to 10, draft.questions[1].min to draft.questions[1].max)
        compose.onAllNodesWithText("Indicador principal")[0].performScrollTo().performClick()
        compose.onAllNodesWithText("Indicador principal")[1].performScrollTo().performClick()
        assertEquals(listOf(false, false, true, false), draft.questions.map { it.primary })
        assertEquals(listOf("q0", "q1", "q2", "q3"), draft.questions.map { it.id })
        compose.onNodeWithText("Um por dia").assertIsSelected()
    }

    @Test fun calendarCellsDescribeContentAndDayShowsNoData() {
        val sources = CalendarSources(habits = HabitSnapshot(habits = listOf(Habit("h", today.minusDays(5), "Ler")),
            records = listOf(HabitRecord("r", "h", today, 1))),
            trackers = TrackerSnapshot(listOf(Tracker("t", "h", today.minusDays(5))),
                entries = listOf(TrackerEntry("e", "t", 1, today.minusDays(1), 0, 0))))
        val f = CalendarFilters()
        var selected: LocalDate? = null
        val state = CalendarUiState(sources, today, loading = false,
            cells = CalendarModel.cells(sources, f, CalendarView.MONTH, today, today),
            selected = CalendarModel.day(sources, f, today, today))
        compose.setContent { MaterialTheme { CalendarContent(state, CalendarActions(select = { selected = it })) } }
        compose.onNodeWithContentDescription("2026-11-06 · tarefas: 0 · registros de hábitos: 0 · acompanhamentos: 1")
            .assertExists()
        compose.onNodeWithContentDescription("2026-11-05 · sem dados").performScrollTo().performClick()
        assertEquals(today.minusDays(2), selected)
        compose.onNodeWithText("Sem dados").performScrollTo().assertExists()
        compose.onNodeWithText("Feito").performScrollTo().assertExists()
        compose.onNodeWithText("Preencher").performScrollTo().assertExists()
    }
}
