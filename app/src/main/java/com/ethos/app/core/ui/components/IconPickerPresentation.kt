package com.ethos.app.core.ui.components

import com.ethos.app.core.icons.AppIcon
import com.ethos.app.core.icons.IconCategory
import java.text.Normalizer

/** One titled block of the icon catalog. */
data class IconSection(val category: IconCategory, val icons: List<AppIcon>)

/**
 * The editor's single row: the principal icons, with the current icon taking the first place when it is not
 * one of them, so the selection is always visible without the row growing.
 */
fun principalRow(principal: List<AppIcon>, selected: AppIcon): List<AppIcon> =
    if (selected in principal) principal else listOf(selected) + principal.dropLast(1)

/**
 * Catalog sections whose icon or category name contains [query], ignoring case and accents ("agua" finds
 * "Água"). A blank query keeps everything.
 */
fun iconSections(
    query: String, iconName: (AppIcon) -> String, categoryName: (IconCategory) -> String,
): List<IconSection> {
    val wanted = query.searchable()
    return IconCategory.entries.mapNotNull { category ->
        val inCategory = AppIcon.entries.filter { it.category == category }
        val categoryMatches = categoryName(category).searchable().contains(wanted)
        val icons = if (categoryMatches) inCategory
            else inCategory.filter { iconName(it).searchable().contains(wanted) }
        icons.takeIf { it.isNotEmpty() }?.let { IconSection(category, it) }
    }
}

/** What the catalog sheet shows for the current search. */
data class IconCatalog(
    val sections: List<IconSection>,
    /** Grid position of each section header, for the category shortcuts. */
    val headerPositions: Map<IconCategory, Int>,
) {
    val isEmpty: Boolean get() = sections.isEmpty()
}

/** Category shortcuts only make sense over the whole catalog; while searching the result is already short. */
fun showsCategoryShortcuts(query: String): Boolean = query.isBlank()

fun iconCatalog(query: String, iconName: (AppIcon) -> String, categoryName: (IconCategory) -> String): IconCatalog {
    val sections = iconSections(query, iconName, categoryName)
    return IconCatalog(sections, sectionHeaderPositions(sections))
}

/** Grid position of each section header, given that every header is followed by its icons. */
fun sectionHeaderPositions(sections: List<IconSection>): Map<IconCategory, Int> {
    var position = 0
    return sections.associate { section ->
        (section.category to position).also { position += 1 + section.icons.size }
    }
}

private fun String.searchable(): String =
    Normalizer.normalize(trim().lowercase(), Normalizer.Form.NFD).replace(DIACRITICS, "")

private val DIACRITICS = Regex("\\p{Mn}+")
