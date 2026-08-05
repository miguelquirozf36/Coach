package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutNotificationTest {
    @Test
    fun warmupUsesRoutineNameAndWarmupText() {
        val content = workoutNotificationContent(workout(phase = TrainingPhase.WARMUP))
        assertEquals("Rutina de prueba", content.title)
        assertEquals("Calentamiento", content.text)
        assertEquals("Calentamiento", content.phase)
    }

    @Test
    fun concentricShowsExerciseSeriesAndRepetition() {
        val content = workoutNotificationContent(
            workout(phase = TrainingPhase.CONCENTRIC, series = 2, repetition = 3)
        )
        assertEquals("Ejercicio uno", content.title)
        assertEquals("Serie 2 de 3 · Repetición 3 de 8", content.text)
        assertEquals("Concéntrica", content.phase)
    }

    @Test
    fun eccentricAndRestHaveTheirVisiblePhase() {
        assertEquals("Excéntrica", workoutNotificationContent(workout(TrainingPhase.ECCENTRIC)).phase)
        assertEquals("Descanso", workoutNotificationContent(workout(TrainingPhase.REST)).phase)
    }

    @Test
    fun pausedWorkoutOverridesTheCurrentPhase() {
        assertEquals(
            "Pausa",
            workoutNotificationContent(workout(TrainingPhase.CONCENTRIC, paused = true)).phase
        )
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
    fun secondsOnlyChangesDoNotUpdateTheNotification() {
        val tracker = WorkoutNotificationTracker()
        tracker.next(workout(TrainingPhase.CONCENTRIC, seconds = 5))
        assertEquals(
            WorkoutNotificationChange.None,
            tracker.next(workout(TrainingPhase.CONCENTRIC, seconds = 4))
        )
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
        assertEquals("Serie 3 de 3 · Repetición 8 de 8", content.text)
    }

    @Test
    fun notificationUsesOneStableIdentifier() {
        assertEquals(1308, WORKOUT_NOTIFICATION_ID)
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
        seconds: Int = 5
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
        currentExerciseNotes = ""
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
