package com.ethos.app.core.ui.components

import com.ethos.app.core.icons.AppIcon
import com.ethos.app.core.icons.IconCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IconPickerPresentationTest {
    private val principal = listOf(AppIcon.LIST, AppIcon.WORK, AppIcon.HOME, AppIcon.STUDY)
    private val iconNames = mapOf(AppIcon.WATER to "Água", AppIcon.PETS to "Animal de estimação")
    private val categoryNames = mapOf(IconCategory.SPORT to "Esporte")
    private fun iconName(icon: AppIcon) = iconNames[icon] ?: icon.name
    private fun categoryName(category: IconCategory) = categoryNames[category] ?: category.name

    @Test fun principalRowKeepsItsLengthAndAlwaysShowsTheSelection() {
        assertEquals(principal, principalRow(principal, AppIcon.HOME))
        assertEquals(listOf(AppIcon.PETS, AppIcon.LIST, AppIcon.WORK, AppIcon.HOME),
            principalRow(principal, AppIcon.PETS))
    }

    @Test fun blankSearchShowsEveryIconGroupedInCategoryOrder() {
        val sections = iconSections(" ", ::iconName, ::categoryName)
        assertEquals(IconCategory.entries, sections.map { it.category })
        assertEquals(AppIcon.entries.toSet(), sections.flatMap { it.icons }.toSet())
    }

    @Test fun searchIgnoresCaseAndAccentsAndMatchesCategoryNames() {
        val water = iconSections("AGUA", ::iconName, ::categoryName)
        assertEquals(listOf(IconSection(IconCategory.HEALTH, listOf(AppIcon.WATER))), water)
        val sport = iconSections("esporte", ::iconName, ::categoryName)
        assertEquals(AppIcon.entries.filter { it.category == IconCategory.SPORT }, sport.single().icons)
        assertTrue(iconCatalog("zzz", ::iconName, ::categoryName).isEmpty)
    }

    @Test fun headerPositionsCountEachHeaderAndItsIcons() {
        val sections = listOf(IconSection(IconCategory.GENERAL, listOf(AppIcon.LIST, AppIcon.STAR)),
            IconSection(IconCategory.HOME, listOf(AppIcon.HOME)),
            IconSection(IconCategory.AVOID, listOf(AppIcon.BLOCK)))
        assertEquals(mapOf(IconCategory.GENERAL to 0, IconCategory.HOME to 3, IconCategory.AVOID to 5),
            sectionHeaderPositions(sections))
    }

    @Test fun categoryShortcutsOnlyWithoutSearch() {
        assertTrue(showsCategoryShortcuts(""))
        assertFalse(showsCategoryShortcuts("água"))
    }
}
