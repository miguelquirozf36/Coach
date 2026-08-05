package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPreferenceRepositoryTest {
    @Test
    fun emptyNameIsTheDefault() {
        assertEquals("", UserPreferenceRepository(InMemoryUserStorage()).loadUserName())
    }

    @Test
    fun savedNameIsRestoredByANewRepository() {
        val storage = InMemoryUserStorage()
        UserPreferenceRepository(storage).saveUserName("Miguel")

        assertEquals("Miguel", UserPreferenceRepository(storage).loadUserName())
    }

    @Test
    fun outerSpacesAreTrimmedBeforeSaving() {
        val repository = UserPreferenceRepository(InMemoryUserStorage())

        assertEquals("Ana María", repository.saveUserName("  Ana María  ").value)
        assertEquals("Ana María", repository.loadUserName())
    }

    @Test
    fun lowerUpperAndMixedCaseNamesAreConvertedToTitleCase() {
        assertEquals("Miguel", validateUserName("miguel").value)
        assertEquals("Miguel", validateUserName("MIGUEL").value)
        assertEquals("Miguel", validateUserName("mIgUeL").value)
    }

    @Test
    fun everyNameWordIsConvertedToTitleCase() {
        assertEquals("Miguel Quiroz", validateUserName("miguel quiroz").value)
        assertEquals("Miguel Quiroz", validateUserName("MIGUEL QUIROZ").value)
    }

    @Test
    fun multipleInternalSpacesAreCollapsedBeforeSaving() {
        val repository = UserPreferenceRepository(InMemoryUserStorage())

        assertEquals("Miguel Quiroz", repository.saveUserName("miguel    quiroz").value)
        assertEquals("Miguel Quiroz", repository.loadUserName())
    }

    @Test
    fun spacesOnlyAreRejectedAndNotStored() {
        val storage = InMemoryUserStorage("Anterior")
        val result = UserPreferenceRepository(storage).saveUserName("   ")

        assertNull(result.value)
        assertTrue(result.message.orEmpty().contains("espacios"))
        assertEquals("Anterior", storage.userName)
    }

    @Test
    fun fortyCharactersAreAccepted() {
        assertEquals("A" + "a".repeat(39), validateUserName("a".repeat(40)).value)
    }

    @Test
    fun moreThanFortyCharactersAreRejected() {
        assertNull(validateUserName("a".repeat(41)).value)
    }

    @Test
    fun cancelingEditKeepsThePreviousName() {
        val storage = InMemoryUserStorage("Miguel")
        val repository = UserPreferenceRepository(storage)
        val unsavedInput = "Otro nombre"

        assertEquals("Otro nombre", unsavedInput)
        assertEquals("Miguel", repository.loadUserName())
    }

    @Test
    fun greetingIncludesSavedName() {
        assertEquals(listOf("Hola, Miguel", "¿Qué quieres entrenar hoy?"), homeGreeting("Miguel"))
    }

    @Test
    fun greetingWithoutNameOnlyShowsTheQuestion() {
        assertEquals(listOf("¿Qué quieres entrenar hoy?"), homeGreeting(""))
    }

    @Test
    fun settingsOpensExistingAppearanceScreen() {
        assertEquals(SettingsDestination.APPEARANCE, openAppearanceFromSettings())
    }

    @Test
    fun backFromAppearanceReturnsToSettings() {
        assertEquals(SettingsDestination.ROOT, backFromSettingsAppearance())
    }

    @Test
    fun settingsShowsCurrentThemeDisplayName() {
        assertEquals("Ocean", CoachTheme.OCEAN.displayName)
    }

    @Test
    fun userPreferencesRemainSeparateFromThemeAndRoutineStorage() {
        val userStorage = InMemoryUserStorage()
        val themeStorage = SeparateThemeStorage()
        UserPreferenceRepository(userStorage).saveUserName("Miguel")
        ThemePreferenceRepository(themeStorage).save(CoachTheme.FOREST)

        assertEquals("Miguel", userStorage.userName)
        assertEquals("forest", themeStorage.themeId)
        assertEquals(setOf("user_name"), userStorage.writtenKeys)
        assertEquals(setOf("selected_theme"), themeStorage.writtenKeys)
    }
}

private class InMemoryUserStorage(var userName: String? = null) : UserPreferenceStorage {
    val writtenKeys = mutableSetOf<String>()
    override fun readUserName(): String? = userName
    override fun writeUserName(name: String): Boolean {
        userName = name
        writtenKeys += "user_name"
        return true
    }
}

private class SeparateThemeStorage : ThemePreferenceStorage {
    var themeId: String? = null
    val writtenKeys = mutableSetOf<String>()
    override fun readThemeId(): String? = themeId
    override fun writeThemeId(id: String): Boolean {
        themeId = id
        writtenKeys += "selected_theme"
        return true
    }
}
