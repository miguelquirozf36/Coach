package com.miguel.coach

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingEngineTest {
    @Test
    fun emptyRoutineIsRejectedBeforeStateAudioOrTimersAreCreated() {
        val fixture = Fixture(emptyList())

        fixture.engine.start(fixture.routine)

        assertEquals(TrainingUiState.Home, fixture.engine.state)
        assertTrue(fixture.voice.phrases.isEmpty())
        assertEquals(0, fixture.voice.stopCalls)
        assertEquals(0, fixture.beep.playCalls)
        assertEquals(0, fixture.beep.stopCalls)
        assertTrue(!fixture.scheduler.hasPendingActions)
    }

    @Test
    fun routineWithOneExerciseStillCreatesWorkoutAndStartsNormally() {
        val fixture = Fixture(seriesExercise(sets = 1, repetitions = 1, restSeconds = 0))

        fixture.engine.start(fixture.routine)

        assertTrue(fixture.engine.state is TrainingUiState.Workout)
        assertEquals(listOf("Comenzamos en diez segundos."), fixture.voice.phrases)
        assertTrue(fixture.scheduler.hasPendingActions)
    }

    @Test
    fun warmupUsesTheConfiguredWarmupRingColor() {
        assertEquals(
            CoachTheme.OBSIDIAN.trainingRingColors.warmupRing,
            trainingRingColor(TrainingPhase.WARMUP, CoachTheme.OBSIDIAN.trainingRingColors)
        )
    }

    @Test
    fun initialCountdownUsesTheConfiguredWarmupRingColor() {
        assertEquals(
            CoachTheme.OCEAN.trainingRingColors.warmupRing,
            trainingRingColor(TrainingPhase.COUNTDOWN, CoachTheme.OCEAN.trainingRingColors)
        )
    }

    @Test
    fun concentricPhaseUsesTheConfiguredConcentricRingColor() {
        assertEquals(
            CoachTheme.FOREST.trainingRingColors.concentricRing,
            trainingRingColor(TrainingPhase.CONCENTRIC, CoachTheme.FOREST.trainingRingColors)
        )
    }

    @Test
    fun eccentricPhaseUsesTheConfiguredEccentricRingColor() {
        assertEquals(
            CoachTheme.LIGHT.trainingRingColors.eccentricRing,
            trainingRingColor(TrainingPhase.ECCENTRIC, CoachTheme.LIGHT.trainingRingColors)
        )
    }

    @Test
    fun restBetweenSeriesUsesItsConfiguredRingColor() {
        assertEquals(
            CoachTheme.OBSIDIAN.trainingRingColors.restSeriesRing,
            trainingRingColor(TrainingPhase.REST, CoachTheme.OBSIDIAN.trainingRingColors)
        )
    }

    @Test
    fun restBetweenExercisesUsesItsConfiguredRingColor() {
        assertEquals(
            CoachTheme.OCEAN.trainingRingColors.restExerciseRing,
            trainingRingColor(
                TrainingPhase.REST_BETWEEN_EXERCISES,
                CoachTheme.OCEAN.trainingRingColors
            )
        )
    }

    @Test
    fun pauseKeepsTheColorOfTheActivePhase() {
        val fixture = Fixture(seriesExercise(sets = 1, restSeconds = 1))
        fixture.startFirstConcentricPhase()
        val colorBeforePause = trainingRingColor(
            fixture.currentWorkout().phase,
            CoachTheme.FOREST.trainingRingColors
        )

        fixture.engine.pause()

        assertTrue(fixture.currentWorkout().isPaused)
        assertEquals(
            colorBeforePause,
            trainingRingColor(
                fixture.currentWorkout().phase,
                CoachTheme.FOREST.trainingRingColors
            )
        )
    }

    @Test
    fun repetitionAnnouncementKeepsTheConcentricVisualColor() {
        val colors = CoachTheme.LIGHT.trainingRingColors

        assertEquals(
            trainingRingColor(TrainingPhase.CONCENTRIC, colors),
            trainingRingColor(TrainingPhase.REPETITION_ANNOUNCEMENT, colors)
        )
    }

    @Test
    fun everyThemeExposesAllRequiredRingColors() {
        CoachTheme.entries.forEach { theme ->
            val colors = theme.trainingRingColors
            listOf(
                colors.warmupRing,
                colors.concentricRing,
                colors.eccentricRing,
                colors.restSeriesRing,
                colors.restExerciseRing
            ).forEach { color ->
                assertTrue("${theme.displayName} contains an unspecified ring color", color != Color.Unspecified)
                assertTrue("${theme.displayName} contains an invisible ring color", color.alpha > 0f)
            }
        }
    }

    @Test
    fun phaseDurationMatchesTheConcentricAndEccentricExerciseDurations() {
        val fixture = Fixture(
            Exercise("timed", "Temporizado", 1, 1, 2, 3, 4)
        )

        fixture.engine.start(fixture.routine)
        repeat(10) { fixture.scheduler.advance() }
        fixture.voice.completeLatest()
        assertEquals(2, fixture.currentWorkout().phaseDurationSeconds)

        repeat(2) { fixture.scheduler.advance() }
        fixture.voice.completeLatest()

        assertEquals(TrainingPhase.ECCENTRIC, fixture.currentWorkout().phase)
        assertEquals(3, fixture.currentWorkout().phaseDurationSeconds)
    }

    @Test
    fun phaseDurationMatchesBothTypesOfRest() {
        val seriesRestFixture = Fixture(seriesExercise(sets = 2, restSeconds = 4))
        seriesRestFixture.startFirstConcentricPhase()
        seriesRestFixture.completeCurrentRepetition()

        assertEquals(TrainingPhase.REST, seriesRestFixture.currentWorkout().phase)
        assertEquals(4, seriesRestFixture.currentWorkout().phaseDurationSeconds)

        val exerciseRestFixture = Fixture(
            listOf(seriesExercise(sets = 1, restSeconds = 4), seriesExercise(sets = 1, restSeconds = 4)),
            restBetweenExercisesSeconds = 12
        )
        exerciseRestFixture.startFirstConcentricPhase()
        exerciseRestFixture.completeCurrentRepetition()

        assertEquals(TrainingPhase.REST_BETWEEN_EXERCISES, exerciseRestFixture.currentWorkout().phase)
        assertEquals(12, exerciseRestFixture.currentWorkout().phaseDurationSeconds)
    }

    @Test
    fun internalAnnouncementKeepsThePreviousVisualPhaseDurationAndNeverBecomesNegative() {
        val fixture = Fixture(seriesExercise(sets = 1, restSeconds = 4))
        fixture.startFirstConcentricPhase()

        assertEquals(1, fixture.currentWorkout().phaseDurationSeconds)
        fixture.scheduler.advance()
        assertEquals(TrainingPhase.REPETITION_ANNOUNCEMENT, fixture.currentWorkout().phase)
        assertEquals(1, fixture.currentWorkout().phaseDurationSeconds)
        assertTrue(fixture.currentWorkout().secondsRemaining >= 0)
        assertTrue(fixture.currentWorkout().phaseDurationSeconds >= 0)
    }

    @Test
    fun estimatedRoutineDurationUsesPhasesRepetitionsAndAllApplicableRests() {
        val routine = Routine(
            id = "estimate",
            name = "Estimación",
            isCustom = false,
            exercises = listOf(
                Exercise("first", "Primero", 2, 3, 10, 5, 20),
                Exercise("second", "Segundo", 1, 2, 8, 2, 30)
            ),
            restBetweenExercisesSeconds = 40,
            warmupSeconds = 0
        )

        assertEquals(3, routine.estimatedDurationMinutes())
    }

    @Test
    fun estimatedRoutineDurationRoundsToTheNearestMinuteAfterEditing() {
        val routine = Routine(
            id = "edited-estimate",
            name = "Estimación editada",
            isCustom = false,
            exercises = listOf(Exercise("only", "Único", 1, 1, 20, 9, 0)),
            restBetweenExercisesSeconds = 0,
            warmupSeconds = 0
        )

        assertEquals(1, routine.estimatedDurationMinutes())
        assertEquals(2, routine.copy(exercises = listOf(
            routine.exercises.single().copy(repetitions = 3)
        )).estimatedDurationMinutes())
    }

    @Test
    fun validRoutineDraftAppliesAllEditableValues() {
        val original = Routines.all.first()
        val editedExercise = original.exercises.first().toDraft().copy(
            name = "Press editado",
            sets = "5",
            repetitions = "8",
            concentricSeconds = "2",
            eccentricSeconds = "4",
            restSeconds = "2"
        )
        val editedDraft = original.toDraft().copy(
            name = "Rutina editada",
            restBetweenExercisesSeconds = "3",
            exercises = listOf(editedExercise) + original.exercises.drop(1).map(Exercise::toDraft)
        )

        val result = editedDraft.validate(original.isCustom)

        val editedRoutine = result.routine ?: error("La rutina válida no se creó.")
        assertEquals("Rutina editada", editedRoutine.name)
        assertEquals(180, editedRoutine.restBetweenExercisesSeconds)
        assertEquals("Press editado", editedRoutine.exercises.first().name)
        assertEquals(5, editedRoutine.exercises.first().sets)
        assertEquals(8, editedRoutine.exercises.first().repetitions)
        assertEquals(2, editedRoutine.exercises.first().concentricSeconds)
        assertEquals(4, editedRoutine.exercises.first().eccentricSeconds)
        assertEquals(120, editedRoutine.exercises.first().restSeconds)
    }

    @Test
    fun routineDraftDisplaysWholeMinutesAndConvertsThemBackToSeconds() {
        val original = Routines.all.first().copy(
            restBetweenExercisesSeconds = 180,
            exercises = listOf(Routines.all.first().exercises.first().copy(restSeconds = 120))
        )

        val draft = original.toDraft()

        assertEquals("3", draft.restBetweenExercisesSeconds)
        assertEquals("2", draft.exercises.single().restSeconds)
        val validated = draft.validate(isCustom = false).routine!!
        assertEquals(180, validated.restBetweenExercisesSeconds)
        assertEquals(120, validated.exercises.single().restSeconds)
    }

    @Test
    fun routineDraftEditsWarmupInWholeMinutesAndStoresSeconds() {
        val draft = Routines.all.first().toDraft().copy(warmupMinutes = "7")

        val routine = draft.validate(isCustom = false).routine!!

        assertEquals("7", draft.warmupMinutes)
        assertEquals(420, routine.warmupSeconds)
    }

    @Test
    fun cancelingAnEditLeavesTheOriginalRoutineUntouched() {
        val original = Routines.all.first()
        val discardedDraft = original.toDraft().copy(
            name = "Cambio descartado",
            restBetweenExercisesSeconds = "0"
        )

        assertEquals("Cambio descartado", discardedDraft.name)
        assertEquals(Routines.all.first(), original)
        assertEquals("DÍA 1 — PECHO Y TRÍCEPS", original.name)
        assertEquals(180, original.restBetweenExercisesSeconds)
    }

    @Test
    fun invalidRoutineDraftDoesNotCreateARoutine() {
        val original = Routines.all.first()
        val invalidExercise = original.exercises.first().toDraft().copy(
            sets = "0",
            repetitions = "-1",
            concentricSeconds = "-1",
            eccentricSeconds = "-1",
            restSeconds = "-1"
        )
        val result = original.toDraft().copy(
            restBetweenExercisesSeconds = "-1",
            exercises = listOf(invalidExercise) + original.exercises.drop(1).map(Exercise::toDraft)
        ).validate(original.isCustom)

        assertEquals(null, result.routine)
        assertTrue(result.message.orEmpty().contains("series"))
        assertTrue(result.message.orEmpty().contains("repeticiones"))
    }

    @Test
    fun engineUsesTheCurrentEditedRoutineWhenStarting() {
        val fixture = Fixture(seriesExercise(sets = 1, restSeconds = 4))
        val editedRoutine = fixture.routine.toDraft().copy(
            exercises = listOf(
                fixture.routine.exercises.single().toDraft().copy(concentricSeconds = "2")
            )
        ).validate(isCustom = false).routine ?: error("La rutina válida no se creó.")

        fixture.engine.start(editedRoutine)
        repeat(10) { fixture.scheduler.advance() }
        fixture.voice.completeLatest()

        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 2, 0, 1, 1, false)
    }

    @Test
    fun seedRoutinesUseTheApprovedSecondBasedDurations() {
        assertEquals(7, Routines.all.size)
        Routines.all.forEach { routine ->
            assertEquals(false, routine.isCustom)
            assertEquals(180, routine.restBetweenExercisesSeconds)
            assertEquals(600, routine.warmupSeconds)
            routine.exercises.forEach { exercise ->
                assertEquals(1, exercise.concentricSeconds)
                assertEquals(if (exercise.id == "pantorrillas-day-7") 1 else 2, exercise.eccentricSeconds)
                assertEquals(120, exercise.restSeconds)
            }
        }
    }

    @Test
    fun lastRepetitionOfASeriesBeepsAnnouncesRestAndStartsTheRestTimer() {
        val fixture = Fixture(seriesExercise(sets = 2, restSeconds = 4))
        fixture.startFirstConcentricPhase()

        fixture.completeCurrentRepetition()

        fixture.assertWorkout(TrainingPhase.REST, 4, 0, 1, 1, false)
        assertEquals("Descansa.", fixture.voice.phrases.last())
        assertEquals(1, fixture.beep.playCalls)
    }

    @Test
    fun restCountsDownWithTheLastThreeAnnouncementsAndStartsTheNextSeriesAfterVamos() {
        val fixture = Fixture(seriesExercise(sets = 2, restSeconds = 4))
        fixture.startFirstConcentricPhase()
        fixture.completeCurrentRepetition()

        fixture.scheduler.advance()
        fixture.assertWorkout(TrainingPhase.REST, 3, 0, 1, 1, false)
        assertEquals("Tres", fixture.voice.phrases.last())
        fixture.scheduler.advance()
        assertEquals("Dos", fixture.voice.phrases.last())
        fixture.scheduler.advance()
        assertEquals("Uno", fixture.voice.phrases.last())
        fixture.scheduler.advance()
        assertEquals("\u00A1Vamos!", fixture.voice.phrases.last())

        fixture.voice.completeLatest()

        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 0, 2, 1, false)
    }

    @Test
    fun lastSeriesStartsAnExerciseRestBeforeTheNextExerciseAndThenCompletesTheWorkout() {
        val fixture = Fixture(
            listOf(
                seriesExercise(sets = 1, restSeconds = 4),
                seriesExercise(sets = 1, restSeconds = 4)
                    .copy(name = "Press inclinado con mancuernas")
            )
        )
        fixture.startFirstConcentricPhase()

        fixture.completeCurrentRepetition()
        fixture.assertWorkout(TrainingPhase.REST_BETWEEN_EXERCISES, 12, 1, 1, 1, false)
        assertEquals(
            "Descansa y prepárate para el siguiente ejercicio. Press inclinado con mancuernas.",
            fixture.voice.phrases.last()
        )

        fixture.engine.skip()
        assertEquals("\u00A1Vamos!", fixture.voice.phrases.last())
        fixture.voice.completeLatest()
        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 1, 1, 1, false)

        fixture.completeCurrentRepetition()

        assertEquals(TrainingUiState.Completed, fixture.engine.state)
        assertEquals("Entrenamiento finalizado.", fixture.voice.phrases.last())
        assertEquals(2, fixture.beep.playCalls)
    }

    @Test
    fun pauseAndResumePreserveTheExactRestSecond() {
        val fixture = Fixture(seriesExercise(sets = 2, restSeconds = 4))
        fixture.startFirstConcentricPhase()
        fixture.completeCurrentRepetition()

        fixture.engine.pause()
        fixture.assertWorkout(TrainingPhase.REST, 4, 0, 1, 1, true)
        fixture.scheduler.advance()
        fixture.assertWorkout(TrainingPhase.REST, 4, 0, 1, 1, true)

        fixture.engine.resume()
        fixture.scheduler.advance()

        fixture.assertWorkout(TrainingPhase.REST, 3, 0, 1, 1, false)
        assertTrue(fixture.voice.stopCalls > 1)
    }

    @Test
    fun skipAdvancesThroughRestAndExerciseTransitionsWithoutDuplicatingAnnouncements() {
        val fixture = Fixture(seriesExercise(sets = 2, restSeconds = 4))
        fixture.startFirstConcentricPhase()
        fixture.completeCurrentRepetition()

        fixture.engine.skip()
        assertEquals("\u00A1Vamos!", fixture.voice.phrases.last())
        fixture.voice.completeLatest()
        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 0, 2, 1, false)

        fixture.engine.skip()
        fixture.assertWorkout(TrainingPhase.REPETITION_ANNOUNCEMENT, 0, 0, 2, 1, false)
        assertEquals("1", fixture.voice.phrases.last())
        val phraseCount = fixture.voice.phrases.size

        fixture.engine.skip()
        fixture.assertWorkout(TrainingPhase.ECCENTRIC, 1, 0, 2, 1, false)
        assertEquals(phraseCount, fixture.voice.phrases.size)
        fixture.engine.skip()

        assertEquals(TrainingUiState.Completed, fixture.engine.state)
        assertEquals(2, fixture.beep.playCalls)
    }

    @Test
    fun finishInvalidatesPendingVoiceCallbacksTimersAndBeeps() {
        val fixture = Fixture(seriesExercise(sets = 2, restSeconds = 4))
        fixture.startFirstConcentricPhase()
        fixture.scheduler.advance()
        fixture.assertWorkout(TrainingPhase.REPETITION_ANNOUNCEMENT, 0, 0, 1, 1, false)

        fixture.engine.finish()
        fixture.voice.completeLatest()
        fixture.scheduler.advance()

        assertEquals(TrainingUiState.Home, fixture.engine.state)
        assertTrue(fixture.voice.stopCalls > 0)
        assertTrue(fixture.beep.stopCalls > 0)
    }

    @Test
    fun pauseAtThreeTwoOneAndZeroInvalidatesOldTimersAndResumesCoherently() {
        listOf(3, 2, 1, 0).forEach { seconds ->
            val fixture = Fixture(seriesExercise(sets = 2, restSeconds = 4))
            fixture.engine.start(fixture.routine)
            repeat(10 - seconds) { fixture.scheduler.advance() }
            fixture.assertWorkout(TrainingPhase.COUNTDOWN, seconds, 0, 1, 1, false)

            fixture.engine.pause()
            fixture.scheduler.advanceCancelled()
            fixture.assertWorkout(TrainingPhase.COUNTDOWN, seconds, 0, 1, 1, true)

            fixture.engine.resume()
            if (seconds == 0) {
                assertEquals("\u00A1Vamos!", fixture.voice.phrases.last())
                fixture.voice.completeLatest()
                fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 0, 1, 1, false)
            } else {
                fixture.scheduler.advance()
                fixture.assertWorkout(TrainingPhase.COUNTDOWN, seconds - 1, 0, 1, 1, false)
            }
            assertEquals(0, fixture.scheduler.overlappingScheduleRequests)
        }
    }

    @Test
    fun resumeDuringRepetitionAnnouncementReplaysOnlyTheCurrentAnnouncement() {
        val fixture = Fixture(seriesExercise(sets = 2, restSeconds = 4))
        fixture.startFirstConcentricPhase()
        fixture.scheduler.advance()
        val phrasesBeforePause = fixture.voice.phrases.size

        fixture.engine.pause()
        fixture.voice.completeLatest()
        fixture.assertWorkout(TrainingPhase.REPETITION_ANNOUNCEMENT, 0, 0, 1, 1, true)

        fixture.engine.resume()

        assertEquals(phrasesBeforePause + 1, fixture.voice.phrases.size)
        assertEquals("1", fixture.voice.phrases.last())
        fixture.voice.completeLatest()
        fixture.assertWorkout(TrainingPhase.ECCENTRIC, 1, 0, 1, 1, false)
    }

    @Test
    fun finishDuringVamosPreventsAnyLaterPhaseTransition() {
        val fixture = Fixture(seriesExercise(sets = 2, restSeconds = 4))
        fixture.engine.start(fixture.routine)
        repeat(10) { fixture.scheduler.advance() }
        assertEquals("\u00A1Vamos!", fixture.voice.phrases.last())

        fixture.engine.finish()
        fixture.voice.completeLatest()
        fixture.scheduler.advanceCancelled()

        assertEquals(TrainingUiState.Home, fixture.engine.state)
        assertEquals(0, fixture.beep.playCalls)
    }

    @Test
    fun restAnnouncesTenSecondsOnlyOnceEvenWhenPausedAndResumed() {
        val fixture = Fixture(seriesExercise(sets = 2, restSeconds = 12))
        fixture.startFirstConcentricPhase()
        fixture.completeCurrentRepetition()

        repeat(2) { fixture.scheduler.advance() }
        fixture.assertWorkout(TrainingPhase.REST, 10, 0, 1, 1, false)
        assertEquals(1, fixture.voice.phrases.count { it == "Quedan diez segundos." })

        fixture.engine.pause()
        fixture.engine.resume()
        fixture.scheduler.advance()

        fixture.assertWorkout(TrainingPhase.REST, 9, 0, 1, 1, false)
        assertEquals(1, fixture.voice.phrases.count { it == "Quedan diez segundos." })
    }

    @Test
    fun exerciseRestShowsTheNextExerciseAndStartsItAfterTheCountdown() {
        val fixture = Fixture(
            listOf(
                seriesExercise(sets = 1, restSeconds = 4),
                seriesExercise(sets = 1, restSeconds = 4)
            )
        )
        fixture.startFirstConcentricPhase()
        fixture.completeCurrentRepetition()

        fixture.assertWorkout(TrainingPhase.REST_BETWEEN_EXERCISES, 12, 1, 1, 1, false)
        repeat(2) { fixture.scheduler.advance() }
        assertEquals("Quedan diez segundos.", fixture.voice.phrases.last())
        repeat(10) { fixture.scheduler.advance() }
        assertEquals("\u00A1Vamos!", fixture.voice.phrases.last())

        fixture.voice.completeLatest()

        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 1, 1, 1, false)
    }

    @Test
    fun pauseResumeAndSkipWorkDuringTheExerciseRest() {
        val fixture = Fixture(
            listOf(
                seriesExercise(sets = 1, restSeconds = 4),
                seriesExercise(sets = 1, restSeconds = 4)
            )
        )
        fixture.startFirstConcentricPhase()
        fixture.completeCurrentRepetition()

        fixture.engine.pause()
        fixture.assertWorkout(TrainingPhase.REST_BETWEEN_EXERCISES, 12, 1, 1, 1, true)
        fixture.scheduler.advanceCancelled()
        fixture.assertWorkout(TrainingPhase.REST_BETWEEN_EXERCISES, 12, 1, 1, 1, true)

        fixture.engine.resume()
        fixture.scheduler.advance()
        fixture.assertWorkout(TrainingPhase.REST_BETWEEN_EXERCISES, 11, 1, 1, 1, false)

        fixture.engine.skip()
        assertEquals("\u00A1Vamos!", fixture.voice.phrases.last())
        fixture.voice.completeLatest()
        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 1, 1, 1, false)
    }

    @Test
    fun minimalWorkoutCompletesNaturallyWithoutUnnecessaryRestOrDuplicateCallbacks() {
        val fixture = Fixture(seriesExercise(sets = 1, repetitions = 1, restSeconds = 1))

        fixture.engine.start(fixture.routine)
        fixture.assertWorkout(TrainingPhase.COUNTDOWN, 10, 0, 1, 1, false)
        repeat(10) { fixture.scheduler.advance() }
        fixture.assertWorkout(TrainingPhase.COUNTDOWN, 0, 0, 1, 1, false)
        fixture.voice.completeLatest()
        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 0, 1, 1, false)

        fixture.scheduler.advance()
        fixture.assertWorkout(TrainingPhase.REPETITION_ANNOUNCEMENT, 0, 0, 1, 1, false)
        assertEquals(1, fixture.voice.phrases.count { it == "1" })
        fixture.voice.completeLatest()
        fixture.assertWorkout(TrainingPhase.ECCENTRIC, 1, 0, 1, 1, false)

        fixture.scheduler.advance()

        assertEquals(TrainingUiState.Completed, fixture.engine.state)
        assertEquals(1, fixture.beep.playCalls)
        assertEquals(0, fixture.voice.phrases.count { it == "Descansa." })
        assertEquals(0, fixture.voice.phrases.count { it.startsWith("Descansa y") })
        assertEquals(1, fixture.voice.phrases.count { it == "Entrenamiento finalizado." })
        assertEquals(0, fixture.voice.pendingCompletionCount)
        assertEquals(false, fixture.scheduler.hasPendingActions)
    }

    @Test
    fun fourRepetitionsAdvanceExactlyOnceThroughEveryConcentricAndEccentricPhase() {
        val fixture = Fixture(seriesExercise(sets = 1, repetitions = 4, restSeconds = 1))
        val concentricRepetitions = mutableListOf<Int>()
        val eccentricRepetitions = mutableListOf<Int>()
        fixture.startFirstConcentricPhase()

        (1..4).forEach { repetition ->
            fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 0, 1, repetition, false)
            concentricRepetitions += fixture.currentWorkout().repetitionNumber
            fixture.scheduler.advance()
            fixture.assertWorkout(TrainingPhase.REPETITION_ANNOUNCEMENT, 0, 0, 1, repetition, false)
            assertEquals(1, fixture.voice.phrases.count { it == repetition.toString() })
            fixture.voice.completeLatest()
            fixture.assertWorkout(TrainingPhase.ECCENTRIC, 1, 0, 1, repetition, false)
            eccentricRepetitions += fixture.currentWorkout().repetitionNumber
            fixture.scheduler.advance()
        }

        assertEquals(listOf(1, 2, 3, 4), concentricRepetitions)
        assertEquals(listOf(1, 2, 3, 4), eccentricRepetitions)
        assertEquals(4, fixture.beep.playCalls)
        assertEquals(TrainingUiState.Completed, fixture.engine.state)
        assertEquals(1, fixture.voice.phrases.count { it == "Entrenamiento finalizado." })
        assertEquals(0, fixture.voice.pendingCompletionCount)
        assertEquals(false, fixture.scheduler.hasPendingActions)
    }

    @Test
    fun multipleSeriesResetRepetitionsAndRestOnlyBetweenSeries() {
        val fixture = Fixture(seriesExercise(sets = 3, repetitions = 2, restSeconds = 2))
        val visitedCounters = mutableListOf<Pair<Int, Int>>()
        fixture.startFirstConcentricPhase()

        (1..3).forEach { series ->
            (1..2).forEach { repetition ->
                fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 0, series, repetition, false)
                visitedCounters += series to repetition
                fixture.completeCurrentRepetition()
            }
            if (series < 3) {
                fixture.assertWorkout(TrainingPhase.REST, 2, 0, series, 2, false)
                repeat(2) { fixture.scheduler.advance() }
                fixture.voice.completeLatest()
                fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 0, series + 1, 1, false)
            }
        }

        assertEquals(
            listOf(1 to 1, 1 to 2, 2 to 1, 2 to 2, 3 to 1, 3 to 2),
            visitedCounters
        )
        assertEquals(2, fixture.voice.phrases.count { it == "Descansa." })
        assertEquals(6, fixture.beep.playCalls)
        assertEquals(TrainingUiState.Completed, fixture.engine.state)
        assertEquals(1, fixture.voice.phrases.count { it == "Entrenamiento finalizado." })
        assertEquals(0, fixture.voice.pendingCompletionCount)
        assertEquals(false, fixture.scheduler.hasPendingActions)
    }

    @Test
    fun multipleExercisesUseConfiguredRestAndVisitEachIndexExactlyOnceInOrder() {
        val first = seriesExercise(sets = 1, repetitions = 2, restSeconds = 1)
            .copy(id = "first-exercise")
        val second = seriesExercise(sets = 1, repetitions = 1, restSeconds = 1)
            .copy(id = "second-exercise", name = "Extensión de tríceps — unilateral")
        val fixture = Fixture(listOf(first, second), restBetweenExercisesSeconds = 3)
        val visitedExerciseIndexes = mutableListOf<Int>()
        fixture.startFirstConcentricPhase()

        (1..2).forEach { repetition ->
            fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 0, 1, repetition, false)
            visitedExerciseIndexes += fixture.currentWorkout().exerciseIndex
            fixture.completeCurrentRepetition()
        }
        fixture.assertWorkout(TrainingPhase.REST_BETWEEN_EXERCISES, 3, 1, 1, 1, false)
        assertEquals(3, fixture.currentWorkout().phaseDurationSeconds)
        val nextExerciseAnnouncement =
            "Descansa y prepárate para el siguiente ejercicio. Extensión de tríceps — unilateral."
        assertEquals(nextExerciseAnnouncement, fixture.voice.phrases.last())
        assertEquals(1, fixture.voice.phrases.count { it == nextExerciseAnnouncement })

        repeat(3) { fixture.scheduler.advance() }
        fixture.voice.completeLatest()
        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 1, 1, 1, false)
        visitedExerciseIndexes += fixture.currentWorkout().exerciseIndex
        fixture.completeCurrentRepetition()

        assertEquals(listOf(0, 0, 1), visitedExerciseIndexes)
        assertEquals(1, fixture.voice.phrases.count { it == nextExerciseAnnouncement })
        assertEquals(3, fixture.beep.playCalls)
        assertEquals(TrainingUiState.Completed, fixture.engine.state)
        assertEquals(1, fixture.voice.phrases.count { it == "Entrenamiento finalizado." })
        assertEquals(0, fixture.voice.pendingCompletionCount)
        assertEquals(false, fixture.scheduler.hasPendingActions)
    }

    @Test
    fun workoutStateStartsWithTheFirstExerciseNoteFromTheSessionRoutineCopy() {
        val exercises = mutableListOf(
            seriesExercise(sets = 1, repetitions = 1, restSeconds = 1)
                .copy(notes = "Nota inicial")
        )
        val fixture = Fixture(exercises)

        fixture.engine.start(fixture.routine)
        exercises.clear()

        fixture.assertWorkout(TrainingPhase.COUNTDOWN, 10, 0, 1, 1, false)
        assertEquals("Nota inicial", fixture.currentWorkout().currentExerciseNotes)
        assertEquals(1, fixture.currentWorkout().routine.exercises.size)
    }

    @Test
    fun enabledWarmupStartsBeforeAnyExercisePhaseWithoutBeeps() {
        val fixture = Fixture(
            exercises = listOf(seriesExercise(sets = 1, repetitions = 1, restSeconds = 1)),
            warmupSeconds = 12
        )

        fixture.engine.start(fixture.routine)

        fixture.assertWorkout(TrainingPhase.WARMUP, 12, 0, 1, 1, false)
        assertEquals("Comienza el calentamiento.", fixture.voice.phrases.single())
        assertEquals(0, fixture.beep.playCalls)
    }

    @Test
    fun disabledWarmupKeepsTheExistingInitialCountdownFlow() {
        val fixture = Fixture(seriesExercise(sets = 1, repetitions = 1, restSeconds = 1))

        fixture.engine.start(fixture.routine)

        fixture.assertWorkout(TrainingPhase.COUNTDOWN, 10, 0, 1, 1, false)
        assertEquals("Comenzamos en diez segundos.", fixture.voice.phrases.single())
    }

    @Test
    fun warmupAnnouncesExactlyTenSecondsOnce() {
        val fixture = Fixture(
            exercises = listOf(seriesExercise(sets = 1, repetitions = 1, restSeconds = 1)),
            warmupSeconds = 12
        )
        fixture.engine.start(fixture.routine)

        repeat(2) { fixture.scheduler.advance() }

        fixture.assertWorkout(TrainingPhase.WARMUP, 10, 0, 1, 1, false)
        assertEquals(1, fixture.voice.phrases.count { it == "Quedan diez segundos." })
        assertEquals(0, fixture.beep.playCalls)
    }

    @Test
    fun warmupFinalCountdownTransitionsDirectlyToConcentricWithoutSecondCountdown() {
        val fixture = Fixture(
            exercises = listOf(seriesExercise(sets = 1, repetitions = 1, restSeconds = 1)),
            warmupSeconds = 4
        )
        fixture.engine.start(fixture.routine)

        repeat(4) { fixture.scheduler.advance() }

        fixture.assertWorkout(TrainingPhase.WARMUP, 0, 0, 1, 1, false)
        assertEquals(listOf("Tres", "Dos", "Uno", "¡Vamos!"), fixture.voice.phrases.takeLast(4))
        fixture.voice.completeLatest()

        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 0, 1, 1, false)
        assertEquals(0, fixture.voice.phrases.count { it == "Comenzamos en diez segundos." })
        assertEquals(1, fixture.voice.phrases.count { it == "¡Vamos!" })
    }

    @Test
    fun pauseAndResumePreserveTheExactWarmupSecond() {
        val fixture = Fixture(
            exercises = listOf(seriesExercise(sets = 1, repetitions = 1, restSeconds = 1)),
            warmupSeconds = 12
        )
        fixture.engine.start(fixture.routine)
        fixture.scheduler.advance()

        fixture.engine.pause()
        fixture.scheduler.advanceCancelled()
        fixture.assertWorkout(TrainingPhase.WARMUP, 11, 0, 1, 1, true)
        fixture.engine.resume()
        fixture.scheduler.advance()

        fixture.assertWorkout(TrainingPhase.WARMUP, 10, 0, 1, 1, false)
        assertEquals(1, fixture.voice.phrases.count { it == "Quedan diez segundos." })
    }

    @Test
    fun skippingWarmupAnnouncesVamosOnceAndStartsConcentric() {
        val fixture = Fixture(
            exercises = listOf(seriesExercise(sets = 1, repetitions = 1, restSeconds = 1)),
            warmupSeconds = 300
        )
        fixture.engine.start(fixture.routine)

        fixture.engine.skip()

        fixture.assertWorkout(TrainingPhase.WARMUP, 0, 0, 1, 1, false)
        assertEquals(1, fixture.voice.phrases.count { it == "¡Vamos!" })
        fixture.voice.completeLatest()
        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 0, 1, 1, false)
        assertEquals(0, fixture.scheduler.overlappingScheduleRequests)
    }

    @Test
    fun finishingDuringWarmupInvalidatesTimerVoiceAndBeepWork() {
        val fixture = Fixture(
            exercises = listOf(seriesExercise(sets = 1, repetitions = 1, restSeconds = 1)),
            warmupSeconds = 300
        )
        fixture.engine.start(fixture.routine)

        fixture.engine.finish()
        fixture.scheduler.advanceCancelled()

        assertEquals(TrainingUiState.Home, fixture.engine.state)
        assertTrue(fixture.voice.stopCalls > 0)
        assertTrue(fixture.beep.stopCalls > 0)
        assertEquals(0, fixture.beep.playCalls)
    }

    @Test
    fun staleWarmupVamosCallbackCannotStartExerciseAfterSkip() {
        val fixture = Fixture(
            exercises = listOf(seriesExercise(sets = 1, repetitions = 1, restSeconds = 1)),
            warmupSeconds = 1
        )
        fixture.engine.start(fixture.routine)
        fixture.scheduler.advance()
        fixture.assertWorkout(TrainingPhase.WARMUP, 0, 0, 1, 1, false)

        fixture.engine.skip()
        fixture.voice.completeOldest()

        fixture.assertWorkout(TrainingPhase.WARMUP, 0, 0, 1, 1, false)
        fixture.voice.completeLatest()
        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 0, 1, 1, false)
        assertEquals(0, fixture.scheduler.overlappingScheduleRequests)
    }

    @Test
    fun workoutStateKeepsAnEmptyExerciseNoteEmpty() {
        val fixture = Fixture(
            seriesExercise(sets = 1, repetitions = 1, restSeconds = 1).copy(notes = "")
        )

        fixture.engine.start(fixture.routine)

        assertEquals("", fixture.currentWorkout().currentExerciseNotes)
    }

    @Test
    fun exerciseNoteRemainsVisibleWhileWorkoutIsPaused() {
        val fixture = Fixture(
            seriesExercise(sets = 1, repetitions = 1, restSeconds = 1)
                .copy(notes = "Conservar durante pausa")
        )
        fixture.startFirstConcentricPhase()

        fixture.engine.pause()

        assertEquals(true, fixture.currentWorkout().isPaused)
        assertEquals("Conservar durante pausa", fixture.currentWorkout().currentExerciseNotes)
    }

    @Test
    fun exerciseNoteRemainsVisibleDuringRestBetweenSeries() {
        val fixture = Fixture(
            seriesExercise(sets = 2, repetitions = 1, restSeconds = 2)
                .copy(notes = "Conservar durante descanso")
        )
        fixture.startFirstConcentricPhase()

        fixture.completeCurrentRepetition()

        fixture.assertWorkout(TrainingPhase.REST, 2, 0, 1, 1, false)
        assertEquals("Conservar durante descanso", fixture.currentWorkout().currentExerciseNotes)
    }

    @Test
    fun completedExerciseNoteRemainsDuringRestBetweenExercises() {
        val first = seriesExercise(sets = 1, repetitions = 1, restSeconds = 1)
            .copy(id = "first-note", notes = "Nota del primero")
        val second = seriesExercise(sets = 1, repetitions = 1, restSeconds = 1)
            .copy(id = "second-note", notes = "Nota del segundo")
        val fixture = Fixture(listOf(first, second), restBetweenExercisesSeconds = 2)
        fixture.startFirstConcentricPhase()

        fixture.completeCurrentRepetition()

        fixture.assertWorkout(TrainingPhase.REST_BETWEEN_EXERCISES, 2, 1, 1, 1, false)
        assertEquals("Nota del primero", fixture.currentWorkout().currentExerciseNotes)
    }

    @Test
    fun noteUpdatesExactlyWhenTheNextExerciseStarts() {
        val first = seriesExercise(sets = 1, repetitions = 1, restSeconds = 1)
            .copy(id = "first-update", notes = "Nota anterior")
        val second = seriesExercise(sets = 1, repetitions = 1, restSeconds = 1)
            .copy(id = "second-update", notes = "Nota nueva")
        val fixture = Fixture(listOf(first, second), restBetweenExercisesSeconds = 2)
        fixture.startFirstConcentricPhase()
        fixture.completeCurrentRepetition()

        repeat(2) { fixture.scheduler.advance() }
        assertEquals("Nota anterior", fixture.currentWorkout().currentExerciseNotes)
        fixture.voice.completeLatest()

        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 1, 1, 1, false)
        assertEquals("Nota nueva", fixture.currentWorkout().currentExerciseNotes)
    }

    @Test
    fun exerciseNotesDoNotChangeVoiceAnnouncementsOrTrainingTransitions() {
        val note = "Esta nota nunca debe pronunciarse"
        val fixture = Fixture(
            seriesExercise(sets = 1, repetitions = 1, restSeconds = 1).copy(notes = note)
        )
        fixture.startFirstConcentricPhase()

        fixture.completeCurrentRepetition()

        assertEquals(TrainingUiState.Completed, fixture.engine.state)
        assertEquals(1, fixture.beep.playCalls)
        assertEquals(1, fixture.voice.phrases.count { it == "1" })
        assertEquals(1, fixture.voice.phrases.count { it == "Entrenamiento finalizado." })
        assertEquals(0, fixture.voice.phrases.count { it.contains(note) })
    }

    @Test
    fun cancelingFinishConfirmationLeavesTheEngineUntouchedAndConfirmingFinishesIt() {
        val fixture = Fixture(seriesExercise(sets = 2, restSeconds = 4))
        fixture.startFirstConcentricPhase()
        val stateBeforeConfirmation = fixture.engine.state

        assertEquals(stateBeforeConfirmation, fixture.engine.state)

        fixture.engine.finish()

        assertEquals(TrainingUiState.Home, fixture.engine.state)
        assertTrue(fixture.voice.stopCalls > 0)
        assertTrue(fixture.beep.stopCalls > 0)
    }

    private class Fixture(
        exercises: List<Exercise>,
        private val restBetweenExercisesSeconds: Int = 12,
        private val warmupSeconds: Int = 0
    ) {
        constructor(exercise: Exercise) : this(listOf(exercise))

        val voice = FakeVoiceSpeaker()
        val beep = FakeBeepPlayer()
        val scheduler = FakeTrainingScheduler()
        val engine = TrainingEngine(voice, beep, scheduler)
        val routine = Routine(
            id = "test",
            name = "Test",
            isCustom = false,
            exercises = exercises,
            restBetweenExercisesSeconds = restBetweenExercisesSeconds,
            warmupSeconds = warmupSeconds
        )

        fun startFirstConcentricPhase() {
            engine.start(routine)
            repeat(10) { scheduler.advance() }
            voice.completeLatest()
            assertWorkout(TrainingPhase.CONCENTRIC, 1, 0, 1, 1, false)
        }

        fun completeCurrentRepetition() {
            scheduler.advance()
            assertWorkout(TrainingPhase.REPETITION_ANNOUNCEMENT, 0, currentExerciseIndex(), currentSeries(), currentRepetition(), false)
            voice.completeLatest()
            repeat(1) { scheduler.advance() }
        }

        fun assertWorkout(
            phase: TrainingPhase,
            secondsRemaining: Int,
            exerciseIndex: Int,
            seriesNumber: Int,
            repetitionNumber: Int,
            isPaused: Boolean
        ) {
            val state = engine.state
            assertTrue(state is TrainingUiState.Workout)
            state as TrainingUiState.Workout
            assertEquals(phase, state.phase)
            assertEquals(secondsRemaining, state.secondsRemaining)
            assertEquals(exerciseIndex, state.exerciseIndex)
            assertEquals(seriesNumber, state.seriesNumber)
            assertEquals(repetitionNumber, state.repetitionNumber)
            assertEquals(isPaused, state.isPaused)
        }

        private fun currentExerciseIndex(): Int = (engine.state as TrainingUiState.Workout).exerciseIndex
        private fun currentSeries(): Int = (engine.state as TrainingUiState.Workout).seriesNumber
        private fun currentRepetition(): Int = (engine.state as TrainingUiState.Workout).repetitionNumber
        fun currentWorkout(): TrainingUiState.Workout = engine.state as TrainingUiState.Workout
    }

    private class FakeVoiceSpeaker : VoiceSpeaker {
        override var isReady = true
        val phrases = mutableListOf<String>()
        var stopCalls = 0
        private val completions = mutableListOf<() -> Unit>()
        val pendingCompletionCount: Int
            get() = completions.size

        override fun speak(phrase: String, onCompleted: (() -> Unit)?) {
            phrases += phrase
            onCompleted?.let(completions::add)
        }

        override fun stop() {
            stopCalls += 1
        }

        fun completeLatest() {
            completions.removeAt(completions.lastIndex).invoke()
        }

        fun completeOldest() {
            completions.removeAt(0).invoke()
        }
    }

    private class FakeBeepPlayer : BeepSoundPlayer {
        var playCalls = 0
        var stopCalls = 0

        override fun play() {
            playCalls += 1
        }

        override fun stop() {
            stopCalls += 1
        }
    }

    private class FakeTrainingScheduler : TrainingScheduler {
        private var pendingAction: (() -> Unit)? = null
        private var cancelledAction: (() -> Unit)? = null
        var overlappingScheduleRequests = 0
            private set
        val hasPendingActions: Boolean
            get() = pendingAction != null || cancelledAction != null

        override fun schedule(delayMillis: Long, action: () -> Unit) {
            if (pendingAction != null) {
                overlappingScheduleRequests += 1
            }
            pendingAction = action
        }

        override fun cancelAll() {
            cancelledAction = pendingAction
            pendingAction = null
        }

        fun advance() {
            val action = pendingAction ?: return
            pendingAction = null
            action()
        }

        fun advanceCancelled() {
            val action = cancelledAction ?: return
            cancelledAction = null
            action()
        }
    }

    private companion object {
        fun seriesExercise(sets: Int, restSeconds: Int, repetitions: Int = 1) = Exercise(
            id = "test-exercise",
            name = "Test exercise",
            sets = sets,
            repetitions = repetitions,
            concentricSeconds = 1,
            eccentricSeconds = 1,
            restSeconds = restSeconds
        )
    }
}
