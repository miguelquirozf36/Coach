package com.miguel.coach

import java.nio.file.Files
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingProgramTest {
    @Test
    fun officialProgramsHaveStableIdsAndApprovedDayCounts() {
        val programs = OfficialTrainingPrograms.create()

        assertEquals(listOf("full-body", "push-pull-legs", "upper-lower", "weider"), programs.map(TrainingProgram::id))
        assertEquals(listOf(3, 6, 4, 7), programs.map { it.routines.size })
        assertTrue(programs.all(TrainingProgram::builtIn))
        assertEquals(Routines.all, programs.single { it.id == "weider" }.routines)
        assertEquals("Weider / Grupos musculares", programDisplayName(programs.single { it.id == "weider" }))
    }

    @Test
    fun onboardingCardsShowTheApprovedProgramGuidance() {
        val programs = OfficialTrainingPrograms.create().associateBy(TrainingProgram::id)

        assertEquals(
            listOf("Ideal si entrenas pocos días.", "Alta frecuencia por músculo.", "Buena opción para empezar."),
            programOnboardingGuidance(programs.getValue(OfficialTrainingPrograms.FULL_BODY_ID))
        )
        assertEquals(
            listOf("Ideal si entrenas con frecuencia.", "Mayor volumen por grupo muscular.", "Requiere 6 días disponibles."),
            programOnboardingGuidance(programs.getValue(OfficialTrainingPrograms.PPL_ID))
        )
        assertEquals(
            listOf("Equilibrio entre frecuencia y descanso.", "Cada grupo se trabaja 2 veces/semana.", "Ideal para 4 días de entrenamiento."),
            programOnboardingGuidance(programs.getValue(OfficialTrainingPrograms.UPPER_LOWER_ID))
        )
        assertEquals(
            listOf("Mayor enfoque en cada grupo muscular.", "Sesiones más especializadas.", "Ideal si prefieres entrenar a diario."),
            programOnboardingGuidance(programs.getValue(OfficialTrainingPrograms.WEIDER_ID))
        )
        assertEquals("7 sesiones", programOnboardingFrequency(programs.getValue(OfficialTrainingPrograms.WEIDER_ID)))
        assertEquals("WEIDER / GRUPOS MUSCULARES", programDisplayName(programs.getValue(OfficialTrainingPrograms.WEIDER_ID)).uppercase())
    }

    @Test
    fun selectedProgramPersistsWithoutDeletingPrograms() {
        val storage = MemoryProgramStorage()
        val repository = TrainingProgramRepository(storage)
        val programs = repository.loadPrograms(Routines.all, existingInstallation = false)

        assertNull(repository.loadSelectedProgramId())
        assertTrue(repository.selectProgram("push-pull-legs"))
        assertEquals("push-pull-legs", TrainingProgramRepository(storage).loadSelectedProgramId())
        assertEquals(programs, TrainingProgramRepository(storage).loadPrograms(emptyList(), false))
    }

    @Test
    fun onboardingOnlyAppearsWithoutASelectionAndActiveProgramFollowsTheId() {
        val programs = OfficialTrainingPrograms.create()

        assertTrue(shouldShowProgramOnboarding(null))
        assertFalse(shouldShowProgramOnboarding("weider"))
        assertEquals("push-pull-legs", activeTrainingProgram(programs, "push-pull-legs")?.id)
        assertNull(activeTrainingProgram(programs, "missing"))
    }

    @Test
    fun legacyInstallationBecomesWeiderAndCustomRoutinesArePreservedIdempotently() {
        val custom = emptyCustomRoutine("legacy-custom").copy(
            name = "Mi día",
            exercises = listOf(emptyCustomExercise("legacy-exercise"))
        )
        val editedWeider = Routines.all.mapIndexed { index, routine ->
            if (index == 0) routine.copy(name = "Día editado", warmupSeconds = 420) else routine
        }
        val storage = MemoryProgramStorage()
        val repository = TrainingProgramRepository(storage)

        val migrated = repository.loadPrograms(editedWeider + custom, existingInstallation = true)
        val reloaded = repository.loadPrograms(emptyList(), existingInstallation = false)

        assertEquals("weider", repository.loadSelectedProgramId())
        assertEquals(editedWeider, migrated.single { it.id == "weider" }.routines)
        assertEquals(listOf(custom), migrated.single { it.id == "my-routines" }.routines)
        assertFalse(migrated.single { it.id == "my-routines" }.builtIn)
        assertEquals(migrated, reloaded)
    }

    @Test
    fun customProgramCanBeRenamedAndContainMultipleRoutineDays() {
        val storage = MemoryProgramStorage()
        val repository = TrainingProgramRepository(storage)
        val official = repository.loadPrograms(Routines.all, false)
        val custom = TrainingProgram(
            id = "custom-program",
            name = "Inicial",
            description = "Programa personalizado.",
            frequency = "2 días",
            routines = listOf(
                emptyCustomRoutine("custom-day-1"),
                emptyCustomRoutine("custom-day-2")
            ),
            builtIn = false
        )

        assertTrue(repository.savePrograms(official + custom))
        val renamed = repository.loadPrograms(emptyList(), false).single { it.id == custom.id }.copy(name = "Renombrado")
        assertTrue(repository.savePrograms(official + renamed))

        assertEquals("Renombrado", repository.loadPrograms(emptyList(), false).single { it.id == custom.id }.name)
        assertEquals(listOf("custom-day-1", "custom-day-2"), renamed.routines.map(Routine::id))
    }

    @Test
    fun catalogCardsUseSelectedBackgroundAndKeepNavigationSeparateFromSelection() {
        val source = Files.readString(
            Paths.get("src/main/java/com/miguel/coach/ProgramsScreen.kt")
        )
        val cardBody = source.substringAfter("private fun ProgramCard").substringBeforeLast("}")
        val detailBody = source.substringAfter("fun ProgramDetailScreen").substringBefore("private fun ProgramCard")

        assertFalse(cardBody.contains("USAR ESTE PROGRAMA"))
        assertFalse(cardBody.contains("Text(\"ACTIVO\""))
        CoachTheme.entries.forEach { theme ->
            assertEquals(theme.colorScheme.primaryContainer, programCardContainerColor(theme.colorScheme, active = true))
            assertEquals(contentCardContainerColor(theme.colorScheme), programCardContainerColor(theme.colorScheme, active = false))
        }
        assertTrue(cardBody.contains(".clickable(onClick = onClick)"))
        assertTrue(detailBody.contains("\"USAR ESTE PROGRAMA\""))

        val storage = MemoryProgramStorage().apply { selected = "weider" }
        var openedProgramId: String? = null
        val program = OfficialTrainingPrograms.create().first()

        openedProgramId = program.id

        assertEquals(program.id, openedProgramId)
        assertEquals("weider", TrainingProgramRepository(storage).loadSelectedProgramId())
        assertTrue(TrainingProgramRepository(storage).selectProgram(program.id))
        assertEquals(program.id, TrainingProgramRepository(storage).loadSelectedProgramId())
    }
}

private class MemoryProgramStorage : TrainingProgramStorage {
    var programs: String? = null
    var selected: String? = null
    override fun readPrograms(): String? = programs
    override fun writePrograms(value: String): Boolean { programs = value; return true }
    override fun readSelectedProgramId(): String? = selected
    override fun writeSelectedProgramId(id: String?): Boolean { selected = id; return true }
}
