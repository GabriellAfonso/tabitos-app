package com.tabitos.app.core.trackers.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

enum class QuestionType { SCALE, CHOICE, TEXT }

data class TrackerOption(val id: String, val label: String, val description: String = "") : java.io.Serializable

data class TrackerQuestion(
    val id: String, val type: QuestionType, val title: String, val required: Boolean = false,
    val primary: Boolean = false, val min: Int = 0, val max: Int = 10, val minLabel: String = "",
    val maxLabel: String = "", val multiple: Boolean = false, val options: List<TrackerOption> = emptyList(),
) : java.io.Serializable

data class TrackerVersion(
    val trackerId: String, val number: Int, val created: Long, val questions: List<TrackerQuestion>,
)

data class Tracker(
    val id: String, val habitId: String, val created: LocalDate, val multiple: Boolean = false,
    val active: Boolean = true, val version: Int = 1, val updated: Long = 0,
)

data class TrackerAnswer(
    val questionId: String, val scale: Int? = null, val choices: List<String> = emptyList(), val text: String = "",
) : java.io.Serializable {
    val empty: Boolean get() = scale == null && choices.isEmpty() && text.isBlank()
}

data class TrackerEntry(
    val id: String, val trackerId: String, val version: Int, val date: LocalDate, val filledAt: Long,
    val updatedAt: Long, val answers: List<TrackerAnswer> = emptyList(), val dayKey: String? = null,
)

data class TrackerSnapshot(
    val trackers: List<Tracker> = emptyList(), val versions: List<TrackerVersion> = emptyList(),
    val entries: List<TrackerEntry> = emptyList(),
) {
    fun tracker(id: String?) = trackers.firstOrNull { it.id == id }
    fun forHabit(habitId: String?) = trackers.firstOrNull { it.habitId == habitId }
    fun version(trackerId: String, number: Int) =
        versions.firstOrNull { it.trackerId == trackerId && it.number == number }
    fun current(trackerId: String) = tracker(trackerId)?.let { version(trackerId, it.version) }
    fun entries(trackerId: String) = entries.filter { it.trackerId == trackerId }
}

sealed interface TrackerAction {
    data class SaveForm(
        val trackerId: String, val habitId: String, val multiple: Boolean, val questions: List<TrackerQuestion>,
    ) : TrackerAction
    data class SetActive(val trackerId: String, val active: Boolean) : TrackerAction
    data class SaveEntry(
        val entryId: String, val trackerId: String, val date: LocalDate, val answers: List<TrackerAnswer>,
    ) : TrackerAction
    data class DeleteEntry(val entryId: String) : TrackerAction
}

enum class TrackerProblem {
    EMPTY_FORM, QUESTION, SCALE, PRIMARY, OPTIONS, DATE, REQUIRED, ANSWER, INACTIVE, DUPLICATE, HABIT,
}

class TrackerRuleException(val problem: TrackerProblem, val questionId: String? = null) :
    IllegalArgumentException(problem.name)

data class TrackerPoint(
    val entryId: String, val date: LocalDate, val filledAt: Long, val value: Int, val min: Int, val max: Int,
)
data class OptionCount(val questionId: String, val question: String, val optionId: String, val label: String,
    val count: Int)
data class TrackerAnalytics(
    val days: Int, val from: LocalDate, val to: LocalDate, val primaryTitle: String?,
    val points: List<TrackerPoint>, val missingDays: List<LocalDate>, val rangesDiffer: Boolean,
    val options: List<OptionCount>,
)

interface TrackerRepository {
    fun observe(): Flow<TrackerSnapshot>
    suspend fun execute(action: TrackerAction)
}
