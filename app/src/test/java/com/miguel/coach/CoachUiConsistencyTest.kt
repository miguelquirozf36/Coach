package com.miguel.coach

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CoachUiConsistencyTest {
    @Test
    fun workoutStartActionUsesTheInitiateLabel() {
        assertEquals("INICIAR", START_WORKOUT_LABEL)
    }

    @Test
    fun emptyRoutineStartProducesValidationMessageWithoutStarting() {
        val routine = routineWithExercises(emptyList())
        var startedRoutine: Routine? = null

        val message = attemptRoutineStart(routine) { startedRoutine = it }

        assertEquals(EMPTY_ROUTINE_START_MESSAGE, message)
        assertEquals("Agrega al menos un ejercicio antes de comenzar.", message)
        assertNull(startedRoutine)
    }

    @Test
    fun routineWithOneExerciseStartsNormallyWithoutValidationMessage() {
        val routine = routineWithExercises(
            listOf(Exercise("exercise", "Ejercicio", 1, 1, 1, 1, 0))
        )
        var startedRoutine: Routine? = null

        val message = attemptRoutineStart(routine) { startedRoutine = it }

        assertNull(message)
        assertEquals(routine, startedRoutine)
    }

    @Test
    fun sharedBackButtonKeepsApprovedAccessibilityMeasurements() {
        assertEquals(48.dp, COACH_BACK_TOUCH_TARGET)
        assertEquals(28.dp, COACH_BACK_ICON_SIZE)
        assertEquals("Volver", COACH_BACK_CONTENT_DESCRIPTION)
    }

    @Test
    fun categoryExpansionUsesTheIconThatMatchesItsState() {
        assertEquals(CategoryExpansionIcon.CHEVRON_RIGHT, categoryExpansionIcon(expanded = false))
        assertEquals(CategoryExpansionIcon.EXPAND_MORE, categoryExpansionIcon(expanded = true))
    }

    @Test
    fun exerciseSearchHasAStableAccessibleLabel() {
        assertEquals("Buscar ejercicios", EXERCISE_SEARCH_LABEL)
    }

    private fun routineWithExercises(exercises: List<Exercise>) = Routine(
        id = "start-validation",
        name = "Rutina",
        isCustom = true,
        exercises = exercises,
        restBetweenExercisesSeconds = 0
    )
}
