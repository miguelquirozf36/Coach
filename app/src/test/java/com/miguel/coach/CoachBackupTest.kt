package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachBackupTest {
    @Test
    fun exportContainsEveryConfigurableComponent() {
        val fixture = Fixture()
        fixture.seedConfiguredData()

        val document = fixture.manager.createDocument()
        val json = CoachBackupCodec.encode(document)

        assertTrue(json.contains("\"backupVersion\": 1"))
        assertTrue(json.contains("\"exportedAt\""))
        assertTrue(json.contains("\"routines\""))
        assertTrue(json.contains("\"customExercises\""))
        assertTrue(json.contains("\"userPreferences\""))
        assertTrue(json.contains("\"themePreference\""))
        assertEquals("2026-08-05T12:00:00Z", document.exportedAt)
    }

    @Test
    fun validJsonRoundTripsWithoutLosingNotesOrWarmup() {
        val fixture = Fixture()
        val document = fixture.backupDocument()

        val decoded = CoachBackupCodec.decode(CoachBackupCodec.encode(document))

        assertTrue(decoded.success)
        assertEquals(document, decoded.document)
        assertEquals("Nota restaurada", decoded.document!!.routines.first().exercises.first().notes)
        assertEquals(420, decoded.document!!.routines.first().warmupSeconds)
    }

    @Test
    fun corruptJsonIsRejected() {
        assertFalse(CoachBackupCodec.decode("{documento roto").success)
    }

    @Test
    fun futureVersionIsRejectedClearly() {
        val fixture = Fixture()
        val json = CoachBackupCodec.encode(fixture.backupDocument()).replace(
            "\"backupVersion\": 1",
            "\"backupVersion\": 2"
        )

        val result = CoachBackupCodec.decode(json)

        assertFalse(result.success)
        assertTrue(result.message.contains("versión"))
    }

    @Test
    fun missingRequiredFieldIsRejected() {
        val fixture = Fixture()
        val json = CoachBackupCodec.encode(fixture.backupDocument())
            .replace("\"exportedAt\"", "\"removedExportedAt\"")

        assertFalse(CoachBackupCodec.decode(json).success)
    }

    @Test
    fun validationHappensBeforeAnyWrite() {
        val fixture = Fixture()
        val invalid = fixture.backupDocument().copy(routines = emptyList())

        val result = fixture.manager.restore(invalid, workoutActive = false)

        assertFalse(result.success)
        assertEquals(0, fixture.routineStorage.writeCount)
        assertEquals(0, fixture.userStorage.writeCount)
        assertEquals(0, fixture.themeStorage.writeCount)
    }

    @Test
    fun cancellationDoesNotChangeData() {
        val fixture = Fixture()
        val before = fixture.manager.createDocument()

        val result = fixture.manager.restore(null, workoutActive = false)

        assertFalse(result.success)
        assertEquals(before.copy(exportedAt = "ignored"), fixture.manager.createDocument().copy(exportedAt = "ignored"))
    }

    @Test
    fun validImportRestoresRoutinesCustomExercisesNameAndTheme() {
        val fixture = Fixture()
        val document = fixture.backupDocument()

        val result = fixture.manager.restore(document, workoutActive = false)

        assertTrue(result.success)
        assertEquals(document.routines, fixture.routineRepository.load())
        assertEquals(document.customExercises, fixture.routineRepository.loadCustomExercises())
        assertEquals("Miguel Quiroz", fixture.userRepository.loadUserName())
        assertEquals(CoachTheme.FOREST, fixture.themeRepository.load())
    }

    @Test
    fun rollbackRestoresEveryPreviousComponentWhenLastWriteFails() {
        val fixture = Fixture()
        fixture.seedConfiguredData()
        val before = fixture.manager.createDocument()
        fixture.themeStorage.failNextWrite = true

        val result = fixture.manager.restore(fixture.backupDocument(), workoutActive = false)

        assertFalse(result.success)
        val after = fixture.manager.createDocument()
        assertEquals(before.routines, after.routines)
        assertEquals(before.customExercises, after.customExercises)
        assertEquals(before.userName, after.userName)
        assertEquals(before.themeId, after.themeId)
    }

    @Test
    fun importIsBlockedDuringActiveWorkoutBeforeWriting() {
        val fixture = Fixture()

        val result = fixture.manager.restore(fixture.backupDocument(), workoutActive = true)

        assertFalse(result.success)
        assertTrue(result.message.contains("entrenamiento activo"))
        assertEquals(0, fixture.routineStorage.writeCount)
    }

    @Test
    fun invalidCustomExerciseRejectsWholeDocument() {
        val fixture = Fixture()
        val invalid = fixture.backupDocument().copy(
            customExercises = listOf(customExercise.copy(category = "CATEGORÍA INEXISTENTE"))
        )

        assertFalse(fixture.manager.restore(invalid, false).success)
        assertEquals(0, fixture.routineStorage.writeCount)
    }

    @Test
    fun unknownThemeRejectsWholeDocument() {
        val fixture = Fixture()
        val invalid = fixture.backupDocument().copy(themeId = "future-theme")

        assertFalse(fixture.manager.restore(invalid, false).success)
        assertEquals(0, fixture.themeStorage.writeCount)
    }

    private class Fixture {
        val routineStorage = RecordingRoutineStorage()
        val userStorage = RecordingUserStorage()
        val themeStorage = RecordingThemeStorage()
        val routineRepository = RoutineRepository(routineStorage)
        val userRepository = UserPreferenceRepository(userStorage)
        val themeRepository = ThemePreferenceRepository(themeStorage)
        val manager = CoachBackupManager(
            routineRepository,
            userRepository,
            themeRepository,
            now = { "2026-08-05T12:00:00Z" }
        )

        fun seedConfiguredData() {
            routineRepository.save(Routines.all)
            routineRepository.saveCustomExercises(listOf(customExercise))
            userRepository.saveUserName("Nombre Anterior")
            themeRepository.save(CoachTheme.OCEAN)
            routineStorage.writeCount = 0
            userStorage.writeCount = 0
            themeStorage.writeCount = 0
        }

        fun backupDocument(): CoachBackupDocument {
            val editedRoutines = Routines.all.mapIndexed { index, routine ->
                if (index != 0) routine else routine.copy(
                    warmupSeconds = 420,
                    exercises = routine.exercises.mapIndexed { exerciseIndex, exercise ->
                        if (exerciseIndex == 0) exercise.copy(notes = "Nota restaurada") else exercise
                    }
                )
            } + emptyCustomRoutine("backup-custom-routine").copy(name = "Rutina importada")
            return CoachBackupDocument(
                backupVersion = 1,
                exportedAt = "2026-08-05T12:00:00Z",
                routines = editedRoutines,
                customExercises = listOf(customExercise),
                userName = "miguel   quiroz",
                themeId = CoachTheme.FOREST.id
            )
        }
    }

    private class RecordingRoutineStorage : RoutineStorage {
        val values = mutableMapOf<String, String>()
        var writeCount = 0
        override fun read(key: String): String? = values[key]
        override fun write(key: String, value: String): Boolean {
            writeCount += 1
            values[key] = value
            return true
        }
    }

    private class RecordingUserStorage : UserPreferenceStorage {
        var value: String? = null
        var writeCount = 0
        override fun readUserName(): String? = value
        override fun writeUserName(name: String): Boolean {
            writeCount += 1
            value = name
            return true
        }
    }

    private class RecordingThemeStorage : ThemePreferenceStorage {
        var value: String? = null
        var writeCount = 0
        var failNextWrite = false
        override fun readThemeId(): String? = value
        override fun writeThemeId(id: String): Boolean {
            writeCount += 1
            if (failNextWrite) {
                failNextWrite = false
                return false
            }
            value = id
            return true
        }
    }

    private companion object {
        val customExercise = ExerciseDefinition(
            id = "custom-exercise-backup",
            name = "Ejercicio de respaldo único",
            category = "PECHO",
            notes = "Nota personalizada"
        )
    }
}
