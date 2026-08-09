package com.miguel.coach

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePreferenceRepositoryTest {

    @Test
    fun `exposes exactly ten themes with unique ids`() {
        assertEquals(10, CoachTheme.entries.size)
        assertEquals(10, CoachTheme.entries.map(CoachTheme::id).toSet().size)
    }

    @Test
    fun `shows dark themes before light themes in the approved appearance order`() {
        assertEquals(
            listOf("Obsidian", "Ocean", "Forest", "Green", "Orange", "Light", "Sand", "Ice", "Mint", "Sky"),
            appearanceThemeOrder.map(CoachTheme::displayName)
        )
        assertEquals(CoachTheme.entries.toSet(), appearanceThemeOrder.toSet())
        assertEquals(5, appearanceDarkThemes.size)
        assertEquals(5, appearanceLightThemes.size)
        assertEquals(listOf("Light", "Sand", "Ice", "Mint", "Sky"), appearanceLightThemes.map(CoachTheme::displayName))
    }

    @Test
    fun `keeps every original theme id compatible`() {
        listOf(
            "obsidian" to CoachTheme.OBSIDIAN,
            "ocean" to CoachTheme.OCEAN,
            "forest" to CoachTheme.FOREST,
            "light" to CoachTheme.LIGHT,
            "green" to CoachTheme.GREEN,
            "orange" to CoachTheme.ORANGE,
            "sand" to CoachTheme.SAND,
            "ice" to CoachTheme.ICE,
            "mint" to CoachTheme.MINT,
            "sky" to CoachTheme.SKY
        ).forEach { (id, expected) -> assertEquals(expected, CoachTheme.fromId(id)) }
    }

    @Test
    fun `new themes use their specified base palettes`() {
        assertBasePalette(CoachTheme.GREEN, 0xFF0D1014, 0xFF1C222B, 0xFF86DF86, 0xFFF5F5F5, 0xFFAEB4BE)
        assertBasePalette(CoachTheme.ORANGE, 0xFF0D1014, 0xFF1C222B, 0xFFEA571C, 0xFFF5F5F5, 0xFFAEB4BE)
        assertBasePalette(CoachTheme.SAND, 0xFFF7F4EF, 0xFFFFFFFF, 0xFFD56A32, 0xFF24211F, 0xFF68645F)
        assertBasePalette(CoachTheme.ICE, 0xFFF3F7FA, 0xFFFFFFFF, 0xFF4F7898, 0xFF20262C, 0xFF65717C)
        assertBasePalette(CoachTheme.MINT, 0xFFF3F8F3, 0xFFFFFFFF, 0xFF4F8A5B, 0xFF20251F, 0xFF5F6960)
        assertBasePalette(CoachTheme.SKY, 0xFFF2F8FA, 0xFFFFFFFF, 0xFF3F8198, 0xFF1E2528, 0xFF5D6D73)
        assertEquals(Color(0xFFDED8D0), CoachTheme.SAND.colorScheme.outline)
        assertEquals(Color(0xFFD9E1E7), CoachTheme.ICE.colorScheme.outline)
        assertEquals(Color(0xFFE2EDE3), CoachTheme.MINT.colorScheme.surfaceVariant)
        assertEquals(Color(0xFFE0EDF1), CoachTheme.SKY.colorScheme.surfaceVariant)
    }

    @Test
    fun `all content cards consume the shared theme color`() {
        assertEquals(Color(0xFFE8EBEF), contentCardContainerColor(CoachTheme.LIGHT.colorScheme))
        assertEquals(Color(0xFFE9E4DE), contentCardContainerColor(CoachTheme.SAND.colorScheme))
        assertEquals(Color(0xFFE3EAF0), contentCardContainerColor(CoachTheme.ICE.colorScheme))
        assertEquals(Color(0xFFE2EDE3), contentCardContainerColor(CoachTheme.MINT.colorScheme))
        assertEquals(Color(0xFFE0EDF1), contentCardContainerColor(CoachTheme.SKY.colorScheme))

        listOf(
            CoachTheme.OBSIDIAN,
            CoachTheme.OCEAN,
            CoachTheme.FOREST,
            CoachTheme.GREEN,
            CoachTheme.ORANGE
        ).forEach { theme ->
            assertEquals(theme.colorScheme.surfaceVariant, contentCardContainerColor(theme.colorScheme))
        }

        assertTrue(emptyCustomRoutine("new-custom").isCustom)
        assertFalse(Routine::class.java.declaredFields.any { it.name.contains("color", ignoreCase = true) })
    }

    @Test
    fun `navigation bar consumes neutral colors for light themes and preserves dark themes`() {
        assertEquals(Color(0xFFF1F3F5), navigationBarContainerColor(CoachTheme.LIGHT))
        assertEquals(Color(0xFFF3EFEA), navigationBarContainerColor(CoachTheme.SAND))
        assertEquals(Color(0xFFEEF3F6), navigationBarContainerColor(CoachTheme.ICE))
        assertEquals(Color(0xFFF0F6F0), navigationBarContainerColor(CoachTheme.MINT))
        assertEquals(Color(0xFFEEF6F8), navigationBarContainerColor(CoachTheme.SKY))

        listOf(
            CoachTheme.OBSIDIAN,
            CoachTheme.OCEAN,
            CoachTheme.FOREST,
            CoachTheme.GREEN,
            CoachTheme.ORANGE
        ).forEach { theme ->
            assertEquals(theme.colorScheme.surfaceContainer, navigationBarContainerColor(theme))
        }
    }

    @Test
    fun `new light themes avoid legacy Material lilac containers`() {
        val legacyContainers = setOf(Color(0xFFF3EDF7), Color(0xFFE6E0E9))

        listOf(CoachTheme.MINT, CoachTheme.SKY).forEach { theme ->
            assertFalse(theme.colorScheme.surfaceVariant in legacyContainers)
            assertFalse(contentCardContainerColor(theme.colorScheme) in legacyContainers)
            assertFalse(navigationBarContainerColor(theme) in legacyContainers)
        }
    }

    @Test
    fun `dialogs use neutral light surfaces and preserve dark containers`() {
        appearanceLightThemes.forEach { theme ->
            assertEquals(theme.colorScheme.surface, dialogContainerColor(theme))
            assertFalse(dialogContainerColor(theme) in setOf(Color(0xFFECE6F0), Color(0xFFF3EDF7)))
        }

        appearanceDarkThemes.forEach { theme ->
            assertEquals(theme.colorScheme.surfaceContainerHigh, dialogContainerColor(theme))
        }
    }

    @Test
    fun `persists and restores every new theme`() {
        listOf(CoachTheme.GREEN, CoachTheme.ORANGE, CoachTheme.SAND, CoachTheme.ICE, CoachTheme.MINT, CoachTheme.SKY).forEach { theme ->
            val storage = InMemoryThemePreferenceStorage()
            val repository = ThemePreferenceRepository(storage)

            assertTrue(repository.save(theme))
            assertEquals(theme.id, storage.themeId)
            assertEquals(theme, repository.load())
        }
    }

    @Test
    fun `uses Obsidian as the default theme`() {
        val repository = ThemePreferenceRepository(InMemoryThemePreferenceStorage())

        assertEquals(CoachTheme.OBSIDIAN, repository.load())
    }

    @Test
    fun `persists only the selected theme identifier`() {
        val storage = InMemoryThemePreferenceStorage()
        val repository = ThemePreferenceRepository(storage)

        repository.save(CoachTheme.OCEAN)

        assertEquals("ocean", storage.themeId)
    }

    @Test
    fun `restores a persisted theme`() {
        val repository = ThemePreferenceRepository(
            InMemoryThemePreferenceStorage(themeId = "forest")
        )

        assertEquals(CoachTheme.FOREST, repository.load())
    }

    @Test
    fun `changing theme replaces the previous selection`() {
        val storage = InMemoryThemePreferenceStorage(themeId = "ocean")
        val repository = ThemePreferenceRepository(storage)

        repository.save(CoachTheme.LIGHT)

        assertEquals(CoachTheme.LIGHT, repository.load())
    }

    @Test
    fun `unknown preference remains compatible by falling back to Obsidian`() {
        val repository = ThemePreferenceRepository(
            InMemoryThemePreferenceStorage(themeId = "theme-from-a-newer-version")
        )

        assertEquals(CoachTheme.OBSIDIAN, repository.load())
    }

    private fun assertBasePalette(
        theme: CoachTheme,
        background: Long,
        surface: Long,
        primary: Long,
        onSurface: Long,
        onSurfaceVariant: Long
    ) {
        assertEquals(Color(background), theme.colorScheme.background)
        assertEquals(Color(surface), theme.colorScheme.surface)
        assertEquals(Color(primary), theme.colorScheme.primary)
        assertEquals(Color(onSurface), theme.colorScheme.onSurface)
        assertEquals(Color(onSurfaceVariant), theme.colorScheme.onSurfaceVariant)
    }
}

private class InMemoryThemePreferenceStorage(
    var themeId: String? = null
) : ThemePreferenceStorage {
    override fun readThemeId(): String? = themeId

    override fun writeThemeId(id: String): Boolean {
        themeId = id
        return true
    }
}
