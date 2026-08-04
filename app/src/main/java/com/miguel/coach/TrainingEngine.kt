package com.miguel.coach

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TrainingEngine(
    private val voiceSpeaker: VoiceSpeaker,
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
        if (workout.isPaused) return
        invalidatePendingWork()
        state = workout.copy(isPaused = true)
    }

    fun resume() {
        val workout = state as? TrainingUiState.Workout ?: return
        if (!workout.isPaused) return

        sessionId += 1
        val activeSession = sessionId
        state = workout.copy(isPaused = false)
        if (workout.phase == TrainingPhase.COUNTDOWN) {
            if (workout.secondsRemaining == 0) announceStart(activeSession)
            else scheduleCountdownTick(activeSession)
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
    }

    private fun scheduleCountdownTick(activeSession: Long) {
        scheduler.schedule(ONE_SECOND_MILLIS) { advanceCountdown(activeSession) }
    }

    private fun advanceCountdown(activeSession: Long) {
        if (activeSession != sessionId) return
        val workout = state as? TrainingUiState.Workout ?: return
        if (workout.isPaused || workout.phase != TrainingPhase.COUNTDOWN) return

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
        voiceSpeaker.speak("¡Vamos!") { startFirstConcentricPhase(activeSession) }
    }

    private fun startFirstConcentricPhase(activeSession: Long) {
        if (activeSession != sessionId) return
        val workout = state as? TrainingUiState.Workout ?: return
        if (workout.isPaused || workout.phase != TrainingPhase.COUNTDOWN) return

        val exercise = workout.routine.exercises[workout.exerciseIndex]
        state = workout.copy(
            phase = TrainingPhase.CONCENTRIC,
            secondsRemaining = ((exercise.concentricDurationMillis + 999) / 1_000).toInt()
        )
    }

    private companion object {
        const val ONE_SECOND_MILLIS = 1_000L
        const val START_ANNOUNCEMENT = "Comenzamos en diez segundos."
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

enum class TrainingPhase { COUNTDOWN, CONCENTRIC }

sealed interface TrainingUiState {
    data object Home : TrainingUiState

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
