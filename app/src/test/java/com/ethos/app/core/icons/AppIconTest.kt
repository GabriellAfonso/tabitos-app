package com.ethos.app.core.icons

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppIconTest {
    // Lists and habits persist indices derived from this order and rewards persist the names: any change
    // here other than appending at the end would redraw or invalidate saved icons.
    @Test fun catalogOrderAndNamesAreAppendOnly() {
        val expected = listOf(
            "LIST", "STAR", "HEART", "FLAG", "DONE", "ENERGY", "IDEA", "ROCKET", "TROPHY", "PIN", "BOOKMARK",
            "EVENT", "CLOCK", "ALARM", "SUN", "MOON", "HOME", "CLEANING", "LAUNDRY", "KITCHEN", "BED", "FURNITURE",
            "GARDEN", "PLANT", "TRASH", "REPAIRS", "SHOWER", "KEY", "WORK", "STUDY", "SCHOOL", "READING", "NOTES",
            "LAPTOP", "CODE", "EMAIL", "CALL", "MEETING", "LANGUAGES", "CALCULATOR", "SCIENCE", "DRAWING",
            "RECORDING", "ANNOUNCEMENT", "LEAF", "WATER", "MEDITATION", "WELLNESS", "MEDICINE", "HEALTH", "MIND",
            "TEETH", "SLEEP", "DOCTOR", "NUTRITION", "MOOD", "GYM", "RUNNING", "WALKING", "CYCLING", "SWIMMING",
            "SOCCER", "BASKETBALL", "TENNIS", "HIKING", "MARTIAL_ARTS", "SKATE", "SURF", "FOOD", "REST", "BAKERY",
            "BREAKFAST", "SNACK", "DINNER", "ICE_CREAM", "PIZZA", "RECIPE", "MONEY", "SAVINGS", "WALLET", "CARD",
            "BILLS", "GROCERIES", "SHOPPING", "STORE", "GIFT", "SALES", "INVESTMENTS", "GAME", "MOVIE", "SERIES",
            "MUSIC", "HEADPHONES", "PIANO", "PAINTING", "PHOTO", "PUZZLE", "PARK", "BEACH", "PARTY", "FAMILY",
            "CHILDREN", "PETS", "FRIENDS", "VOLUNTEERING", "CHURCH", "BIRTHDAY", "CHAT", "COMMITMENT", "FLIGHT",
            "LUGGAGE", "CAR", "BUS", "TRAIN", "HOTEL", "MAP", "FUEL", "BLOCK", "CIGARETTE", "DRINK",
            "ADULT_CONTENT", "SCREEN_TIME", "FAST_FOOD", "SWEETS", "BETTING", "PHONE", "COMPLAINING",
        )
        assertEquals(expected, AppIcon.entries.map { it.name })
    }

    @Test fun tablesKeepTheIconsSavedBeforeTheCatalog() {
        assertEquals(listOf(AppIcon.LIST, AppIcon.WORK, AppIcon.HOME, AppIcon.STUDY, AppIcon.HEART, AppIcon.STAR),
            AppIconTables.list.take(6))
        assertEquals(listOf(AppIcon.LEAF, AppIcon.STUDY, AppIcon.HOME, AppIcon.WORK, AppIcon.HEART, AppIcon.STAR,
            AppIcon.BLOCK, AppIcon.DRINK, AppIcon.CIGARETTE, AppIcon.ADULT_CONTENT, AppIcon.SCREEN_TIME,
            AppIcon.FAST_FOOD, AppIcon.SWEETS, AppIcon.BETTING, AppIcon.GAME, AppIcon.GROCERIES, AppIcon.SLEEP),
            AppIconTables.habit.take(17))
    }

    @Test fun everyTableOffersEachIconOnce() {
        listOf(AppIconTables.list, AppIconTables.habit).forEach { table ->
            assertEquals(AppIcon.entries.toSet(), table.toSet())
            assertEquals(AppIcon.entries.size, table.size)
        }
    }

    @Test fun rewardKeysResolveByName() {
        assertEquals(listOf(AppIcon.GIFT, AppIcon.GAME, AppIcon.FOOD, AppIcon.REST),
            listOf("GIFT", "GAME", "FOOD", "REST").map { AppIcon.fromKey(it) })
        assertNull(AppIcon.fromKey("UNKNOWN"))
    }
}
