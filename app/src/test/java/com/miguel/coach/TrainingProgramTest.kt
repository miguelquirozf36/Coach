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
    fun weiderPushSessionUsesTheApprovedExercisesAndConfiguration() {
        val session = Routines.all.single { it.id == "day-1-chest-triceps" }

        assertEquals(
            listOf(
                "Press inclinado con mancuernas",
                "Fondos en paralelas",
                "Aperturas en máquina",
                "Hombro frontal",
                "Extensión de tríceps en polea baja",
                "Extensión de tríceps en polea alta"
            ),
            session.exercises.map(Exercise::name)
        )
        assertFalse(session.exercises.any { it.id == "press-banca-plana-mancuernas" })
        assertEquals(
            Exercise("press-inclinado-mancuernas", "Press inclinado con mancuernas", 4, 12, 1, 2, 120),
            session.exercises[0]
        )
        assertEquals(
            Exercise("fondos-triceps", "Fondos en paralelas", 4, 10, 1, 1, 120),
            session.exercises[1]
        )
        assertEquals(
            Exercise("aperturas-maquina", "Aperturas en máquina", 4, 12, 1, 2, 120),
            session.exercises[2]
        )
        assertEquals(
            Exercise("hombro-frontal", "Hombro frontal", 4, 12, 1, 2, 120),
            session.exercises[3]
        )
        assertEquals(
            Exercise("extension-triceps-alta", "Extensión de tríceps en polea baja", 4, 12, 1, 2, 120),
            session.exercises[4]
        )
        assertEquals(
            Exercise("extension-triceps-polea-alta", "Extensión de tríceps en polea alta", 3, 12, 1, 2, 120),
            session.exercises[5]
        )
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
    fun exactLegacyWeiderDayOneIsMigratedPersistedAndKeepsSelection() {
        val original = programsWithLegacyWeiderDayOne()
        val storage = MemoryProgramStorage().apply {
            programs = TrainingProgramCodec.encode(original)
            selected = OfficialTrainingPrograms.WEIDER_ID
        }

        val loaded = TrainingProgramRepository(storage).loadPrograms(emptyList(), existingInstallation = true)

        assertEquals(currentWeiderDayOne(), loaded.weiderDayOne())
        assertEquals(OfficialTrainingPrograms.WEIDER_ID, storage.selected)
        assertEquals(loaded, TrainingProgramCodec.decode(storage.programs))
        assertEquals(1, storage.programWriteCount)
        assertTrue("weider_day1_template_migration_v18" in storage.completedMigrations)
        assertEquals(original.filterNot { it.id == OfficialTrainingPrograms.WEIDER_ID }, loaded.filterNot { it.id == OfficialTrainingPrograms.WEIDER_ID })
    }

    @Test
    fun manuallyEditedLegacyWeiderDayOneIsPreserved() {
        val edited = legacyWeiderDayOne().copy(
            exercises = legacyWeiderDayOne().exercises.mapIndexed { index, exercise ->
                if (index == 0) exercise.copy(repetitions = 12) else exercise
            }
        )
        val storage = storedProgramsWithWeiderDayOne(edited)

        val loaded = TrainingProgramRepository(storage).loadPrograms(emptyList(), existingInstallation = true)

        assertEquals(edited, loaded.weiderDayOne())
        assertEquals(0, storage.programWriteCount)
        assertTrue("weider_day1_template_migration_v18" in storage.completedMigrations)
    }

    @Test
    fun reorderedOrStructurallyChangedLegacyRoutineIsPreserved() {
        val legacy = legacyWeiderDayOne()
        val variants = listOf(
            legacy.copy(exercises = legacy.exercises.reversed()),
            legacy.copy(exercises = legacy.exercises.dropLast(1)),
            legacy.copy(exercises = legacy.exercises + legacy.exercises.last().copy(id = "added-exercise"))
        )

        variants.forEach { variant ->
            val storage = storedProgramsWithWeiderDayOne(variant)
            val loaded = TrainingProgramRepository(storage).loadPrograms(emptyList(), existingInstallation = true)

            assertEquals(variant, loaded.weiderDayOne())
            assertEquals(0, storage.programWriteCount)
        }
    }

    @Test
    fun newTemplateCustomProgramsAndOtherOfficialProgramsRemainUnchanged() {
        val custom = TrainingProgram(
            id = "custom",
            name = "Personalizado",
            description = "Sin cambios.",
            frequency = "1 día",
            routines = listOf(emptyCustomRoutine("custom-day")),
            builtIn = false
        )
        val original = OfficialTrainingPrograms.create() + custom
        val storage = MemoryProgramStorage().apply { programs = TrainingProgramCodec.encode(original) }

        val loaded = TrainingProgramRepository(storage).loadPrograms(emptyList(), existingInstallation = true)

        assertEquals(original, loaded)
        assertEquals(0, storage.programWriteCount)
        assertTrue("weider_day1_template_migration_v18" in storage.completedMigrations)
    }

    @Test
    fun migrationMarkerPreventsASecondEvaluationAndMissingJsonUsesCurrentTemplates() {
        val storage = storedProgramsWithWeiderDayOne(legacyWeiderDayOne())
        val repository = TrainingProgramRepository(storage)
        repository.loadPrograms(emptyList(), existingInstallation = true)
        storage.programs = TrainingProgramCodec.encode(programsWithLegacyWeiderDayOne())

        val secondLoad = repository.loadPrograms(emptyList(), existingInstallation = true)

        assertEquals(legacyWeiderDayOne(), secondLoad.weiderDayOne())
        assertEquals(1, storage.programWriteCount)

        val cleanStorage = MemoryProgramStorage()
        val cleanLoad = TrainingProgramRepository(cleanStorage).loadPrograms(emptyList(), existingInstallation = false)
        assertEquals(currentWeiderDayOne(), cleanLoad.weiderDayOne())
        assertEquals(1, cleanStorage.programWriteCount)
    }

    @Test
    fun previousFiveExerciseTemplateMigratesEvenWhenV18IsComplete() {
        val original = programsWithWeiderDayOne(previousWeiderDayOne())
        val storage = MemoryProgramStorage().apply {
            programs = TrainingProgramCodec.encode(original)
            selected = OfficialTrainingPrograms.WEIDER_ID
            completedMigrations += "weider_day1_template_migration_v18"
        }

        val loaded = TrainingProgramRepository(storage).loadPrograms(emptyList(), existingInstallation = true)

        assertEquals(currentWeiderDayOne(), loaded.weiderDayOne())
        assertEquals(OfficialTrainingPrograms.WEIDER_ID, storage.selected)
        assertEquals(1, storage.programWriteCount)
        assertTrue("weider_day1_machine_flyes_migration_v19" in storage.completedMigrations)
        assertEquals(original.filterNot { it.id == OfficialTrainingPrograms.WEIDER_ID }, loaded.filterNot { it.id == OfficialTrainingPrograms.WEIDER_ID })
        assertEquals(original.weiderOtherDays(), loaded.weiderOtherDays())
    }

    @Test
    fun editedPreviousTemplateVariantsAreNotMigrated() {
        val previous = previousWeiderDayOne()
        val variants = listOf(
            previous.copy(exercises = previous.exercises.mapIndexed { index, exercise ->
                if (index == 0) exercise.copy(repetitions = 11) else exercise
            }),
            previous.copy(exercises = previous.exercises.reversed()),
            previous.copy(exercises = previous.exercises.dropLast(1)),
            previous.copy(exercises = previous.exercises + previous.exercises.last().copy(id = "manual-extra"))
        )

        variants.forEach { variant ->
            val storage = storedProgramsWithWeiderDayOne(variant).apply {
                completedMigrations += "weider_day1_template_migration_v18"
            }
            val loaded = TrainingProgramRepository(storage).loadPrograms(emptyList(), existingInstallation = true)

            assertEquals(variant, loaded.weiderDayOne())
            assertEquals(0, storage.programWriteCount)
            assertTrue("weider_day1_machine_flyes_migration_v19" in storage.completedMigrations)
        }
    }

    @Test
    fun v19MarkerPreventsRepeatedMigration() {
        val storage = storedProgramsWithWeiderDayOne(previousWeiderDayOne()).apply {
            completedMigrations += "weider_day1_template_migration_v18"
        }
        val repository = TrainingProgramRepository(storage)
        repository.loadPrograms(emptyList(), existingInstallation = true)
        storage.programs = TrainingProgramCodec.encode(programsWithWeiderDayOne(previousWeiderDayOne()))

        val secondLoad = repository.loadPrograms(emptyList(), existingInstallation = true)

        assertEquals(previousWeiderDayOne(), secondLoad.weiderDayOne())
        assertEquals(1, storage.programWriteCount)
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
    var programWriteCount = 0
    val completedMigrations = mutableSetOf<String>()
    override fun readPrograms(): String? = programs
    override fun writePrograms(value: String): Boolean { programs = value; programWriteCount++; return true }
    override fun readSelectedProgramId(): String? = selected
    override fun writeSelectedProgramId(id: String?): Boolean { selected = id; return true }
    override fun isMigrationComplete(key: String): Boolean = key in completedMigrations
    override fun markMigrationComplete(key: String): Boolean = completedMigrations.add(key)
}

private fun legacyWeiderDayOne() = Routine(
    id = "day-1-chest-triceps",
    name = "DÍA 1 — PECHO Y TRÍCEPS",
    isCustom = false,
    exercises = listOf(
        Exercise("press-banca-plana-mancuernas", "Press banca plana mancuernas", 3, 10, 1, 2, 120),
        Exercise("press-inclinado-mancuernas", "Press inclinado mancuernas", 4, 10, 1, 2, 120),
        Exercise("aperturas", "Aperturas", 4, 10, 1, 2, 120),
        Exercise("hombro-frontal", "Hombro frontal", 4, 12, 1, 2, 120),
        Exercise("extension-triceps-alta", "Extensión de tríceps alta", 4, 10, 1, 2, 120),
        Exercise("extension-triceps-polea-alta", "Extensión de tríceps polea alta", 4, 10, 1, 2, 120)
    ),
    restBetweenExercisesSeconds = 180,
    warmupSeconds = 600
)

private fun currentWeiderDayOne(): Routine = Routines.all.single { it.id == "day-1-chest-triceps" }

private fun previousWeiderDayOne() = Routine(
    id = "day-1-chest-triceps",
    name = "DÍA 1 — PECHO Y TRÍCEPS",
    isCustom = false,
    exercises = listOf(
        Exercise("press-inclinado-mancuernas", "Press inclinado con mancuernas", 4, 12, 1, 2, 120),
        Exercise("fondos-triceps", "Fondos en paralelas", 4, 10, 1, 1, 120),
        Exercise("hombro-frontal", "Hombro frontal", 4, 12, 1, 2, 120),
        Exercise("extension-triceps-alta", "Extensión de tríceps en polea baja", 4, 12, 1, 2, 120),
        Exercise("extension-triceps-polea-alta", "Extensión de tríceps en polea alta", 3, 12, 1, 2, 120)
    ),
    restBetweenExercisesSeconds = 180,
    warmupSeconds = 600
)

private fun programsWithWeiderDayOne(routine: Routine): List<TrainingProgram> = OfficialTrainingPrograms.create().map { program ->
    if (program.id != OfficialTrainingPrograms.WEIDER_ID) program else program.copy(
        routines = program.routines.map { if (it.id == routine.id) routine else it }
    )
}

private fun programsWithLegacyWeiderDayOne(): List<TrainingProgram> = programsWithWeiderDayOne(legacyWeiderDayOne())

private fun storedProgramsWithWeiderDayOne(routine: Routine) = MemoryProgramStorage().apply {
    programs = TrainingProgramCodec.encode(programsWithWeiderDayOne(routine))
}

private fun List<TrainingProgram>.weiderDayOne(): Routine =
    single { it.id == OfficialTrainingPrograms.WEIDER_ID }.routines.single { it.id == "day-1-chest-triceps" }

private fun List<TrainingProgram>.weiderOtherDays(): List<Routine> =
    single { it.id == OfficialTrainingPrograms.WEIDER_ID }.routines.filterNot { it.id == "day-1-chest-triceps" }
