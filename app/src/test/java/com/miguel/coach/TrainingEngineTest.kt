package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingEngineTest {
    @Test
    fun startOpensTheFirstExerciseOfTheRoutine() {
        val engine = TrainingEngine()
        val routine = Routines.all.first()

        engine.start(routine)

        val state = engine.state
        assertTrue(state is TrainingUiState.Workout)
        state as TrainingUiState.Workout
        assertEquals(routine, state.routine)
        assertEquals(0, state.exerciseIndex)
        assertEquals(1, state.seriesNumber)
        assertEquals(1, state.repetitionNumber)
    }

    @Test
    fun finishReturnsToRoutineSelection() {
        val engine = TrainingEngine()

        engine.start(Routines.all.first())
        engine.finish()

        assertEquals(TrainingUiState.Home, engine.state)
    }
}
