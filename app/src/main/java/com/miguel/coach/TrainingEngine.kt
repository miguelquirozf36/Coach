package com.miguel.coach

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TrainingEngine(
    private val voiceSpeaker: VoiceSpeaker,
    private val beepPlayer: BeepSoundPlayer,
    private val scheduler: TrainingScheduler = AndroidTrainingScheduler()
) {
    var state: TrainingUiState by mutableStateOf(TrainingUiState.Home)
        private set

    val isVoiceReady: Boolean
        get() = voiceSpeaker.isReady

    private var sessionId = 0L

    fun start(routine: Routine) {
        if (!voiceSpeaker.isReady) return

        beginNewSession()
        state = TrainingUiState.Workout(routine, 0, 1, 1, TrainingPhase.COUNTDOWN, 10, false)
        voiceSpeaker.speak(START_ANNOUNCEMENT)
        scheduleCountdownTick(sessionId)
    }

    fun pause() {
        val workout = state as? TrainingUiState.Workout ?: return
        if (workout.isPaused || workout.phase == TrainingPhase.SERIES_COMPLETE) return

        invalidatePendingWork()
        state = workout.copy(isPaused = true)
    }

    fun resume() {
        val workout = state as? TrainingUiState.Workout ?: return
        if (!workout.isPaused) return

        sessionId += 1
        val activeSession = sessionId
        state = workout.copy(isPaused = false)
        resumePhase(activeSession)
    }

    fun skip() {
        val workout = state as? TrainingUiState.Workout ?: return
        if (workout.isPaused || workout.phase == TrainingPhase.COUNTDOWN ||
            workout.phase == TrainingPhase.SERIES_COMPLETE
        ) return

        invalidatePendingWork()
        sessionId += 1
        val activeSession = sessionId
        when (workout.phase) {
            TrainingPhase.CONCENTRIC -> {
                state = workout.copy(phase = TrainingPhase.REPETITION_ANNOUNCEMENT, secondsRemaining = 0)
                announceRepetition(activeSession)
            }

            TrainingPhase.REPETITION_ANNOUNCEMENT -> startEccentricPhase(activeSession)
            TrainingPhase.ECCENTRIC -> completeEccentricPhase(activeSession)
            TrainingPhase.REST -> announceNextSeries(activeSession)
            TrainingPhase.COUNTDOWN,
            TrainingPhase.SERIES_COMPLETE -> Unit
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
            TrainingPhase.SERIES_COMPLETE -> Unit
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
            workout.phase != TrainingPhase.REST
        ) return

        val exercise = workout.routine.exercises[workout.exerciseIndex]
        state = workout.copy(
            phase = TrainingPhase.CONCENTRIC,
            secondsRemaining = secondsFor(exercise.concentricDurationMillis)
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
            secondsRemaining = secondsFor(exercise.eccentricDurationMillis)
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
            TrainingPhase.SERIES_COMPLETE -> Unit
        }
    }

    private fun completeEccentricPhase(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.ECCENTRIC) return

        beepPlayer.play()
        val exercise = workout.routine.exercises[workout.exerciseIndex]
        if (workout.repetitionNumber != exercise.repetitions) {
            state = workout.copy(repetitionNumber = workout.repetitionNumber + 1)
            startConcentricPhase(activeSession)
            return
        }

        if (workout.seriesNumber != exercise.sets) {
            state = workout.copy(
                phase = TrainingPhase.REST,
                secondsRemaining = secondsFor(exercise.restDurationMillis)
            )
            voiceSpeaker.speak(REST_ANNOUNCEMENT)
            scheduleRestTick(activeSession)
            return
        }

        if (workout.exerciseIndex != workout.routine.exercises.lastIndex) {
            state = workout.copy(
                exerciseIndex = workout.exerciseIndex + 1,
                seriesNumber = 1,
                repetitionNumber = 1
            )
            startConcentricPhase(activeSession)
            return
        }

        completeTraining()
    }

    private fun scheduleRestTick(activeSession: Long) {
        scheduler.schedule(ONE_SECOND_MILLIS) { advanceRest(activeSession) }
    }

    private fun advanceRest(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.REST) return

        val secondsRemaining = (workout.secondsRemaining - 1).coerceAtLeast(0)
        state = workout.copy(secondsRemaining = secondsRemaining)
        when (secondsRemaining) {
            3 -> voiceSpeaker.speak("Tres")
            2 -> voiceSpeaker.speak("Dos")
            1 -> voiceSpeaker.speak("Uno")
            0 -> {
                announceNextSeries(activeSession)
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

    private fun startNextSeries(activeSession: Long) {
        val workout = activeWorkout(activeSession) ?: return
        if (workout.phase != TrainingPhase.REST) return
        val exercise = workout.routine.exercises[workout.exerciseIndex]
        if (workout.seriesNumber == exercise.sets) return

        state = workout.copy(
            seriesNumber = workout.seriesNumber + 1,
            repetitionNumber = 1
        )
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

    private fun secondsFor(durationMillis: Long): Int = ((durationMillis + 999) / 1_000).toInt()

    private companion object {
        const val ONE_SECOND_MILLIS = 1_000L
        const val START_ANNOUNCEMENT = "Comenzamos en diez segundos."
        const val REST_ANNOUNCEMENT = "Descansa."
        const val TRAINING_COMPLETE_ANNOUNCEMENT = "Entrenamiento finalizado."
    }
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
    SERIES_COMPLETE
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
        val isPaused: Boolean
    ) : TrainingUiState
}
