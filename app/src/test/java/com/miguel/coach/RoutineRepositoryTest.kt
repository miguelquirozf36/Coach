package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutineRepositoryTest {
    @Test
    fun initialLoadWithoutDataReturnsAndStoresSeedRoutines() {
        val storage = InMemoryStorage()

        val loaded = RoutineRepository(storage).load()

        assertEquals(Routines.all, loaded)
        assertNotNull(storage.values[PRIMARY])
        assertNull(storage.values[BACKUP])
    }

    @Test
    fun savingAndReloadingKeepsEditedRoutines() {
        val storage = InMemoryStorage()
        val repository = RoutineRepository(storage)
        repository.load()
        val edited = editedRoutines("Rutina persistida")

        assertTrue(repository.save(edited))

        assertEquals(edited, RoutineRepository(storage).load())
    }

    @Test
    fun oldRoutineJsonWithoutNotesLoadsWithAnEmptyNote() {
        val storage = InMemoryStorage().apply {
            values[PRIMARY] = """{"routines":[{"id":"old","name":"Anterior","isCustom":false,"restBetweenExercisesSeconds":60,"exercises":[{"id":"old-exercise","name":"Ejercicio anterior","sets":1,"repetitions":1,"concentricSeconds":1,"eccentricSeconds":1,"restSeconds":60}]}]}"""
        }

        val loaded = RoutineRepository(storage).load()

        assertEquals(1, loaded.size)
        assertEquals("", loaded.single().exercises.single().notes)
        assertEquals(600, loaded.single().warmupSeconds)
        assertEquals(180, loaded.single().restBetweenExercisesSeconds)
        assertEquals(2, loaded.single().exercises.single().eccentricSeconds)
        assertEquals(120, loaded.single().exercises.single().restSeconds)
    }

    @Test
    fun warmupSecondsAreSavedAndReloadedAfterRestart() {
        val storage = InMemoryStorage()
        val repository = RoutineRepository(storage)
        val routines = repository.load()
        val updated = routines.mapIndexed { index, routine ->
            if (index == 0) routine.copy(warmupSeconds = 420) else routine
        }

        assertTrue(repository.save(updated))

        assertEquals(420, RoutineRepository(storage).load().first().warmupSeconds)
    }

    @Test
    fun v15DefaultsMigrationRunsOnceAndPreservesOtherFields() {
        val storage = InMemoryStorage()
        val original = Routines.all.first().copy(
            name = "Nombre conservado",
            warmupSeconds = 15,
            restBetweenExercisesSeconds = 25,
            exercises = Routines.all.first().exercises.mapIndexed { index, exercise ->
                exercise.copy(
                    sets = exercise.sets + 1,
                    repetitions = exercise.repetitions + 1,
                    concentricSeconds = 7,
                    eccentricSeconds = 9,
                    restSeconds = 35,
                    notes = if (index == 0) "Nota conservada" else ""
                )
            }
        )
        storage.values[PRIMARY] = RoutineJsonCodec.encode(listOf(original))

        val migrated = RoutineRepository(storage, listOf(original)).load().single()

        assertEquals(600, migrated.warmupSeconds)
        assertEquals(180, migrated.restBetweenExercisesSeconds)
        assertTrue(migrated.exercises.all { it.eccentricSeconds == 2 && it.restSeconds == 120 })
        assertEquals(original.id, migrated.id)
        assertEquals(original.name, migrated.name)
        assertEquals(original.exercises.map { it.id }, migrated.exercises.map { it.id })
        assertEquals(original.exercises.map { it.sets }, migrated.exercises.map { it.sets })
        assertEquals(original.exercises.map { it.repetitions }, migrated.exercises.map { it.repetitions })
        assertEquals(original.exercises.map { it.concentricSeconds }, migrated.exercises.map { it.concentricSeconds })
        assertEquals("Nota conservada", migrated.exercises.first().notes)

        val userEdited = listOf(migrated.copy(
            warmupSeconds = 60,
            exercises = migrated.exercises.map { it.copy(eccentricSeconds = 4) }
        ))
        assertTrue(RoutineRepository(storage).save(userEdited))
        assertEquals(userEdited, RoutineRepository(storage).load())
    }

    @Test
    fun newCustomRoutineStartsEmptyAndCannotBeValidatedUntilExerciseIsAdded() {
        val routine = emptyCustomRoutine("new-empty")

        assertTrue(routine.exercises.isEmpty())
        assertNull(routine.toDraft().validate(true).routine)
        assertTrue(routine.toDraft().validate(true).message.orEmpty().contains("al menos un ejercicio"))
    }

    @Test
    fun routineExerciseNotesAreTrimmedSavedAndReloadedAfterRestart() {
        val storage = InMemoryStorage()
        val repository = RoutineRepository(storage)
        val routines = repository.load()
        val target = routines.first()
        val edited = target.toDraft().copy(
            exercises = target.toDraft().exercises.mapIndexed { index, exercise ->
                if (index == 0) exercise.copy(notes = "  Usar banda azul.\nMantener tensión.  ") else exercise
            }
        ).validate(isCustom = false).routine!!
        val updated = routines.map { if (it.id == target.id) edited else it }

        assertTrue(repository.save(updated))

        val reloaded = RoutineRepository(storage).load().first { it.id == target.id }
        assertEquals("Usar banda azul.\nMantener tensión.", reloaded.exercises.first().notes)
    }

    @Test
    fun emptyExerciseNotesAreValidAndThreeHundredCharactersIsTheLimit() {
        val original = Routines.all.first().toDraft()
        val empty = original.copy(
            exercises = original.exercises.mapIndexed { index, exercise ->
                if (index == 0) exercise.copy(notes = "") else exercise
            }
        )
        val atLimit = empty.copy(
            exercises = empty.exercises.mapIndexed { index, exercise ->
                if (index == 0) exercise.copy(notes = "n".repeat(300)) else exercise
            }
        )
        val overLimit = atLimit.copy(
            exercises = atLimit.exercises.mapIndexed { index, exercise ->
                if (index == 0) exercise.copy(notes = "n".repeat(301)) else exercise
            }
        )

        assertNotNull(empty.validate(isCustom = false).routine)
        assertEquals(300, atLimit.validate(isCustom = false).routine!!.exercises.first().notes.length)
        assertNull(overLimit.validate(isCustom = false).routine)
        assertTrue(overLimit.validate(isCustom = false).message.orEmpty().contains("300"))
    }

    @Test
    fun cancelingANoteEditLeavesTheStoredRoutineUntouched() {
        val storage = InMemoryStorage()
        val repository = RoutineRepository(storage)
        val original = repository.load()
        val discarded = original.first().toDraft().copy(
            exercises = original.first().toDraft().exercises.mapIndexed { index, exercise ->
                if (index == 0) exercise.copy(notes = "No guardar") else exercise
            }
        )

        assertEquals("No guardar", discarded.exercises.first().notes)
        assertEquals("", RoutineRepository(storage).load().first().exercises.first().notes)
    }

    @Test
    fun savingKeepsThePreviousValidDocumentAsBackup() {
        val storage = InMemoryStorage()
        val repository = RoutineRepository(storage)
        repository.load()
        val previousPrimary = storage.values[PRIMARY]

        assertTrue(repository.save(editedRoutines("Nueva versión")))

        assertEquals(previousPrimary, storage.values[BACKUP])
        assertTrue(storage.values[PRIMARY] != previousPrimary)
    }

    @Test
    fun corruptedPrimaryRecoversFromTheBackupAndRepairsIt() {
        val storage = InMemoryStorage()
        val repository = RoutineRepository(storage)
        val original = repository.load()
        val validBackup = storage.values[PRIMARY]!!
        storage.values[PRIMARY] = "{documento corrupto"
        storage.values[BACKUP] = validBackup

        val loaded = RoutineRepository(storage).load()

        assertEquals(original, loaded)
        assertEquals(validBackup, storage.values[PRIMARY])
    }

    @Test
    fun twoCorruptedDocumentsUseSeedsWithoutOverwritingThem() {
        val storage = InMemoryStorage().apply {
            values[PRIMARY] = "{corrupto"
            values[BACKUP] = "[corrupto"
        }

        val loaded = RoutineRepository(storage).load()

        assertEquals(Routines.all, loaded)
        assertEquals("{corrupto", storage.values[PRIMARY])
        assertEquals("[corrupto", storage.values[BACKUP])
    }

    @Test
    fun invalidRoutinesAreNotSaved() {
        val storage = InMemoryStorage()
        val repository = RoutineRepository(storage)
        repository.load()
        val primaryBeforeSave = storage.values[PRIMARY]
        val invalid = Routines.all.mapIndexed { index, routine ->
            if (index == 0) routine.copy(exercises = emptyList()) else routine
        }

        assertFalse(repository.save(invalid))
        assertEquals(primaryBeforeSave, storage.values[PRIMARY])
    }

    @Test
    fun duplicateIdentifiersAreNotSaved() {
        val storage = InMemoryStorage()
        val repository = RoutineRepository(storage)
        repository.load()
        val duplicate = Routines.all.mapIndexed { index, routine ->
            if (index == 1) routine.copy(id = Routines.all.first().id) else routine
        }

        assertFalse(repository.save(duplicate))
    }

    @Test
    fun cancelingAnEditWithoutSavingDoesNotPersistIt() {
        val storage = InMemoryStorage()
        val repository = RoutineRepository(storage)
        val original = repository.load()
        val discardedDraft = original.first().toDraft().copy(name = "Cambio descartado")

        assertEquals("Cambio descartado", discardedDraft.name)
        assertEquals(original, RoutineRepository(storage).load())
    }

    @Test
    fun customRoutineCanBeCreatedEditedAndReloadedSeparatelyFromSeeds() {
        val storage = InMemoryStorage()
        val repository = RoutineRepository(storage)
        val custom = emptyCustomRoutine("custom-1").copy(
            name = "Personalizada",
            exercises = listOf(emptyCustomExercise("custom-1-exercise"))
        )
        val all = repository.load() + custom

        assertTrue(repository.save(all))
        val reloaded = RoutineRepository(storage).load()

        assertEquals(6, reloaded.count { !it.isCustom })
        assertEquals(listOf(custom), reloaded.filter { it.isCustom })
    }

    @Test
    fun customExerciseOrderIsSavedAndReloaded() {
        val storage = InMemoryStorage()
        val repository = RoutineRepository(storage)
        val custom = emptyCustomRoutine("custom-order").copy(
            exercises = listOf(
                emptyCustomExercise("third").copy(name = "Tercero"),
                emptyCustomExercise("first").copy(name = "Primero"),
                emptyCustomExercise("second").copy(name = "Segundo")
            )
        )

        assertTrue(repository.save(repository.load() + custom))

        assertEquals(listOf("Tercero", "Primero", "Segundo"),
            RoutineRepository(storage).load().first { it.id == custom.id }.exercises.map(Exercise::name))
    }

    @Test
    fun deletingCustomRoutinePersistsWhileCancelingDeletionDoesNotChangeData() {
        val storage = InMemoryStorage()
        val repository = RoutineRepository(storage)
        val custom = emptyCustomRoutine("custom-delete").copy(
            exercises = listOf(emptyCustomExercise("custom-delete-exercise"))
        )
        val saved = repository.load() + custom
        assertTrue(repository.save(saved))

        assertEquals(saved, RoutineRepository(storage).load())
        assertTrue(repository.save(saved.filterNot { it.id == custom.id }))
        assertTrue(RoutineRepository(storage).load().none { it.id == custom.id })
    }

    @Test
    fun customDraftCanAddAndRemoveExercisesBeforeSaving() {
        val routine = emptyCustomRoutine("custom-draft")
        val draftWithAddedExercise = routine.toDraft().copy(
            exercises = routine.toDraft().exercises + emptyCustomExercise("new").toDraft()
        )
        val savedRoutine = draftWithAddedExercise.validate(true).routine!!
        val draftAfterRemoval = savedRoutine.toDraft().copy(exercises = savedRoutine.toDraft().exercises.dropLast(1))

        assertEquals(1, savedRoutine.exercises.size)
        assertNull(draftAfterRemoval.validate(true).routine)
    }

    @Test
    fun myRoutineCanAddExercise() {
        val original = Routines.all.first().toDraft()
        val added = emptyCustomExercise("added-to-seed").toDraft()

        val updated = original.addExercise(added)

        assertEquals(original.exercises.size + 1, updated.exercises.size)
        assertEquals(added, updated.exercises.last())
        assertNotNull(updated.validate(isCustom = false).routine)
    }

    @Test
    fun myRoutineCanRemoveExercise() {
        val original = Routines.all.first().toDraft()
        val removedId = original.exercises.first().id

        val updated = original.removeExercise(removedId)

        assertEquals(original.exercises.size - 1, updated.exercises.size)
        assertTrue(updated.exercises.none { it.id == removedId })
    }

    @Test
    fun expandingAnotherExerciseCollapsesThePreviousAndTappingItAgainCollapsesIt() {
        val first = "first"
        val second = "second"

        assertEquals(first, toggledExpandedExercise(null, first))
        assertEquals(second, toggledExpandedExercise(first, second))
        assertNull(toggledExpandedExercise(second, second))
    }

    @Test
    fun changedDraftRequiresSaveDialogWhileUntouchedDraftDoesNot() {
        val original = Routines.all.first().toDraft()

        assertFalse(original.hasChangesFrom(original))
        assertTrue(original.copy(name = "Editada").hasChangesFrom(original))
    }

    @Test
    fun discardChoiceExitsWithoutSavingAndCancelStaysInEditor() {
        assertEquals(EditorExitResult.STAY, editorExitResult(EditorExitChoice.CANCEL))
        assertEquals(EditorExitResult.EXIT_WITHOUT_SAVING, editorExitResult(EditorExitChoice.DISCARD))
    }

    @Test
    fun saveChoiceFromBackArrowSavesAndExits() {
        assertEquals(EditorExitResult.SAVE_AND_EXIT, editorExitResult(EditorExitChoice.SAVE))
    }

    @Test
    fun movingExercisesPreservesTheirOrderAndBoundaries() {
        val original = Routines.all.first().toDraft()
        val firstId = original.exercises.first().id
        val secondId = original.exercises[1].id

        val moved = original.moveExercise(firstId, 1)

        assertEquals(secondId, moved.exercises[0].id)
        assertEquals(firstId, moved.exercises[1].id)
        assertEquals(original, original.moveExercise(firstId, -1))
    }

    @Test
    fun addedRemovedAndReorderedSeedExercisesPersistAfterRestart() {
        val storage = InMemoryStorage()
        val repository = RoutineRepository(storage)
        val initial = repository.load()
        val target = initial.first()
        val added = emptyCustomExercise("seed-new-exercise").copy(name = "Persistente")
        val editedTarget = target.toDraft()
            .addExercise(added.toDraft())
            .removeExercise(target.exercises.first().id)
            .moveExercise(added.id, -1)
            .validate(isCustom = false).routine!!
        val updated = initial.map { if (it.id == target.id) editedTarget else it }

        assertTrue(repository.save(updated))

        val restarted = RoutineRepository(storage).load().first { it.id == target.id }
        assertEquals(editedTarget.exercises.map(Exercise::id), restarted.exercises.map(Exercise::id))
        assertEquals("Persistente", restarted.exercises[restarted.exercises.lastIndex - 1].name)
    }

    private fun editedRoutines(name: String): List<Routine> = Routines.all.mapIndexed { index, routine ->
        if (index == 0) routine.copy(name = name, restBetweenExercisesSeconds = 90) else routine
    }

    private class InMemoryStorage : RoutineStorage {
        val values = mutableMapOf<String, String>()

        override fun read(key: String): String? = values[key]

        override fun write(key: String, value: String): Boolean {
            values[key] = value
            return true
        }
    }

    private companion object {
        const val PRIMARY = "routines_json"
        const val BACKUP = "routines_backup_json"
    }
}
