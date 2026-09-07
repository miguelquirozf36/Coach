package com.miguel.coach

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.lerp
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutLayoutTest {
    @Test
    fun warmupReplacesTheNormalHeaderTitleWithoutAddingASecondWarmupLabel() {
        val visibleHeaderLines = listOfNotNull(
            workoutHeaderTitle(TrainingPhase.WARMUP, "Entrenamiento"),
            "DÍA 1 — PECHO Y TRÍCEPS",
            warmupNextExerciseText(TrainingPhase.WARMUP, "Press inclinado mancuernas")
        )

        assertEquals(
            listOf(
                "Calentamiento",
                "DÍA 1 — PECHO Y TRÍCEPS",
                "A continuación: Press inclinado mancuernas"
            ),
            visibleHeaderLines
        )
        assertEquals(1, visibleHeaderLines.count { it == "Calentamiento" })
    }

    @Test
    fun normalPhasesKeepTheWorkoutTitleAndNormalNextExerciseVisibility() {
        TrainingPhase.entries.filterNot { it == TrainingPhase.WARMUP }.forEach { phase ->
            assertEquals("Entrenamiento", workoutHeaderTitle(phase, "Entrenamiento"))
            assertEquals(null, warmupNextExerciseText(phase, "Press inclinado mancuernas"))
        }
    }

    @Test
    fun workoutHeaderTypographyUsesTheRequestedAbsoluteSizes() {
        assertEquals(25.sp, WORKOUT_HEADER_TITLE_FONT_SIZE)
        assertEquals(29.sp, WORKOUT_HEADER_PRIMARY_FONT_SIZE)
        assertEquals(20.sp, WORKOUT_HEADER_NEXT_FONT_SIZE)
    }

    @Test
    fun warmupShowsTheRealNextExerciseNameIncludingLongNames() {
        assertEquals(
            "A continuación: Elevaciones laterales alternadas",
            warmupNextExerciseText(TrainingPhase.WARMUP, "Elevaciones laterales alternadas")
        )
    }

    @Test
    fun nextExercisePresentationExistsOnlyDuringWarmup() {
        TrainingPhase.entries.filterNot { it == TrainingPhase.WARMUP }.forEach { phase ->
            assertEquals(null, warmupNextExerciseText(phase, "Jalón al pecho"))
        }
    }

    @Test
    fun warmupDoesNotRenderAnEmptyNextExerciseLine() {
        assertEquals(null, warmupNextExerciseText(TrainingPhase.WARMUP, null))
        assertEquals(null, warmupNextExerciseText(TrainingPhase.WARMUP, "  "))
    }

    @Test
    fun portraitAndLandscapeShareTheSameWarmupPresentationSource() {
        val presentation = warmupNextExerciseText(TrainingPhase.WARMUP, "Press de banca")

        assertEquals(presentation, warmupNextExerciseText(TrainingPhase.WARMUP, "Press de banca"))
    }

    @Test
    fun workoutExerciseListShowsOnlyTheCurrentExerciseAsActive() {
        val current = workoutExerciseListItemPresentation(
            Exercise("current", "Press de banca", 1, 1, 1, 1, 0),
            index = 1,
            currentExerciseIndex = 1
        )
        val inactive = workoutExerciseListItemPresentation(
            Exercise("other", "Aperturas", 1, 1, 1, 1, 0),
            index = 0,
            currentExerciseIndex = 1
        )

        assertEquals(2, current.number)
        assertEquals("Press de banca", current.name)
        assertEquals(1, current.exerciseIndex)
        assertEquals(true, current.isActive)
        assertEquals(false, inactive.isActive)
    }

    @Test
    fun workoutExerciseListUsesTheExactExerciseIndexForTheRightSidePlayAction() {
        val presentation = workoutExerciseListItemPresentation(
            Exercise("press", "Press inclinado", 4, 10, 1, 2, 60),
            index = 2,
            currentExerciseIndex = 0
        )

        assertEquals(2, presentation.exerciseIndex)
        assertEquals("Press inclinado", presentation.name)
        assertEquals("Iniciar desde Press inclinado", startFromExerciseContentDescription(presentation.name))
    }

    @Test
    fun workoutExerciseListAddsDividersOnlyBetweenRows() {
        assertEquals(true, workoutExerciseListShowsDivider(index = 0, exerciseCount = 2))
        assertEquals(false, workoutExerciseListShowsDivider(index = 1, exerciseCount = 2))
        assertEquals(false, workoutExerciseListShowsDivider(index = 0, exerciseCount = 1))
    }

    @Test
    fun workoutNoteAppearsOnlyBelowMetricsForNonWarmupExercises() {
        assertEquals("Mantén la espalda recta", workoutNoteBelowMetrics(TrainingPhase.CONCENTRIC, "mantén la espalda recta"))
        assertEquals(null, workoutNoteBelowMetrics(TrainingPhase.WARMUP, "mantén la espalda recta"))
        assertEquals(null, workoutNoteBelowMetrics(TrainingPhase.CONCENTRIC, "  \n"))
    }

    @Test
    fun landscapeShowsNotesOnlyInsideItsCenteredControlsBlock() {
        assertEquals(false, workoutControlsShowsNote(WorkoutLayout.PORTRAIT))
        assertEquals(true, workoutControlsShowsNote(WorkoutLayout.LANDSCAPE))
    }

    @Test
    fun workoutExerciseListViewportFitsExactlyOneCompleteRow() {
        assertEquals(1, workoutExerciseListFullyVisibleItemCount())
        assertEquals(64.dp, WORKOUT_EXERCISE_LIST_VIEWPORT_HEIGHT)
        assertEquals(52.dp, WORKOUT_EXERCISE_LIST_ITEM_HEIGHT)
    }

    @Test
    fun workoutExerciseListTracksTheCurrentExerciseAndUsesBothFadeEdges() {
        assertEquals(2, workoutExerciseListScrollTarget(2))
        assertEquals(0, workoutExerciseListScrollTarget(-1))
        assertEquals(
            listOf(0f to 0f, 0.18f to 1f, 0.82f to 1f, 1f to 0f),
            workoutExerciseListFadeStops()
        )
    }

    @Test
    fun repetitionCounterChangesAtTheSameBoundaryAsTheVoice() {
        assertEquals(0, workoutState(TrainingPhase.CONCENTRIC, repetitionNumber = 1).completedRepetitions)
        assertEquals(1, workoutState(TrainingPhase.REPETITION_ANNOUNCEMENT, repetitionNumber = 1).completedRepetitions)
        assertEquals(1, workoutState(TrainingPhase.ECCENTRIC, repetitionNumber = 1).completedRepetitions)
        assertEquals(1, workoutState(TrainingPhase.CONCENTRIC, repetitionNumber = 2).completedRepetitions)
        assertEquals(2, workoutState(TrainingPhase.REPETITION_ANNOUNCEMENT, repetitionNumber = 2).completedRepetitions)
        assertEquals(2, workoutState(TrainingPhase.ECCENTRIC, repetitionNumber = 2).completedRepetitions)
        assertEquals(2, workoutState(TrainingPhase.CONCENTRIC, repetitionNumber = 3).completedRepetitions)
        assertEquals(3, workoutState(TrainingPhase.REPETITION_ANNOUNCEMENT, repetitionNumber = 3).completedRepetitions)
    }

    @Test
    fun beepAndReturnPhasesKeepTheLastSpokenRepetition() {
        assertEquals(1, workoutState(TrainingPhase.ECCENTRIC, repetitionNumber = 1).completedRepetitions)
        assertEquals(1, workoutState(TrainingPhase.ISOMETRIC, repetitionNumber = 1).completedRepetitions)
        assertEquals(1, workoutState(TrainingPhase.CONCENTRIC, repetitionNumber = 2).completedRepetitions)
    }

    @Test
    fun shortenedAndStretchedIsometricsDoNotDoubleCount() {
        assertEquals(2, workoutState(TrainingPhase.ISOMETRIC, repetitionNumber = 2).completedRepetitions)
        assertEquals(2, workoutState(TrainingPhase.ECCENTRIC, repetitionNumber = 2).completedRepetitions)
        assertEquals(2, workoutState(TrainingPhase.CONCENTRIC, repetitionNumber = 3).completedRepetitions)
    }

    @Test
    fun lastConcentricShowsTheTotalBeforeRest() {
        assertEquals(9, workoutState(TrainingPhase.CONCENTRIC, repetitionNumber = 10).completedRepetitions)
        assertEquals(10, workoutState(TrainingPhase.REPETITION_ANNOUNCEMENT, repetitionNumber = 10).completedRepetitions)
        assertEquals(10, workoutState(TrainingPhase.REST, repetitionNumber = 10).completedRepetitions)
    }

    @Test
    fun bilateralAndBothUnilateralSidesShareTheCompletedCountSemantics() {
        val bilateral = workoutState(TrainingPhase.REPETITION_ANNOUNCEMENT, repetitionNumber = 2).completedRepetitions
        val rightSide = workoutState(TrainingPhase.REPETITION_ANNOUNCEMENT, repetitionNumber = 2).completedRepetitions
        val leftSide = workoutState(TrainingPhase.REPETITION_ANNOUNCEMENT, repetitionNumber = 2).completedRepetitions

        assertEquals(2, bilateral)
        assertEquals(bilateral, rightSide)
        assertEquals(bilateral, leftSide)
    }

    @Test
    fun sideSeriesAndExerciseTransitionsResetBeforeTheirFirstConcentricCompletes() {
        assertEquals(3, workoutState(TrainingPhase.REST, repetitionNumber = 3).completedRepetitions)
        assertEquals(0, workoutState(TrainingPhase.REST, inStartDelay = true).completedRepetitions)
        assertEquals(0, workoutState(TrainingPhase.CONCENTRIC).completedRepetitions)
        assertEquals(0, workoutState(TrainingPhase.REST_BETWEEN_EXERCISES).completedRepetitions)
        assertEquals(0, workoutState(TrainingPhase.REST_BETWEEN_EXERCISES, inStartDelay = true).completedRepetitions)
    }

    @Test
    fun warmupCountdownPauseAndResumeDoNotInventCompletedRepetitions() {
        assertEquals(0, workoutState(TrainingPhase.WARMUP).completedRepetitions)
        assertEquals(0, workoutState(TrainingPhase.COUNTDOWN).completedRepetitions)
        TrainingPhase.entries.forEach { phase ->
            val beforePause = workoutState(phase, repetitionNumber = 2).completedRepetitions
            val duringPause = workoutState(phase, repetitionNumber = 2, paused = true).completedRepetitions
            val afterResume = workoutState(phase, repetitionNumber = 2).completedRepetitions
            assertEquals(beforePause, duringPause)
            assertEquals(duringPause, afterResume)
        }
        val startDelay = workoutState(TrainingPhase.REST, inStartDelay = true)
        assertEquals(startDelay.completedRepetitions, startDelay.copy(isPaused = true).completedRepetitions)
    }

    @Test
    fun portraitAndLandscapeRenderTheSameCompletedRepetitionText() {
        val completed = workoutState(
            TrainingPhase.REPETITION_ANNOUNCEMENT,
            repetitionNumber = 3
        ).completedRepetitions
        val sharedMetrics = workoutMetricTexts(1, 1, completed, 10, "Concéntrica")
        val portraitText = sharedMetrics.repetition
        val landscapeText = sharedMetrics.repetition

        assertEquals("3 de 10", portraitText)
        assertEquals(portraitText, landscapeText)
    }

    @Test
    fun workoutMetricsKeepTheExistingDynamicValues() {
        assertEquals(
            WorkoutMetricTexts("2 de 4", "7 de 12", "Concéntrica"),
            workoutMetricTexts(2, 4, 7, 12, "Concéntrica")
        )
        assertEquals(
            "Excéntrica",
            workoutMetricTexts(2, 4, 7, 12, "Excéntrica").phase
        )
    }

    @Test
    fun betweenExerciseRestUsesTheShortMetricLabelOnly() {
        assertEquals("Descanso", workoutMetricPhaseLabel(TrainingPhase.REST_BETWEEN_EXERCISES))
        assertEquals("Concéntrica", workoutMetricPhaseLabel(TrainingPhase.CONCENTRIC))
        assertEquals("Excéntrica", workoutMetricPhaseLabel(TrainingPhase.ECCENTRIC))
        assertEquals("Isométrica", workoutMetricPhaseLabel(TrainingPhase.ISOMETRIC))
    }

    @Test
    fun usefulCenterExcludesSystemInsets() {
        assertEquals(544f, usefulAreaCenter(1080, 2400, 24, 80, 16, 120).first)
        assertEquals(1180f, usefulAreaCenter(1080, 2400, 24, 80, 16, 120).second)
    }

    @Test
    fun usefulCenterChangesWithAsymmetricInsets() {
        assertEquals(490f, usefulAreaCenter(1000, 2000, 20, 100, 40, 200).first)
        assertEquals(950f, usefulAreaCenter(1000, 2000, 20, 100, 40, 200).second)
    }

    @Test
    fun ringCenterDoesNotDependOnHeaderHeight() {
        val center = usefulAreaCenter(1080, 2200, 0, 100, 0, 100)
        assertEquals(center, usefulAreaCenter(1080, 2200, 0, 100, 0, 100))
    }

    @Test
    fun ringCenterDoesNotDependOnButtonHeight() {
        val center = usefulAreaCenter(1080, 2200, 0, 100, 0, 100)
        assertEquals(1100f, center.second)
    }

    @Test
    fun ringDiameterIsLimitedOnSmallAndLargeScreens() {
        assertEquals(160.dp, workoutRingDiameter(320.dp, 560.dp))
        assertEquals(240.dp, workoutRingDiameter(500.dp, 900.dp))
    }

    @Test
    fun portraitAndLandscapeSelectTheirDedicatedLayouts() {
        assertEquals(WorkoutLayout.PORTRAIT, workoutLayoutFor(400.dp, 800.dp))
        assertEquals(WorkoutLayout.PORTRAIT, workoutLayoutFor(600.dp, 600.dp))
        assertEquals(WorkoutLayout.LANDSCAPE, workoutLayoutFor(800.dp, 400.dp))
    }

    @Test
    fun landscapeRingFitsItsCenterColumnAndShortScreens() {
        assertEquals(240.dp, landscapeWorkoutRingDiameter(320.dp, 400.dp))
        assertEquals(148.dp, landscapeWorkoutRingDiameter(240.dp, 200.dp))
    }

    @Test
    fun landscapeUsesSymmetricSideColumnsAroundTheCenteredTimer() {
        assertEquals(224.dp, landscapeWorkoutSideWidth(800.dp))
        assertEquals(0.28f, LANDSCAPE_WORKOUT_SIDE_FRACTION)
    }

    @Test
    fun portraitAndLandscapeMetricsBothHaveTwoInternalSeparators() {
        assertEquals(2, workoutMetricSeparatorCount(WorkoutLayout.PORTRAIT))
        assertEquals(2, workoutMetricSeparatorCount(WorkoutLayout.LANDSCAPE))
    }

    @Test
    fun repetitionLabelUsesTheCorrectUtf8TextInBothLayouts() {
        assertEquals("REPETICIÓN", WORKOUT_REPETITION_LABEL)
    }

    @Test
    fun timerProgressAlwaysUsesThemePrimary() {
        CoachTheme.entries.forEach { theme ->
            assertEquals(theme.colorScheme.primary, timerProgressColor(theme.colorScheme))
        }
    }

    @Test
    fun lightWorkoutSurfacesAreBrighterAndDarkThemesStayUnchanged() {
        appearanceLightThemes.forEach { theme ->
            assertEquals(
                lerp(theme.colorScheme.surfaceVariant, theme.colorScheme.surface, 0.35f),
                workoutSupportingContainerColor(theme.colorScheme)
            )
        }
        appearanceDarkThemes.forEach { theme ->
            assertEquals(
                theme.colorScheme.surfaceVariant,
                workoutSupportingContainerColor(theme.colorScheme)
            )
        }
    }

    @Test
    fun lightMetricDividersGainContrastAndDarkDividersStayUnchanged() {
        appearanceLightThemes.forEach { theme ->
            assertEquals(
                theme.colorScheme.outline.copy(alpha = LIGHT_WORKOUT_DIVIDER_ALPHA),
                workoutMetricDividerColor(theme.colorScheme)
            )
        }
        appearanceDarkThemes.forEach { theme ->
            assertEquals(
                theme.colorScheme.outline.copy(alpha = DARK_WORKOUT_DIVIDER_ALPHA),
                workoutMetricDividerColor(theme.colorScheme)
            )
        }
        assertEquals(0.55f, LIGHT_WORKOUT_DIVIDER_ALPHA)
        assertEquals(0.28f, DARK_WORKOUT_DIVIDER_ALPHA)
    }

    @Test
    fun pauseButtonKeepsTheExistingPauseAndResumeCallbacks() {
        var paused = 0
        var resumed = 0
        val onPause: () -> Unit = { paused++ }
        val onResume: () -> Unit = { resumed++ }

        workoutPauseAction(isPaused = false, onPause, onResume).invoke()
        workoutPauseAction(isPaused = true, onPause, onResume).invoke()

        assertEquals(1, paused)
        assertEquals(1, resumed)
    }

    @Test
    fun previousControlWaitsForTheDoubleTapWindowBeforeRestarting() {
        val scheduler = FakeWorkoutControlDelayScheduler()
        var restarts = 0
        var previous = 0
        val resolver = WorkoutPreviousStageTapResolver(scheduler, { restarts++ }, { previous++ })

        resolver.onTap()
        assertEquals(0, restarts)
        assertEquals(0, previous)

        scheduler.runPending()
        assertEquals(1, restarts)
        assertEquals(0, previous)
    }

    @Test
    fun secondPreviousTapCancelsRestartAndNavigatesBackOnce() {
        val scheduler = FakeWorkoutControlDelayScheduler()
        var restarts = 0
        var previous = 0
        val resolver = WorkoutPreviousStageTapResolver(scheduler, { restarts++ }, { previous++ })

        resolver.onTap()
        resolver.onTap()
        scheduler.runCancelled()

        assertEquals(0, restarts)
        assertEquals(1, previous)
    }

    @Test
    fun disposingPreviousControlCancelsItsPendingRestart() {
        val scheduler = FakeWorkoutControlDelayScheduler()
        var restarts = 0
        val resolver = WorkoutPreviousStageTapResolver(scheduler, { restarts++ }, {})

        resolver.onTap()
        resolver.dispose()
        scheduler.runCancelled()

        assertEquals(0, restarts)
    }

    @Test
    fun centerControlUsesPauseAndPlaySymbolsWithoutChangingCallbacks() {
        assertEquals("||", workoutPlaybackSymbol(isPaused = false))
        assertEquals("▶", workoutPlaybackSymbol(isPaused = true))
    }

    @Test
    fun workoutNoteCapitalizesOnlyItsFirstVisibleCharacter() {
        assertEquals(
            "  Recuerda no abrir los Codos",
            workoutNoteText("  recuerda no abrir los Codos")
        )
        assertEquals("Ángulo estable", workoutNoteText("ángulo estable"))
        assertEquals("YA estaba así", workoutNoteText("YA estaba así"))
    }

    @Test
    fun blankWorkoutNoteDoesNotProduceVisibleContent() {
        assertEquals(null, workoutNoteText(""))
        assertEquals(null, workoutNoteText(" \t\n"))
    }

    @Test
    fun timerCategoryMapsWarmupAndRestPhases() {
        assertEquals("CALENTAMIENTO", workoutTimerCategory(TrainingPhase.WARMUP))
        assertEquals("DESCANSO", workoutTimerCategory(TrainingPhase.REST))
        assertEquals("DESCANSO", workoutTimerCategory(TrainingPhase.REST_BETWEEN_EXERCISES))
    }

    @Test
    fun timerCategoryMapsEveryExecutionPhaseWithoutLeakingSpecificPhaseLabels() {
        val executionPhases = listOf(
            TrainingPhase.COUNTDOWN,
            TrainingPhase.CONCENTRIC,
            TrainingPhase.REPETITION_ANNOUNCEMENT,
            TrainingPhase.ECCENTRIC,
            TrainingPhase.ISOMETRIC
        )

        executionPhases.forEach { phase ->
            assertEquals("ENTRENAMIENTO", workoutTimerCategory(phase))
        }
    }

    @Test
    fun activeExecutionShowsTheCurrentSide() {
        assertEquals(ExerciseSide.RIGHT, workoutTimerSide(TrainingPhase.CONCENTRIC, 3, ExerciseSide.RIGHT))
        assertEquals(ExerciseSide.LEFT, workoutTimerSide(TrainingPhase.ECCENTRIC, 3, ExerciseSide.LEFT))
        assertEquals(ExerciseSide.LEFT, workoutTimerSide(TrainingPhase.ISOMETRIC, 3, ExerciseSide.LEFT))
    }

    @Test
    fun restHidesTheSideUntilTheFinalTenSecondsThenShowsTheNextSide() {
        assertEquals(null, workoutTimerSide(TrainingPhase.REST, 30, ExerciseSide.RIGHT))
        assertEquals(null, workoutTimerSide(TrainingPhase.REST, 11, ExerciseSide.RIGHT))
        assertEquals(ExerciseSide.LEFT, workoutTimerSide(TrainingPhase.REST, 10, ExerciseSide.RIGHT))
        assertEquals(ExerciseSide.RIGHT, workoutTimerSide(TrainingPhase.REST, 10, ExerciseSide.LEFT))
        assertEquals(ExerciseSide.LEFT, workoutTimerSide(TrainingPhase.REST, 8, ExerciseSide.RIGHT))
    }

    @Test
    fun warmupAndCountdownPrepareTheExistingFirstSideAtTheThreshold() {
        assertEquals(null, workoutTimerSide(TrainingPhase.WARMUP, 11, ExerciseSide.RIGHT))
        assertEquals(ExerciseSide.RIGHT, workoutTimerSide(TrainingPhase.WARMUP, 10, ExerciseSide.RIGHT))
        assertEquals(ExerciseSide.RIGHT, workoutTimerSide(TrainingPhase.WARMUP, 3, ExerciseSide.RIGHT))
        assertEquals(ExerciseSide.RIGHT, workoutTimerSide(TrainingPhase.COUNTDOWN, 10, ExerciseSide.RIGHT))
        assertEquals(null, workoutTimerSide(TrainingPhase.WARMUP, 3, null))
        assertEquals(null, workoutTimerSide(TrainingPhase.COUNTDOWN, 10, null))
    }

    @Test
    fun betweenExerciseRestPreparesOnlyAnUpcomingUnilateralExercise() {
        assertEquals(null, workoutTimerSide(TrainingPhase.REST_BETWEEN_EXERCISES, 11, ExerciseSide.RIGHT))
        assertEquals(ExerciseSide.RIGHT, workoutTimerSide(TrainingPhase.REST_BETWEEN_EXERCISES, 10, ExerciseSide.RIGHT))
        assertEquals(ExerciseSide.RIGHT, workoutTimerSide(TrainingPhase.REST_BETWEEN_EXERCISES, 3, ExerciseSide.RIGHT))
        assertEquals(null, workoutTimerSide(TrainingPhase.REST_BETWEEN_EXERCISES, 3, null))
    }

    @Test
    fun restToRightExecutionKeepsThePreparedSideThroughStartDelay() {
        val observedSides = listOf(
            workoutTimerSide(TrainingPhase.REST, 1, ExerciseSide.LEFT),
            workoutTimerSide(TrainingPhase.REST, 0, ExerciseSide.RIGHT, isInStartDelay = true),
            workoutTimerSide(TrainingPhase.CONCENTRIC, 3, ExerciseSide.RIGHT)
        )

        assertEquals(listOf(ExerciseSide.RIGHT, ExerciseSide.RIGHT, ExerciseSide.RIGHT), observedSides)
    }

    @Test
    fun restToLeftExecutionKeepsThePreparedSideThroughStartDelay() {
        val observedSides = listOf(
            workoutTimerSide(TrainingPhase.REST, 1, ExerciseSide.RIGHT),
            workoutTimerSide(TrainingPhase.REST, 0, ExerciseSide.LEFT, isInStartDelay = true),
            workoutTimerSide(TrainingPhase.CONCENTRIC, 3, ExerciseSide.LEFT)
        )

        assertEquals(listOf(ExerciseSide.LEFT, ExerciseSide.LEFT, ExerciseSide.LEFT), observedSides)
    }

    @Test
    fun initialPreparationTransitionsKeepRightContinuously() {
        val warmup = listOf(
            workoutTimerSide(TrainingPhase.WARMUP, 1, ExerciseSide.RIGHT),
            workoutTimerSide(TrainingPhase.WARMUP, 0, ExerciseSide.RIGHT, isInStartDelay = true),
            workoutTimerSide(TrainingPhase.CONCENTRIC, 3, ExerciseSide.RIGHT)
        )
        val countdown = listOf(
            workoutTimerSide(TrainingPhase.COUNTDOWN, 1, ExerciseSide.RIGHT),
            workoutTimerSide(TrainingPhase.COUNTDOWN, 0, ExerciseSide.RIGHT, isInStartDelay = true),
            workoutTimerSide(TrainingPhase.CONCENTRIC, 3, ExerciseSide.RIGHT)
        )
        val betweenExercises = listOf(
            workoutTimerSide(TrainingPhase.REST_BETWEEN_EXERCISES, 1, ExerciseSide.RIGHT),
            workoutTimerSide(TrainingPhase.REST_BETWEEN_EXERCISES, 0, ExerciseSide.RIGHT, isInStartDelay = true),
            workoutTimerSide(TrainingPhase.CONCENTRIC, 3, ExerciseSide.RIGHT)
        )

        val expected = listOf(ExerciseSide.RIGHT, ExerciseSide.RIGHT, ExerciseSide.RIGHT)
        assertEquals(expected, warmup)
        assertEquals(expected, countdown)
        assertEquals(expected, betweenExercises)
    }
}

private class FakeWorkoutControlDelayScheduler : WorkoutControlDelayScheduler {
    private var pending: (() -> Unit)? = null
    private var cancelled: (() -> Unit)? = null

    override fun schedule(delayMillis: Long, action: () -> Unit): PendingWorkoutControlAction {
        assertEquals(WORKOUT_PREVIOUS_STAGE_DOUBLE_TAP_MILLIS, delayMillis)
        pending = action
        return PendingWorkoutControlAction {
            cancelled = pending
            pending = null
        }
    }

    fun runPending() {
        pending?.invoke()
        pending = null
    }

    fun runCancelled() {
        cancelled?.invoke()
        cancelled = null
    }
}

private fun workoutState(
    phase: TrainingPhase,
    repetitionNumber: Int = 1,
    inStartDelay: Boolean = false,
    paused: Boolean = false
): TrainingUiState.Workout {
    val exercise = Exercise("skip", "Skip", 1, 1, 1, 1, 1)
    val segment = PlannedWorkoutSegment(
        type = if (inStartDelay) {
            PlannedWorkoutSegmentType.START_DELAY
        } else {
            PlannedWorkoutSegmentType.INITIAL_COUNTDOWN
        },
        durationSeconds = 1,
        exerciseIndex = 0,
        seriesNumber = 1
    )
    return TrainingUiState.Workout(
        routine = Routine("skip", "Skip", false, listOf(exercise), 1),
        exerciseIndex = 0,
        seriesNumber = 1,
        repetitionNumber = repetitionNumber,
        phase = phase,
        secondsRemaining = 0,
        phaseDurationSeconds = 1,
        phaseStartedAtMillis = 0,
        phasePausedAtMillis = null,
        isPaused = paused,
        currentExerciseNotes = "",
        isStartingExecution = inStartDelay,
        plannedTimeline = PlannedWorkoutTimeline(listOf(segment))
    )
}
