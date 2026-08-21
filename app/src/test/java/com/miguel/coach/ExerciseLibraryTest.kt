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
                "GLÚTEOS",
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
    fun glutesContainsTheRequiredOfficialExercisesAndReusesStableIds() {
        val glutes = ExerciseLibrary.byCategory(ExerciseLibrary.GLUTES)
        val names = glutes.map(ExerciseDefinition::name).toSet()
        val required = setOf(
            "Hip thrust",
            "Puente de glúteos",
            "Hip thrust unilateral",
            "Puente de glúteos unilateral",
            "Patada de glúteo en polea",
            "Patada de glúteo en máquina",
            "Abducción de cadera en máquina",
            "Abducción de cadera en polea",
            "Abducción de cadera acostado",
            "Caminata lateral con banda",
            "Sentadilla sumo",
            "Peso muerto rumano",
            "Peso muerto rumano unilateral",
            "Búlgaras",
            "Step-up",
            "Zancada hacia atrás",
            "Extensión de cadera en polea",
            "Extensión de cadera en máquina"
        )

        assertTrue(names.containsAll(required))
        assertEquals("GLÚTEOS", ExerciseLibrary.find("hip-thrust")?.category)
        assertEquals("GLÚTEOS", ExerciseLibrary.find("bulgaras")?.category)
        assertEquals("GLÚTEOS", ExerciseLibrary.find("peso-muerto-rumano")?.category)
        assertEquals("GLÚTEOS", ExerciseLibrary.find("extension-cadera")?.category)
    }

    @Test
    fun glutesSearchFindsOfficialVariantsAndDisplaysTheirCategory() {
        val hipThrust = ExerciseLibrary.search("Hip thrust")
        val gluteKicks = ExerciseLibrary.search("Patada").filter { "glúteo" in it.name }
        val abductions = ExerciseLibrary.search("Abducción")

        assertEquals(setOf("hip-thrust", "hip-thrust-unilateral"), hipThrust.map { it.id }.toSet())
        assertEquals(setOf("patada-gluteo-maquina", "patada-gluteo-polea"), gluteKicks.map { it.id }.toSet())
        assertEquals(3, abductions.size)
        assertTrue((hipThrust + gluteKicks + abductions).all { it.category == ExerciseLibrary.GLUTES })
    }

    @Test
    fun addingGlutesDoesNotRewriteCustomExerciseIdentityOrCategory() {
        val custom = ExerciseDefinition("custom-gluteos", "Glúteos", ExerciseLibrary.LEGS)
        try {
            ExerciseLibrary.replaceCustom(listOf(custom))

            assertEquals(custom, ExerciseLibrary.find(custom.id))
            assertTrue(custom in ExerciseLibrary.byCategory(ExerciseLibrary.LEGS))
        } finally {
            ExerciseLibrary.replaceCustom(emptyList())
        }
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
            "Prensa o sentadillas",
            "Búlgaras intercalado",
            "Extensión de pierna intercalado",
            "Pullover en polea alta",
            "Elevaciones laterales alternadas",
            "Elevaciones laterales ligas",
            "Curl femoral alternado",
            "Curl de bíceps predicador alternado",
            "Curl de bíceps con barra Z",
            "Fondos en paralelas",
            "Extensión de tríceps polea baja"
        )

        assertTrue(seedNames.all { it in libraryNames || it in approvedRoutineSpecificNames })
    }

    private fun normalized(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase()
}
