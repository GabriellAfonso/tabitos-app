package com.tabitos.app.core.trackers

import com.tabitos.app.core.habits.domain.*
import com.tabitos.app.core.trackers.domain.*
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class TrackerRulesTest {
    private val created = LocalDate.of(2026, 11, 1)
    private val today = LocalDate.of(2026, 11, 7)
    private fun habits(status: HabitStatus = HabitStatus.ACTIVE) = HabitSnapshot(
        habits = listOf(Habit("h", created, "Hábito", status = status)),
        rules = listOf(HabitRule("r", "h", created)), intervals = listOf(ActiveInterval("i", "h", created)))
    private val urge = TrackerQuestion("urge", QuestionType.SCALE, "Intensidade da vontade", required = true,
        primary = true, minLabel = "Nenhuma", maxLabel = "Muito forte")
    private val impact = TrackerQuestion("impact", QuestionType.CHOICE, "Quanto prejudicou sua rotina?",
        options = listOf(TrackerOption("n", "Nada"), TrackerOption("l", "Levemente"),
            TrackerOption("m", "Moderadamente", "Atrapalhou parte do dia"), TrackerOption("x", "Muito")))
    private val triggers = TrackerQuestion("triggers", QuestionType.CHOICE, "Quais gatilhos você percebeu?",
        multiple = true, options = listOf(TrackerOption("t1", "Tédio"), TrackerOption("t2", "Cansaço")))
    private val notes = TrackerQuestion("notes", QuestionType.TEXT, "Observações")

    private fun apply(s: TrackerSnapshot, a: TrackerAction, h: HabitSnapshot = habits(), day: LocalDate = created,
                      now: Long = 1L) = TrackerRules.apply(s, h, a, day, now)
    private fun form(questions: List<TrackerQuestion> = listOf(notes, impact, urge, triggers), multiple: Boolean = false) =
        apply(TrackerSnapshot(), TrackerAction.SaveForm("t", "h", multiple, questions))
    private fun entry(s: TrackerSnapshot, id: String, date: LocalDate, value: Int = 5, now: Long = 10L,
                      answers: List<TrackerAnswer> = listOf(TrackerAnswer("urge", scale = value))) =
        apply(s, TrackerAction.SaveEntry(id, "t", date, answers), day = today, now = now)
    private fun problem(block: () -> Unit): TrackerProblem =
        try { block(); fail("expected rule failure"); error("") } catch (e: TrackerRuleException) { e.problem }

    @Test fun formKeepsTypesFlagsOrderAndPrimaryPosition() {
        val s = form()
        val v = s.current("t")!!
        assertEquals(listOf("notes", "impact", "urge", "triggers"), v.questions.map { it.id })
        assertEquals(1, s.tracker("t")!!.version)
        assertFalse(s.tracker("t")!!.multiple)
        assertTrue(v.questions[2].primary)
        assertTrue(v.questions[2].required)
        assertEquals(0, v.questions[2].min); assertEquals(10, v.questions[2].max)
        assertTrue(v.questions[3].multiple)
        assertEquals(created, s.tracker("t")!!.created)
    }

    @Test fun formValidationRejectsInvalidStructures() {
        assertEquals(TrackerProblem.EMPTY_FORM, problem { form(emptyList()) })
        assertEquals(TrackerProblem.SCALE, problem { form(listOf(urge.copy(min = 5, max = 5))) })
        assertEquals(TrackerProblem.PRIMARY, problem { form(listOf(urge, urge.copy(id = "u2"))) })
        assertEquals(TrackerProblem.OPTIONS, problem { form(listOf(impact.copy(options = emptyList()))) })
        assertEquals(TrackerProblem.QUESTION, problem { form(listOf(notes.copy(title = " "))) })
        assertEquals(TrackerProblem.HABIT, problem {
            TrackerRules.apply(TrackerSnapshot(), habits(HabitStatus.EXCLUDED),
                TrackerAction.SaveForm("t", "h", false, listOf(notes)), created, 1) })
        val custom = form(listOf(urge.copy(min = 1, max = 5))).current("t")!!.questions.single()
        assertEquals(1 to 5, custom.min to custom.max)
    }

    @Test fun entriesRespectDateLimitsAndSingleDayMode() {
        val s = entry(form(), "e1", today.minusDays(1))
        val e = s.entries.single()
        assertEquals(today.minusDays(1), e.date)
        assertEquals(10L, e.filledAt)
        assertEquals(TrackerProblem.DATE, problem { entry(s, "e2", today.plusDays(1)) })
        assertEquals(TrackerProblem.DATE, problem { entry(s, "e2", created.minusDays(1)) })
        assertEquals(TrackerProblem.DUPLICATE, problem { entry(s, "e2", today.minusDays(1)) })
        assertEquals("e1", TrackerRules.entryFor(s, "t", today.minusDays(1))!!.id)
        val edited = entry(s, "e1", today.minusDays(1), 8, now = 20L).entries.single()
        assertEquals(8, edited.answers.single().scale)
        assertEquals(10L, edited.filledAt); assertEquals(20L, edited.updatedAt)
        assertEquals(today.minusDays(1).toString(), edited.dayKey)
    }

    @Test fun multipleModeKeepsIndividualEntries() {
        var s = form(multiple = true)
        listOf(2, 9, 4).forEachIndexed { i, v -> s = entry(s, "e$i", today, v, now = 10L + i) }
        assertEquals(3, s.entries.size)
        assertTrue(s.entries.all { it.dayKey == null })
        val points = TrackerRules.analytics(s, "t", today, 7).points
        assertEquals(listOf(2, 9, 4), points.map { it.value })
    }

    @Test fun answersAreValidatedAgainstVersion() {
        val s = form()
        assertEquals(TrackerProblem.REQUIRED, problem { entry(s, "e", today, answers = listOf(
            TrackerAnswer("notes", text = "texto"))) })
        assertEquals(TrackerProblem.ANSWER, problem { entry(s, "e", today, 11) })
        assertEquals(TrackerProblem.ANSWER, problem { entry(s, "e", today, answers = listOf(
            TrackerAnswer("urge", scale = 3), TrackerAnswer("impact", choices = listOf("n", "l")))) })
        assertEquals(TrackerProblem.ANSWER, problem { entry(s, "e", today, answers = listOf(
            TrackerAnswer("urge", scale = 3), TrackerAnswer("impact", choices = listOf("missing")))) })
        val ok = entry(s, "e", today, answers = listOf(TrackerAnswer("urge", scale = 0),
            TrackerAnswer("triggers", choices = listOf("t1", "t2")), TrackerAnswer("notes", text = "  "),
            TrackerAnswer("unknown", text = "x")))
        assertEquals(listOf("urge", "triggers"), ok.entries.single().answers.map { it.questionId })
    }

    @Test fun formChangesCreateVersionsAndOldEntriesKeepTheirStructure() {
        var s = entry(form(), "old", today.minusDays(2), answers = listOf(TrackerAnswer("urge", scale = 6),
            TrackerAnswer("impact", choices = listOf("m"))))
        val v2 = listOf(TrackerQuestion("new", QuestionType.TEXT, "Nova obrigatória", required = true),
            urge.copy(title = "Vontade"), impact.copy(options = impact.options.filterNot { it.id == "m" }))
        s = apply(s, TrackerAction.SaveForm("t", "h", false, v2), day = today)
        assertEquals(2, s.tracker("t")!!.version)
        assertEquals(1, s.entries.single().version)
        assertEquals(4, s.version("t", 1)!!.questions.size)
        // Reorder-free re-save with the same structure does not create versions.
        assertEquals(2, apply(s, TrackerAction.SaveForm("t", "h", false, v2), day = today).versions.size)
        // Old entry edit uses version 1: no new required question, removed option still valid.
        s = entry(s, "old", today.minusDays(2), answers = listOf(TrackerAnswer("urge", scale = 7),
            TrackerAnswer("impact", choices = listOf("m"))))
        assertEquals(1, s.entries.single().version)
        assertEquals(TrackerProblem.REQUIRED, problem { entry(s, "fresh", today) })
        assertEquals(TrackerProblem.ANSWER, problem { entry(s, "fresh", today, answers = listOf(
            TrackerAnswer("new", text = "a"), TrackerAnswer("urge", scale = 3),
            TrackerAnswer("impact", choices = listOf("m")))) })
        val counts = TrackerRules.analytics(s, "t", today, 7).options.filter { it.questionId == "impact" }
        assertEquals("Moderadamente", counts.first { it.optionId == "m" }.label)
        assertEquals(1, counts.first { it.optionId == "m" }.count)
        assertEquals(0, counts.first { it.optionId == "n" }.count)
    }

    @Test fun analyticsShowsGapsWithoutAveragingAndFlagsRangeChanges() {
        var s = form(listOf(urge))
        s = entry(s, "a", today.minusDays(3), 4)
        s = apply(s, TrackerAction.SaveForm("t", "h", true, listOf(urge.copy(min = 1, max = 5))), day = today)
        s = entry(s, "b", today, 2, now = 30L)
        s = entry(s, "c", today, 5, now = 40L)
        val a = TrackerRules.analytics(s, "t", today, 7)
        assertEquals(listOf(4, 2, 5), a.points.map { it.value })
        assertEquals(listOf(0 to 10, 1 to 5, 1 to 5), a.points.map { it.min to it.max })
        assertTrue(a.rangesDiffer)
        assertEquals(5, a.missingDays.size)
        assertFalse(today in a.missingDays)
        assertEquals(today.minusDays(6), a.from)
        assertTrue(TrackerRules.analytics(s, "t", today, 90).missingDays.size == 88)
    }

    @Test fun lifecycleArchivesAndPendingFollowsHabit() {
        var s = form()
        val minute = mapOf("t" to 9 * 60)
        assertEquals(1, TrackerRules.pending(s, habits(), minute, today, 10 * 60).size)
        assertTrue(TrackerRules.pending(s, habits(), minute, today, 8 * 60).isEmpty())
        assertTrue(TrackerRules.pending(s, habits(HabitStatus.PAUSED), minute, today, 10 * 60).isEmpty())
        assertTrue(TrackerRules.pending(s, habits(), emptyMap(), today, 10 * 60).isEmpty())
        val filled = entry(s, "e", today)
        assertTrue(TrackerRules.pending(filled, habits(), minute, today, 10 * 60).isEmpty())
        // Paused habit keeps past dates editable; excluded habit is read-only.
        entry(s, "p", today, answers = listOf(TrackerAnswer("urge", scale = 1))).let { assertEquals(1, it.entries.size) }
        assertEquals(TrackerProblem.INACTIVE, problem {
            TrackerRules.apply(filled, habits(HabitStatus.EXCLUDED), TrackerAction.DeleteEntry("e"), today, 1) })
        s = apply(filled, TrackerAction.SetActive("t", false), day = today)
        assertEquals(TrackerProblem.INACTIVE, problem { entry(s, "x", today.minusDays(1)) })
        assertTrue(TrackerRules.pending(s, habits(), minute, today, 10 * 60).isEmpty())
        assertEquals(1, s.entries.size)
        s = apply(s, TrackerAction.SetActive("t", true), day = today)
        assertEquals(0, apply(s, TrackerAction.DeleteEntry("e"), day = today).entries.size)
    }

    @Test fun trackerActionsNeverChangeHabitData() {
        val h = habits()
        var s = form()
        s = entry(s, "e", today)
        s = apply(s, TrackerAction.DeleteEntry("e"), h, today)
        assertEquals(habits(), h)
        assertTrue(h.records.isEmpty() && h.reviews.isEmpty() && h.overrides.isEmpty())
    }
}
