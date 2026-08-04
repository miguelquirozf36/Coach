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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun CoachApp(trainingEngine: TrainingEngine) {
    var routines by remember { mutableStateOf(Routines.all) }
    var selectedRoutineId by rememberSaveable { mutableStateOf<String?>(null) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (val state = trainingEngine.state) {
                TrainingUiState.Home -> {
                    val selectedRoutine = selectedRoutineId?.let { id ->
                        routines.firstOrNull { it.id == id }
                    }
                    if (selectedRoutine == null) {
                        HomeScreen(routines = routines, onOpen = { selectedRoutineId = it.id })
                    } else {
                        RoutineDetailScreen(
                            routine = selectedRoutine,
                            onBack = { selectedRoutineId = null },
                            onStart = trainingEngine::start,
                            isVoiceReady = trainingEngine.isVoiceReady,
                            onSave = { updatedRoutine ->
                                routines = routines.map { routine ->
                                    if (routine.id == updatedRoutine.id) updatedRoutine else routine
                                }
                            }
                        )
                    }
                }

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
    onOpen: (Routine) -> Unit
) {
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
                            .clickable { onOpen(routine) }
                    ) {
                        ListItem(
                            headlineContent = { Text(routine.name) },
                            supportingContent = {
                                Text("${routine.exercises.size} ejercicios")
                            },
                            trailingContent = { Text("VER") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RoutineDetailScreen(
    routine: Routine,
    onBack: () -> Unit,
    onStart: (Routine) -> Unit,
    isVoiceReady: Boolean,
    onSave: (Routine) -> Unit
) {
    var isEditing by rememberSaveable(routine.id) { mutableStateOf(false) }
    var draft by remember(routine.id) { mutableStateOf(routine.toDraft()) }
    var validationMessage by remember(routine.id) { mutableStateOf<String?>(null) }

    if (isEditing) {
        RoutineEditorScreen(
            draft = draft,
            validationMessage = validationMessage,
            onDraftChange = { draft = it },
            onSave = {
                val validation = draft.validate(routine.isCustom)
                if (validation.routine == null) {
                    validationMessage = validation.message
                } else {
                    onSave(validation.routine)
                    validationMessage = null
                    isEditing = false
                }
            },
            onCancel = {
                draft = routine.toDraft()
                validationMessage = null
                isEditing = false
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle de rutina") },
                navigationIcon = { TextButton(onClick = onBack) { Text("VOLVER") } }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(routine.name, style = MaterialTheme.typography.headlineSmall)
            Text("Descanso entre ejercicios: ${routine.restBetweenExercisesSeconds.toClockFormat()}")
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(routine.exercises, key = Exercise::id) { exercise ->
                    ExerciseSummary(exercise)
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = isVoiceReady,
                onClick = { onStart(routine) }
            ) {
                Text("COMENZAR")
            }
            if (!isVoiceReady) Text("Inicializando voz")
            TextButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    draft = routine.toDraft()
                    validationMessage = null
                    isEditing = true
                }
            ) {
                Text("EDITAR")
            }
        }
    }
}

@Composable
private fun ExerciseSummary(exercise: Exercise) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(exercise.name, style = MaterialTheme.typography.titleMedium)
            Text("${exercise.sets} series · ${exercise.repetitions} repeticiones")
            Text("Concéntrica: ${exercise.concentricSeconds.toClockFormat()} · Excéntrica: ${exercise.eccentricSeconds.toClockFormat()}")
            Text("Descanso: ${exercise.restSeconds.toClockFormat()}")
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RoutineEditorScreen(
    draft: RoutineDraft,
    validationMessage: String?,
    onDraftChange: (RoutineDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("Editar rutina") }) }) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DraftTextField(
                    value = draft.name,
                    label = "Nombre de la rutina",
                    onValueChange = { onDraftChange(draft.copy(name = it)) }
                )
            }
            item {
                DraftTextField(
                    value = draft.restBetweenExercisesSeconds,
                    label = "Descanso entre ejercicios (segundos)",
                    keyboardType = KeyboardType.Number,
                    onValueChange = { onDraftChange(draft.copy(restBetweenExercisesSeconds = it)) }
                )
            }
            items(draft.exercises, key = ExerciseDraft::id) { exerciseDraft ->
                ExerciseEditor(
                    exercise = exerciseDraft,
                    onChange = { updatedExercise ->
                        onDraftChange(
                            draft.copy(
                                exercises = draft.exercises.map { exercise ->
                                    if (exercise.id == updatedExercise.id) updatedExercise else exercise
                                }
                            )
                        )
                    }
                )
            }
            validationMessage?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
            item {
                Button(modifier = Modifier.fillMaxWidth(), onClick = onSave) { Text("GUARDAR") }
            }
            item {
                TextButton(modifier = Modifier.fillMaxWidth(), onClick = onCancel) { Text("CANCELAR") }
            }
        }
    }
}

@Composable
private fun ExerciseEditor(exercise: ExerciseDraft, onChange: (ExerciseDraft) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Ejercicio", style = MaterialTheme.typography.titleMedium)
            DraftTextField(exercise.name, "Nombre", onValueChange = { onChange(exercise.copy(name = it)) })
            DraftTextField(exercise.sets, "Series", KeyboardType.Number, { onChange(exercise.copy(sets = it)) })
            DraftTextField(exercise.repetitions, "Repeticiones", KeyboardType.Number, { onChange(exercise.copy(repetitions = it)) })
            DraftTextField(exercise.concentricSeconds, "Tiempo concéntrico (segundos)", KeyboardType.Number, { onChange(exercise.copy(concentricSeconds = it)) })
            DraftTextField(exercise.eccentricSeconds, "Tiempo excéntrico (segundos)", KeyboardType.Number, { onChange(exercise.copy(eccentricSeconds = it)) })
            DraftTextField(exercise.restSeconds, "Descanso entre series (segundos)", KeyboardType.Number, { onChange(exercise.copy(restSeconds = it)) })
        }
    }
}

@Composable
private fun DraftTextField(
    value: String,
    label: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
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
