package com.miguel.coach

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemePreferenceRepositoryTest {

    @Test
    fun `exposes exactly the eight themes in the approved order with unique ids`() {
        assertEquals(
            listOf("Obsidian", "Ocean", "Forest", "Light", "Green", "Orange", "Sand", "Ice"),
            CoachTheme.entries.map(CoachTheme::displayName)
        )
        assertEquals(8, CoachTheme.entries.size)
        assertEquals(8, CoachTheme.entries.map(CoachTheme::id).toSet().size)
    }

    @Test
    fun `keeps every original theme id compatible`() {
        listOf(
            "obsidian" to CoachTheme.OBSIDIAN,
            "ocean" to CoachTheme.OCEAN,
            "forest" to CoachTheme.FOREST,
            "light" to CoachTheme.LIGHT
        ).forEach { (id, expected) -> assertEquals(expected, CoachTheme.fromId(id)) }
    }

    @Test
    fun `new themes use their specified base palettes`() {
        assertBasePalette(CoachTheme.GREEN, 0xFF0D1014, 0xFF1C222B, 0xFF86DF86, 0xFFF5F5F5, 0xFFAEB4BE)
        assertBasePalette(CoachTheme.ORANGE, 0xFF0D1014, 0xFF1C222B, 0xFFEA571C, 0xFFF5F5F5, 0xFFAEB4BE)
        assertBasePalette(CoachTheme.SAND, 0xFFF7F4EF, 0xFFFFFFFF, 0xFFD56A32, 0xFF24211F, 0xFF68645F)
        assertBasePalette(CoachTheme.ICE, 0xFFF3F7FA, 0xFFFFFFFF, 0xFF4F7898, 0xFF20262C, 0xFF65717C)
        assertEquals(Color(0xFFDED8D0), CoachTheme.SAND.colorScheme.outline)
        assertEquals(Color(0xFFD9E1E7), CoachTheme.ICE.colorScheme.outline)
    }

    @Test
    fun `persists and restores every new theme`() {
        listOf(CoachTheme.GREEN, CoachTheme.ORANGE, CoachTheme.SAND, CoachTheme.ICE).forEach { theme ->
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
