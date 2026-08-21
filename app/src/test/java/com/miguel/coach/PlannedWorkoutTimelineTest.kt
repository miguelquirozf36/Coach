package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlannedWorkoutTimelineTest {
    @Test
    fun builtInRoundedDurationsRemainStableWhileUsingExactTimelines() {
        assertEquals(
            listOf(50, 54, 50, 54, 44, 46, 18),
            Routines.all.map(Routine::estimatedDurationMinutes)
        )
    }

    @Test
    fun bilateralWarmupTimelineIncludesIntermediateEccentricsRestsAndEveryStartDelay() {
        val routine = routine(
            exercises = listOf(exercise(sets = 2, repetitions = 3, concentric = 2, eccentric = 4, rest = 7)),
            warmup = 30
        )
        val timeline = routine.plannedTimeline()

        assertEquals(30 + 2 + 2 * (3 * 2 + 2 * 4) + 7, timeline.totalDurationSeconds)
        assertEquals(1, timeline.count(PlannedWorkoutSegmentType.WARMUP))
        assertEquals(2, timeline.count(PlannedWorkoutSegmentType.START_DELAY))
        assertEquals(6, timeline.count(PlannedWorkoutSegmentType.CONCENTRIC))
        assertEquals(4, timeline.count(PlannedWorkoutSegmentType.ECCENTRIC))
        assertEquals(1, timeline.count(PlannedWorkoutSegmentType.REST))
    }

    @Test
    fun noWarmupUsesTenSecondCountdownAndOneSecondInitialDelay() {
        val timeline = routine(listOf(exercise(repetitions = 1)), warmup = 0).plannedTimeline()

        assertEquals(INITIAL_COUNTDOWN_SECONDS, timeline.segments.first().durationSeconds)
        assertEquals(PlannedWorkoutSegmentType.INITIAL_COUNTDOWN, timeline.segments.first().type)
        assertEquals(START_DELAY_SECONDS, timeline.segments[1].durationSeconds)
    }

    @Test
    fun oneRepetitionHasNoFinalEccentricOrIsometricSegment() {
        IsometricPauseMode.entries.forEach { mode ->
            val timeline = routine(listOf(exercise(repetitions = 1, isometricMode = mode, isometric = 9)))
                .plannedTimeline()

            assertEquals(1, timeline.count(PlannedWorkoutSegmentType.CONCENTRIC))
            assertEquals(0, timeline.count(PlannedWorkoutSegmentType.ECCENTRIC))
            assertEquals(0, timeline.count(PlannedWorkoutSegmentType.ISOMETRIC_SHORTENED))
            assertEquals(0, timeline.count(PlannedWorkoutSegmentType.ISOMETRIC_STRETCHED))
        }
    }

    @Test
    fun shortenedAndStretchedApplyOnlyToIntermediateRepetitionsInEngineOrder() {
        val shortened = routine(listOf(exercise(repetitions = 3, isometricMode = IsometricPauseMode.SHORTENED)))
            .plannedTimeline().workTypes()
        val stretched = routine(listOf(exercise(repetitions = 3, isometricMode = IsometricPauseMode.STRETCHED)))
            .plannedTimeline().workTypes()

        assertEquals(
            listOf(
                PlannedWorkoutSegmentType.CONCENTRIC,
                PlannedWorkoutSegmentType.ISOMETRIC_SHORTENED,
                PlannedWorkoutSegmentType.ECCENTRIC,
                PlannedWorkoutSegmentType.CONCENTRIC,
                PlannedWorkoutSegmentType.ISOMETRIC_SHORTENED,
                PlannedWorkoutSegmentType.ECCENTRIC,
                PlannedWorkoutSegmentType.CONCENTRIC
            ),
            shortened
        )
        assertEquals(
            listOf(
                PlannedWorkoutSegmentType.CONCENTRIC,
                PlannedWorkoutSegmentType.ECCENTRIC,
                PlannedWorkoutSegmentType.ISOMETRIC_STRETCHED,
                PlannedWorkoutSegmentType.CONCENTRIC,
                PlannedWorkoutSegmentType.ECCENTRIC,
                PlannedWorkoutSegmentType.ISOMETRIC_STRETCHED,
                PlannedWorkoutSegmentType.CONCENTRIC
            ),
            stretched
        )
    }

    @Test
    fun unilateralTimelinePreservesRightLeftSeriesOrderRestsAndDelays() {
        val timeline = routine(listOf(exercise(
            sets = 2,
            repetitions = 1,
            mode = ExerciseExecutionMode.ONE_SIDE_AT_A_TIME
        ))).plannedTimeline()
        val concentrics = timeline.segments.filter { it.type == PlannedWorkoutSegmentType.CONCENTRIC }

        assertEquals(
            listOf(1 to ExerciseSide.RIGHT, 1 to ExerciseSide.LEFT, 2 to ExerciseSide.RIGHT, 2 to ExerciseSide.LEFT),
            concentrics.map { it.seriesNumber to it.side }
        )
        assertEquals(3, timeline.count(PlannedWorkoutSegmentType.REST))
        assertEquals(4, timeline.count(PlannedWorkoutSegmentType.START_DELAY))
    }

    @Test
    fun twoExercisesHaveOneBetweenExerciseRestAndDelayBeforeSecondExercise() {
        val timeline = routine(
            listOf(exercise(id = "one", repetitions = 1), exercise(id = "two", repetitions = 1)),
            betweenExercises = 13
        ).plannedTimeline()
        val betweenIndex = timeline.segments.indexOfFirst {
            it.type == PlannedWorkoutSegmentType.REST_BETWEEN_EXERCISES
        }

        assertEquals(13, timeline.segments[betweenIndex].durationSeconds)
        assertEquals(PlannedWorkoutSegmentType.START_DELAY, timeline.segments[betweenIndex + 1].type)
        assertEquals(1, timeline.segments[betweenIndex + 1].exerciseIndex)
    }

    @Test
    fun startFromExerciseExcludesEarlierExercisesAndWarmupButKeepsFollowingTimeline() {
        val routine = routine(
            listOf(
                exercise(id = "one", repetitions = 1),
                exercise(id = "two", repetitions = 2),
                exercise(id = "three", repetitions = 1)
            ),
            warmup = 600,
            betweenExercises = 20
        )
        val timeline = routine.plannedTimelineFromExercise(1)

        assertEquals(PlannedWorkoutSegmentType.INITIAL_COUNTDOWN, timeline.segments.first().type)
        assertFalse(timeline.segments.any { it.type == PlannedWorkoutSegmentType.WARMUP })
        assertFalse(timeline.segments.any { it.exerciseIndex == 0 })
        assertTrue(timeline.segments.any { it.exerciseIndex == 1 })
        assertTrue(timeline.segments.any { it.exerciseIndex == 2 })
        assertEquals(1, timeline.count(PlannedWorkoutSegmentType.REST_BETWEEN_EXERCISES))
    }

    @Test
    fun exactSecondsAndRemainingSegmentsStayAvailableWithoutMinuteRounding() {
        val timeline = routine(listOf(exercise(repetitions = 2, concentric = 2, eccentric = 3))).plannedTimeline()

        assertEquals(timeline.segments.sumOf { it.durationSeconds.toLong() }, timeline.totalDurationSeconds)
        assertEquals(
            timeline.segments.drop(2).sumOf { it.durationSeconds.toLong() },
            timeline.remainingDurationSeconds(2)
        )
    }

    @Test
    fun estimatedMinutesAreOnlyARoundedPresentationOfExactTimeline() {
        val routine = routine(listOf(exercise(repetitions = 3, concentric = 20, eccentric = 9)))

        assertEquals(89L, routine.plannedDurationSeconds())
        assertEquals(1, routine.estimatedDurationMinutes())
    }

    private fun PlannedWorkoutTimeline.count(type: PlannedWorkoutSegmentType): Int =
        segments.count { it.type == type }

    private fun PlannedWorkoutTimeline.workTypes(): List<PlannedWorkoutSegmentType> = segments
        .map(PlannedWorkoutSegment::type)
        .filter { it in setOf(
            PlannedWorkoutSegmentType.CONCENTRIC,
            PlannedWorkoutSegmentType.ECCENTRIC,
            PlannedWorkoutSegmentType.ISOMETRIC_SHORTENED,
            PlannedWorkoutSegmentType.ISOMETRIC_STRETCHED
        ) }

    private fun routine(
        exercises: List<Exercise>,
        warmup: Int = 0,
        betweenExercises: Int = 0
    ) = Routine("timeline", "Timeline", false, exercises, betweenExercises, warmup)

    private fun exercise(
        id: String = "exercise",
        sets: Int = 1,
        repetitions: Int = 2,
        concentric: Int = 1,
        eccentric: Int = 2,
        rest: Int = 3,
        mode: ExerciseExecutionMode = ExerciseExecutionMode.SIMULTANEOUS,
        isometricMode: IsometricPauseMode = IsometricPauseMode.NONE,
        isometric: Int = 1
    ) = Exercise(
        id,
        id,
        sets,
        repetitions,
        concentric,
        eccentric,
        rest,
        executionMode = mode,
        isometricPauseMode = isometricMode,
        isometricDurationSeconds = isometric
    )
}
