package com.ethos.app.core.tasks.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import java.util.UUID

object TaskRules {
    fun apply(snapshot: TaskSnapshot, action: TaskAction, today: LocalDate, now: Long): TaskSnapshot =
        Mutation(snapshot, if (action is TaskAction.Materialize) maxOf(today, action.through) else today, now)
            .run(action)

    fun suggestDifficulty(fields: TaskFields, difficulty: Int): TaskFields = fields.copy(
        difficulty = difficulty,
        importance = if (difficulty == 2 && !fields.importanceManual) 2 else fields.importance,
    )

    private class Mutation(snapshot: TaskSnapshot, val today: LocalDate, val now: Long) {
        val lists = snapshot.lists.toMutableList()
        val definitions = snapshot.definitions.toMutableList()
        val occurrences = snapshot.occurrences.toMutableList()
        val history = snapshot.history.toMutableList()

        fun fail(problem: TaskProblem): Nothing = throw TaskRuleException(problem)
        fun requireInput(condition: Boolean) { if (!condition) fail(TaskProblem.INVALID_INPUT) }
        fun occurrence(id: String) = occurrences.firstOrNull { it.id == id } ?: fail(TaskProblem.INVALID_INPUT)
        fun definition(id: String) = definitions.first { it.id == id }
        fun put(o: TaskOccurrence) {
            val i = occurrences.indexOfFirst { it.id == o.id }
            if (i < 0) occurrences.add(o) else occurrences[i] = o
        }
        fun put(d: TaskDefinition) {
            val i = definitions.indexOfFirst { it.id == d.id }
            if (i < 0) definitions.add(d) else definitions[i] = d
        }
        fun audit(o: TaskOccurrence, kind: AuditKind, actual: LocalDate? = null, related: String? = null): String {
            val id = UUID.randomUUID().toString()
            history.add(TaskAudit(id, o.id, kind, now, actual, o.dueDate, related))
            return id
        }
        fun validate(fields: TaskFields, rule: RepeatRule): TaskFields {
            requireInput(fields.title.isNotBlank() && fields.difficulty in 0..2 && fields.importance in 0..2)
            requireInput(fields.listId == null || lists.any { it.id == fields.listId && !it.deleted })
            requireInput(fields.deadline == DeadlineKind.NONE || fields.date != null)
            requireInput(fields.minute == null || (fields.minute in 0..1439 && fields.deadline == DeadlineKind.DAY))
            requireInput(rule.interval in 1..36500 && rule.monthDay in 1..31)
            requireInput(rule.count == null || rule.count > 0)
            requireInput(rule.until == null || rule.until >= rule.start)
            if (rule.kind in listOf(RepeatKind.WEEKLY, RepeatKind.CUSTOM)) {
                requireInput(rule.weekdays in 1..127)
                if (rule.kind == RepeatKind.WEEKLY) requireInput(Integer.bitCount(rule.weekdays) == 1)
            }
            if (rule.kind != RepeatKind.NONE) requireInput(fields.deadline != DeadlineKind.NONE)
            return fields.copy(title = fields.title.trim(), description = fields.description.trim(),
                checklist = fields.checklist.map { it.trim() }.filter { it.isNotEmpty() })
        }

        fun firstDate(rule: RepeatRule): LocalDate = when (rule.kind) {
            RepeatKind.WEEKDAYS -> matchingDay(rule.start, 31)
            RepeatKind.WEEKLY, RepeatKind.CUSTOM -> matchingDay(rule.start, rule.weekdays)
            RepeatKind.MONTHLY -> {
                val candidate = rule.start.withDayOfMonth(minOf(rule.monthDay, rule.start.lengthOfMonth()))
                if (candidate >= rule.start) candidate else monthly(rule.start.plusMonths(1), rule.monthDay)
            }
            else -> rule.start
        }
        fun matchingDay(start: LocalDate, mask: Int): LocalDate =
            (0L..6L).map { start.plusDays(it) }.first { mask and (1 shl (it.dayOfWeek.value - 1)) != 0 }
        fun monthly(date: LocalDate, day: Int) = date.withDayOfMonth(minOf(day, date.lengthOfMonth()))
        fun nextDate(rule: RepeatRule, date: LocalDate): LocalDate = when (rule.kind) {
            RepeatKind.WEEKDAYS -> matchingDay(date.plusDays(1), 31)
            RepeatKind.WEEKLY, RepeatKind.CUSTOM -> matchingDay(date.plusDays(1), rule.weekdays)
            RepeatKind.MONTHLY -> monthly(date.withDayOfMonth(1).plusMonths(1), rule.monthDay)
            RepeatKind.FIXED_INTERVAL, RepeatKind.AFTER_COMPLETION -> date.plusDays(rule.interval.toLong())
            else -> date.plusDays(1)
        }
        fun makeOccurrence(d: TaskDefinition, date: LocalDate?, ordinal: Int): TaskOccurrence {
            val f = d.fields.copy(date = date)
            val start = when (f.deadline) {
                DeadlineKind.WEEK, DeadlineKind.WORK_WEEK -> date!!
                    .with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                else -> date
            }
            val due = when (f.deadline) {
                DeadlineKind.NONE -> null
                DeadlineKind.DAY -> date
                DeadlineKind.WEEK -> start!!.plusDays(6)
                DeadlineKind.WORK_WEEK -> start!!.plusDays(4)
            }
            return TaskOccurrence("${d.id}:$ordinal", d.id, ordinal, f, date, start, due,
                checks = f.checklist.map { CheckItem(it) }, position = occurrences.size)
        }
        fun allowed(d: TaskDefinition, date: LocalDate, ordinal: Int) = !d.stopped &&
            (d.rule.count == null || ordinal <= d.rule.count) && (d.rule.until == null || date <= d.rule.until)

        fun generate() {
            definitions.toList().forEach { original ->
                var d = original
                if (d.rule.kind == RepeatKind.NONE || d.stopped) return@forEach
                if (d.rule.kind != RepeatKind.AFTER_COMPLETION && occurrences.any {
                        it.taskId == d.id && it.ordinal == d.nextOrdinal - 1 &&
                            it.status != TaskStatus.DELETED && it.plannedDate?.isAfter(today) == true
                    }) return@forEach
                if (d.rule.kind == RepeatKind.AFTER_COMPLETION && occurrences.any {
                        it.taskId == d.id && it.ordinal == d.nextOrdinal - 1 && it.status == TaskStatus.OPEN
                    }) return@forEach
                var date = d.nextDate ?: return@forEach
                var ordinal = d.nextOrdinal
                while (allowed(d, date, ordinal)) {
                    val existing = occurrences.firstOrNull { it.id == "${d.id}:$ordinal" }
                    if (existing == null || (existing.status == TaskStatus.DELETED && !existing.touched)) {
                        val generated = makeOccurrence(d, date, ordinal)
                        put(generated)
                        if (existing == null) audit(generated, AuditKind.CREATED)
                    }
                    ordinal++
                    val following = nextDate(d.rule, date)
                    d = d.copy(nextDate = following, nextOrdinal = ordinal)
                    put(d)
                    if (original.rule.kind == RepeatKind.AFTER_COMPLETION || date > today) break
                    date = following
                }
            }
        }
        fun run(action: TaskAction): TaskSnapshot {
            when (action) {
                is TaskAction.SaveList -> {
                    val list = action.list
                    requireInput(list.name.isNotBlank() && list.color in 0 until TaskList.COLOR_COUNT &&
                        list.icon in 0 until TaskList.ICON_COUNT)
                    val i = lists.indexOfFirst { it.id == action.list.id }
                    val value = action.list.copy(name = action.list.name.trim())
                    if (i < 0) lists.add(value.copy(position = lists.size)) else lists[i] = value
                }
                is TaskAction.OrderLists -> {
                    val active = lists.filterNot { it.deleted }.map { it.id }
                    requireInput(action.ids.size == active.size && action.ids.toSet() == active.toSet())
                    action.ids.forEachIndexed { index, id ->
                        val i = lists.indexOfFirst { it.id == id }
                        lists[i] = lists[i].copy(position = index)
                    }
                }
                is TaskAction.DeleteList -> {
                    val i = lists.indexOfFirst { it.id == action.id }
                    requireInput(i >= 0)
                    lists[i] = lists[i].copy(deleted = true)
                    definitions.toList().filter { it.fields.listId == action.id }.forEach {
                        put(it.copy(stopped = true))
                    }
                    occurrences.toList().filter { it.fields.listId == action.id && it.status != TaskStatus.DELETED }
                        .forEach {
                            audit(it, AuditKind.DELETED)
                            put(it.copy(status = TaskStatus.DELETED, touched = true))
                        }
                }
                is TaskAction.Create -> if (definitions.none { it.id == action.id }) {
                    val fields = validate(action.fields, action.rule)
                    val d = TaskDefinition(action.id, today, fields, action.rule,
                        if (action.rule.kind == RepeatKind.NONE) null else firstDate(action.rule))
                    put(d)
                    if (d.rule.kind == RepeatKind.NONE) {
                        val o = makeOccurrence(d, fields.date, 1)
                        put(o); audit(o, AuditKind.CREATED)
                    }
                }
                is TaskAction.Check -> {
                    val o = occurrence(action.id)
                    if (o.status != TaskStatus.OPEN) fail(TaskProblem.NOT_OPEN)
                    requireInput(action.index in o.checks.indices)
                    val changed = o.checks.mapIndexed { i, c ->
                        if (i == action.index) c.copy(checked = action.checked) else c
                    }
                    val independent = history.any { it.occurrenceId == o.id &&
                        it.kind in listOf(AuditKind.EDITED, AuditKind.MISSED_DEADLINE) }
                    put(o.copy(checks = changed, touched = changed.any { it.checked } || independent))
                    audit(o, AuditKind.CHECKED)
                }
                is TaskAction.Resolve -> resolve(action)
                is TaskAction.SetCompleted -> {
                    val o = occurrence(action.id)
                    if (action.done && o.status != TaskStatus.DONE) {
                        resolve(TaskAction.Resolve(action.id, true, action.actualDate))
                    } else if (!action.done && o.status != TaskStatus.OPEN) {
                        if (o.status != TaskStatus.DONE) fail(TaskProblem.NOT_OPEN)
                        undo(action.id)
                    }
                }
                is TaskAction.Undo -> undo(action.id)
                is TaskAction.Edit -> edit(action)
                is TaskAction.Postpone -> {
                    val o = postponable(action.id)
                    if (action.to < today || action.to == o.fields.date) fail(TaskProblem.INVALID_DATE)
                    moveDate(o, action.to)
                }
                is TaskAction.UndoPostpone -> {
                    val o = postponable(action.id)
                    if (action.to != o.fields.date) moveDate(o, action.to)
                }
                is TaskAction.Delete -> delete(action)
                is TaskAction.Move -> {
                    val o = occurrence(action.id)
                    requireInput(action.listId == null || lists.any { it.id == action.listId && !it.deleted })
                    put(o.copy(fields = o.fields.copy(listId = action.listId), touched = true))
                    audit(o, AuditKind.EDITED)
                }
                is TaskAction.Reorder -> {
                    val o = occurrence(action.id)
                    val ordered = occurrences.filter { it.status == TaskStatus.OPEN &&
                        it.fields.listId == o.fields.listId }.sortedBy { it.position }.toMutableList()
                    val i = ordered.indexOfFirst { it.id == o.id }
                    if (i >= 0) {
                        val moving = ordered.removeAt(i)
                        ordered.add((i + action.direction).coerceIn(0, ordered.size), moving)
                        ordered.forEachIndexed { index, item -> put(item.copy(position = index)) }
                    }
                }
                is TaskAction.Order -> {
                    requireInput(action.ids.isNotEmpty() && action.ids.distinct().size == action.ids.size)
                    val selected = action.ids.map { occurrence(it) }
                    requireInput(selected.all { it.status == TaskStatus.OPEN &&
                        it.fields.listId == selected.first().fields.listId })
                    val ordered = occurrences.filter { it.status == TaskStatus.OPEN &&
                        it.fields.listId == selected.first().fields.listId }
                        .sortedWith(compareBy<TaskOccurrence> { it.position }.thenBy { it.id })
                    val selectedIds = action.ids.toSet()
                    val replacements = selected.iterator()
                    ordered.forEachIndexed { index, item ->
                        val next = if (item.id in selectedIds) replacements.next() else item
                        put(next.copy(position = index))
                    }
                }
                TaskAction.Refresh, is TaskAction.Materialize -> Unit
            }
            generate()
            return TaskSnapshot(lists.toList(), definitions.toList(), occurrences.toList(), history.toList())
        }
        fun resolve(action: TaskAction.Resolve) {
            val o = occurrence(action.id)
            if (o.status != TaskStatus.OPEN) fail(TaskProblem.NOT_OPEN)
            val d = definition(o.taskId)
            if (action.done && (action.actualDate < d.created || action.actualDate > today)) {
                fail(TaskProblem.INVALID_DATE)
            }
            val outcome = audit(o, if (action.done) AuditKind.COMPLETED else AuditKind.NOT_DONE,
                if (action.done) action.actualDate else null)
            put(o.copy(status = if (action.done) TaskStatus.DONE else TaskStatus.NOT_DONE,
                completedDate = if (action.done) action.actualDate else null, activeOutcome = outcome, touched = true))
            if (d.rule.kind == RepeatKind.AFTER_COMPLETION && !d.stopped && o.ordinal == d.nextOrdinal - 1) {
                val anchor = if (action.done) action.actualDate else o.plannedDate!!
                put(d.copy(nextDate = anchor.plusDays(d.rule.interval.toLong()), nextOrdinal = o.ordinal + 1))
            }
        }
        fun later(o: TaskOccurrence) = occurrences.filter { it.taskId == o.taskId && it.ordinal > o.ordinal }
        fun guardLater(o: TaskOccurrence) {
            if (later(o).any { it.touched }) fail(TaskProblem.DEPENDENT_ACTIONS)
        }
        fun undo(id: String) {
            val o = occurrence(id)
            if (o.status != TaskStatus.DONE && o.status != TaskStatus.NOT_DONE) fail(TaskProblem.NOT_OPEN)
            val d = definition(o.taskId)
            if (d.rule.kind == RepeatKind.AFTER_COMPLETION) {
                guardLater(o)
                later(o).forEach { put(it.copy(status = TaskStatus.DELETED, touched = false)) }
                put(d.copy(nextDate = o.plannedDate, nextOrdinal = o.ordinal + 1))
            }
            audit(o, AuditKind.UNDONE, related = o.activeOutcome)
            val hasIndependentChanges = o.checks.any { it.checked } || history.any {
                it.occurrenceId == o.id && it.kind in listOf(AuditKind.EDITED, AuditKind.MISSED_DEADLINE)
            }
            put(o.copy(status = TaskStatus.OPEN, completedDate = null, activeOutcome = null,
                touched = hasIndependentChanges))
        }
        fun edit(action: TaskAction.Edit) {
            val o = occurrence(action.id)
            if (o.status != TaskStatus.OPEN) fail(TaskProblem.NOT_OPEN)
            val fields = validate(action.fields, action.rule)
            val dateChanged = fields.deadline != o.fields.deadline || fields.date != o.fields.date
            if (dateChanged && o.needsReview(today)) {
                if (!action.reviewedDeadline) fail(TaskProblem.REVIEW_REQUIRED)
                audit(o, AuditKind.MISSED_DEADLINE)
            }
            val d = definition(o.taskId)
            if (action.scope == ChangeScope.THIS_AND_FUTURE && d.rule.kind != RepeatKind.NONE) {
                requireInput(action.rule.count == null || action.rule.count >= o.ordinal)
                requireInput(action.rule.until == null || fields.date == null || action.rule.until >= fields.date)
                guardLater(o)
                later(o).filter { it.status != TaskStatus.DELETED || !it.touched }.forEach {
                    put(it.copy(status = TaskStatus.DELETED, touched = false))
                }
                val updated = d.copy(fields = fields, rule = action.rule, nextDate =
                    if (action.rule.kind == RepeatKind.NONE) null else nextDate(action.rule, fields.date!!),
                    nextOrdinal = o.ordinal + 1, stopped = action.rule.kind == RepeatKind.NONE)
                put(updated)
            } else if (d.rule.kind == RepeatKind.NONE) {
                put(d.copy(fields = fields, rule = action.rule, nextDate =
                    if (action.rule.kind == RepeatKind.NONE) null else nextDate(action.rule, fields.date!!),
                    nextOrdinal = o.ordinal + 1, stopped = false))
            }
            val replacement = makeOccurrence(d.copy(fields = fields), fields.date, o.ordinal)
            val checks = fields.checklist.mapIndexed { index, label ->
                CheckItem(label, o.checks.getOrNull(index)?.takeIf { it.label == label }?.checked ?: false)
            }
            put(replacement.copy(checks = checks, position = o.position, touched = true,
                plannedDate = if (action.scope == ChangeScope.ONLY_THIS && d.rule.kind != RepeatKind.NONE)
                    o.plannedDate else replacement.plannedDate))
            audit(o, AuditKind.EDITED)
        }
        fun postponable(id: String): TaskOccurrence {
            val o = occurrence(id)
            if (o.status != TaskStatus.OPEN) fail(TaskProblem.NOT_OPEN)
            requireInput(o.canPostpone)
            return o
        }
        /**
         * Postponing is an "only this occurrence" date edit: an overdue date records the missed deadline,
         * recurring series keep their own dates and history stays append-only.
         */
        fun moveDate(o: TaskOccurrence, to: LocalDate) {
            edit(TaskAction.Edit(o.id, o.fields.copy(date = to), definition(o.taskId).rule, ChangeScope.ONLY_THIS,
                reviewedDeadline = true))
        }
        fun delete(action: TaskAction.Delete) {
            val o = occurrence(action.id)
            val d = definition(o.taskId)
            if (action.scope == ChangeScope.THIS_AND_FUTURE) {
                guardLater(o)
                put(d.copy(stopped = true))
                later(o).forEach {
                    audit(it, AuditKind.DELETED)
                    put(it.copy(status = TaskStatus.DELETED, touched = true))
                }
            }
            audit(o, AuditKind.DELETED)
            put(o.copy(status = TaskStatus.DELETED, touched = true))
            if (d.rule.kind == RepeatKind.AFTER_COMPLETION && action.scope == ChangeScope.ONLY_THIS &&
                o.status == TaskStatus.OPEN && o.ordinal == d.nextOrdinal - 1) {
                put(d.copy(nextDate = nextDate(d.rule, o.plannedDate!!), nextOrdinal = o.ordinal + 1))
            }
        }
    }
}
