package com.ethos.app.core.accounting.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val ACCOUNTING_MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS accounting_activation " +
            "(id INTEGER NOT NULL PRIMARY KEY, time INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS accounting_prices (category INTEGER NOT NULL PRIMARY KEY, " +
            "coins INTEGER NOT NULL, xp INTEGER NOT NULL, penalty INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS accounting_entitlements (id TEXT NOT NULL PRIMARY KEY, " +
            "`key` TEXT NOT NULL, owner TEXT NOT NULL, title TEXT NOT NULL, date INTEGER NOT NULL, " +
            "coins INTEGER NOT NULL, xp INTEGER NOT NULL, active INTEGER NOT NULL, exempt INTEGER NOT NULL, " +
            "lastEntry INTEGER, priced INTEGER NOT NULL, reward INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS accounting_ledger (id INTEGER NOT NULL PRIMARY KEY, " +
            "sequence INTEGER NOT NULL, `key` TEXT NOT NULL, title TEXT NOT NULL, date INTEGER NOT NULL, " +
            "time INTEGER NOT NULL, kind TEXT NOT NULL, nominalCoins INTEGER NOT NULL, coins INTEGER NOT NULL, " +
            "xp INTEGER NOT NULL, balance INTEGER NOT NULL, totalXp INTEGER NOT NULL, related INTEGER)")
    }
}
