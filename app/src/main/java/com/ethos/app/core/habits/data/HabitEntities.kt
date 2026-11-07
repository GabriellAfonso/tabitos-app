package com.ethos.app.core.habits.data

import androidx.room.Embedded
import androidx.room.Entity
import com.ethos.app.core.habits.domain.*

@Entity(tableName = "habits", primaryKeys = ["id"])
data class HabitEntity(@Embedded val value: Habit)
@Entity(tableName = "habit_rules", primaryKeys = ["id"])
data class HabitRuleEntity(@Embedded val value: HabitRule)
@Entity(tableName = "habit_intervals", primaryKeys = ["id"])
data class HabitIntervalEntity(@Embedded val value: ActiveInterval)
@Entity(tableName = "habit_overrides", primaryKeys = ["key"])
data class HabitOverrideEntity(@Embedded val value: HabitOverride)
@Entity(tableName = "habit_records", primaryKeys = ["id"])
data class HabitRecordEntity(@Embedded val value: HabitRecord)
@Entity(tableName = "habit_reviews", primaryKeys = ["key"])
data class HabitReviewEntity(@Embedded val value: HabitReview)
@Entity(tableName = "habit_audits", primaryKeys = ["id"])
data class HabitAuditEntity(@Embedded val value: HabitAudit)
