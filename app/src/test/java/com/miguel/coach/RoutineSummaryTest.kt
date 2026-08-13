package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Test

class RoutineSummaryTest {
    @Test
    fun simultaneousExercisesUseConfiguredSetsAndRepetitions() {
        val routine = routineWith(
            exercise(sets = 4, repetitions = 10),
            exercise(sets = 3, repetitions = 12)
        )

        assertEquals(7, routine.totalExecutionSets())
        assertEquals(76, routine.totalRepetitions())
    }

    @Test
    fun oneSideAtATimeExerciseCountsBothSides() {
        val routine = routineWith(
            exercise(
                sets = 3,
                repetitions = 10,
                executionMode = ExerciseExecutionMode.ONE_SIDE_AT_A_TIME
            )
        )

        assertEquals(6, routine.totalExecutionSets())
        assertEquals(60, routine.totalRepetitions())
    }

    @Test
    fun mixedExercisesCountTheirRealExecutions() {
        val routine = routineWith(
            exercise(sets = 4, repetitions = 10),
            exercise(
                sets = 3,
                repetitions = 10,
                executionMode = ExerciseExecutionMode.ONE_SIDE_AT_A_TIME
            )
        )

        assertEquals(10, routine.totalExecutionSets())
        assertEquals(100, routine.totalRepetitions())
    }

    private fun routineWith(vararg exercises: Exercise) = Routine(
        id = "summary-test",
        name = "Summary test",
        isCustom = false,
        exercises = exercises.toList(),
        restBetweenExercisesSeconds = 180
    )

    private fun exercise(
        sets: Int,
        repetitions: Int,
        executionMode: ExerciseExecutionMode = ExerciseExecutionMode.SIMULTANEOUS
    ) = Exercise(
        id = "exercise-$sets-$repetitions-$executionMode",
        name = "Exercise",
        sets = sets,
        repetitions = repetitions,
        concentricSeconds = 1,
        eccentricSeconds = 1,
        restSeconds = 60,
        executionMode = executionMode
    )
}
