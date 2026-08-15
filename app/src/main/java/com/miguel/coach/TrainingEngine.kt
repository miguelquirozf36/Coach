package com.miguel.coach

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TrainingEngine(
    private val voiceSpeaker: VoiceSpeaker,
    private val beepPlayer: BeepSoundPlayer,
    private val scheduler: TrainingScheduler = AndroidTrainingScheduler(),
    private val monotonicClock: MonotonicClock = SystemMonotonicClock,
    private val loadHistory: WorkoutLoadHistory = NoWorkoutLoadHistory,
    private val onStateChanged: (TrainingUiState) -> Unit = {}
) {
    private var mutableState by mutableStateOf<TrainingUiState>(TrainingUiState.Home)
    var state: TrainingUiState
        get() = mutableState
        private set(value) {
            mutableState = value
            onStateChanged(value)
        }

    val isVoiceReady: Boolean
        get() = voiceSpeaker.isReady

    private var sessionId = 0L
    private val announcedThresholds = mutableSetOf<Int>()
    private var previousTimedSeconds = 0
    private var startDelayRemainingMillis: Long? = null
    private var startDelayDeadlineMillis: Long? = null

    fun start(routine: Routine) {
        startSession(routine, exerciseIndex = 0, useRoutineWarmup = true)
    }

    fun startFromExercise(routine: Routine, exerciseIndex: Int) {
        startSession(routine, exerciseIndex, useRoutineWarmup = false)
    }

    private fun startSession(routine: Routine, exerciseIndex: Int, useRoutineWarmup: Boolean) {
        if (routine.exercises.isEmpty() || exerciseIndex !in routine.exercises.indices) return
        if (!voiceSpeaker.isReady) return

        beginNewSession()
        clearStartDelay()
        val sessionRoutine = routine.copy(exercises = routine.exercises.toList())
        val hasWarmup = useRoutineWarmup && sessionRoutine.warmupSeconds > 0
        val initialExercise = sessionRoutine.exercises[exerciseIndex]
        val previousLoad = loadHistory.previousLoad(initialExercise.id)
        state = TrainingUiState.Workout(
            routine = sessionRoutine,
            exerciseIndex = exerciseIndex,
            seriesNumber = 1,
            repetitionNumber = 1,
            phase = if (hasWarmup) TrainingPhase.WARMUP else TrainingPhase.COUNTDOWN,
            secondsRemaining = if (hasWarmup) sessionRoutine.warmupSeconds else INITIAL_COUNTDOWN_SECONDS,
            phaseDurationSeconds = if (hasWarmup) sessionRoutine.warmupSeconds else INITIAL_COUNTDOWN_SECONDS,
            phaseStartedAtMillis = monotonicClock.nowMillis(),
            phasePausedAtMillis = null,
            isPaused = false,
            currentExerciseNotes = initialExercise.notes,
            currentSide = initialExercise.initialSide(),
            currentLoad = previousLoad.orEmpty(),
            previousLoad = previousLoad
        )
        resetTimedAnnouncements(if (hasWarmup) sessionRoutine.warmupSeconds else INITIAL_COUNTDOWN_SECONDS)
        if (hasWarmup) {
            voiceSpeaker.speak(WARMUP_ANNOUNCEMENT)
            scheduleWarmupTick(sessionId)
        } else {
            voiceSpeaker.speak(if (useRoutineWarmup) START_ANNOUNCEMENT else START_FROM_EXERCISE_ANNOUNCEMENT)
            scheduleCountdownTick(sessionId)
        }
    }

    fun pause() {
        var workout = state as? TrainingUiState.Workout ?: return
        if (workout.isPaused) return

        val pausedAtMillis = monotonicClock.nowMillis()
        startDelayDeadlineMillis?.let { deadline ->
            startDelayRemainingMillis = (deadline - pausedAtMillis).coerceAtLeast(0L)
            startDelayDeadlineMillis = null
        }
        if (workout.phase.usesElapsedTime) {
            workout = workout.copy(secondsRemaining = remainingSeconds(workout, pausedAtMillis))
        }
        invalidatePendingWork()
        state = workout.copy(isPaused = true, phasePausedAtMillis = pausedAtMillis)
    }

    fun resume() {
        val workout = state as? TrainingUiState.Workout ?: return
        if (!workout.isPaused) return

        val resumedAtMillis = monotonicClock.nowMillis()
        val pauseDurationMillis = (resumedAtMillis - (workout.phasePausedAtMillis ?: resumedAtMillis))
            .coerceAtLeast(0L)
        sessionId += 1
        val activeSession = sessionId
        state = workout.copy(
            isPaused = false,
            phaseStartedAtMillis = workout.phaseStartedAtMillis + pauseDurationMillis,
            phasePausedAtMillis = null
        )
        resumePhase(activeSession)
    }

    fun skip() {
        val workout = state as? TrainingUiState.Workout ?: return
        if (workout.isPaused || workout.phase == TrainingPhase.COUNTDOWN) return

        invalidatePendingWork()
        sessionId += 1
        val activeSession = sessionId
        if (startDelayRemainingMillis != null) {
            clearStartDelay()
            startConcentricPhase(activeSession)
            return
        }
        when (workout.phase) {
            TrainingPhase.WARMUP -> {
                state = workout.copy(secondsRemaining = 0)
                announceStart(activeSession)
            }
            TrainingPhase.CONCENTRIC -> {
                state = workout.copy(
                    phase = TrainingPhase.REPETITION_ANNOUNCEMENT,
                    secondsRemaining = 0
                )
                announceRepetition(activeSession)
            }

            TrainingPhase.REPETITION_ANNOUNCEMENT -> startEccentricPhase(activeSession)
            TrainingPhase.ECCENTRIC -> completeEccentricPhase(activeSession)
            TrainingPhase.ISOMETRIC -> completeIsometricPhase(activeSession)
            TrainingPhase.REST -> announceNextSeries(activeSession)
            TrainingPhase.REST_BETWEEN_EXERCISES -> announceNextExercise(activeSession)
            TrainingPhase.COUNTDOWN -> Unit
        }
    }

    fun finish() {
        invalidatePendingWork()
        clearStartDelay()
        state = TrainingUiState.Home
    }

    fun updateCurrentLoad(load: String) {
        val workout = state as? TrainingUiState.Workout ?: return
        state = workout.copy(currentLoad = load)
    }

    private fun beginNewSession() {
        invalidatePendingWork()
        sessionId += 1
    }

    private fun invalidatePendingWork() {
        sessionId += 1
        scheduler.cancelAll()
        voiceSpeaker.stop()
        beepPlayer.stop()
    }

    private fun resumePhase(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (startDelayRemainingMillis != null) {
            scheduleStartDelay(activeSession)
            return
        }
        when (workout.phase) {
            TrainingPhase.WARMUP -> {
                if (workout.secondsRemaining == 0) announceStart(activeSession)
                else scheduleWarmupTick(activeSession)
            }
            TrainingPhase.COUNTDOWN -> {
                if (workout.secondsRemaining == 0) announceStart(activeSession)
                else scheduleCountdownTick(activeSession)
            }
            TrainingPhase.CONCENTRIC -> schedulePhaseTick(activeSession)
            TrainingPhase.REPETITION_ANNOUNCEMENT -> announceRepetition(activeSession)
            TrainingPhase.ECCENTRIC -> schedulePhaseTick(activeSession)
            TrainingPhase.ISOMETRIC -> schedulePhaseTick(activeSession)
            TrainingPhase.REST -> {
                if (workout.secondsRemaining == 0) announceNextSeries(activeSession)
                else scheduleRestTick(activeSession)
            }
            TrainingPhase.REST_BETWEEN_EXERCISES -> {
                if (workout.secondsRemaining == 0) announceNextExercise(activeSession)
                else scheduleRestTick(activeSession)
            }
        }
    }

    private fun scheduleCountdownTick(activeSession: Long) {
        scheduleTimedTick(activeSession) { advanceCountdown(activeSession) }
    }

    private fun scheduleWarmupTick(activeSession: Long) {
        scheduleTimedTick(activeSession) { advanceWarmup(activeSession) }
    }

    private fun advanceWarmup(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.WARMUP) return

        val secondsRemaining = remainingSeconds(workout)
        state = workout.copy(secondsRemaining = secondsRemaining)
        if (secondsRemaining == 0) {
            announceStart(activeSession)
            return
        }
        announceCrossedThreshold(
            secondsRemaining,
            listOf(60 to ONE_MINUTE_ANNOUNCEMENT, 10 to TEN_SECONDS_ANNOUNCEMENT, 3 to "Tres", 2 to "Dos", 1 to "Uno")
        )
        previousTimedSeconds = secondsRemaining
        scheduleWarmupTick(activeSession)
    }

    private fun advanceCountdown(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.COUNTDOWN) return

        val secondsRemaining = remainingSeconds(workout)
        state = workout.copy(secondsRemaining = secondsRemaining)
        if (secondsRemaining == 0) {
            announceStart(activeSession)
            return
        }
        announceCrossedThreshold(
            secondsRemaining,
            listOf(3 to "Tres", 2 to "Dos", 1 to "Uno")
        )
        previousTimedSeconds = secondsRemaining
        scheduleCountdownTick(activeSession)
    }

    private fun announceStart(activeSession: Long) {
        voiceSpeaker.speak("\u00A1Vamos!") { startStartDelay(activeSession) }
    }

    private fun startStartDelay(activeSession: Long) {
        activeWorkout(activeSession) ?: return
        startDelayRemainingMillis = ONE_SECOND_MILLIS
        scheduleStartDelay(activeSession)
    }

    private fun scheduleStartDelay(activeSession: Long) {
        activeWorkout(activeSession) ?: return
        val delayMillis = startDelayRemainingMillis ?: return
        startDelayDeadlineMillis = monotonicClock.nowMillis() + delayMillis
        scheduler.schedule(delayMillis) {
            activeWorkout(activeSession) ?: return@schedule
            startDelayRemainingMillis = null
            startDelayDeadlineMillis = null
            startConcentricPhase(activeSession)
        }
    }

    private fun startConcentricPhase(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.COUNTDOWN &&
            workout.phase != TrainingPhase.WARMUP &&
            workout.phase != TrainingPhase.ECCENTRIC &&
            workout.phase != TrainingPhase.REST &&
            workout.phase != TrainingPhase.REST_BETWEEN_EXERCISES &&
            workout.phase != TrainingPhase.ISOMETRIC
        ) return

        val exercise = workout.routine.exercises[workout.exerciseIndex]
        beepPlayer.play()
        state = workout.copy(
            phase = TrainingPhase.CONCENTRIC,
            secondsRemaining = exercise.concentricSeconds,
            phaseDurationSeconds = exercise.concentricSeconds,
            phaseStartedAtMillis = monotonicClock.nowMillis(),
            phasePausedAtMillis = null,
            currentExerciseNotes = exercise.notes
        )
        schedulePhaseTick(activeSession)
    }

    private fun announceRepetition(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.REPETITION_ANNOUNCEMENT) return
        voiceSpeaker.speak(workout.repetitionNumber.toString()) {
            val exercise = workout.routine.exercises[workout.exerciseIndex]
            if (exercise.isometricPauseMode == IsometricPauseMode.SHORTENED) {
                startIsometricPhase(activeSession)
            } else {
                startEccentricPhase(activeSession)
            }
        }
    }

    private fun startEccentricPhase(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.REPETITION_ANNOUNCEMENT &&
            workout.phase != TrainingPhase.ISOMETRIC
        ) return

        val exercise = workout.routine.exercises[workout.exerciseIndex]
        if (workout.phase == TrainingPhase.ISOMETRIC) beepPlayer.play()
        state = workout.copy(
            phase = TrainingPhase.ECCENTRIC,
            secondsRemaining = exercise.eccentricSeconds,
            phaseDurationSeconds = exercise.eccentricSeconds,
            phaseStartedAtMillis = monotonicClock.nowMillis(),
            phasePausedAtMillis = null
        )
        schedulePhaseTick(activeSession)
    }

    private fun startIsometricPhase(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        val exercise = workout.routine.exercises[workout.exerciseIndex]
        if (exercise.isometricPauseMode == IsometricPauseMode.NONE) return
        if (exercise.isometricPauseMode == IsometricPauseMode.STRETCHED) beepPlayer.play()
        state = workout.copy(
            phase = TrainingPhase.ISOMETRIC,
            secondsRemaining = exercise.isometricDurationSeconds,
            phaseDurationSeconds = exercise.isometricDurationSeconds,
            phaseStartedAtMillis = monotonicClock.nowMillis(),
            phasePausedAtMillis = null
        )
        schedulePhaseTick(activeSession)
    }

    private fun schedulePhaseTick(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        val delay = if (workout.phase == TrainingPhase.ISOMETRIC) {
            nextTickDelayMillis(workout)
        } else {
            ONE_SECOND_MILLIS
        }
        scheduler.schedule(delay) { advanceExercisePhase(activeSession) }
    }

    private fun advanceExercisePhase(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.CONCENTRIC &&
            workout.phase != TrainingPhase.ECCENTRIC &&
            workout.phase != TrainingPhase.ISOMETRIC
        ) return

        val secondsRemaining = if (workout.phase == TrainingPhase.ISOMETRIC) {
            remainingSeconds(workout)
        } else {
            (workout.secondsRemaining - 1).coerceAtLeast(0)
        }
        state = workout.copy(secondsRemaining = secondsRemaining)
        if (secondsRemaining > 0) {
            schedulePhaseTick(activeSession)
            return
        }

        when (workout.phase) {
            TrainingPhase.CONCENTRIC -> {
                state = (state as TrainingUiState.Workout).copy(
                    phase = TrainingPhase.REPETITION_ANNOUNCEMENT,
                    secondsRemaining = 0
                )
                announceRepetition(activeSession)
            }

            TrainingPhase.ECCENTRIC -> completeEccentricPhase(activeSession)
            TrainingPhase.ISOMETRIC -> completeIsometricPhase(activeSession)
            TrainingPhase.WARMUP,
            TrainingPhase.COUNTDOWN,
            TrainingPhase.REPETITION_ANNOUNCEMENT,
            TrainingPhase.REST,
            TrainingPhase.REST_BETWEEN_EXERCISES -> Unit
        }
    }

    private fun completeIsometricPhase(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.ISOMETRIC) return
        val exercise = workout.routine.exercises[workout.exerciseIndex]
        if (exercise.isometricPauseMode == IsometricPauseMode.SHORTENED) {
            startEccentricPhase(activeSession)
        } else {
            completeRepetition(activeSession)
        }
    }

    private fun completeEccentricPhase(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.ECCENTRIC) return

        val exercise = workout.routine.exercises[workout.exerciseIndex]
        if (exercise.isometricPauseMode == IsometricPauseMode.STRETCHED) {
            startIsometricPhase(activeSession)
            return
        }
        completeRepetition(activeSession)
    }

    private fun completeRepetition(activeSession: Long) {
        var workout = activeWorkout(activeSession) ?: return
        val exercise = workout.routine.exercises[workout.exerciseIndex]
        if (workout.repetitionNumber < exercise.repetitions) {
            state = workout.copy(repetitionNumber = workout.repetitionNumber + 1)
            startConcentricPhase(activeSession)
            return
        }

        if (workout.currentSide != ExerciseSide.RIGHT) {
            workout = workout.withConsolidatedCurrentSeries(exercise.id)
            state = workout
        }

        val hasAnotherExecution = workout.currentSide == ExerciseSide.RIGHT ||
            workout.seriesNumber < exercise.sets
        if (hasAnotherExecution) {
            state = workout.copy(
                phase = TrainingPhase.REST,
                secondsRemaining = exercise.restSeconds,
                phaseDurationSeconds = exercise.restSeconds,
                phaseStartedAtMillis = monotonicClock.nowMillis(),
                phasePausedAtMillis = null
            )
            voiceSpeaker.speak(REST_ANNOUNCEMENT)
            resetTimedAnnouncements(exercise.restSeconds)
            scheduleRestTick(activeSession)
            return
        }

        if (workout.exerciseIndex < workout.routine.exercises.lastIndex) {
            val nextExerciseIndex = workout.exerciseIndex + 1
            val nextExercise = workout.routine.exercises[nextExerciseIndex]
            val nextExerciseName = nextExercise.name
            val previousLoad = loadHistory.previousLoad(nextExercise.id)
            state = workout.copy(
                exerciseIndex = nextExerciseIndex,
                seriesNumber = 1,
                repetitionNumber = 1,
                currentSide = workout.routine.exercises[nextExerciseIndex].initialSide(),
                phase = TrainingPhase.REST_BETWEEN_EXERCISES,
                secondsRemaining = workout.routine.restBetweenExercisesSeconds,
                phaseDurationSeconds = workout.routine.restBetweenExercisesSeconds,
                phaseStartedAtMillis = monotonicClock.nowMillis(),
                phasePausedAtMillis = null,
                currentLoad = previousLoad.orEmpty(),
                previousLoad = previousLoad
            )
            voiceSpeaker.speak("$REST_BETWEEN_EXERCISES_ANNOUNCEMENT $nextExerciseName.")
            resetTimedAnnouncements(workout.routine.restBetweenExercisesSeconds)
            scheduleRestTick(activeSession)
            return
        }

        completeTraining()
    }

    private fun scheduleRestTick(activeSession: Long) {
        scheduleTimedTick(activeSession) { advanceRest(activeSession) }
    }

    private fun scheduleTimedTick(activeSession: Long, action: () -> Unit) {
        val workout = activeWorkout(activeSession) ?: return
        scheduler.schedule(nextTickDelayMillis(workout)) { action() }
    }

    private fun advanceRest(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.REST &&
            workout.phase != TrainingPhase.REST_BETWEEN_EXERCISES
        ) return

        val secondsRemaining = remainingSeconds(workout)
        state = workout.copy(secondsRemaining = secondsRemaining)
        if (secondsRemaining == 0) {
            when (workout.phase) {
                TrainingPhase.REST -> announceNextSeries(activeSession)
                TrainingPhase.REST_BETWEEN_EXERCISES -> announceNextExercise(activeSession)
                else -> Unit
            }
            return
        }
        announceCrossedThreshold(
            secondsRemaining,
            listOf(30 to THIRTY_SECONDS_ANNOUNCEMENT, 10 to TEN_SECONDS_ANNOUNCEMENT, 3 to "Tres", 2 to "Dos", 1 to "Uno")
        )
        previousTimedSeconds = secondsRemaining
        scheduleRestTick(activeSession)
    }

    private fun resetTimedAnnouncements(durationSeconds: Int) {
        announcedThresholds.clear()
        previousTimedSeconds = durationSeconds + 1
    }

    private fun announceCrossedThreshold(
        secondsRemaining: Int,
        thresholds: List<Pair<Int, String>>
    ) {
        val useful = thresholds
            .filter { (threshold, _) ->
                threshold <= previousTimedSeconds &&
                    threshold >= secondsRemaining &&
                    threshold - secondsRemaining <= MAX_USEFUL_ANNOUNCEMENT_LATENESS_SECONDS &&
                    threshold !in announcedThresholds
            }
            .minByOrNull { (threshold, _) -> threshold - secondsRemaining }
            ?: return
        announcedThresholds += useful.first
        voiceSpeaker.speak(useful.second)
    }

    private fun remainingSeconds(
        workout: TrainingUiState.Workout,
        nowMillis: Long = monotonicClock.nowMillis()
    ): Int {
        val endMillis = workout.phaseStartedAtMillis +
            workout.phaseDurationSeconds.coerceAtLeast(0) * ONE_SECOND_MILLIS
        val remainingMillis = (endMillis - nowMillis).coerceAtLeast(0L)
        return ((remainingMillis + ONE_SECOND_MILLIS - 1) / ONE_SECOND_MILLIS).toInt()
    }

    private fun nextTickDelayMillis(workout: TrainingUiState.Workout): Long {
        val endMillis = workout.phaseStartedAtMillis +
            workout.phaseDurationSeconds.coerceAtLeast(0) * ONE_SECOND_MILLIS
        val remainingMillis = (endMillis - monotonicClock.nowMillis()).coerceAtLeast(0L)
        if (remainingMillis == 0L) return 0L
        return (remainingMillis % ONE_SECOND_MILLIS).takeIf { it > 0L }
            ?: ONE_SECOND_MILLIS
    }

    private fun announceNextSeries(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.REST) return
        voiceSpeaker.speak("\u00A1Vamos!") { startNextSeries(activeSession) }
    }

    private fun announceNextExercise(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.REST_BETWEEN_EXERCISES) return
        voiceSpeaker.speak("\u00A1Vamos!") { startNextExercise(activeSession) }
    }

    private fun startNextSeries(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.REST) return
        val exercise = workout.routine.exercises[workout.exerciseIndex]
        val nextSide = when (workout.currentSide) {
            ExerciseSide.RIGHT -> ExerciseSide.LEFT
            ExerciseSide.LEFT -> ExerciseSide.RIGHT
            null -> null
        }
        val nextSeries = if (workout.currentSide == ExerciseSide.RIGHT) {
            workout.seriesNumber
        } else {
            workout.seriesNumber + 1
        }
        if (nextSeries > exercise.sets) return
        state = workout.copy(
            seriesNumber = nextSeries,
            currentSide = nextSide,
            repetitionNumber = 1
        )
        startStartDelay(activeSession)
    }

    private fun startNextExercise(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.REST_BETWEEN_EXERCISES) return
        startStartDelay(activeSession)
    }

    private fun clearStartDelay() {
        startDelayRemainingMillis = null
        startDelayDeadlineMillis = null
    }

    private fun completeTraining() {
        val workout = state as? TrainingUiState.Workout ?: return
        invalidatePendingWork()
        loadHistory.saveCompletedSession(workout.seriesLoads)
        state = TrainingUiState.Completed
        voiceSpeaker.speak(TRAINING_COMPLETE_ANNOUNCEMENT)
    }

    private fun activeWorkout(activeSession: Long): TrainingUiState.Workout? {
        if (activeSession != sessionId) return null
        val workout = state as? TrainingUiState.Workout ?: return null
        return workout.takeUnless { it.isPaused }
    }

    private companion object {
        const val ONE_SECOND_MILLIS = 1_000L
        const val INITIAL_COUNTDOWN_SECONDS = 10
        const val START_ANNOUNCEMENT = "Comenzamos en diez segundos."
        const val START_FROM_EXERCISE_ANNOUNCEMENT = "Comenzamos en 10 segundos."
        const val WARMUP_ANNOUNCEMENT = "Comienza el calentamiento."
        const val ONE_MINUTE_ANNOUNCEMENT = "Queda 1 minuto"
        const val THIRTY_SECONDS_ANNOUNCEMENT = "Quedan 30 segundos"
        const val REST_ANNOUNCEMENT = "Descansa."
        const val REST_BETWEEN_EXERCISES_ANNOUNCEMENT =
            "Descansa y prepárate para el siguiente ejercicio."
        const val TEN_SECONDS_ANNOUNCEMENT = "Quedan 10 segundos"
        const val MAX_USEFUL_ANNOUNCEMENT_LATENESS_SECONDS = 1
        const val TRAINING_COMPLETE_ANNOUNCEMENT = "Entrenamiento finalizado."
    }
}

fun interface MonotonicClock {
    fun nowMillis(): Long
}

private object SystemMonotonicClock : MonotonicClock {
    override fun nowMillis(): Long = SystemClock.elapsedRealtime()
}

interface TrainingScheduler {
    fun schedule(delayMillis: Long, action: () -> Unit)
    fun cancelAll()
}

class AndroidTrainingScheduler : TrainingScheduler {
    private val handler = Handler(Looper.getMainLooper())
    private var pendingAction: Runnable? = null

    override fun schedule(delayMillis: Long, action: () -> Unit) {
        cancelAll()
        val runnable = Runnable { pendingAction = null; action() }
        pendingAction = runnable
        handler.postDelayed(runnable, delayMillis)
    }

    override fun cancelAll() {
        pendingAction?.let(handler::removeCallbacks)
        pendingAction = null
    }
}

enum class TrainingPhase {
    WARMUP,
    COUNTDOWN,
    CONCENTRIC,
    REPETITION_ANNOUNCEMENT,
    ECCENTRIC,
    ISOMETRIC,
    REST,
    REST_BETWEEN_EXERCISES
}

private val TrainingPhase.usesElapsedTime: Boolean
    get() = this == TrainingPhase.WARMUP ||
        this == TrainingPhase.COUNTDOWN ||
        this == TrainingPhase.REST ||
        this == TrainingPhase.REST_BETWEEN_EXERCISES ||
        this == TrainingPhase.ISOMETRIC

sealed interface TrainingUiState {
    data object Home : TrainingUiState
    data object Completed : TrainingUiState

    data class Workout(
        val routine: Routine,
        val exerciseIndex: Int,
        val seriesNumber: Int,
        val repetitionNumber: Int,
        val phase: TrainingPhase,
        val secondsRemaining: Int,
        val phaseDurationSeconds: Int,
        val phaseStartedAtMillis: Long,
        val phasePausedAtMillis: Long?,
        val isPaused: Boolean,
        val currentExerciseNotes: String,
        val currentSide: ExerciseSide? = null,
        val currentLoad: String = "",
        val previousLoad: String? = null,
        val seriesLoads: List<SeriesLoadRecord> = emptyList()
    ) : TrainingUiState
}

private fun TrainingUiState.Workout.withConsolidatedCurrentSeries(exerciseId: String): TrainingUiState.Workout {
    val record = SeriesLoadRecord(exerciseId, seriesNumber, currentLoad)
    return copy(seriesLoads = seriesLoads.filterNot {
        it.exerciseId == exerciseId && it.seriesNumber == seriesNumber
    } + record)
}

private fun Exercise.initialSide(): ExerciseSide? =
    if (executionMode == ExerciseExecutionMode.ONE_SIDE_AT_A_TIME) ExerciseSide.RIGHT else null

internal fun ExerciseSide?.displayLabel(): String? = when (this) {
    ExerciseSide.RIGHT -> "Lado derecho"
    ExerciseSide.LEFT -> "Lado izquierdo"
    null -> null
}
