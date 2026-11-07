package com.ethos.app.core.habits.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val HABIT_MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""CREATE TABLE IF NOT EXISTS habits (
            id TEXT NOT NULL PRIMARY KEY, created INTEGER NOT NULL, name TEXT NOT NULL,
            description TEXT NOT NULL, listId TEXT, color INTEGER NOT NULL, icon INTEGER NOT NULL,
            difficulty INTEGER NOT NULL, importance INTEGER NOT NULL, importanceManual INTEGER NOT NULL,
            status TEXT NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS habit_rules (
            id TEXT NOT NULL PRIMARY KEY, habitId TEXT NOT NULL, effective INTEGER NOT NULL,
            type TEXT NOT NULL, unit TEXT NOT NULL, target INTEGER NOT NULL, weekdays INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS habit_intervals (
            id TEXT NOT NULL PRIMARY KEY, habitId TEXT NOT NULL, start INTEGER NOT NULL, `end` INTEGER)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS habit_overrides (
            `key` TEXT NOT NULL PRIMARY KEY, target INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS habit_records (
            id TEXT NOT NULL PRIMARY KEY, habitId TEXT NOT NULL, date INTEGER NOT NULL,
            entered INTEGER NOT NULL, deleted INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS habit_reviews (
            `key` TEXT NOT NULL PRIMARY KEY, outcome TEXT NOT NULL, invalidated INTEGER NOT NULL)""")
        db.execSQL("""CREATE TABLE IF NOT EXISTS habit_audits (
            id TEXT NOT NULL PRIMARY KEY, habitId TEXT NOT NULL, reference TEXT NOT NULL,
            time INTEGER NOT NULL, kind TEXT NOT NULL, `before` TEXT NOT NULL, `after` TEXT NOT NULL)""")
    }
}
