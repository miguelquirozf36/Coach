package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingLoadTest {
    @Test
    fun warmupWithoutHistoryPreparesSeriesOneWithoutCreatingAnotherSeries() {
        val fixture = LoadFixture(listOf(exercise("press", sets = 2)), FakeLoadHistory(), warmupSeconds = 2)

        fixture.engine.start(fixture.routine)
        assertEquals(TrainingPhase.WARMUP, fixture.workout().phase)
        assertEquals("", fixture.workout().currentLoad)
        assertNull(fixture.workout().previousLoad)
        fixture.engine.updateCurrentLoad("3 barras")
        assertTrue(fixture.workout().seriesLoads.isEmpty())
        assertEquals(1, fixture.workout().seriesNumber)
        assertEquals(2, fixture.workout().routine.exercises.single().sets)

        fixture.finishWarmup()

        assertEquals(TrainingPhase.CONCENTRIC, fixture.workout().phase)
        assertEquals(1, fixture.workout().seriesNumber)
        assertEquals("3 barras", fixture.workout().currentLoad)
        assertTrue(fixture.workout().seriesLoads.isEmpty())
    }

    @Test
    fun warmupAutofillsHistoryAndOnlyItsLatestEditReachesSeriesOne() {
        val history = FakeLoadHistory(mapOf("press" to "20 kg"))
        val fixture = LoadFixture(listOf(exercise("press")), history, warmupSeconds = 3)

        fixture.engine.start(fixture.routine)
        assertEquals("20 kg", fixture.workout().currentLoad)
        assertEquals("20 kg", fixture.workout().previousLoad)
        fixture.engine.updateCurrentLoad("22.5 kg")
        fixture.engine.updateCurrentLoad("25 kg")
        assertEquals("20 kg", fixture.workout().previousLoad)
        assertTrue(fixture.workout().seriesLoads.isEmpty())

        fixture.finishWarmup()

        assertEquals("25 kg", fixture.workout().currentLoad)
        assertTrue(fixture.workout().seriesLoads.isEmpty())
        fixture.completeRepetition()
        assertEquals(listOf(SeriesLoadRecord("press", 1, "25 kg")), history.saved.single())
    }

    @Test
    fun pausingWarmupPreservesPreparedLoadAndDeadlineFlow() {
        val fixture = LoadFixture(listOf(exercise("press")), FakeLoadHistory(), warmupSeconds = 2)
        fixture.engine.start(fixture.routine)
        fixture.engine.updateCurrentLoad("3 barras")

        fixture.engine.pause()
        assertEquals("3 barras", fixture.workout().currentLoad)
        fixture.engine.resume()
        assertEquals("3 barras", fixture.workout().currentLoad)

        fixture.finishWarmup()
        assertEquals(TrainingPhase.CONCENTRIC, fixture.workout().phase)
        assertEquals("3 barras", fixture.workout().currentLoad)
    }

    @Test
    fun startFromExerciseKeepsCountdownAndPreparedLoadWithoutWarmup() {
        val fixture = LoadFixture(
            listOf(exercise("first"), exercise("second")),
            FakeLoadHistory(mapOf("second" to "4 placas")),
            warmupSeconds = 600
        )

        fixture.engine.startFromExercise(fixture.routine, 1)

        assertEquals(TrainingPhase.COUNTDOWN, fixture.workout().phase)
        assertEquals(10, fixture.workout().secondsRemaining)
        assertEquals("4 placas", fixture.workout().currentLoad)
        assertEquals("4 placas", fixture.workout().previousLoad)
    }

    @Test
    fun previousLoadAutofillsCurrentAndRemainsStableWhileEditing() {
        val history = FakeLoadHistory(mapOf("press" to "3 barras"))
        val fixture = LoadFixture(listOf(exercise("press")), history)

        fixture.engine.start(fixture.routine)
        assertEquals("3 barras", fixture.workout().currentLoad)
        assertEquals("3 barras", fixture.workout().previousLoad)

        fixture.engine.updateCurrentLoad("4 barras")
        assertEquals("4 barras", fixture.workout().currentLoad)
        assertEquals("3 barras", fixture.workout().previousLoad)
    }

    @Test
    fun firstWorkoutStartsEmptyAndLoadEditingSurvivesPauseResume() {
        val fixture = LoadFixture(listOf(exercise("press", sets = 2)), FakeLoadHistory())
        fixture.engine.start(fixture.routine)
        assertEquals("", fixture.workout().currentLoad)
        assertNull(fixture.workout().previousLoad)

        fixture.engine.updateCurrentLoad("Banda roja")
        fixture.engine.pause()
        fixture.engine.resume()

        assertEquals("Banda roja", fixture.workout().currentLoad)
    }

    @Test
    fun completedSeriesIsFrozenAndNextSeriesInheritsEditableLoad() {
        val fixture = LoadFixture(listOf(exercise("press", sets = 2)), FakeLoadHistory())
        fixture.startConcentric()
        fixture.engine.updateCurrentLoad("3 barras")
        fixture.completeRepetition()

        assertEquals(listOf(SeriesLoadRecord("press", 1, "3 barras")), fixture.workout().seriesLoads)
        fixture.engine.updateCurrentLoad("4 barras")
        assertEquals("3 barras", fixture.workout().seriesLoads.single().load)
        fixture.startAfterRest()
        assertEquals(2, fixture.workout().seriesNumber)
        assertEquals("4 barras", fixture.workout().currentLoad)

        fixture.completeRepetition()
        assertEquals(listOf("3 barras", "4 barras"), fixture.history.saved.single().map(SeriesLoadRecord::load))
    }

    @Test
    fun oneSideAtATimeCreatesOneLoadRecordPerPhysicalSeries() {
        val unilateral = exercise("unilateral", mode = ExerciseExecutionMode.ONE_SIDE_AT_A_TIME)
        val fixture = LoadFixture(listOf(unilateral), FakeLoadHistory())
        fixture.startConcentric()
        fixture.engine.updateCurrentLoad("Nivel 7")
        fixture.completeRepetition()
        assertTrue(fixture.workout().seriesLoads.isEmpty())

        fixture.startAfterRest()
        assertEquals(ExerciseSide.LEFT, fixture.workout().currentSide)
        fixture.completeRepetition()

        assertEquals(listOf(SeriesLoadRecord("unilateral", 1, "Nivel 7")), fixture.history.saved.single())
    }

    @Test
    fun startFromIntermediateExerciseLoadsThatExerciseHistory() {
        val history = FakeLoadHistory(mapOf("second" to "4 placas"))
        val fixture = LoadFixture(listOf(exercise("first"), exercise("second")), history)

        fixture.engine.startFromExercise(fixture.routine, 1)

        assertEquals(1, fixture.workout().exerciseIndex)
        assertEquals("4 placas", fixture.workout().currentLoad)
        assertEquals("4 placas", fixture.workout().previousLoad)
    }

    @Test
    fun nextExerciseGetsItsOwnFrozenHistoricalReferenceDuringRest() {
        val history = FakeLoadHistory(mapOf("first" to "10 kg", "second" to "20 kg"))
        val fixture = LoadFixture(listOf(exercise("first"), exercise("second")), history)
        fixture.startConcentric()
        fixture.completeRepetition()

        assertEquals(1, fixture.workout().exerciseIndex)
        assertEquals("20 kg", fixture.workout().currentLoad)
        assertEquals("20 kg", fixture.workout().previousLoad)
        fixture.engine.updateCurrentLoad("25 kg")
        assertEquals("20 kg", fixture.workout().previousLoad)
    }

    @Test
    fun manualFinishDoesNotPublishPartialSessionButNormalCompletionDoes() {
        val history = FakeLoadHistory()
        val abandoned = LoadFixture(listOf(exercise("press")), history)
        abandoned.startConcentric()
        abandoned.engine.updateCurrentLoad("80 kg")
        abandoned.engine.finish()
        assertTrue(history.saved.isEmpty())

        val completed = LoadFixture(listOf(exercise("press")), history)
        completed.startConcentric()
        completed.engine.updateCurrentLoad("80 kg")
        completed.completeRepetition()
        assertEquals("80 kg", history.saved.single().single().load)
    }
}

private class FakeLoadHistory(private val previous: Map<String, String> = emptyMap()) : WorkoutLoadHistory {
    val saved = mutableListOf<List<SeriesLoadRecord>>()
    override fun previousLoad(exerciseId: String): String? = previous[exerciseId]
    override fun saveCompletedSession(seriesLoads: List<SeriesLoadRecord>): Boolean {
        saved += seriesLoads.toList()
        return true
    }
}

private class LoadFixture(
    exercises: List<Exercise>,
    val history: FakeLoadHistory,
    warmupSeconds: Int = 0
) {
    private val voice = LoadVoice()
    private val scheduler = LoadScheduler()
    val engine = TrainingEngine(voice, LoadBeep(), scheduler, MonotonicClock { scheduler.now }, history)
    val routine = Routine("routine", "Routine", false, exercises, 1, warmupSeconds)

    fun workout() = engine.state as TrainingUiState.Workout

    fun startConcentric() {
        engine.start(routine)
        repeat(10) { scheduler.advance() }
        voice.complete()
        scheduler.advance()
        assertEquals(TrainingPhase.CONCENTRIC, workout().phase)
    }

    fun completeRepetition() {
        scheduler.advance()
        voice.complete()
        scheduler.advance()
    }

    fun finishWarmup() {
        while (workout().phase == TrainingPhase.WARMUP && workout().secondsRemaining > 0) {
            scheduler.advance()
        }
        voice.complete()
        scheduler.advance()
    }

    fun startAfterRest() {
        scheduler.advance()
        voice.complete()
        scheduler.advance()
    }
}

private class LoadVoice : VoiceSpeaker {
    override val isReady = true
    private val completions = mutableListOf<() -> Unit>()
    override fun speak(phrase: String, onCompleted: (() -> Unit)?) { onCompleted?.let(completions::add) }
    override fun stop() = Unit
    fun complete() { completions.removeAt(0).invoke() }
}

private class LoadBeep : BeepSoundPlayer {
    override fun play() = Unit
    override fun stop() = Unit
}

private class LoadScheduler : TrainingScheduler {
    private var pending: Pair<Long, () -> Unit>? = null
    var now = 0L
    override fun schedule(delayMillis: Long, action: () -> Unit) { pending = delayMillis to action }
    override fun cancelAll() { pending = null }
    fun advance() {
        val action = pending ?: return
        pending = null
        now += action.first
        action.second()
    }
}

private fun exercise(
    id: String,
    sets: Int = 1,
    mode: ExerciseExecutionMode = ExerciseExecutionMode.SIMULTANEOUS
) = Exercise(id, id, sets, 1, 1, 1, 1, executionMode = mode)
