package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Test

class PlannedWorkoutProgressTest {
    @Test
    fun startIsExactlyZeroAndNormalCompletionIsExactlyOne() {
        val state = workout(index = 0, startedAtMillis = 1_000L)

        assertEquals(0f, workoutOverallProgress(state, 1_000L), 0f)
        assertEquals(1f, workoutOverallProgress(TrainingUiState.Completed, 99_000L), 0f)
    }

    @Test
    fun halfWarmupUsesExactPartialMillisOverTheFullTimeline() {
        val state = workout(index = 0, startedAtMillis = 2_000L)

        assertEquals(0.1f, workoutOverallProgress(state, 4_000L), 0.0001f)
    }

    @Test
    fun endWarmupEqualsItsExactPlannedShare() {
        val state = workout(index = 0, startedAtMillis = 2_000L)

        assertEquals(0.2f, workoutOverallProgress(state, 6_000L), 0.0001f)
    }

    @Test
    fun partialConcentricIncludesAllEarlierSegments() {
        val state = workout(index = 2, startedAtMillis = 10_000L)

        assertEquals(0.325f, workoutOverallProgress(state, 11_500L), 0.0001f)
    }

    @Test
    fun partialRestIncludesOnlyTheElapsedPartOfThatRest() {
        val state = workout(index = 4, startedAtMillis = 20_000L)

        assertEquals(0.7f, workoutOverallProgress(state, 22_000L), 0.0001f)
    }

    @Test
    fun pausedSegmentUsesFrozenMonotonicInstant() {
        val state = workout(index = 4, startedAtMillis = 20_000L).copy(
            isPaused = true,
            phasePausedAtMillis = 21_000L,
            plannedSegmentPausedAtMillis = 21_000L
        )

        assertEquals(
            workoutOverallProgress(state, 21_000L),
            workoutOverallProgress(state, 99_000L),
            0f
        )
    }

    private fun workout(index: Int, startedAtMillis: Long): TrainingUiState.Workout =
        TrainingUiState.Workout(
            routine = Routine("test", "Test", false, emptyList(), 0, 0),
            exerciseIndex = 0,
            seriesNumber = 1,
            repetitionNumber = 1,
            phase = TrainingPhase.CONCENTRIC,
            secondsRemaining = 1,
            phaseDurationSeconds = 1,
            phaseStartedAtMillis = startedAtMillis,
            phasePausedAtMillis = null,
            isPaused = false,
            currentExerciseNotes = "",
            plannedTimeline = timeline,
            plannedSegmentIndex = index,
            plannedSegmentStartedAtMillis = startedAtMillis
        )

    private val timeline = PlannedWorkoutTimeline(
        listOf(
            segment(PlannedWorkoutSegmentType.WARMUP, 4),
            segment(PlannedWorkoutSegmentType.START_DELAY, 1),
            segment(PlannedWorkoutSegmentType.CONCENTRIC, 3),
            segment(PlannedWorkoutSegmentType.ECCENTRIC, 4),
            segment(PlannedWorkoutSegmentType.REST, 4),
            segment(PlannedWorkoutSegmentType.START_DELAY, 1),
            segment(PlannedWorkoutSegmentType.CONCENTRIC, 3)
        )
    )

    private fun segment(type: PlannedWorkoutSegmentType, seconds: Int) =
        PlannedWorkoutSegment(type, seconds)
}
