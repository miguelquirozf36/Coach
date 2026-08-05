package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemePreferenceRepositoryTest {

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
