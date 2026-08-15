package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutLoadRepositoryTest {
    @Test
    fun freeTextAndEmptyLoadsRoundTripExactly() {
        val storage = MemoryWorkoutLoadStorage()
        val repository = WorkoutLoadRepository(storage) { 123L }

        assertTrue(repository.saveCompletedSession(listOf(
            SeriesLoadRecord("press", 1, "3 barras"),
            SeriesLoadRecord("press", 2, "80 kg"),
            SeriesLoadRecord("press", 3, "")
        )))

        assertEquals(
            listOf("3 barras", "80 kg", ""),
            repository.loadSessions().single().seriesLoads.map(SeriesLoadRecord::load)
        )
        assertEquals("80 kg", repository.previousLoad("press"))
    }

    @Test
    fun oldInstallWithoutLoadDataHasNoPreviousValue() {
        assertNull(WorkoutLoadRepository(MemoryWorkoutLoadStorage()).previousLoad("exercise"))
    }

    @Test
    fun historyUsesStableExerciseIdInsteadOfDisplayName() {
        val storage = MemoryWorkoutLoadStorage()
        val repository = WorkoutLoadRepository(storage)
        repository.saveCompletedSession(listOf(SeriesLoadRecord("stable-id", 1, "Peso corporal")))

        assertEquals("Peso corporal", repository.previousLoad("stable-id"))
        assertNull(repository.previousLoad("renamed-exercise"))
    }

    @Test
    fun latestCompletedSessionWinsAndMalformedLegacyDataIsSafe() {
        val storage = MemoryWorkoutLoadStorage("old data without load schema")
        val repository = WorkoutLoadRepository(storage)
        assertNull(repository.previousLoad("press"))

        repository.saveCompletedSession(listOf(SeriesLoadRecord("press", 1, "+10 kg")))
        repository.saveCompletedSession(listOf(SeriesLoadRecord("press", 1, "20 kg c/u")))

        assertEquals("20 kg c/u", repository.previousLoad("press"))
    }
}

private class MemoryWorkoutLoadStorage(initial: String? = null) : WorkoutLoadStorage {
    private var value: String? = initial
    override fun read(): String? = value
    override fun write(value: String): Boolean { this.value = value; return true }
}
