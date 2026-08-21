package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutNotificationTest {
    @Test
    fun coachIconIsTheStablePrimarySmallIconForRunningAndPausedStates() {
        val runningSmallIcon = workoutNotificationSmallIconResId()
        val pausedSmallIcon = workoutNotificationSmallIconResId()

        assertEquals(R.drawable.ic_notification_coach, runningSmallIcon)
        assertEquals(runningSmallIcon, pausedSmallIcon)
        assertNotEquals(R.drawable.ic_notification_play, runningSmallIcon)
        assertNotEquals(R.drawable.ic_notification_pause, runningSmallIcon)
    }

    @Test
    fun warmupUsesRoutineNameAndDeadlineDerivedCountdown() {
        val content = workoutNotificationContent(workout(phase = TrainingPhase.WARMUP, seconds = 30))
        assertEquals("Rutina de prueba", content.title)
        assertEquals("Calentamiento · 00:30", content.text)
        assertEquals(false, content.isPaused)
    }

    @Test
    fun warmupTicksUpdateTheNotificationAtEachPublishedSecond() {
        val tracker = WorkoutNotificationTracker()

        val first = tracker.next(workout(TrainingPhase.WARMUP, seconds = 30)) as WorkoutNotificationChange.Show
        val second = tracker.next(workout(TrainingPhase.WARMUP, seconds = 29)) as WorkoutNotificationChange.Show

        assertEquals("Calentamiento · 00:30", first.content.text)
        assertEquals("Calentamiento · 00:29", second.content.text)
    }

    @Test
    fun warmupFormatsSingleDigitAndLastSecondsWithTwoDigits() {
        assertEquals(
            "Calentamiento · 00:09",
            workoutNotificationContent(workout(TrainingPhase.WARMUP, seconds = 9)).text
        )
        assertEquals(
            "Calentamiento · 00:01",
            workoutNotificationContent(workout(TrainingPhase.WARMUP, seconds = 1)).text
        )
    }

    @Test
    fun completedWarmupImmediatelyChangesToPreparationWithoutZeroCountdown() {
        val content = workoutNotificationContent(
            workout(TrainingPhase.WARMUP, seconds = 0, startingExecution = true)
        )

        assertEquals("Preparando entrenamiento", content.text)
        assertTrue("Calentamiento" !in content.text && "00:00" !in content.text)
    }

    @Test
    fun pausedWarmupKeepsTheFrozenPublishedSecondAndResumeAction() {
        val content = workoutNotificationContent(
            workout(TrainingPhase.WARMUP, paused = true, seconds = 21)
        )

        assertEquals("Calentamiento · 00:21", content.text)
        assertTrue(content.isPaused)
        assertEquals(
            WorkoutSessionService.ACTION_RESUME_WORKOUT,
            workoutNotificationActions(content).single().serviceAction
        )
    }

    @Test
    fun resumedWarmupContinuesFromTheNextPublishedDeadlineSecond() {
        val paused = workoutNotificationContent(
            workout(TrainingPhase.WARMUP, paused = true, seconds = 21)
        )
        val resumed = workoutNotificationContent(workout(TrainingPhase.WARMUP, seconds = 20))

        assertEquals("Calentamiento · 00:21", paused.text)
        assertEquals("Calentamiento · 00:20", resumed.text)
    }

    @Test
    fun leavingWarmupRemovesItsCountdownWithoutAStaleUpdate() {
        val tracker = WorkoutNotificationTracker()
        tracker.next(workout(TrainingPhase.WARMUP, seconds = 17))

        val execution = tracker.next(workout(TrainingPhase.CONCENTRIC, seconds = 2))
            as WorkoutNotificationChange.Show
        assertEquals("Serie 1 de 3 · Repetición 0 de 8", execution.content.text)
        assertTrue("Calentamiento" !in execution.content.text)
        assertEquals(WorkoutNotificationChange.Remove, tracker.next(TrainingUiState.Home))
    }

    @Test
    fun concentricShowsOnlyCompletedRepetitions() {
        val content = workoutNotificationContent(
            workout(phase = TrainingPhase.CONCENTRIC, series = 2, repetition = 3)
        )
        assertEquals("Ejercicio uno", content.title)
        assertEquals("Serie 2 de 3 · Repetición 2 de 8", content.text)
        assertEquals(false, content.isPaused)
    }

    @Test
    fun executionNeverShowsPhaseNamesOrRestText() {
        TrainingPhase.entries.forEach { phase ->
            if (phase == TrainingPhase.WARMUP || phase == TrainingPhase.COUNTDOWN || phase.isRestForTest) return@forEach
            val text = workoutNotificationContent(workout(phase, repetition = 2)).text
            listOf("Concéntrica", "Excéntrica", "SHORTENED", "STRETCHED", "Fase", "Descanso")
                .forEach { forbidden -> assertTrue(forbidden !in text) }
        }
    }

    @Test
    fun notificationChangesAtTheSameConcentricCompletionBoundaryAsWorkoutScreen() {
        val phases = listOf(
            Triple(TrainingPhase.CONCENTRIC, 3, 2),
            Triple(TrainingPhase.REPETITION_ANNOUNCEMENT, 3, 3),
            Triple(TrainingPhase.ECCENTRIC, 3, 3),
            Triple(TrainingPhase.ISOMETRIC, 3, 3),
            Triple(TrainingPhase.CONCENTRIC, 4, 3),
            Triple(TrainingPhase.REPETITION_ANNOUNCEMENT, 4, 4)
        )

        phases.forEach { (phase, repetition, expectedCompleted) ->
            val state = workout(phase, repetition = repetition)
            val screenCompleted = state.completedRepetitions
            val notification = workoutNotificationContent(state)

            assertEquals(expectedCompleted, screenCompleted)
            assertEquals("Serie 1 de 3 · Repetición $screenCompleted de 8", notification.text)
        }
    }

    @Test
    fun initialConcentricDoesNotAdvanceAndCompletionImmediatelyShowsOne() {
        assertEquals(
            "Serie 1 de 3 · Repetición 0 de 8",
            workoutNotificationContent(workout(TrainingPhase.CONCENTRIC, repetition = 1)).text
        )
        assertEquals(
            "Serie 1 de 3 · Repetición 1 de 8",
            workoutNotificationContent(workout(TrainingPhase.REPETITION_ANNOUNCEMENT, repetition = 1)).text
        )
    }

    @Test
    fun eccentricBeepAndIsometriesKeepTheLastAnnouncedRepetition() {
        listOf(
            TrainingPhase.ECCENTRIC,
            TrainingPhase.REPETITION_ANNOUNCEMENT,
            TrainingPhase.ISOMETRIC
        ).forEach { phase ->
            assertEquals(
                "Serie 1 de 3 · Repetición 3 de 8",
                workoutNotificationContent(workout(phase, repetition = 3)).text
            )
        }
    }

    @Test
    fun bilateralAndBothUnilateralSidesUseTheSharedCompletedProjection() {
        listOf(null, ExerciseSide.RIGHT, ExerciseSide.LEFT).forEach { side ->
            val content = workoutNotificationContent(
                workout(TrainingPhase.ECCENTRIC, repetition = 4, currentSide = side)
            )
            assertEquals("Serie 1 de 3 · Repetición 4 de 8", content.text)
        }
    }

    @Test
    fun pauseAndResumeDoNotChangeCompletedRepetitions() {
        val running = workoutNotificationContent(workout(TrainingPhase.ECCENTRIC, repetition = 3))
        val paused = workoutNotificationContent(
            workout(TrainingPhase.ECCENTRIC, repetition = 3, paused = true)
        )
        val resumed = workoutNotificationContent(workout(TrainingPhase.ECCENTRIC, repetition = 3))

        assertEquals(running.text, paused.text)
        assertEquals(paused.text, resumed.text)
    }

    @Test
    fun pausedWorkoutSelectsTheResumeActionState() {
        assertTrue(workoutNotificationContent(workout(TrainingPhase.CONCENTRIC, paused = true)).isPaused)
    }

    @Test
    fun runningWorkoutUsesOnePauseIconActionWithAccessibleLabel() {
        val actions = workoutNotificationActions(workoutNotificationContent(workout(TrainingPhase.CONCENTRIC)))

        assertEquals(1, actions.size)
        assertEquals(R.drawable.ic_notification_pause, actions.single().iconResId)
        assertEquals("Pausar entrenamiento", actions.single().accessibilityLabel)
        assertEquals(WorkoutSessionService.ACTION_PAUSE_WORKOUT, actions.single().serviceAction)
        assertEquals(1, actions.single().requestCode)
    }

    @Test
    fun pausedWorkoutUsesOnePlayIconActionWithAccessibleLabel() {
        val actions = workoutNotificationActions(
            workoutNotificationContent(workout(TrainingPhase.CONCENTRIC, paused = true))
        )

        assertEquals(1, actions.size)
        assertEquals(R.drawable.ic_notification_play, actions.single().iconResId)
        assertEquals("Reanudar entrenamiento", actions.single().accessibilityLabel)
        assertEquals(WorkoutSessionService.ACTION_RESUME_WORKOUT, actions.single().serviceAction)
        assertEquals(2, actions.single().requestCode)
    }

    @Test
    fun pauseWordsAreNotAddedToVisibleNotificationContent() {
        listOf(false, true).forEach { paused ->
            val content = workoutNotificationContent(workout(TrainingPhase.CONCENTRIC, paused = paused))
            val visibleText = "${content.title} ${content.text}"
            assertTrue(!visibleText.contains("PAUSAR") && !visibleText.contains("REANUDAR"))
        }
    }

    @Test
    fun countdownUsesRoutineNameAndPreparationText() {
        val content = workoutNotificationContent(workout(TrainingPhase.COUNTDOWN))
        assertEquals("Rutina de prueba", content.title)
        assertEquals("Preparando entrenamiento", content.text)
    }

    @Test
    fun seriesRepetitionAndExerciseChangesProduceNewContent() {
        val tracker = WorkoutNotificationTracker()
        assertTrue(tracker.next(workout(TrainingPhase.CONCENTRIC)) is WorkoutNotificationChange.Show)
        assertTrue(tracker.next(workout(TrainingPhase.CONCENTRIC, series = 2)) is WorkoutNotificationChange.Show)
        assertTrue(tracker.next(workout(TrainingPhase.CONCENTRIC, series = 2, repetition = 2)) is WorkoutNotificationChange.Show)
        assertTrue(tracker.next(workout(TrainingPhase.CONCENTRIC, exerciseIndex = 1)) is WorkoutNotificationChange.Show)
    }

    @Test
    fun executionSecondsOnlyChangesDoNotUpdateTheNotification() {
        val tracker = WorkoutNotificationTracker()
        tracker.next(workout(TrainingPhase.CONCENTRIC, seconds = 5))
        assertEquals(
            WorkoutNotificationChange.None,
            tracker.next(workout(TrainingPhase.CONCENTRIC, seconds = 4))
        )
    }

    @Test
    fun restStartsImmediatelyWithTwoDigitDeadlineDerivedCountdown() {
        val content = workoutNotificationContent(
            workout(TrainingPhase.REST, series = 1, repetition = 8, seconds = 90)
        )

        assertEquals("Serie 1 de 3 · Repetición 8 de 8 · Descanso 01:30", content.text)
    }

    @Test
    fun restSecondChangesProduceNotificationUpdates() {
        val tracker = WorkoutNotificationTracker()
        val texts = listOf(10, 9, 8).map { seconds ->
            val change = tracker.next(
                workout(TrainingPhase.REST, repetition = 8, seconds = seconds)
            ) as WorkoutNotificationChange.Show
            change.content.text
        }

        assertEquals(
            listOf(
                "Serie 1 de 3 · Repetición 8 de 8 · Descanso 00:10",
                "Serie 1 de 3 · Repetición 8 de 8 · Descanso 00:09",
                "Serie 1 de 3 · Repetición 8 de 8 · Descanso 00:08"
            ),
            texts
        )
    }

    @Test
    fun completedLastRepetitionRemainsVisibleDuringSeriesRest() {
        val content = workoutNotificationContent(
            workout(TrainingPhase.REST, series = 2, repetition = 8, seconds = 30)
        )

        assertEquals("Serie 2 de 3 · Repetición 8 de 8 · Descanso 00:30", content.text)
    }

    @Test
    fun pausedRestKeepsTheFrozenStateCountdownAndResumeAction() {
        val content = workoutNotificationContent(
            workout(TrainingPhase.REST, repetition = 8, paused = true, seconds = 29)
        )

        assertEquals("Serie 1 de 3 · Repetición 8 de 8 · Descanso 00:29", content.text)
        assertTrue(content.isPaused)
        assertEquals(
            WorkoutSessionService.ACTION_RESUME_WORKOUT,
            workoutNotificationActions(content).single().serviceAction
        )
    }

    @Test
    fun resumedRestContinuesFromTheNextPublishedDeadlineSecond() {
        val paused = workoutNotificationContent(
            workout(TrainingPhase.REST, repetition = 8, paused = true, seconds = 29)
        )
        val resumed = workoutNotificationContent(
            workout(TrainingPhase.REST, repetition = 8, seconds = 28)
        )

        assertEquals("Serie 1 de 3 · Repetición 8 de 8 · Descanso 00:29", paused.text)
        assertEquals("Serie 1 de 3 · Repetición 8 de 8 · Descanso 00:28", resumed.text)
    }

    @Test
    fun endingOrSkippingRestRemovesTheCountdownImmediately() {
        val finishedRest = workoutNotificationContent(
            workout(TrainingPhase.REST, repetition = 8, seconds = 0, startingExecution = true)
        )
        val skippedRest = workoutNotificationContent(
            workout(TrainingPhase.CONCENTRIC, series = 2, repetition = 1, seconds = 2)
        )

        assertEquals("Serie 1 de 3 · Repetición 0 de 8", finishedRest.text)
        assertEquals("Serie 2 de 3 · Repetición 0 de 8", skippedRest.text)
        assertTrue("Descanso" !in finishedRest.text && "Descanso" !in skippedRest.text)
    }

    @Test
    fun finishingDuringRestRemovesNotificationAndRejectsLateOldState() {
        val tracker = WorkoutNotificationTracker()
        tracker.next(workout(TrainingPhase.REST, repetition = 8, seconds = 10))

        assertEquals(WorkoutNotificationChange.Remove, tracker.next(TrainingUiState.Home))
        assertEquals(WorkoutNotificationChange.None, tracker.next(TrainingUiState.Completed))
    }

    @Test
    fun unilateralSideRestUsesTheSameCountdownWithoutSideOrPhaseText() {
        val content = workoutNotificationContent(
            workout(
                TrainingPhase.REST,
                repetition = 8,
                seconds = 20,
                currentSide = ExerciseSide.RIGHT
            )
        )

        assertEquals("Serie 1 de 3 · Repetición 8 de 8 · Descanso 00:20", content.text)
        assertTrue("RIGHT" !in content.text && "LEFT" !in content.text && "Fase" !in content.text)
    }

    @Test
    fun notificationIsRemovedExactlyOnceWhenTrainingEnds() {
        val tracker = WorkoutNotificationTracker()
        tracker.next(workout(TrainingPhase.CONCENTRIC))
        assertEquals(WorkoutNotificationChange.Remove, tracker.next(TrainingUiState.Completed))
        assertEquals(WorkoutNotificationChange.None, tracker.next(TrainingUiState.Home))
    }

    @Test
    fun notificationCanBeCreatedAgainForASecondSession() {
        val tracker = WorkoutNotificationTracker()
        assertTrue(tracker.next(workout(TrainingPhase.CONCENTRIC)) is WorkoutNotificationChange.Show)
        assertEquals(WorkoutNotificationChange.Remove, tracker.next(TrainingUiState.Home))
        assertTrue(tracker.next(workout(TrainingPhase.CONCENTRIC)) is WorkoutNotificationChange.Show)
    }

    @Test
    fun restBetweenExercisesKeepsTheCompletedExercise() {
        val state = workout(TrainingPhase.REST_BETWEEN_EXERCISES, exerciseIndex = 1)
        val content = workoutNotificationContent(state)

        assertEquals(0, state.completedRepetitions)
        assertEquals(0, state.completedExerciseIndex)
        assertEquals(1, state.upcomingExerciseIndex)
        assertEquals("Ejercicio uno", content.title)
        assertEquals("Serie 3 de 3 · Repetición 8 de 8 · Descanso 00:05", content.text)
    }

    @Test
    fun restBetweenExercisesUsesCompletedContextAndCountdown() {
        val content = workoutNotificationContent(
            workout(TrainingPhase.REST_BETWEEN_EXERCISES, exerciseIndex = 1, seconds = 60)
        )

        assertEquals("Ejercicio uno", content.title)
        assertEquals("Serie 3 de 3 · Repetición 8 de 8 · Descanso 01:00", content.text)
    }

    @Test
    fun durationFormattingUsesMinutesAndTwoDigitSecondsOnly() {
        assertEquals("02:00", formatNotificationDuration(120))
        assertEquals("00:45", formatNotificationDuration(45))
        assertEquals("00:09", formatNotificationDuration(9))
        assertEquals("00:01", formatNotificationDuration(1))
    }

    @Test
    fun notificationUsesOneStableIdentifier() {
        assertEquals(1308, WORKOUT_NOTIFICATION_ID)
    }

    @Test
    fun pauseAndResumeUseDistinctInternalServiceActions() {
        assertEquals("com.miguel.coach.action.PAUSE_WORKOUT", WorkoutSessionService.ACTION_PAUSE_WORKOUT)
        assertEquals("com.miguel.coach.action.RESUME_WORKOUT", WorkoutSessionService.ACTION_RESUME_WORKOUT)
        assertTrue(WorkoutSessionService.ACTION_PAUSE_WORKOUT != WorkoutSessionService.ACTION_RESUME_WORKOUT)
    }

    @Test
    fun permissionPolicyHandlesAcceptedRejectedAndFirstRequest() {
        assertEquals(
            NotificationPermissionAction.START_ALLOWED,
            notificationPermissionAction(33, permissionGranted = true, previouslyAsked = false)
        )
        assertEquals(
            NotificationPermissionAction.START_WITHOUT_REQUEST,
            notificationPermissionAction(33, permissionGranted = false, previouslyAsked = true)
        )
        assertEquals(
            NotificationPermissionAction.REQUEST_WITH_EXPLANATION,
            notificationPermissionAction(33, permissionGranted = false, previouslyAsked = false)
        )
    }

    private fun workout(
        phase: TrainingPhase,
        series: Int = 1,
        repetition: Int = 1,
        exerciseIndex: Int = 0,
        paused: Boolean = false,
        seconds: Int = 5,
        currentSide: ExerciseSide? = null,
        startingExecution: Boolean = false
    ): TrainingUiState.Workout = TrainingUiState.Workout(
        routine = routine,
        exerciseIndex = exerciseIndex,
        seriesNumber = series,
        repetitionNumber = repetition,
        phase = phase,
        secondsRemaining = seconds,
        phaseDurationSeconds = 5,
        phaseStartedAtMillis = 0,
        phasePausedAtMillis = if (paused) 1 else null,
        isPaused = paused,
        currentExerciseNotes = "",
        currentSide = currentSide,
        completedExerciseIndex = if (phase == TrainingPhase.REST_BETWEEN_EXERCISES) 0 else null,
        upcomingExerciseIndex = if (phase == TrainingPhase.REST_BETWEEN_EXERCISES) exerciseIndex else null,
        isStartingExecution = startingExecution,
        plannedTimeline = if (startingExecution) {
            PlannedWorkoutTimeline(
                listOf(
                    PlannedWorkoutSegment(
                        type = PlannedWorkoutSegmentType.START_DELAY,
                        durationSeconds = START_DELAY_SECONDS,
                        exerciseIndex = exerciseIndex,
                        seriesNumber = series,
                        side = currentSide
                    )
                )
            )
        } else {
            PlannedWorkoutTimeline(emptyList())
        }
    )

    private companion object {
        val routine = Routine(
            id = "notification-routine",
            name = "Rutina de prueba",
            isCustom = false,
            exercises = listOf(
                Exercise("one", "Ejercicio uno", 3, 8, 2, 2, 30),
                Exercise("two", "Ejercicio dos", 2, 6, 2, 2, 30)
            ),
            restBetweenExercisesSeconds = 60
        )
    }
}

private val TrainingPhase.isRestForTest: Boolean
    get() = this == TrainingPhase.REST || this == TrainingPhase.REST_BETWEEN_EXERCISES
