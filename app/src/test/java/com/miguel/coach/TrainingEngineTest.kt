package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingEngineTest {
    @Test
    fun startRunsTheCountdownAndStartsTheFirstConcentricPhaseAfterVamos() {
        val voice = FakeVoiceSpeaker()
        val scheduler = FakeTrainingScheduler()
        val engine = TrainingEngine(voice, scheduler)

        engine.start(Routines.all.first())

        assertWorkout(engine, TrainingPhase.COUNTDOWN, 10, false)
        assertEquals(listOf("Comenzamos en diez segundos."), voice.phrases)

        repeat(7) { scheduler.advance() }
        assertWorkout(engine, TrainingPhase.COUNTDOWN, 3, false)
        assertEquals("Tres", voice.phrases.last())

        scheduler.advance()
        assertEquals("Dos", voice.phrases.last())
        scheduler.advance()
        assertEquals("Uno", voice.phrases.last())
        scheduler.advance()
        assertWorkout(engine, TrainingPhase.COUNTDOWN, 0, false)
        assertEquals("¡Vamos!", voice.phrases.last())

        voice.completeLatest()

        assertWorkout(engine, TrainingPhase.CONCENTRIC, 1, false)
    }

    @Test
    fun pauseCancelsTheCountdownAndResumeContinuesFromTheSameSecond() {
        val voice = FakeVoiceSpeaker()
        val scheduler = FakeTrainingScheduler()
        val engine = TrainingEngine(voice, scheduler)

        engine.start(Routines.all.first())
        scheduler.advance()
        engine.pause()

        assertWorkout(engine, TrainingPhase.COUNTDOWN, 9, true)
        assertEquals(2, voice.stopCalls)
        scheduler.advance()
        assertWorkout(engine, TrainingPhase.COUNTDOWN, 9, true)

        engine.resume()

        assertWorkout(engine, TrainingPhase.COUNTDOWN, 9, false)
        scheduler.advance()
        assertWorkout(engine, TrainingPhase.COUNTDOWN, 8, false)
    }

    @Test
    fun finishInvalidatesThePendingVamosCallbackAndReturnsHome() {
        val voice = FakeVoiceSpeaker()
        val scheduler = FakeTrainingScheduler()
        val engine = TrainingEngine(voice, scheduler)

        engine.start(Routines.all.first())
        repeat(10) { scheduler.advance() }
        assertEquals("¡Vamos!", voice.phrases.last())

        engine.finish()
        voice.completeLatest()

        assertEquals(TrainingUiState.Home, engine.state)
        assertTrue(voice.stopCalls > 0)
    }

    private fun assertWorkout(
        engine: TrainingEngine,
        phase: TrainingPhase,
        secondsRemaining: Int,
        isPaused: Boolean
    ) {
        val state = engine.state
        assertTrue(state is TrainingUiState.Workout)
        state as TrainingUiState.Workout
        assertEquals(phase, state.phase)
        assertEquals(secondsRemaining, state.secondsRemaining)
        assertEquals(isPaused, state.isPaused)
        assertEquals(0, state.exerciseIndex)
        assertEquals(1, state.seriesNumber)
        assertEquals(1, state.repetitionNumber)
    }

    private class FakeVoiceSpeaker : VoiceSpeaker {
        override var isReady = true
        val phrases = mutableListOf<String>()
        var stopCalls = 0
        private val completions = mutableListOf<() -> Unit>()

        override fun speak(phrase: String, onCompleted: (() -> Unit)?) {
            phrases += phrase
            onCompleted?.let(completions::add)
        }

        override fun stop() {
            stopCalls += 1
        }

        fun completeLatest() {
            completions.last().invoke()
        }
    }

    private class FakeTrainingScheduler : TrainingScheduler {
        private var pendingAction: (() -> Unit)? = null

        override fun schedule(delayMillis: Long, action: () -> Unit) {
            pendingAction = action
        }

        override fun cancelAll() {
            pendingAction = null
        }

        fun advance() {
            val action = pendingAction ?: return
            pendingAction = null
            action()
        }
    }
}
