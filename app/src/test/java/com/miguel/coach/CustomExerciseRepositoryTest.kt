package com.miguel.coach

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomExerciseRepositoryTest {
    @After
    fun resetLibrary() {
        ExerciseLibrary.replaceCustom(emptyList())
    }

    @Test
    fun createsACustomExercise() {
        val result = CustomExerciseRepository(InMemoryStorage()).create("Press personalizado", "PECHO")

        assertTrue(result.success)
        assertEquals("Press personalizado", result.exercise?.name)
        assertTrue(result.exercise?.id?.startsWith("custom-exercise-") == true)
    }

    @Test
    fun createsAndReloadsAnOptionalMultilineNote() {
        val storage = InMemoryStorage()
        val repository = CustomExerciseRepository(storage)

        val created = repository.create(
            "Ejercicio con nota",
            "PECHO",
            "  Primera línea.\nSegunda línea.  "
        ).exercise!!

        assertEquals("Primera línea.\nSegunda línea.", created.notes)
        assertEquals(created, CustomExerciseRepository(storage).load().single())
    }

    @Test
    fun oldCustomExerciseFormatWithoutNotesLoadsAnEmptyNote() {
        val storage = InMemoryStorage()
        storage.write("custom_exercises", "custom-exercise-old\tAnterior\tPECHO")

        val loaded = CustomExerciseRepository(storage).load()

        assertEquals(1, loaded.size)
        assertEquals("", loaded.single().notes)
    }

    @Test
    fun savesAValidCustomExerciseList() {
        val repository = CustomExerciseRepository(InMemoryStorage())
        val exercise = customDefinition("saved", "Ejercicio guardado", "PIERNAS")

        assertTrue(repository.save(listOf(exercise)))
        assertEquals(listOf(exercise), repository.load())
    }

    @Test
    fun reloadsExercisesFromTheSameStorage() {
        val storage = InMemoryStorage()
        val created = CustomExerciseRepository(storage).create("Remo personal", "ESPALDA").exercise!!

        val reloaded = CustomExerciseRepository(storage).load()

        assertEquals(listOf(created), reloaded)
    }

    @Test
    fun librarySearchIncludesOfficialAndCustomExercisesAlphabetically() {
        val repository = CustomExerciseRepository(InMemoryStorage())
        repository.create("Remo artesanal", "ESPALDA")
        ExerciseLibrary.replaceCustom(repository.load())

        val results = ExerciseLibrary.search("remo")

        assertTrue(results.any { it.name == "Remo artesanal" })
        assertTrue(results.any { ExerciseLibrary.isOfficial(it.id) })
        assertEquals(
            results.map { normalizeExerciseText(it.name) }.sorted(),
            results.map { normalizeExerciseText(it.name) }
        )
    }

    @Test
    fun editsOnlyTheRequestedCustomExercise() {
        val repository = CustomExerciseRepository(InMemoryStorage())
        val created = repository.create("Nombre inicial", "PECHO").exercise!!

        val result = repository.edit(created.id, "Nombre editado", "HOMBROS")

        assertTrue(result.success)
        assertEquals(created.id, result.exercise?.id)
        assertEquals("Nombre editado", result.exercise?.name)
        assertEquals("HOMBROS", result.exercise?.category)
    }

    @Test
    fun editsAndPersistsACustomExerciseNote() {
        val storage = InMemoryStorage()
        val repository = CustomExerciseRepository(storage)
        val created = repository.create("Nota editable", "PIERNAS").exercise!!

        val result = repository.edit(created.id, created.name, created.category, "Última serie al fallo.")

        assertTrue(result.success)
        assertEquals("Última serie al fallo.", result.exercise?.notes)
        assertEquals("Última serie al fallo.", CustomExerciseRepository(storage).load().single().notes)
    }

    @Test
    fun customExerciseNotesRejectMoreThanThreeHundredCharacters() {
        val repository = CustomExerciseRepository(InMemoryStorage())

        assertTrue(repository.create("Límite válido", "PECHO", "n".repeat(300)).success)
        val invalid = repository.create("Límite inválido", "PECHO", "n".repeat(301))

        assertFalse(invalid.success)
        assertTrue(invalid.message.orEmpty().contains("300"))
    }

    @Test
    fun deletesAnUnusedCustomExercise() {
        val repository = CustomExerciseRepository(InMemoryStorage())
        val created = repository.create("Ejercicio eliminable", "ANTEBRAZO").exercise!!

        val result = repository.delete(created.id, Routines.all)

        assertTrue(result.success)
        assertTrue(repository.load().isEmpty())
    }

    @Test
    fun rejectsDuplicateNamesIgnoringCaseAndAccents() {
        val repository = CustomExerciseRepository(InMemoryStorage())
        assertTrue(repository.create("Rotación especial", "HOMBROS").success)

        val customDuplicate = repository.create("ROTACION ESPECIAL", "PIERNAS")
        val officialDuplicate = repository.create("press MILITAR con MANCUERNAS", "HOMBROS")

        assertFalse(customDuplicate.success)
        assertEquals("Ya existe un ejercicio con ese nombre.", customDuplicate.message)
        assertFalse(officialDuplicate.success)
    }

    @Test
    fun rejectsEmptyNamesAndUnknownCategoriesWithClearMessages() {
        val repository = CustomExerciseRepository(InMemoryStorage())

        val emptyName = repository.create("   ", "PECHO")
        val invalidCategory = repository.create("Ejercicio válido", "OTRA")

        assertFalse(emptyName.success)
        assertEquals("El nombre del ejercicio es obligatorio.", emptyName.message)
        assertFalse(invalidCategory.success)
        assertEquals("Selecciona una categoría válida.", invalidCategory.message)
    }

    @Test
    fun multipleChangesPersistAcrossRepositoryRestarts() {
        val storage = InMemoryStorage()
        val firstRepository = CustomExerciseRepository(storage)
        val first = firstRepository.create("Uno personal", "PECHO").exercise!!
        firstRepository.create("Dos personal", "TRÍCEPS")
        assertTrue(firstRepository.edit(first.id, "Uno editado", "BÍCEPS").success)

        val restarted = CustomExerciseRepository(storage).load()

        assertEquals(2, restarted.size)
        assertNotNull(restarted.firstOrNull { it.name == "Uno editado" })
        assertNotNull(restarted.firstOrNull { it.name == "Dos personal" })
    }

    @Test
    fun detectsCustomExerciseUseInsideRoutinesIgnoringAccentsAndCase() {
        val repository = CustomExerciseRepository(InMemoryStorage())
        val created = repository.create("Rotación propia", "HOMBROS").exercise!!
        val routineUsingIt = routineUsing("ROTACION PROPIA")

        val result = repository.delete(created.id, listOf(routineUsingIt))

        assertFalse(result.success)
        assertEquals("Este ejercicio está siendo utilizado por una rutina.", result.message)
    }

    @Test
    fun deletionRestrictionKeepsTheUsedExercisePersisted() {
        val repository = CustomExerciseRepository(InMemoryStorage())
        val created = repository.create("Ejercicio en uso", "ABDOMINALES").exercise!!

        assertFalse(repository.delete(created.id, listOf(routineUsing(created.name))).success)

        assertEquals(listOf(created), repository.load())
    }

    private fun routineUsing(name: String): Routine {
        val routine = Routines.all.first()
        return routine.copy(exercises = listOf(routine.exercises.first().copy(name = name)))
    }

    private fun customDefinition(id: String, name: String, category: String) =
        ExerciseDefinition("custom-exercise-$id", name, category)

    private class InMemoryStorage : RoutineStorage {
        private val values = mutableMapOf<String, String>()

        override fun read(key: String): String? = values[key]

        override fun write(key: String, value: String): Boolean {
            values[key] = value
            return true
        }
    }
}
