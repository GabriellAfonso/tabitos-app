package com.ethos.app.core.trackers.domain

import com.ethos.app.core.habits.domain.HabitRules
import com.ethos.app.core.habits.domain.HabitSnapshot
import com.ethos.app.core.habits.domain.HabitStatus
import java.time.LocalDate

/** Pure tracker rules. Trackers never read or write habit records, goals or accounting. */
object TrackerRules {
    const val SCALE_LIMIT = 1000
    val PERIODS = listOf(7, 14, 30, 90)

    private fun fail(problem: TrackerProblem, question: String? = null): Nothing =
        throw TrackerRuleException(problem, question)

    private fun habitOpen(habits: HabitSnapshot, habitId: String) =
        habits.habits.firstOrNull { it.id == habitId }?.let { it.status != HabitStatus.EXCLUDED } == true

    /** Whether new entries or edits are allowed for [date]. Paused habits keep their tracker editable. */
    fun fillable(tracker: Tracker, habits: HabitSnapshot, date: LocalDate, today: LocalDate): Boolean =
        tracker.active && habitOpen(habits, tracker.habitId) && date >= tracker.created && date <= today

    fun editable(tracker: Tracker, habits: HabitSnapshot) = tracker.active && habitOpen(habits, tracker.habitId)

    /** Reminder slots exist only while the habit is active on that civil date. */
    fun remindable(tracker: Tracker, habits: HabitSnapshot, date: LocalDate): Boolean = tracker.active &&
        date >= tracker.created && habits.habits.firstOrNull { it.id == tracker.habitId }?.status ==
        HabitStatus.ACTIVE && HabitRules.active(habits, tracker.habitId, date)

    fun entryFor(s: TrackerSnapshot, trackerId: String, date: LocalDate): TrackerEntry? = s.entries
        .filter { it.trackerId == trackerId && it.date == date }.maxWithOrNull(compareBy({ it.filledAt }, { it.id }))

    fun hasEntry(s: TrackerSnapshot, trackerId: String, date: LocalDate) =
        s.entries.any { it.trackerId == trackerId && it.date == date }

    fun pending(
        s: TrackerSnapshot, habits: HabitSnapshot, reminderMinutes: Map<String, Int>, today: LocalDate,
        minuteNow: Int,
    ): List<Tracker> = s.trackers.filter { t ->
        val minute = reminderMinutes[t.id] ?: return@filter false
        minute <= minuteNow && remindable(t, habits, today) && !hasEntry(s, t.id, today)
    }

    fun normalize(questions: List<TrackerQuestion>): List<TrackerQuestion> {
        if (questions.isEmpty()) fail(TrackerProblem.EMPTY_FORM)
        if (questions.map { it.id }.toSet().size != questions.size) fail(TrackerProblem.QUESTION)
        val result = questions.map { q ->
            val title = q.title.trim()
            if (q.id.isBlank() || title.isEmpty()) fail(TrackerProblem.QUESTION, q.id)
            when (q.type) {
                QuestionType.SCALE -> {
                    if (q.min >= q.max || q.min < -SCALE_LIMIT || q.max > SCALE_LIMIT) fail(TrackerProblem.SCALE, q.id)
                    q.copy(title = title, minLabel = q.minLabel.trim(), maxLabel = q.maxLabel.trim(),
                        multiple = false, options = emptyList())
                }
                QuestionType.CHOICE -> {
                    val options = q.options.map { it.copy(label = it.label.trim(), description = it.description.trim()) }
                    if (options.isEmpty() || options.any { it.id.isBlank() || it.label.isEmpty() } ||
                        options.map { it.id }.toSet().size != options.size) fail(TrackerProblem.OPTIONS, q.id)
                    if (q.primary) fail(TrackerProblem.PRIMARY, q.id)
                    q.copy(title = title, options = options, min = 0, max = 10, minLabel = "", maxLabel = "")
                }
                QuestionType.TEXT -> {
                    if (q.primary) fail(TrackerProblem.PRIMARY, q.id)
                    q.copy(title = title, multiple = false, options = emptyList(), min = 0, max = 10,
                        minLabel = "", maxLabel = "")
                }
            }
        }
        if (result.count { it.primary } > 1) fail(TrackerProblem.PRIMARY)
        return result
    }

    fun apply(
        s: TrackerSnapshot, habits: HabitSnapshot, action: TrackerAction, today: LocalDate, now: Long,
    ): TrackerSnapshot = when (action) {
        is TrackerAction.SaveForm -> saveForm(s, habits, action, today, now)
        is TrackerAction.SetActive -> {
            val t = s.tracker(action.trackerId) ?: fail(TrackerProblem.INACTIVE)
            if (action.active && !habitOpen(habits, t.habitId)) fail(TrackerProblem.HABIT)
            s.copy(trackers = s.trackers.map {
                if (it.id == t.id) it.copy(active = action.active, updated = now) else it })
        }
        is TrackerAction.SaveEntry -> saveEntry(s, habits, action, today, now)
        is TrackerAction.DeleteEntry -> {
            val e = s.entries.firstOrNull { it.id == action.entryId } ?: fail(TrackerProblem.ANSWER)
            val t = s.tracker(e.trackerId) ?: fail(TrackerProblem.INACTIVE)
            if (!editable(t, habits)) fail(TrackerProblem.INACTIVE)
            s.copy(entries = s.entries.filterNot { it.id == e.id })
        }
    }

    private fun saveForm(
        s: TrackerSnapshot, habits: HabitSnapshot, a: TrackerAction.SaveForm, today: LocalDate, now: Long,
    ): TrackerSnapshot {
        if (!habitOpen(habits, a.habitId)) fail(TrackerProblem.HABIT)
        val questions = normalize(a.questions)
        val old = s.tracker(a.trackerId)
        val other = s.forHabit(a.habitId)
        if (other != null && other.id != a.trackerId) fail(TrackerProblem.HABIT)
        if (old == null) {
            val t = Tracker(a.trackerId, a.habitId, today, a.multiple, true, 1, now)
            return s.copy(trackers = s.trackers + t, versions = s.versions + TrackerVersion(t.id, 1, now, questions))
        }
        if (old.habitId != a.habitId) fail(TrackerProblem.HABIT)
        if (!old.active) fail(TrackerProblem.INACTIVE)
        val current = s.version(old.id, old.version)
        // Only structural changes create a version, so history keeps the exact form each entry answered.
        val changed = current?.questions != questions
        val number = if (changed) (s.versions.filter { it.trackerId == old.id }.maxOfOrNull { it.number } ?: 0) + 1
            else old.version
        val updated = old.copy(multiple = a.multiple, version = number, updated = now)
        return s.copy(trackers = s.trackers.map { if (it.id == old.id) updated else it },
            versions = if (changed) s.versions + TrackerVersion(old.id, number, now, questions) else s.versions)
    }

    fun validateAnswers(version: TrackerVersion, answers: List<TrackerAnswer>): List<TrackerAnswer> {
        val byId = answers.associateBy { it.questionId }
        val cleaned = version.questions.mapNotNull { q ->
            val a = byId[q.id]?.let { it.copy(text = it.text.trim(), choices = it.choices.distinct()) }
                ?.takeUnless { it.empty }
            if (a == null) {
                if (q.required) fail(TrackerProblem.REQUIRED, q.id)
                return@mapNotNull null
            }
            val valid = when (q.type) {
                QuestionType.SCALE -> a.scale != null && a.scale in q.min..q.max && a.choices.isEmpty() &&
                    a.text.isEmpty()
                QuestionType.CHOICE -> a.scale == null && a.text.isEmpty() &&
                    a.choices.all { id -> q.options.any { it.id == id } } && (q.multiple || a.choices.size == 1)
                QuestionType.TEXT -> a.scale == null && a.choices.isEmpty()
            }
            if (!valid) fail(TrackerProblem.ANSWER, q.id)
            a
        }
        if (cleaned.isEmpty()) fail(TrackerProblem.REQUIRED)
        return cleaned
    }

    private fun saveEntry(
        s: TrackerSnapshot, habits: HabitSnapshot, a: TrackerAction.SaveEntry, today: LocalDate, now: Long,
    ): TrackerSnapshot {
        val t = s.tracker(a.trackerId) ?: fail(TrackerProblem.INACTIVE)
        if (!editable(t, habits)) fail(TrackerProblem.INACTIVE)
        val old = s.entries.firstOrNull { it.id == a.entryId }
        if (old != null && (old.trackerId != t.id || old.date != a.date)) fail(TrackerProblem.DATE)
        if (!fillable(t, habits, a.date, today)) fail(TrackerProblem.DATE)
        if (old == null && !t.multiple && hasEntry(s, t.id, a.date)) fail(TrackerProblem.DUPLICATE)
        val number = old?.version ?: t.version
        val version = s.version(t.id, number) ?: fail(TrackerProblem.INACTIVE)
        val entry = TrackerEntry(a.entryId, t.id, number, a.date, old?.filledAt ?: now, now,
            validateAnswers(version, a.answers), old?.dayKey ?: if (t.multiple) null else a.date.toString())
        return s.copy(entries = s.entries.filterNot { it.id == entry.id } + entry)
    }

    fun analytics(s: TrackerSnapshot, trackerId: String, today: LocalDate, days: Int): TrackerAnalytics {
        require(days > 0)
        val from = today.minusDays(days - 1L)
        val entries = s.entries.filter { it.trackerId == trackerId && it.date in from..today }
            .sortedWith(compareBy({ it.date }, { it.filledAt }, { it.id }))
        val current = s.current(trackerId)
        val primary = current?.questions?.firstOrNull { it.primary }
        val points = if (primary == null) emptyList() else entries.mapNotNull { e ->
            val q = s.version(trackerId, e.version)?.questions
                ?.firstOrNull { it.id == primary.id && it.type == QuestionType.SCALE } ?: return@mapNotNull null
            val value = e.answers.firstOrNull { it.questionId == primary.id }?.scale ?: return@mapNotNull null
            TrackerPoint(e.id, e.date, e.filledAt, value, q.min, q.max)
        }
        val filled = entries.map { it.date }.toSet()
        val missing = generateSequence(from) { it.plusDays(1) }.takeWhile { it <= today }.filter { it !in filled }
            .toList()
        val counts = linkedMapOf<Pair<String, String>, Int>()
        val labels = mutableMapOf<Pair<String, String>, Pair<Int, String>>()
        val titles = mutableMapOf<String, Pair<Int, String>>()
        current?.questions?.filter { it.type == QuestionType.CHOICE }?.forEach { q ->
            titles[q.id] = current.number to q.title
            q.options.forEach { o -> counts[q.id to o.id] = 0; labels[q.id to o.id] = current.number to o.label }
        }
        entries.forEach { e ->
            val v = s.version(trackerId, e.version) ?: return@forEach
            e.answers.forEach { a ->
                val q = v.questions.firstOrNull { it.id == a.questionId && it.type == QuestionType.CHOICE }
                    ?: return@forEach
                if ((titles[q.id]?.first ?: -1) < v.number) titles[q.id] = v.number to q.title
                a.choices.forEach { id ->
                    val key = q.id to id
                    counts[key] = (counts[key] ?: 0) + 1
                    val label = q.options.firstOrNull { it.id == id }?.label ?: return@forEach
                    if ((labels[key]?.first ?: -1) < v.number) labels[key] = v.number to label
                }
            }
        }
        val options = counts.map { (key, count) ->
            OptionCount(key.first, titles[key.first]?.second.orEmpty(), key.second, labels[key]?.second.orEmpty(), count)
        }
        return TrackerAnalytics(days, from, today, primary?.title, points, missing,
            points.map { it.min to it.max }.distinct().size > 1, options)
    }
}
