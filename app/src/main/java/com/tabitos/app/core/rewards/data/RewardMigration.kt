package com.tabitos.app.core.rewards.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val REWARD_MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        val fields = "`reward_id` TEXT NOT NULL, `reward_name` TEXT NOT NULL, " +
            "`reward_description` TEXT NOT NULL, `reward_icon` TEXT NOT NULL, `reward_cost` INTEGER NOT NULL, " +
            "`reward_limit` INTEGER, `reward_period` TEXT NOT NULL, `reward_position` INTEGER NOT NULL, " +
            "`reward_revision` INTEGER NOT NULL, `reward_deleted` INTEGER NOT NULL"
        db.execSQL("CREATE TABLE IF NOT EXISTS `rewards` (`id` TEXT NOT NULL, $fields, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `reward_intents` (`id` TEXT NOT NULL, $fields, " +
            "`date` INTEGER NOT NULL, `balance` INTEGER NOT NULL, `used` INTEGER NOT NULL, " +
            "`closed` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `reward_purchases` (`id` TEXT NOT NULL, $fields, " +
            "`date` INTEGER NOT NULL, `time` INTEGER NOT NULL, `ledgerSequence` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_reward_purchases_ledgerSequence` " +
            "ON `reward_purchases` (`ledgerSequence`)")
    }
}
