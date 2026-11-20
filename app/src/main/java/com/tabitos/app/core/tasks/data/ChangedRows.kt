package com.tabitos.app.core.tasks.data

import com.tabitos.app.core.tasks.domain.TaskSnapshot

/**
 * Rows of [after] that are new or differ from the stored row with the same key. Upserting only these leaves the
 * table exactly as upserting every row of [after] would, without rewriting (and invalidating) unchanged rows.
 */
fun <T, K> changedRows(stored: List<T>, after: List<T>, key: (T) -> K): List<T> {
    val storedByKey = stored.associateBy(key)
    return after.filter { storedByKey[key(it)] != it }
}

/** The task tables as stored, read in one transaction. */
data class TaskRows(
    val lists: List<ListEntity>, val definitions: List<DefinitionEntity>, val occurrences: List<OccurrenceEntity>,
    val checks: List<ChecklistEntity>, val history: List<AuditEntity>,
) {
    fun snapshot(): TaskSnapshot {
        val checksByOwner = checks.groupBy { it.ownerId }
        return TaskSnapshot(lists.map { it.domain() },
            definitions.map { it.domain(checksByOwner[templateOwner(it.id)].orEmpty()) },
            occurrences.map { it.domain(checksByOwner[it.id].orEmpty()) }, history.map { it.domain() })
    }
}

/** Template checklist items are stored under this owner, apart from each occurrence's own items. */
fun templateOwner(definitionId: String) = "template:$definitionId"

/** Checklist rows per owner as a save stores them: every definition's template and every occurrence. */
fun TaskSnapshot.checklistRows(): Map<String, List<ChecklistEntity>> {
    val templates = definitions.associate { d ->
        val owner = templateOwner(d.id)
        owner to d.fields.checklist.mapIndexed { i, label -> ChecklistEntity("$owner:$i", owner, i, label, false) }
    }
    val occurrenceItems = occurrences.associate { o ->
        o.id to o.checks.mapIndexed { i, c -> ChecklistEntity("${o.id}:$i", o.id, i, c.label, c.checked) }
    }
    return templates + occurrenceItems
}

/**
 * What saving a task snapshot must write so the tables end as a full rewrite of it would leave them (spec 027):
 * changed rows are upserted, and an owner's checklist is replaced only when its items changed.
 */
class TaskWrites(
    val lists: List<ListEntity>, val definitions: List<DefinitionEntity>, val occurrences: List<OccurrenceEntity>,
    val checklists: Map<String, List<ChecklistEntity>>, val history: List<AuditEntity>,
) {
    val isEmpty: Boolean get() = lists.isEmpty() && definitions.isEmpty() && occurrences.isEmpty() &&
        checklists.isEmpty() && history.isEmpty()

    companion object {
        fun between(stored: TaskRows, after: TaskSnapshot): TaskWrites {
            val storedChecks = stored.checks.groupBy { it.ownerId }.mapValues { (_, rows) -> rows.toSet() }
            return TaskWrites(
                lists = changedRows(stored.lists, after.lists.map { it.entity() }) { it.id },
                definitions = changedRows(stored.definitions, after.definitions.map { it.entity() }) { it.id },
                occurrences = changedRows(stored.occurrences, after.occurrences.map { it.entity() }) { it.id },
                checklists = after.checklistRows().filter { (owner, rows) ->
                    storedChecks[owner].orEmpty() != rows.toSet()
                },
                history = changedRows(stored.history, after.history.map { it.entity() }) { it.id },
            )
        }
    }
}
