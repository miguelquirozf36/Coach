package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingEngineTest {
    @Test
    fun repetitionRunsConcentricVoiceEccentricBeepAndTheNextRepetition() {
        val fixture = Fixture()
        fixture.startFirstConcentricPhase()

        fixture.scheduler.advance()
        fixture.assertWorkout(TrainingPhase.REPETITION_ANNOUNCEMENT, 0, 1, false)
        assertEquals("1", fixture.voice.phrases.last())

        fixture.voice.completeLatest()
        fixture.assertWorkout(TrainingPhase.ECCENTRIC, 3, 1, false)
        repeat(3) { fixture.scheduler.advance() }

        assertEquals(1, fixture.beep.playCalls)
        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 2, false)
    }

    @Test
    fun lastRepetitionBeepsAndLeavesTheSeriesReadyWithoutStartingAnotherRepetition() {
        val fixture = Fixture()
        fixture.startFirstConcentricPhase()
        val repetitions = fixture.routine.exercises.first().repetitions

        repeat(repetitions) {
            fixture.scheduler.advance()
            fixture.voice.completeLatest()
            repeat(3) { fixture.scheduler.advance() }
        }

        fixture.assertWorkout(TrainingPhase.SERIES_COMPLETE, 0, repetitions, false)
        assertEquals(repetitions, fixture.beep.playCalls)
    }

    @Test
    fun pauseFreezesThePhaseAndResumeContinuesFromTheSavedSecond() {
        val fixture = Fixture()
        fixture.startFirstConcentricPhase()

        fixture.engine.pause()
        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 1, true)
        fixture.scheduler.advance()
        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 1, true)

        fixture.engine.resume()
        fixture.scheduler.advance()
        fixture.assertWorkout(TrainingPhase.REPETITION_ANNOUNCEMENT, 0, 1, false)
        assertTrue(fixture.voice.stopCalls > 1)
    }

    @Test
    fun skipFollowsEachNaturalTransitionWithoutDuplicatingVoiceOrBeeps() {
        val fixture = Fixture()
        fixture.startFirstConcentricPhase()

        fixture.engine.skip()
        fixture.assertWorkout(TrainingPhase.REPETITION_ANNOUNCEMENT, 0, 1, false)
        assertEquals("1", fixture.voice.phrases.last())
        val voiceCount = fixture.voice.phrases.size

        fixture.engine.skip()
        fixture.assertWorkout(TrainingPhase.ECCENTRIC, 3, 1, false)
        assertEquals(voiceCount, fixture.voice.phrases.size)

        fixture.engine.skip()
        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 2, false)
        assertEquals(1, fixture.beep.playCalls)
    }

    @Test
    fun finishInvalidatesPendingVoiceCallbacksAndStopsTheBeepPlayer() {
        val fixture = Fixture()
        fixture.startFirstConcentricPhase()
        fixture.scheduler.advance()
        fixture.assertWorkout(TrainingPhase.REPETITION_ANNOUNCEMENT, 0, 1, false)

        fixture.engine.finish()
        fixture.voice.completeLatest()

        assertEquals(TrainingUiState.Home, fixture.engine.state)
        assertTrue(fixture.voice.stopCalls > 0)
        assertTrue(fixture.beep.stopCalls > 0)
    }

    private class Fixture {
        val voice = FakeVoiceSpeaker()
        val beep = FakeBeepPlayer()
        val scheduler = FakeTrainingScheduler()
        val engine = TrainingEngine(voice, beep, scheduler)
        val routine = Routines.all.first()

        fun startFirstConcentricPhase() {
            engine.start(routine)
            repeat(10) { scheduler.advance() }
            voice.completeLatest()
            assertWorkout(TrainingPhase.CONCENTRIC, 1, 1, false)
        }

        fun assertWorkout(
            phase: TrainingPhase,
            secondsRemaining: Int,
            repetitionNumber: Int,
            isPaused: Boolean
        ) {
            val state = engine.state
            assertTrue(state is TrainingUiState.Workout)
            state as TrainingUiState.Workout
            assertEquals(phase, state.phase)
            assertEquals(secondsRemaining, state.secondsRemaining)
            assertEquals(repetitionNumber, state.repetitionNumber)
            assertEquals(isPaused, state.isPaused)
            assertEquals(0, state.exerciseIndex)
            assertEquals(1, state.seriesNumber)
        }
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

    private class FakeBeepPlayer : BeepSoundPlayer {
        var playCalls = 0
        var stopCalls = 0

        override fun play() {
            playCalls += 1
        }

        override fun stop() {
            stopCalls += 1
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
