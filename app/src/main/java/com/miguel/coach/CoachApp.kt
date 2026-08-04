package com.miguel.coach

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CoachApp(trainingEngine: TrainingEngine) {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (val state = trainingEngine.state) {
                TrainingUiState.Home -> HomeScreen(
                    routines = Routines.all,
                    onStart = trainingEngine::start,
                    isVoiceReady = trainingEngine.isVoiceReady
                )

                is TrainingUiState.Workout -> WorkoutScreen(
                    state = state,
                    onPause = trainingEngine::pause,
                    onResume = trainingEngine::resume,
                    onSkip = trainingEngine::skip,
                    onFinish = trainingEngine::finish
                )

                TrainingUiState.Completed -> CompletionScreen(onFinish = trainingEngine::finish)
            }
        }
    }
}

@Composable
fun CompletionScreen(onFinish: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(WindowInsets.safeDrawing.asPaddingValues())
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Entrenamiento finalizado",
                style = MaterialTheme.typography.headlineMedium
            )
            Button(modifier = Modifier.fillMaxWidth(), onClick = onFinish) {
                Text("VOLVER AL INICIO")
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun HomeScreen(
    routines: List<Routine>,
    onStart: (Routine) -> Unit,
    isVoiceReady: Boolean
) {
    var selectedRoutineId by rememberSaveable { mutableStateOf(routines.first().id) }
    val selectedRoutine = routines.first { it.id == selectedRoutineId }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.app_name)) }) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.select_routine),
                style = MaterialTheme.typography.headlineSmall
            )
            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(routines, key = Routine::id) { routine ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedRoutineId = routine.id }
                    ) {
                        ListItem(
                            headlineContent = { Text(routine.name) },
                            supportingContent = {
                                Text(
                                    text = pluralStringResource(
                                        R.plurals.exercise_count,
                                        routine.exercises.size,
                                        routine.exercises.size
                                    )
                                )
                            },
                            trailingContent = {
                                if (routine.id == selectedRoutine.id) {
                                    Text(stringResource(R.string.routine_selected))
                                }
                            }
                        )
                    }
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = isVoiceReady,
                onClick = { onStart(selectedRoutine) }
            ) {
                Text(stringResource(R.string.start_workout))
            }
            if (!isVoiceReady) Text("Inicializando voz")
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun WorkoutScreen(
    state: TrainingUiState.Workout,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit
) {
    val exercise = state.routine.exercises[state.exerciseIndex]
    var showFinishConfirmation by rememberSaveable { mutableStateOf(false) }

    if (showFinishConfirmation) {
        AlertDialog(
            onDismissRequest = { showFinishConfirmation = false },
            title = { Text("¿Finalizar entrenamiento?") },
            text = { Text("Se perderá el progreso de la sesión actual.") },
            dismissButton = {
                TextButton(onClick = { showFinishConfirmation = false }) {
                    Text("CANCELAR")
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showFinishConfirmation = false
                    onFinish()
                }) {
                    Text("FINALIZAR")
                }
            }
        )
    }

    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.workout_title)) }) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.phase == TrainingPhase.REST_BETWEEN_EXERCISES) {
                Text(
                    text = "Descanso entre ejercicios",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text("Siguiente: ${exercise.name}")
            } else {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.headlineMedium
                )
            }
            Text(stringResource(R.string.current_series, state.seriesNumber, exercise.sets))
            Text(stringResource(R.string.current_repetition, state.repetitionNumber, exercise.repetitions))
            state.phase.label?.let { phase ->
                Text("Fase: $phase")
            }
            Text("Tiempo restante")
            Text(
                text = state.secondsRemaining.toClockFormat(),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp)
            )
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = if (state.isPaused) onResume else onPause
            ) {
                Text(if (state.isPaused) "REANUDAR" else "PAUSA")
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isPaused &&
                    state.phase != TrainingPhase.COUNTDOWN,
                onClick = onSkip
            ) {
                Text("OMITIR")
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { showFinishConfirmation = true }
            ) {
                Text(stringResource(R.string.finish_workout))
            }
        }
    }
}

private val TrainingPhase.label: String?
    get() = when (this) {
        TrainingPhase.COUNTDOWN -> null
        TrainingPhase.CONCENTRIC -> "Concéntrica"
        TrainingPhase.REPETITION_ANNOUNCEMENT -> "Concéntrica"
        TrainingPhase.ECCENTRIC -> "Excéntrica"
        TrainingPhase.REST -> "Descanso"
        TrainingPhase.REST_BETWEEN_EXERCISES -> "Descanso entre ejercicios"
    }

private fun Int.toClockFormat(): String {
    val safeSeconds = coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
