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
    fun warmupUsesRoutineNameAndWarmupText() {
        val content = workoutNotificationContent(workout(phase = TrainingPhase.WARMUP))
        assertEquals("Rutina de prueba", content.title)
        assertEquals("Calentamiento", content.text)
        assertEquals(false, content.isPaused)
    }

    @Test
    fun concentricShowsExerciseSeriesAndRepetition() {
        val content = workoutNotificationContent(
            workout(phase = TrainingPhase.CONCENTRIC, series = 2, repetition = 3)
        )
        assertEquals("Ejercicio uno", content.title)
        assertEquals("Serie 2 de 3 · Repetición 3 de 8", content.text)
        assertEquals(false, content.isPaused)
    }

    @Test
    fun executionNeverShowsPhaseNamesOrRestText() {
        listOf(
            TrainingPhase.CONCENTRIC,
            TrainingPhase.ECCENTRIC,
            TrainingPhase.ISOMETRIC,
            TrainingPhase.REPETITION_ANNOUNCEMENT
        ).forEach { phase ->
            val text = workoutNotificationContent(workout(phase)).text
            assertEquals("Serie 1 de 3 · Repetición 1 de 8", text)
            listOf("Concéntrica", "Excéntrica", "SHORTENED", "STRETCHED", "Fase", "Descanso")
                .forEach { forbidden -> assertTrue(forbidden !in text) }
        }
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

        assertEquals("Serie 1 de 3 · Repetición 8 de 8", finishedRest.text)
        assertEquals("Serie 2 de 3 · Repetición 1 de 8", skippedRest.text)
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
        val content = workoutNotificationContent(
            workout(TrainingPhase.REST_BETWEEN_EXERCISES, exerciseIndex = 1)
        )
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
        isStartingExecution = startingExecution
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
