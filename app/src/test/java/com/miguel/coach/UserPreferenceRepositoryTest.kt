package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UserPreferenceRepositoryTest {
    @Test
    fun beepVolumeDefaultsToLevelFiveAndNormalizesStoredValues() {
        val storage = InMemoryUserStorage()
        val repository = UserPreferenceRepository(storage)
        assertEquals(5, repository.loadBeepVolumeLevel())
        assertEquals(100, beepToneVolume(repository.loadBeepVolumeLevel()))
        storage.beepVolumeLevel = 99
        assertEquals(5, repository.loadBeepVolumeLevel())
        storage.beepVolumeLevel = -4
        assertEquals(1, repository.loadBeepVolumeLevel())
    }

    @Test
    fun savedBeepVolumeIsRestoredByANewRepository() {
        val storage = InMemoryUserStorage()
        assertTrue(UserPreferenceRepository(storage).saveBeepVolumeLevel(4))
        assertEquals(4, UserPreferenceRepository(storage).loadBeepVolumeLevel())
        assertTrue("workout_beep_volume_level" in storage.writtenKeys)
    }

    @Test
    fun trainerVoiceVolumeDefaultsToLevelFiveAndPersistsSelection() {
        val storage = InMemoryUserStorage()
        val defaultLevel = UserPreferenceRepository(storage).loadTrainerVoiceVolumeLevel()
        assertEquals(5, defaultLevel)
        assertEquals(1.0f, relativeAudioVolume(defaultLevel))
        assertTrue(UserPreferenceRepository(storage).saveTrainerVoiceVolumeLevel(3))
        assertEquals(3, UserPreferenceRepository(storage).loadTrainerVoiceVolumeLevel())
        assertTrue("trainer_voice_volume_level" in storage.writtenKeys)
    }

    @Test
    fun trainerVoiceVolumeNormalizesInvalidStoredValues() {
        val storage = InMemoryUserStorage()
        storage.trainerVoiceVolumeLevel = -1
        assertEquals(1, UserPreferenceRepository(storage).loadTrainerVoiceVolumeLevel())
        storage.trainerVoiceVolumeLevel = 9
        assertEquals(5, UserPreferenceRepository(storage).loadTrainerVoiceVolumeLevel())
    }

    @Test
    fun trainerVoiceDefaultsToDeviceDefaultAndPersistsSelection() {
        val storage = InMemoryUserStorage()
        assertEquals(DEFAULT_TRAINER_VOICE_ID, UserPreferenceRepository(storage).loadTrainerVoiceId())
        assertTrue(UserPreferenceRepository(storage).saveTrainerVoiceId("engine-voice-id"))
        assertEquals("engine-voice-id", UserPreferenceRepository(storage).loadTrainerVoiceId())
        assertTrue("trainer_voice_id" in storage.writtenKeys)
    }
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
    fun exportBackupUsesTheCompactDescription() {
        assertEquals("Guardar los datos en un archivo JSON.", EXPORT_BACKUP_DESCRIPTION)
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

    @Test
    fun newInstallationRequiresOnboardingUntilTourCompletes() {
        val storage = InMemoryUserStorage()
        val repository = UserPreferenceRepository(storage)

        assertTrue(repository.initializeOnboarding(existingInstallation = false))
        assertTrue(!repository.isTourCompleted())
        assertTrue(repository.completeTour())
        assertTrue(repository.isTourCompleted())
        assertTrue(!UserPreferenceRepository(storage).initializeOnboarding(existingInstallation = false))
    }

    @Test
    fun existingInstallationIsMigratedWithoutAutomaticOnboarding() {
        val storage = InMemoryUserStorage()
        val repository = UserPreferenceRepository(storage)

        assertTrue(!repository.initializeOnboarding(existingInstallation = true))
        assertTrue(repository.isTourCompleted())
    }

    @Test
    fun replayingTourDoesNotChangeName() {
        val storage = InMemoryUserStorage()
        val repository = UserPreferenceRepository(storage)
        repository.saveUserName("  Ana  ")
        repository.initializeOnboarding(existingInstallation = false)

        assertEquals("Ana", repository.loadUserName())
        assertTrue(repository.completeTour())
        assertEquals("Ana", repository.loadUserName())
    }
}

private class InMemoryUserStorage(var userName: String? = null) : UserPreferenceStorage {
    val writtenKeys = mutableSetOf<String>()
    var tourCompleted: Boolean? = null
    var beepVolumeLevel: Int? = null
    var trainerVoiceVolumeLevel: Int? = null
    var trainerVoiceId: String? = null
    override fun readUserName(): String? = userName
    override fun writeUserName(name: String): Boolean {
        userName = name
        writtenKeys += "user_name"
        return true
    }
    override fun readTourCompleted(): Boolean? = tourCompleted
    override fun writeTourCompleted(completed: Boolean): Boolean {
        tourCompleted = completed
        writtenKeys += "onboarding_tour_completed_v17"
        return true
    }
    override fun readBeepVolumeLevel(): Int? = beepVolumeLevel
    override fun writeBeepVolumeLevel(level: Int): Boolean {
        beepVolumeLevel = level
        writtenKeys += "workout_beep_volume_level"
        return true
    }
    override fun readTrainerVoiceVolumeLevel(): Int? = trainerVoiceVolumeLevel
    override fun writeTrainerVoiceVolumeLevel(level: Int): Boolean {
        trainerVoiceVolumeLevel = level
        writtenKeys += "trainer_voice_volume_level"
        return true
    }
    override fun readTrainerVoiceId(): String? = trainerVoiceId
    override fun writeTrainerVoiceId(voiceId: String): Boolean {
        trainerVoiceId = voiceId
        writtenKeys += "trainer_voice_id"
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
