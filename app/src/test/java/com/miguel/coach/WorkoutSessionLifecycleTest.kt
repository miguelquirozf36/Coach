package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutSessionLifecycleTest {
    @Test
    fun manualFinishAllowsASecondWorkoutWithTheSameReadyInfrastructure() {
        val voice = ReusableVoiceSpeaker()
        val beep = ReusableBeepPlayer()
        val scheduler = QueueScheduler()
        val engine = TrainingEngine(voice, beep, scheduler)
        val originalEngine = engine

        engine.start(routine)
        assertTrue(engine.state is TrainingUiState.Workout)
        engine.finish()

        assertTrue(engine.isVoiceReady)
        assertEquals(TrainingUiState.Home, engine.state)
        engine.start(routine)
        assertTrue(engine.state is TrainingUiState.Workout)
        assertSame(originalEngine, engine)
    }

    @Test
    fun repeatedStartFinishSequencesInvalidateOldCallbacksWithoutNewResources() {
        val voice = ReusableVoiceSpeaker()
        val beep = ReusableBeepPlayer()
        val scheduler = QueueScheduler()
        val engine = TrainingEngine(voice, beep, scheduler)

        repeat(3) {
            engine.start(routine)
            assertTrue(engine.state is TrainingUiState.Workout)
            engine.finish()
            assertEquals(TrainingUiState.Home, engine.state)
            assertTrue(engine.isVoiceReady)
        }

        assertTrue(scheduler.cancelCount >= 6)
        assertTrue(voice.stopCount >= 6)
        assertTrue(beep.stopCount >= 6)
    }

    @Test
    fun normalCompletionCanReturnHomeAndStartANewWorkout() {
        val voice = ReusableVoiceSpeaker()
        val scheduler = QueueScheduler()
        val engine = TrainingEngine(voice, ReusableBeepPlayer(), scheduler)

        engine.start(routine)
        repeat(12) { scheduler.runNext() }
        assertEquals(TrainingUiState.Completed, engine.state)

        engine.finish()
        assertTrue(engine.isVoiceReady)
        engine.start(routine)
        assertTrue(engine.state is TrainingUiState.Workout)
    }

    private class ReusableVoiceSpeaker : VoiceSpeaker {
        override val isReady = true
        var stopCount = 0
        override fun speak(phrase: String, onCompleted: (() -> Unit)?) {
            onCompleted?.invoke()
        }
        override fun stop() {
            stopCount += 1
        }
    }

    private class ReusableBeepPlayer : BeepSoundPlayer {
        var stopCount = 0
        override fun play() = Unit
        override fun stop() {
            stopCount += 1
        }
    }

    private class QueueScheduler : TrainingScheduler {
        private var action: (() -> Unit)? = null
        var cancelCount = 0
        override fun schedule(delayMillis: Long, action: () -> Unit) {
            this.action = action
        }
        override fun cancelAll() {
            action = null
            cancelCount += 1
        }
        fun runNext() {
            val next = action ?: return
            action = null
            next()
        }
    }

    private companion object {
        val routine = Routine(
            id = "repeatable-session",
            name = "Sesión repetible",
            isCustom = false,
            exercises = listOf(
                Exercise("exercise", "Ejercicio", 1, 1, 1, 1, 0)
            ),
            restBetweenExercisesSeconds = 0,
            warmupSeconds = 0
        )
    }
}
