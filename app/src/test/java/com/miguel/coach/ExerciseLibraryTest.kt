package com.miguel.coach

import java.text.Normalizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExerciseLibraryTest {
    @Test
    fun exposesAllRequiredCategoriesInStableOrder() {
        assertEquals(
            listOf(
                "PECHO",
                "ESPALDA",
                "PIERNAS",
                "HOMBROS",
                "BÍCEPS",
                "TRÍCEPS",
                "ANTEBRAZO",
                "PANTORRILLAS",
                "ABDOMINALES"
            ),
            ExerciseLibrary.categories()
        )
    }

    @Test
    fun searchIgnoresCase() {
        assertEquals(ExerciseLibrary.search("REMO"), ExerciseLibrary.search("remo"))
        assertTrue(ExerciseLibrary.search("REMO").isNotEmpty())
    }

    @Test
    fun searchIgnoresAccents() {
        val withoutAccent = ExerciseLibrary.search("triceps")

        assertTrue(withoutAccent.isNotEmpty())
        assertEquals(withoutAccent, ExerciseLibrary.search("tríceps"))
    }

    @Test
    fun searchUsesPartialMatches() {
        val results = ExerciseLibrary.search("remo")

        assertTrue(results.size >= 5)
        assertTrue(results.all { normalized(it.name).contains("remo") })
    }

    @Test
    fun everyExerciseHasAUniqueStableId() {
        val exercises = ExerciseLibrary.all()

        assertTrue(exercises.all { it.id.isNotBlank() })
        assertEquals(exercises.size, exercises.map(ExerciseDefinition::id).distinct().size)
    }

    @Test
    fun exercisesAreAlphabeticalInsideEveryCategory() {
        ExerciseLibrary.categories().forEach { category ->
            val names = ExerciseLibrary.byCategory(category).map { normalized(it.name) }
            assertEquals(names.sorted(), names)
        }
    }

    @Test
    fun nonexistentSearchReturnsAnEmptyList() {
        assertTrue(ExerciseLibrary.search("ejercicio que no existe xyz").isEmpty())
    }

    @Test
    fun findReturnsTheMatchingDefinitionOrNull() {
        val press = ExerciseLibrary.find("press-militar-mancuernas")

        assertNotNull(press)
        assertEquals("Press militar con mancuernas", press?.name)
        assertEquals("HOMBROS", press?.category)
        assertNull(ExerciseLibrary.find("id-inexistente"))
    }

    @Test
    fun libraryContainsEveryDistinctSeedExerciseName() {
        val seedNames = Routines.all.flatMap(Routine::exercises).map(Exercise::name).distinct()
        val libraryNames = ExerciseLibrary.all().map(ExerciseDefinition::name).toSet()
        val approvedRoutineSpecificNames = setOf(
            "Press inclinado con mancuernas",
            "Fondos en paralelas",
            "Extensión de tríceps en polea baja",
            "Extensión de tríceps en polea alta"
        )

        assertTrue(seedNames.all { it in libraryNames || it in approvedRoutineSpecificNames })
    }

    private fun normalized(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase()
}
