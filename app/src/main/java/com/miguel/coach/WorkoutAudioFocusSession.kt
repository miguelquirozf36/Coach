package com.miguel.coach

internal class WorkoutAudioFocusSession(
    private val controller: WorkoutAudioFocusController
) : VoiceUtteranceLifecycleListener {
    private var currentState: TrainingUiState = TrainingUiState.Home

    fun setEnabled(enabled: Boolean) {
        controller.setEnabled(enabled)
    }

    fun onStateChanged(state: TrainingUiState) {
        currentState = state
        controller.setContinuousDuckingDesired(shouldUseContinuousWorkoutDucking(state))
    }

    override fun onUtteranceSubmitted(utteranceId: String) {
        if (shouldUseTransientWorkoutDucking(currentState)) {
            controller.acquireTransient(utteranceId)
        }
    }

    override fun onUtteranceTerminated(utteranceId: String) {
        controller.releaseTransient(utteranceId)
    }

    fun cleanup() {
        currentState = TrainingUiState.Home
        controller.cleanup()
    }
}

internal fun shouldUseContinuousWorkoutDucking(state: TrainingUiState): Boolean {
    val workout = state as? TrainingUiState.Workout ?: return false
    if (workout.isPaused) return false
    if (workout.isInStartDelay) return true
    return when (workout.phase) {
        TrainingPhase.CONCENTRIC,
        TrainingPhase.ECCENTRIC,
        TrainingPhase.ISOMETRIC,
        TrainingPhase.REPETITION_ANNOUNCEMENT -> true
        TrainingPhase.WARMUP,
        TrainingPhase.COUNTDOWN,
        TrainingPhase.REST,
        TrainingPhase.REST_BETWEEN_EXERCISES -> false
    }
}

private fun shouldUseTransientWorkoutDucking(state: TrainingUiState): Boolean {
    val workout = state as? TrainingUiState.Workout ?: return false
    if (workout.isPaused || workout.isInStartDelay) return false
    return workout.phase == TrainingPhase.WARMUP ||
        workout.phase == TrainingPhase.COUNTDOWN ||
        workout.phase == TrainingPhase.REST ||
        workout.phase == TrainingPhase.REST_BETWEEN_EXERCISES
}
