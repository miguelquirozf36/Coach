package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingEngineTest {
    @Test
    fun lastRepetitionOfASeriesBeepsAnnouncesRestAndStartsTheRestTimer() {
        val fixture = Fixture(seriesExercise(sets = 2, restMillis = 4_000))
        fixture.startFirstConcentricPhase()

        fixture.completeCurrentRepetition()

        fixture.assertWorkout(TrainingPhase.REST, 4, 0, 1, 1, false)
        assertEquals("Descansa.", fixture.voice.phrases.last())
        assertEquals(1, fixture.beep.playCalls)
    }

    @Test
    fun restCountsDownWithTheLastThreeAnnouncementsAndStartsTheNextSeriesAfterVamos() {
        val fixture = Fixture(seriesExercise(sets = 2, restMillis = 4_000))
        fixture.startFirstConcentricPhase()
        fixture.completeCurrentRepetition()

        fixture.scheduler.advance()
        fixture.assertWorkout(TrainingPhase.REST, 3, 0, 1, 1, false)
        assertEquals("Tres", fixture.voice.phrases.last())
        fixture.scheduler.advance()
        assertEquals("Dos", fixture.voice.phrases.last())
        fixture.scheduler.advance()
        assertEquals("Uno", fixture.voice.phrases.last())
        fixture.scheduler.advance()
        assertEquals("\u00A1Vamos!", fixture.voice.phrases.last())

        fixture.voice.completeLatest()

        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 0, 2, 1, false)
    }

    @Test
    fun lastSeriesStartsAnExerciseRestBeforeTheNextExerciseAndThenCompletesTheWorkout() {
        val fixture = Fixture(
            listOf(
                seriesExercise(sets = 1, restMillis = 4_000),
                seriesExercise(sets = 1, restMillis = 4_000)
            )
        )
        fixture.startFirstConcentricPhase()

        fixture.completeCurrentRepetition()
        fixture.assertWorkout(TrainingPhase.REST_BETWEEN_EXERCISES, 12, 1, 1, 1, false)
        assertEquals("Descansa y prepárate para el siguiente ejercicio.", fixture.voice.phrases.last())

        fixture.engine.skip()
        assertEquals("\u00A1Vamos!", fixture.voice.phrases.last())
        fixture.voice.completeLatest()
        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 1, 1, 1, false)

        fixture.completeCurrentRepetition()

        assertEquals(TrainingUiState.Completed, fixture.engine.state)
        assertEquals("Entrenamiento finalizado.", fixture.voice.phrases.last())
        assertEquals(2, fixture.beep.playCalls)
    }

    @Test
    fun pauseAndResumePreserveTheExactRestSecond() {
        val fixture = Fixture(seriesExercise(sets = 2, restMillis = 4_000))
        fixture.startFirstConcentricPhase()
        fixture.completeCurrentRepetition()

        fixture.engine.pause()
        fixture.assertWorkout(TrainingPhase.REST, 4, 0, 1, 1, true)
        fixture.scheduler.advance()
        fixture.assertWorkout(TrainingPhase.REST, 4, 0, 1, 1, true)

        fixture.engine.resume()
        fixture.scheduler.advance()

        fixture.assertWorkout(TrainingPhase.REST, 3, 0, 1, 1, false)
        assertTrue(fixture.voice.stopCalls > 1)
    }

    @Test
    fun skipAdvancesThroughRestAndExerciseTransitionsWithoutDuplicatingAnnouncements() {
        val fixture = Fixture(seriesExercise(sets = 2, restMillis = 4_000))
        fixture.startFirstConcentricPhase()
        fixture.completeCurrentRepetition()

        fixture.engine.skip()
        assertEquals("\u00A1Vamos!", fixture.voice.phrases.last())
        fixture.voice.completeLatest()
        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 0, 2, 1, false)

        fixture.engine.skip()
        fixture.assertWorkout(TrainingPhase.REPETITION_ANNOUNCEMENT, 0, 0, 2, 1, false)
        assertEquals("1", fixture.voice.phrases.last())
        val phraseCount = fixture.voice.phrases.size

        fixture.engine.skip()
        fixture.assertWorkout(TrainingPhase.ECCENTRIC, 1, 0, 2, 1, false)
        assertEquals(phraseCount, fixture.voice.phrases.size)
        fixture.engine.skip()

        assertEquals(TrainingUiState.Completed, fixture.engine.state)
        assertEquals(2, fixture.beep.playCalls)
    }

    @Test
    fun finishInvalidatesPendingVoiceCallbacksTimersAndBeeps() {
        val fixture = Fixture(seriesExercise(sets = 2, restMillis = 4_000))
        fixture.startFirstConcentricPhase()
        fixture.scheduler.advance()
        fixture.assertWorkout(TrainingPhase.REPETITION_ANNOUNCEMENT, 0, 0, 1, 1, false)

        fixture.engine.finish()
        fixture.voice.completeLatest()
        fixture.scheduler.advance()

        assertEquals(TrainingUiState.Home, fixture.engine.state)
        assertTrue(fixture.voice.stopCalls > 0)
        assertTrue(fixture.beep.stopCalls > 0)
    }

    @Test
    fun pauseAtThreeTwoOneAndZeroInvalidatesOldTimersAndResumesCoherently() {
        listOf(3, 2, 1, 0).forEach { seconds ->
            val fixture = Fixture(seriesExercise(sets = 2, restMillis = 4_000))
            fixture.engine.start(fixture.routine)
            repeat(10 - seconds) { fixture.scheduler.advance() }
            fixture.assertWorkout(TrainingPhase.COUNTDOWN, seconds, 0, 1, 1, false)

            fixture.engine.pause()
            fixture.scheduler.advanceCancelled()
            fixture.assertWorkout(TrainingPhase.COUNTDOWN, seconds, 0, 1, 1, true)

            fixture.engine.resume()
            if (seconds == 0) {
                assertEquals("\u00A1Vamos!", fixture.voice.phrases.last())
                fixture.voice.completeLatest()
                fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 0, 1, 1, false)
            } else {
                fixture.scheduler.advance()
                fixture.assertWorkout(TrainingPhase.COUNTDOWN, seconds - 1, 0, 1, 1, false)
            }
            assertEquals(0, fixture.scheduler.overlappingScheduleRequests)
        }
    }

    @Test
    fun resumeDuringRepetitionAnnouncementReplaysOnlyTheCurrentAnnouncement() {
        val fixture = Fixture(seriesExercise(sets = 2, restMillis = 4_000))
        fixture.startFirstConcentricPhase()
        fixture.scheduler.advance()
        val phrasesBeforePause = fixture.voice.phrases.size

        fixture.engine.pause()
        fixture.voice.completeLatest()
        fixture.assertWorkout(TrainingPhase.REPETITION_ANNOUNCEMENT, 0, 0, 1, 1, true)

        fixture.engine.resume()

        assertEquals(phrasesBeforePause + 1, fixture.voice.phrases.size)
        assertEquals("1", fixture.voice.phrases.last())
        fixture.voice.completeLatest()
        fixture.assertWorkout(TrainingPhase.ECCENTRIC, 1, 0, 1, 1, false)
    }

    @Test
    fun finishDuringVamosPreventsAnyLaterPhaseTransition() {
        val fixture = Fixture(seriesExercise(sets = 2, restMillis = 4_000))
        fixture.engine.start(fixture.routine)
        repeat(10) { fixture.scheduler.advance() }
        assertEquals("\u00A1Vamos!", fixture.voice.phrases.last())

        fixture.engine.finish()
        fixture.voice.completeLatest()
        fixture.scheduler.advanceCancelled()

        assertEquals(TrainingUiState.Home, fixture.engine.state)
        assertEquals(0, fixture.beep.playCalls)
    }

    @Test
    fun restAnnouncesTenSecondsOnlyOnceEvenWhenPausedAndResumed() {
        val fixture = Fixture(seriesExercise(sets = 2, restMillis = 12_000))
        fixture.startFirstConcentricPhase()
        fixture.completeCurrentRepetition()

        repeat(2) { fixture.scheduler.advance() }
        fixture.assertWorkout(TrainingPhase.REST, 10, 0, 1, 1, false)
        assertEquals(1, fixture.voice.phrases.count { it == "Quedan diez segundos." })

        fixture.engine.pause()
        fixture.engine.resume()
        fixture.scheduler.advance()

        fixture.assertWorkout(TrainingPhase.REST, 9, 0, 1, 1, false)
        assertEquals(1, fixture.voice.phrases.count { it == "Quedan diez segundos." })
    }

    @Test
    fun exerciseRestShowsTheNextExerciseAndStartsItAfterTheCountdown() {
        val fixture = Fixture(
            listOf(
                seriesExercise(sets = 1, restMillis = 4_000),
                seriesExercise(sets = 1, restMillis = 4_000)
            )
        )
        fixture.startFirstConcentricPhase()
        fixture.completeCurrentRepetition()

        fixture.assertWorkout(TrainingPhase.REST_BETWEEN_EXERCISES, 12, 1, 1, 1, false)
        repeat(2) { fixture.scheduler.advance() }
        assertEquals("Quedan diez segundos.", fixture.voice.phrases.last())
        repeat(10) { fixture.scheduler.advance() }
        assertEquals("\u00A1Vamos!", fixture.voice.phrases.last())

        fixture.voice.completeLatest()

        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 1, 1, 1, false)
    }

    @Test
    fun pauseResumeAndSkipWorkDuringTheExerciseRest() {
        val fixture = Fixture(
            listOf(
                seriesExercise(sets = 1, restMillis = 4_000),
                seriesExercise(sets = 1, restMillis = 4_000)
            )
        )
        fixture.startFirstConcentricPhase()
        fixture.completeCurrentRepetition()

        fixture.engine.pause()
        fixture.assertWorkout(TrainingPhase.REST_BETWEEN_EXERCISES, 12, 1, 1, 1, true)
        fixture.scheduler.advanceCancelled()
        fixture.assertWorkout(TrainingPhase.REST_BETWEEN_EXERCISES, 12, 1, 1, 1, true)

        fixture.engine.resume()
        fixture.scheduler.advance()
        fixture.assertWorkout(TrainingPhase.REST_BETWEEN_EXERCISES, 11, 1, 1, 1, false)

        fixture.engine.skip()
        assertEquals("\u00A1Vamos!", fixture.voice.phrases.last())
        fixture.voice.completeLatest()
        fixture.assertWorkout(TrainingPhase.CONCENTRIC, 1, 1, 1, 1, false)
    }

    @Test
    fun cancelingFinishConfirmationLeavesTheEngineUntouchedAndConfirmingFinishesIt() {
        val fixture = Fixture(seriesExercise(sets = 2, restMillis = 4_000))
        fixture.startFirstConcentricPhase()
        val stateBeforeConfirmation = fixture.engine.state

        assertEquals(stateBeforeConfirmation, fixture.engine.state)

        fixture.engine.finish()

        assertEquals(TrainingUiState.Home, fixture.engine.state)
        assertTrue(fixture.voice.stopCalls > 0)
        assertTrue(fixture.beep.stopCalls > 0)
    }

    private class Fixture(
        exercises: List<Exercise>,
        private val restBetweenExercisesMillis: Long = 12_000
    ) {
        constructor(exercise: Exercise) : this(listOf(exercise))

        val voice = FakeVoiceSpeaker()
        val beep = FakeBeepPlayer()
        val scheduler = FakeTrainingScheduler()
        val engine = TrainingEngine(voice, beep, scheduler)
        val routine = Routine(
            id = "test",
            nameRes = R.string.routine_full_body,
            exercises = exercises,
            restBetweenExercisesMillis = restBetweenExercisesMillis
        )

        fun startFirstConcentricPhase() {
            engine.start(routine)
            repeat(10) { scheduler.advance() }
            voice.completeLatest()
            assertWorkout(TrainingPhase.CONCENTRIC, 1, 0, 1, 1, false)
        }

        fun completeCurrentRepetition() {
            scheduler.advance()
            assertWorkout(TrainingPhase.REPETITION_ANNOUNCEMENT, 0, currentExerciseIndex(), currentSeries(), currentRepetition(), false)
            voice.completeLatest()
            repeat(1) { scheduler.advance() }
        }

        fun assertWorkout(
            phase: TrainingPhase,
            secondsRemaining: Int,
            exerciseIndex: Int,
            seriesNumber: Int,
            repetitionNumber: Int,
            isPaused: Boolean
        ) {
            val state = engine.state
            assertTrue(state is TrainingUiState.Workout)
            state as TrainingUiState.Workout
            assertEquals(phase, state.phase)
            assertEquals(secondsRemaining, state.secondsRemaining)
            assertEquals(exerciseIndex, state.exerciseIndex)
            assertEquals(seriesNumber, state.seriesNumber)
            assertEquals(repetitionNumber, state.repetitionNumber)
            assertEquals(isPaused, state.isPaused)
        }

        private fun currentExerciseIndex(): Int = (engine.state as TrainingUiState.Workout).exerciseIndex
        private fun currentSeries(): Int = (engine.state as TrainingUiState.Workout).seriesNumber
        private fun currentRepetition(): Int = (engine.state as TrainingUiState.Workout).repetitionNumber
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
        private var cancelledAction: (() -> Unit)? = null
        var overlappingScheduleRequests = 0
            private set

        override fun schedule(delayMillis: Long, action: () -> Unit) {
            if (pendingAction != null) {
                overlappingScheduleRequests += 1
            }
            pendingAction = action
        }

        override fun cancelAll() {
            cancelledAction = pendingAction
            pendingAction = null
        }

        fun advance() {
            val action = pendingAction ?: return
            pendingAction = null
            action()
        }

        fun advanceCancelled() {
            val action = cancelledAction ?: return
            cancelledAction = null
            action()
        }
    }

    private companion object {
        fun seriesExercise(sets: Int, restMillis: Long) = Exercise(
            nameRes = R.string.exercise_squat,
            sets = sets,
            repetitions = 1,
            concentricDurationMillis = 1_000,
            eccentricDurationMillis = 1_000,
            restDurationMillis = restMillis
        )
    }
}
