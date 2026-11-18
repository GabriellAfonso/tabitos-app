package com.ethos.app.features.trackers

import com.ethos.app.core.habits.domain.Habit
import com.ethos.app.core.habits.domain.HabitSnapshot
import com.ethos.app.core.habits.domain.HabitStatus
import com.ethos.app.core.trackers.domain.OptionCount
import com.ethos.app.core.trackers.domain.QuestionType
import com.ethos.app.core.trackers.domain.Tracker
import com.ethos.app.core.trackers.domain.TrackerAnalytics
import com.ethos.app.core.trackers.domain.TrackerAnswer
import com.ethos.app.core.trackers.domain.TrackerEntry
import com.ethos.app.core.trackers.domain.TrackerOption
import com.ethos.app.core.trackers.domain.TrackerQuestion
import com.ethos.app.core.trackers.domain.TrackerSnapshot
import com.ethos.app.core.trackers.domain.TrackerVersion
import com.ethos.app.core.ui.format.DateText
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackerPresentationTest {
    // Wednesday.
    private val today = LocalDate.of(2026, 11, 18)
    private val scale = TrackerQuestion("u", QuestionType.SCALE, "Energia", required = true, primary = true)
    private val choice = TrackerQuestion("c", QuestionType.CHOICE, "O que atrapalhou", multiple = true,
        options = listOf(TrackerOption("a", "Sono ruim"), TrackerOption("b", "Trabalho")))
    private val habits = HabitSnapshot(habits = listOf(Habit("h", today.minusDays(60), "Meditar")))

    private fun tracker(created: LocalDate = today.minusDays(9), multiple: Boolean = false, active: Boolean = true) =
        Tracker("t", "h", created, multiple, active)

    private fun snapshot(t: Tracker, entries: List<TrackerEntry>) =
        TrackerSnapshot(listOf(t), listOf(TrackerVersion("t", 1, 0, listOf(scale, choice))), entries)

    private fun entry(id: String, date: LocalDate, value: Int, filledAt: Long = 0, choices: List<String> = emptyList()) =
        TrackerEntry(id, "t", 1, date, filledAt, filledAt, listOf(TrackerAnswer("u", scale = value),
            TrackerAnswer("c", choices = choices)).filterNot { it.empty })

    @Test fun recordsGroupMondayWeeksNewestFirstFromCreation() {
        val t = tracker()
        val list = recordList(snapshot(t, listOf(entry("e", today.minusDays(1), 6, choices = listOf("b", "a")))),
            t, habits, today, weeks = 4)
        assertEquals(listOf(LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 9)), list.weeks.map { it.start })
        assertEquals(today, list.weeks.first().days.first().date)
        assertEquals(t.created, list.weeks.last().days.last().date)
        assertEquals(10, list.weeks.sumOf { it.days.size })
        val yesterday = list.weeks.first().days[1]
        assertEquals(listOf(RecordEntry("e", 6, listOf("Trabalho", "Sono ruim"))), yesterday.entries)
        assertTrue(list.weeks.first().days.first().fillable)
        assertFalse(list.hasMore)
    }

    @Test fun recordsWindowOffersMoreWhenOlderDaysExist() {
        val t = tracker(created = today.minusDays(80))
        val list = recordList(snapshot(t, emptyList()), t, habits, today, weeks = 4)
        assertEquals(4, list.weeks.size)
        assertEquals(weekStart(today).minusWeeks(3), list.weeks.last().days.last().date)
        assertTrue(list.hasMore)
    }

    @Test fun archivedTrackerDaysAreNotFillable() {
        val t = tracker(active = false)
        val list = recordList(snapshot(t, emptyList()), t, habits, today, weeks = 4)
        assertTrue(list.weeks.flatMap { it.days }.none { it.fillable })
    }

    @Test fun weekTitlesNameCurrentAndPreviousWeeks() {
        assertEquals(WeekTitle.Current, weekTitle(weekStart(today), today))
        assertEquals(WeekTitle.Previous, weekTitle(weekStart(today).minusWeeks(1), today))
        val older = weekStart(today).minusWeeks(2)
        assertEquals(WeekTitle.Of(older), weekTitle(older, today))
    }

    @Test fun recordDayLabelUsesRelativeWordsThenWeekday() {
        assertEquals(DateText.Today, recordDayLabel(today, today))
        assertEquals(DateText.Yesterday, recordDayLabel(today.minusDays(1), today))
        assertEquals(DateText.Plain("seg, 16 nov"), recordDayLabel(today.minusDays(2), today))
    }

    @Test fun todayOpensTheSingleEntryAndMultipleModeAlwaysAddsAnother() {
        val single = tracker()
        val empty = todaySummary(snapshot(single, emptyList()), single, habits, today)
        assertEquals(TodayKind.EMPTY, empty.kind)
        assertFalse(empty.opensExisting)
        val filled = todaySummary(snapshot(single, listOf(entry("e", today, 7))), single, habits, today)
        assertEquals(TodaySummary(TodayKind.FILLED, 1, "e", 7, allowsAnother = false), filled)
        assertTrue(filled.opensExisting)
        val multiple = tracker(multiple = true)
        val many = todaySummary(snapshot(multiple, listOf(entry("a", today, 3, 1), entry("b", today, 8, 2))),
            multiple, habits, today)
        assertEquals("b", many.latestEntryId)
        assertEquals(2, many.entries)
        assertFalse(many.opensExisting)
    }

    @Test fun todayIsReadOnlyWhenArchivedOrHabitExcluded() {
        val archived = tracker(active = false)
        assertEquals(TodayKind.ARCHIVED, todaySummary(snapshot(archived, emptyList()), archived, habits, today).kind)
        val excluded = HabitSnapshot(habits = listOf(Habit("h", today, "Meditar", status = HabitStatus.EXCLUDED)))
        val t = tracker()
        assertEquals(TodayKind.HABIT_EXCLUDED, todaySummary(snapshot(t, emptyList()), t, excluded, today).kind)
    }

    @Test fun changingKindDropsIndicatorAndAddsFirstOption() {
        val asChoice = scale.withKind(QuestionKind.SINGLE) { TrackerOption("n", "") }
        assertEquals(QuestionType.CHOICE, asChoice.type)
        assertFalse(asChoice.primary)
        assertEquals(listOf(TrackerOption("n", "")), asChoice.options)
        assertEquals(QuestionKind.MULTIPLE, choice.kind)
        assertEquals(choice.options, choice.withKind(QuestionKind.SINGLE) { error("not needed") }.options)
        assertEquals(scale, scale.withKind(QuestionKind.SCALE) { error("not needed") })
    }

    @Test fun primaryIsExclusive() {
        val other = scale.copy(id = "v", primary = false)
        val result = listOf(scale, other, choice).withPrimary("v", true)
        assertEquals(listOf(false, true, false), result.map { it.primary })
        assertEquals(listOf(false, false, false), result.withPrimary("v", false).map { it.primary })
    }

    @Test fun scaleInputDependsOnRangeSize() {
        assertEquals(ScaleInput.TRACK, scaleInput(scale))
        assertEquals(ScaleInput.CHIPS, scaleInput(scale.copy(min = -10, max = 10)))
        assertEquals(ScaleInput.NUMBER, scaleInput(scale.copy(max = 100)))
        assertTrue(choicesAsChips(choice))
        assertFalse(choicesAsChips(choice.copy(options = listOf(TrackerOption("a", "Corrida", "Pouco tempo")))))
    }

    @Test fun frequencyBarsAreRelativeToTheMostChosenOptionOfEachQuestion() {
        val a = TrackerAnalytics(7, today.minusDays(6), today, null, emptyList(), emptyList(), false, listOf(
            OptionCount("c", "O que atrapalhou", "a", "Sono ruim", 1),
            OptionCount("c", "O que atrapalhou", "b", "Trabalho", 4),
            OptionCount("d", "Humor", "x", "Bem", 0)))
        val groups = frequencyGroups(a)
        assertEquals(listOf("Trabalho", "Sono ruim"), groups[0].bars.map { it.label })
        assertEquals(listOf(1f, 0.25f), groups[0].bars.map { it.fraction })
        assertEquals(0f, groups[1].bars.single().fraction)
    }

    @Test fun summaryWithoutPrimaryHasNoValue() {
        val t = tracker()
        val s = TrackerSnapshot(listOf(t), listOf(TrackerVersion("t", 1, 0, listOf(choice))),
            listOf(TrackerEntry("e", "t", 1, today, 0, 0, listOf(TrackerAnswer("c", choices = listOf("a"))))))
        assertNull(todaySummary(s, t, habits, today).primary)
    }
}
