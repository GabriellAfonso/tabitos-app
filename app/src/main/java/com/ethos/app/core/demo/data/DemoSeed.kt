package com.ethos.app.core.demo.data

import com.ethos.app.core.habits.data.RoomHabitRepository
import com.ethos.app.core.habits.domain.*
import com.ethos.app.core.preferences.FixedGameMode
import com.ethos.app.core.rewards.data.RoomRewardRepository
import com.ethos.app.core.rewards.domain.Reward
import com.ethos.app.core.rewards.domain.PurchaseResult
import com.ethos.app.core.tasks.data.RoomTaskRepository
import com.ethos.app.core.tasks.data.TaskDatabase
import com.ethos.app.core.tasks.domain.*
import com.ethos.app.core.timers.data.RoomTimerRepository
import com.ethos.app.core.timers.domain.*
import com.ethos.app.core.trackers.data.RoomTrackerRepository
import com.ethos.app.core.trackers.domain.*
import java.time.LocalDate
import java.time.ZoneId

/** Runs inside the reset transaction, using real rules/ledger rather than fabricated balances. */
internal class DemoSeed(
    private val db: TaskDatabase, private val realClock: TaskClock,
    private val timerClock: TimerClock, private val text: Array<String>,
) {
    private val today = realClock.today()
    private val start = HabitRules.start(today, HabitUnit.WEEK).minusWeeks(2)
    private var day = start
    private val clock = object : TaskClock {
        override fun today(): LocalDate = day
        override fun nowMillis(): Long = minOf(realClock.nowMillis(),
            day.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
    }
    // The fictitious wallet is always scored and the user's game mode preference is never written (spec 017).
    private val tasks = RoomTaskRepository(db, clock, FixedGameMode(on = true))
    private val habits = RoomHabitRepository(db, clock, FixedGameMode(on = true))
    private val trackers = RoomTrackerRepository(db, clock)

    suspend fun populate() {
        listOf("home", "work", "health", "study").forEachIndexed { i, id ->
            tasks.execute(TaskAction.SaveList(TaskList("demo-$id", text[i], color = i, icon = i, position = i)))
        }
        habit("read", 4, "study")
        habit("walk", 5, "health", HabitUnit.WEEK, 3)
        habit("water", 6, "health")
        habit("avoid", 7, "health", type = HabitType.AVOID, target = 0)
        habit("coffee", 8, "health", type = HabitType.LIMIT, target = 2)
        habit("stretch", 9, "health")
        trackers.execute(TrackerAction.SaveForm("demo-tracker", "demo-walk", false, listOf(
            TrackerQuestion("energy", QuestionType.SCALE, text[10], required = true, primary = true,
                min = 1, max = 5, minLabel = text[11], maxLabel = text[12]),
            TrackerQuestion("place", QuestionType.CHOICE, text[13], options = listOf(
                TrackerOption("park", text[14]), TrackerOption("street", text[15]))),
            TrackerQuestion("note", QuestionType.TEXT, text[16]),
        )))
        tasks.execute(TaskAction.Create("demo-tidy", TaskFields(text[17], listId = "demo-home",
            deadline = DeadlineKind.DAY, date = start), RepeatRule(RepeatKind.DAILY, start)))
        var offset = 0
        while (day <= today) {
            tasks.execute(TaskAction.Refresh)
            if (day < today) {
                if (offset % 4 != 0) record("read", offset)
                record("water", offset)
                if (day.dayOfWeek.value in listOf(1, 3, 5)) record("walk", offset)
                if (offset % 5 == 0) record("avoid", offset)
                repeat(if (offset % 4 == 0) 3 else 1) { record("coffee", offset * 10 + it) }
                if (offset % 3 != 0) tasks.execute(TaskAction.Resolve("demo-tidy:${offset + 1}", true, day))
                if (offset % 3 != 0) trackers.execute(TrackerAction.SaveEntry("demo-entry-$offset",
                    "demo-tracker", day, listOf(TrackerAnswer("energy", scale = 2 + offset % 4),
                        TrackerAnswer("place", choices = listOf(if (offset % 2 == 0) "park" else "street")),
                        TrackerAnswer("note", text = text[18]))))
            }
            if (day == today) break
            day = day.plusDays(1)
            offset++
        }
        record("water", offset)
        record("coffee", offset * 10)
        habits.execute(HabitAction.Status("demo-stretch", HabitStatus.PAUSED))
        createTask("groceries", 19, "home", today, listOf(text[20], text[21], text[22]))
        tasks.execute(TaskAction.Check("demo-groceries:1", 0, true))
        createTask("proposal", 23, "work", today)
        createTask("dentist", 24, "health", today.plusDays(2))
        createTask("course", 25, "study", today.plusDays(1))
        createTask("bill", 26, "home", today.minusDays(2))
        createTask("closet", 27, "home", null)
        createTask("finished", 28, "work", today)
        tasks.execute(TaskAction.Resolve("demo-finished:1", true, today))
        val rewards = RoomRewardRepository(db, clock, timerClock)
        rewards.save(Reward("demo-coffee-reward", text[29], icon = "FOOD", cost = 20, limit = 1))
        rewards.save(Reward("demo-game-reward", text[30], icon = "GAME", cost = 40, minutes = 30))
        rewards.save(Reward("demo-film-reward", text[31], icon = "REST", cost = 120, limit = 1, period = "WEEK"))
        rewards.preview("demo-coffee-reward")
        // Do not start an invalidation Flow while holding the reset's write transaction.
        val intent = db.rewards().pending()?.domain() ?: error("Missing demo purchase")
        check(rewards.confirm(intent) == PurchaseResult.PURCHASED)
        val timers = RoomTimerRepository(db, timerClock)
        timers.saveGoal("task:demo-proposal", 25)
        timers.saveGoal("habit:demo-read", 15)
    }

    private suspend fun habit(
        id: String, label: Int, list: String, unit: HabitUnit = HabitUnit.DAY,
        target: Int = 1, type: HabitType = HabitType.PRACTICE,
    ) {
        habits.execute(HabitAction.Save(Habit("demo-$id", start, text[label], listId = "demo-$list"),
            HabitRule("demo-rule-$id", "demo-$id", start, type, unit, target)))
    }

    private suspend fun record(id: String, index: Int) {
        habits.execute(HabitAction.Record("demo-record-$id-$index", "demo-$id", day))
    }

    private suspend fun createTask(id: String, label: Int, list: String, date: LocalDate?,
        checks: List<String> = emptyList()) {
        tasks.execute(TaskAction.Create("demo-$id", TaskFields(text[label], listId = "demo-$list",
            deadline = if (date == null) DeadlineKind.NONE else DeadlineKind.DAY,
            date = date, checklist = checks), RepeatRule(start = today)))
    }
}
