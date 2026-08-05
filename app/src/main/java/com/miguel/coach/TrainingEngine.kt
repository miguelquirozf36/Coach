package com.miguel.coach

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TrainingEngine(
    private val voiceSpeaker: VoiceSpeaker,
    private val beepPlayer: BeepSoundPlayer,
    private val scheduler: TrainingScheduler = AndroidTrainingScheduler(),
    private val monotonicClock: MonotonicClock = SystemMonotonicClock
) {
    var state: TrainingUiState by mutableStateOf(TrainingUiState.Home)
        private set

    val isVoiceReady: Boolean
        get() = voiceSpeaker.isReady

    private var sessionId = 0L

    fun start(routine: Routine) {
        if (!voiceSpeaker.isReady) return

        beginNewSession()
        val sessionRoutine = routine.copy(exercises = routine.exercises.toList())
        state = TrainingUiState.Workout(
            routine = sessionRoutine,
            exerciseIndex = 0,
            seriesNumber = 1,
            repetitionNumber = 1,
            phase = TrainingPhase.COUNTDOWN,
            secondsRemaining = INITIAL_COUNTDOWN_SECONDS,
            phaseDurationSeconds = INITIAL_COUNTDOWN_SECONDS,
            phaseStartedAtMillis = monotonicClock.nowMillis(),
            phasePausedAtMillis = null,
            isPaused = false,
            currentExerciseNotes = sessionRoutine.exercises.first().notes
        )
        voiceSpeaker.speak(START_ANNOUNCEMENT)
        scheduleCountdownTick(sessionId)
    }

    fun pause() {
        val workout = state as? TrainingUiState.Workout ?: return
        if (workout.isPaused) return

        val pausedAtMillis = monotonicClock.nowMillis()
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
        when (workout.phase) {
            TrainingPhase.CONCENTRIC -> {
                state = workout.copy(
                    phase = TrainingPhase.REPETITION_ANNOUNCEMENT,
                    secondsRemaining = 0
                )
                announceRepetition(activeSession)
            }

            TrainingPhase.REPETITION_ANNOUNCEMENT -> startEccentricPhase(activeSession)
            TrainingPhase.ECCENTRIC -> completeEccentricPhase(activeSession)
            TrainingPhase.REST -> announceNextSeries(activeSession)
            TrainingPhase.REST_BETWEEN_EXERCISES -> announceNextExercise(activeSession)
            TrainingPhase.COUNTDOWN -> Unit
        }
    }

    fun finish() {
        invalidatePendingWork()
        state = TrainingUiState.Home
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
        when (workout.phase) {
            TrainingPhase.COUNTDOWN -> {
                if (workout.secondsRemaining == 0) announceStart(activeSession)
                else scheduleCountdownTick(activeSession)
            }

            TrainingPhase.CONCENTRIC -> schedulePhaseTick(activeSession)
            TrainingPhase.REPETITION_ANNOUNCEMENT -> announceRepetition(activeSession)
            TrainingPhase.ECCENTRIC -> schedulePhaseTick(activeSession)
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
        scheduler.schedule(ONE_SECOND_MILLIS) { advanceCountdown(activeSession) }
    }

    private fun advanceCountdown(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.COUNTDOWN) return

        val secondsRemaining = (workout.secondsRemaining - 1).coerceAtLeast(0)
        state = workout.copy(secondsRemaining = secondsRemaining)
        when (secondsRemaining) {
            3 -> voiceSpeaker.speak("Tres")
            2 -> voiceSpeaker.speak("Dos")
            1 -> voiceSpeaker.speak("Uno")
            0 -> {
                announceStart(activeSession)
                return
            }
        }
        scheduleCountdownTick(activeSession)
    }

    private fun announceStart(activeSession: Long) {
        voiceSpeaker.speak("\u00A1Vamos!") { startConcentricPhase(activeSession) }
    }

    private fun startConcentricPhase(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.COUNTDOWN &&
            workout.phase != TrainingPhase.ECCENTRIC &&
            workout.phase != TrainingPhase.REST &&
            workout.phase != TrainingPhase.REST_BETWEEN_EXERCISES
        ) return

        val exercise = workout.routine.exercises[workout.exerciseIndex]
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
            startEccentricPhase(activeSession)
        }
    }

    private fun startEccentricPhase(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.REPETITION_ANNOUNCEMENT) return

        val exercise = workout.routine.exercises[workout.exerciseIndex]
        state = workout.copy(
            phase = TrainingPhase.ECCENTRIC,
            secondsRemaining = exercise.eccentricSeconds,
            phaseDurationSeconds = exercise.eccentricSeconds,
            phaseStartedAtMillis = monotonicClock.nowMillis(),
            phasePausedAtMillis = null
        )
        schedulePhaseTick(activeSession)
    }

    private fun schedulePhaseTick(activeSession: Long) {
        scheduler.schedule(ONE_SECOND_MILLIS) { advanceExercisePhase(activeSession) }
    }

    private fun advanceExercisePhase(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.CONCENTRIC && workout.phase != TrainingPhase.ECCENTRIC) return

        val secondsRemaining = (workout.secondsRemaining - 1).coerceAtLeast(0)
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
            TrainingPhase.COUNTDOWN,
            TrainingPhase.REPETITION_ANNOUNCEMENT,
            TrainingPhase.REST,
            TrainingPhase.REST_BETWEEN_EXERCISES -> Unit
        }
    }

    private fun completeEccentricPhase(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.ECCENTRIC) return

        beepPlayer.play()
        val exercise = workout.routine.exercises[workout.exerciseIndex]
        if (workout.repetitionNumber < exercise.repetitions) {
            state = workout.copy(repetitionNumber = workout.repetitionNumber + 1)
            startConcentricPhase(activeSession)
            return
        }

        if (workout.seriesNumber < exercise.sets) {
            state = workout.copy(
                phase = TrainingPhase.REST,
                secondsRemaining = exercise.restSeconds,
                phaseDurationSeconds = exercise.restSeconds,
                phaseStartedAtMillis = monotonicClock.nowMillis(),
                phasePausedAtMillis = null
            )
            voiceSpeaker.speak(REST_ANNOUNCEMENT)
            scheduleRestTick(activeSession)
            return
        }

        if (workout.exerciseIndex < workout.routine.exercises.lastIndex) {
            state = workout.copy(
                exerciseIndex = workout.exerciseIndex + 1,
                seriesNumber = 1,
                repetitionNumber = 1,
                phase = TrainingPhase.REST_BETWEEN_EXERCISES,
                secondsRemaining = workout.routine.restBetweenExercisesSeconds,
                phaseDurationSeconds = workout.routine.restBetweenExercisesSeconds,
                phaseStartedAtMillis = monotonicClock.nowMillis(),
                phasePausedAtMillis = null
            )
            voiceSpeaker.speak(REST_BETWEEN_EXERCISES_ANNOUNCEMENT)
            scheduleRestTick(activeSession)
            return
        }

        completeTraining()
    }

    private fun scheduleRestTick(activeSession: Long) {
        scheduler.schedule(ONE_SECOND_MILLIS) { advanceRest(activeSession) }
    }

    private fun advanceRest(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.REST &&
            workout.phase != TrainingPhase.REST_BETWEEN_EXERCISES
        ) return

        val secondsRemaining = (workout.secondsRemaining - 1).coerceAtLeast(0)
        state = workout.copy(secondsRemaining = secondsRemaining)
        when (secondsRemaining) {
            10 -> voiceSpeaker.speak(TEN_SECONDS_ANNOUNCEMENT)
            3 -> voiceSpeaker.speak("Tres")
            2 -> voiceSpeaker.speak("Dos")
            1 -> voiceSpeaker.speak("Uno")
            0 -> {
                when (workout.phase) {
                    TrainingPhase.REST -> announceNextSeries(activeSession)
                    TrainingPhase.REST_BETWEEN_EXERCISES -> announceNextExercise(activeSession)
                    else -> Unit
                }
                return
            }
        }
        scheduleRestTick(activeSession)
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
        if (workout.seriesNumber >= exercise.sets) return

        state = workout.copy(
            seriesNumber = workout.seriesNumber + 1,
            repetitionNumber = 1
        )
        startConcentricPhase(activeSession)
    }

    private fun startNextExercise(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.REST_BETWEEN_EXERCISES) return
        startConcentricPhase(activeSession)
    }

    private fun completeTraining() {
        invalidatePendingWork()
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
        const val REST_ANNOUNCEMENT = "Descansa."
        const val REST_BETWEEN_EXERCISES_ANNOUNCEMENT =
            "Descansa y prepárate para el siguiente ejercicio."
        const val TEN_SECONDS_ANNOUNCEMENT = "Quedan diez segundos."
        const val TRAINING_COMPLETE_ANNOUNCEMENT = "Entrenamiento finalizado."
    }
}

fun interface MonotonicClock {
    fun nowMillis(): Long
}

private object SystemMonotonicClock : MonotonicClock {
    override fun nowMillis(): Long = System.nanoTime() / 1_000_000L
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
    COUNTDOWN,
    CONCENTRIC,
    REPETITION_ANNOUNCEMENT,
    ECCENTRIC,
    REST,
    REST_BETWEEN_EXERCISES
}

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
        val currentExerciseNotes: String
    ) : TrainingUiState
}
