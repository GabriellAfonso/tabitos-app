package com.ethos.app.core.habits.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import java.time.LocalDate

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits") suspend fun habits(): List<HabitEntity>
    @Query("SELECT * FROM habit_rules") suspend fun rules(): List<HabitRuleEntity>
    @Query("SELECT * FROM habit_intervals") suspend fun intervals(): List<HabitIntervalEntity>
    @Query("SELECT * FROM habit_overrides") suspend fun overrides(): List<HabitOverrideEntity>
    @Query("SELECT * FROM habit_records") suspend fun records(): List<HabitRecordEntity>
    @Query("SELECT * FROM habit_reviews") suspend fun reviews(): List<HabitReviewEntity>
    @Query("SELECT * FROM habit_audits") suspend fun audits(): List<HabitAuditEntity>
    @Upsert suspend fun habits(rows: List<HabitEntity>)
    @Upsert suspend fun rules(rows: List<HabitRuleEntity>)
    @Upsert suspend fun intervals(rows: List<HabitIntervalEntity>)
    @Upsert suspend fun overrides(rows: List<HabitOverrideEntity>)
    @Upsert suspend fun records(rows: List<HabitRecordEntity>)
    @Upsert suspend fun reviews(rows: List<HabitReviewEntity>)
    @Upsert suspend fun audits(rows: List<HabitAuditEntity>)
    @Query("DELETE FROM habit_rules WHERE id NOT IN (:ids)") suspend fun removeReplacedRules(ids: List<String>)
    @Query("UPDATE habit_intervals SET `end` = :today WHERE `end` IS NULL AND habitId IN " +
        "(SELECT id FROM habits WHERE listId = :listId AND status = 'ACTIVE')")
    suspend fun closeListIntervals(listId: String, today: LocalDate)
    @Query("UPDATE habits SET status = 'EXCLUDED' WHERE listId = :listId")
    suspend fun excludeList(listId: String)
    @Query("DELETE FROM habits WHERE id = :id") suspend fun removeHabit(id: String)
    @Query("DELETE FROM habit_rules WHERE habitId = :id") suspend fun removeRules(id: String)
    @Query("DELETE FROM habit_intervals WHERE habitId = :id") suspend fun removeIntervals(id: String)
    @Query("DELETE FROM habit_records WHERE habitId = :id") suspend fun removeRecords(id: String)
    @Query("DELETE FROM habit_audits WHERE habitId = :id") suspend fun removeAudits(id: String)
    /** Period keys start with the habit id ([com.ethos.app.core.habits.domain.HabitRules.key]). */
    @Query("DELETE FROM habit_overrides WHERE `key` LIKE :prefix || '%'") suspend fun removeOverrides(prefix: String)
    @Query("DELETE FROM habit_reviews WHERE `key` LIKE :prefix || '%'") suspend fun removeReviews(prefix: String)
}
