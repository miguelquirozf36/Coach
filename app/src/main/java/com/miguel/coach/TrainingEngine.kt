package com.miguel.coach

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TrainingEngine {
    var state: TrainingUiState by mutableStateOf(TrainingUiState.Home)
        private set

    fun start(routine: Routine) {
        state = TrainingUiState.Workout(
            routine = routine,
            exerciseIndex = 0,
            seriesNumber = 1,
            repetitionNumber = 1
        )
    }

    fun finish() {
        state = TrainingUiState.Home
    }
}

sealed interface TrainingUiState {
    data object Home : TrainingUiState

    data class Workout(
        val routine: Routine,
        val exerciseIndex: Int,
        val seriesNumber: Int,
        val repetitionNumber: Int
    ) : TrainingUiState
}
