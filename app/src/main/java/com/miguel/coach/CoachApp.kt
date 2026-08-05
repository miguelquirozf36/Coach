package com.miguel.coach

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.activity.compose.BackHandler
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LocalSystemBackAction = compositionLocalOf<((() -> Unit)?) -> Unit> {
    error("System back action registrar is not available")
}

internal enum class SystemBackOutcome { CLOSE_DIALOG, SHOW_CONFIRMATION, NAVIGATE_BACK }

internal fun editorSystemBackOutcome(hasChanges: Boolean, dialogOpen: Boolean): SystemBackOutcome = when {
    dialogOpen -> SystemBackOutcome.CLOSE_DIALOG
    hasChanges -> SystemBackOutcome.SHOW_CONFIRMATION
    else -> SystemBackOutcome.NAVIGATE_BACK
}

internal fun workoutSystemBackOutcome(dialogOpen: Boolean): SystemBackOutcome =
    if (dialogOpen) SystemBackOutcome.CLOSE_DIALOG else SystemBackOutcome.SHOW_CONFIRMATION

@Composable
private fun SystemBackHost(content: @Composable () -> Unit) {
    var currentAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    BackHandler(enabled = currentAction != null) { currentAction?.invoke() }
    androidx.compose.runtime.CompositionLocalProvider(
        LocalSystemBackAction provides { action -> currentAction = action },
        content = content
    )
}

@Composable
internal fun RegisterSystemBackAction(action: () -> Unit) {
    val register = LocalSystemBackAction.current
    val currentAction by androidx.compose.runtime.rememberUpdatedState(action)
    val stableAction = remember { { currentAction() } }
    DisposableEffect(register, stableAction) {
        register(stableAction)
        onDispose { register(null) }
    }
}

@Composable
fun CoachApp(trainingEngine: TrainingEngine, routineRepository: RoutineRepository) {
    val applicationContext = LocalContext.current.applicationContext
    val themeRepository = remember(applicationContext) {
        ThemePreferenceRepository(
            SharedPreferencesThemeStorage(
                applicationContext.getSharedPreferences("coach_appearance", Context.MODE_PRIVATE)
            )
        )
    }
    val userPreferenceRepository = remember(applicationContext) {
        UserPreferenceRepository(
            SharedPreferencesUserStorage(
                applicationContext.getSharedPreferences("coach_user", Context.MODE_PRIVATE)
            )
        )
    }
    var selectedThemeId by rememberSaveable { mutableStateOf(themeRepository.load().id) }
    var userName by rememberSaveable { mutableStateOf(userPreferenceRepository.loadUserName()) }
    val selectedTheme = remember(selectedThemeId) { CoachTheme.fromId(selectedThemeId) }
    var routines by remember { mutableStateOf<List<Routine>>(emptyList()) }
    var customExercises by remember { mutableStateOf<List<ExerciseDefinition>>(emptyList()) }
    var isLoadingRoutines by remember { mutableStateOf(true) }
    var selectedRoutineId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showAppearance by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(routineRepository) {
        routines = routineRepository.load()
        customExercises = routineRepository.loadCustomExercises()
        ExerciseLibrary.replaceCustom(customExercises)
        isLoadingRoutines = false
    }

    CoachTheme(selectedTheme) {
        SystemBackHost {
            Surface(modifier = Modifier.fillMaxSize()) {
                when (val state = trainingEngine.state) {
                TrainingUiState.Home -> {
                    if (showAppearance) {
                        AppearanceScreen(
                            selectedTheme = selectedTheme,
                            onBack = { showAppearance = false },
                            onThemeSelected = { theme ->
                                selectedThemeId = theme.id
                                themeRepository.save(theme)
                            }
                        )
                        return@Surface
                    }
                    if (isLoadingRoutines) {
                        LoadingRoutinesScreen()
                        return@Surface
                    }
                    val selectedRoutine = selectedRoutineId?.let { id ->
                        routines.firstOrNull { it.id == id }
                    }
                    if (selectedRoutine == null) {
                        if (selectedTab == 2) {
                            SettingsScreen(
                                userName = userName,
                                currentTheme = selectedTheme,
                                onSaveUserName = { input ->
                                    val validation = userPreferenceRepository.saveUserName(input)
                                    validation.value?.let { userName = it }
                                    validation.message
                                },
                                onAppearance = { showAppearance = true },
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it }
                            )
                        } else {
                            HomeScreen(
                                routines = routines.filter { it.isCustom == (selectedTab == 1) },
                                isCustomTab = selectedTab == 1,
                                userName = userName,
                                onOpen = { selectedRoutineId = it.id },
                                onCreate = {
                                    val newRoutine = emptyCustomRoutine("custom-${System.nanoTime()}")
                                    val updated = routines + newRoutine
                                    if (routineRepository.save(updated)) {
                                        routines = updated
                                        selectedRoutineId = newRoutine.id
                                    }
                                },
                                onDelete = { routine ->
                                    val updated = routines.filterNot { it.id == routine.id }
                                    if (routineRepository.save(updated)) routines = updated
                                },
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it }
                            )
                        }
                    } else {
                        RoutineDetailScreen(
                            routine = selectedRoutine,
                            onBack = { selectedRoutineId = null },
                            onStart = trainingEngine::start,
                            isVoiceReady = trainingEngine.isVoiceReady,
                            customExercises = customExercises,
                            onSaveCustomExercise = { id, name, category, notes ->
                                val result = if (id == null) {
                                    routineRepository.createCustomExercise(name, category, notes)
                                } else {
                                    routineRepository.editCustomExercise(id, name, category, notes)
                                }
                                if (result.success) {
                                    customExercises = result.exercises
                                    ExerciseLibrary.replaceCustom(result.exercises)
                                }
                                result.message
                            },
                            onDeleteCustomExercise = { definition ->
                                val result = routineRepository.deleteCustomExercise(definition.id, routines)
                                if (result.success) {
                                    customExercises = result.exercises
                                    ExerciseLibrary.replaceCustom(result.exercises)
                                }
                                result.message
                            },
                            onSave = { updatedRoutine ->
                                val updatedRoutines = routines.map { routine ->
                                    if (routine.id == updatedRoutine.id) updatedRoutine else routine
                                }
                                if (routineRepository.save(updatedRoutines)) {
                                    routines = updatedRoutines
                                    true
                                } else {
                                    false
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
}

@Composable
private fun LoadingRoutinesScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Cargando rutinas...")
    }
}

@Composable
fun CompletionScreen(onFinish: () -> Unit) {
    RegisterSystemBackAction(onFinish)
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
    isCustomTab: Boolean,
    userName: String,
    onOpen: (Routine) -> Unit,
    onCreate: () -> Unit,
    onDelete: (Routine) -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    var routinePendingDeletion by remember { mutableStateOf<Routine?>(null) }
    routinePendingDeletion?.let { routine ->
        AlertDialog(
            onDismissRequest = { routinePendingDeletion = null },
            title = { Text("¿Eliminar rutina?") },
            text = { Text("Esta acción no se puede deshacer.") },
            dismissButton = { TextButton(onClick = { routinePendingDeletion = null }) { Text("CANCELAR") } },
            confirmButton = { TextButton(onClick = { onDelete(routine); routinePendingDeletion = null }) { Text("ELIMINAR") } }
        )
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isCustomTab) "PERSONALIZADO" else stringResource(R.string.app_name)) }
            )
        },
        bottomBar = { MainNavigationBar(selectedTab, onTabSelected) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (isCustomTab) {
                Text("Crea, edita o inicia tus rutinas.", style = MaterialTheme.typography.headlineSmall)
            } else {
                homeGreeting(userName).forEachIndexed { index, line ->
                    Text(
                        text = line,
                        style = if (index == 0 && userName.isNotEmpty()) {
                            MaterialTheme.typography.headlineMedium
                        } else {
                            MaterialTheme.typography.headlineSmall
                        }
                    )
                }
            }
            if (isCustomTab && routines.isEmpty()) {
                Text("Crea tu primera rutina personalizada.")
                Button(modifier = Modifier.fillMaxWidth(), onClick = onCreate) { Text("+ CREAR RUTINA") }
            } else LazyColumn(
                modifier = Modifier.weight(1f),
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
                                Text(
                                    "${routine.exercises.size} ejercicios · " +
                                        "${routine.estimatedDurationMinutes()} min"
                                )
                            },
                            trailingContent = { Text("VER") }
                        )
                        if (isCustomTab) {
                            TextButton(onClick = { routinePendingDeletion = routine }) { Text("ELIMINAR") }
                        }
                    }
                }
            }
            if (isCustomTab && routines.isNotEmpty()) {
                Button(modifier = Modifier.fillMaxWidth(), onClick = onCreate) { Text("CREAR RUTINA") }
            }
        }
    }
}

@Composable
internal fun MainNavigationBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            icon = { Text("MI RUTINA", style = MaterialTheme.typography.labelMedium) }
        )
        NavigationBarItem(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            icon = { Text("PERSONALIZADO", style = MaterialTheme.typography.labelMedium) }
        )
        NavigationBarItem(
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            icon = { Text("AJUSTES", style = MaterialTheme.typography.labelMedium) }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RoutineDetailScreen(
    routine: Routine,
    onBack: () -> Unit,
    onStart: (Routine) -> Unit,
    isVoiceReady: Boolean,
    customExercises: List<ExerciseDefinition>,
    onSaveCustomExercise: (String?, String, String, String) -> String?,
    onDeleteCustomExercise: (ExerciseDefinition) -> String?,
    onSave: (Routine) -> Boolean
) {
    var isEditing by rememberSaveable(routine.id) { mutableStateOf(false) }
    var draft by remember(routine.id) { mutableStateOf(routine.toDraft()) }
    var validationMessage by remember(routine.id) { mutableStateOf<String?>(null) }

    if (!isEditing) RegisterSystemBackAction(onBack)

    if (isEditing) {
        val originalDraft = remember(routine) { routine.toDraft() }
        RoutineEditorScreen(
            draft = draft,
            originalDraft = originalDraft,
            customExercises = customExercises,
            onSaveCustomExercise = onSaveCustomExercise,
            onDeleteCustomExercise = onDeleteCustomExercise,
            validationMessage = validationMessage,
            onDraftChange = { draft = it },
            onSave = {
                val validation = draft.validate(routine.isCustom)
                if (validation.routine == null) {
                    validationMessage = validation.message
                } else if (onSave(validation.routine)) {
                    validationMessage = null
                    isEditing = false
                } else {
                    validationMessage = "No se pudo guardar la rutina. Inténtalo de nuevo."
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
            Text("Descanso entre ejercicios: ${routine.restBetweenExercisesSeconds.toClockFormat()} min")
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
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("${exercise.sets} series · ${exercise.repetitions} repeticiones")
            Text("Concéntrica: ${exercise.concentricSeconds.toClockFormat()} · Excéntrica: ${exercise.eccentricSeconds.toClockFormat()}")
            Text("Descanso entre series: ${exercise.restSeconds.toClockFormat()} min")
            if (exercise.notes.isNotBlank()) {
                Text(
                    text = exercise.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RoutineEditorScreen(
    draft: RoutineDraft,
    originalDraft: RoutineDraft,
    customExercises: List<ExerciseDefinition>,
    onSaveCustomExercise: (String?, String, String, String) -> String?,
    onDeleteCustomExercise: (ExerciseDefinition) -> String?,
    validationMessage: String?,
    onDraftChange: (RoutineDraft) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    var expandedExerciseId by rememberSaveable(draft.id) { mutableStateOf<String?>(null) }
    var showSaveDialog by rememberSaveable(draft.id) { mutableStateOf(false) }
    var exerciseBeingSelectedId by rememberSaveable(draft.id) { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    val requestExit = {
        if (draft.hasChangesFrom(originalDraft)) showSaveDialog = true else onCancel()
    }

    exerciseBeingSelectedId?.let { exerciseId ->
        ExercisePickerScreen(
            customExercises = customExercises,
            onBack = { exerciseBeingSelectedId = null },
            onSaveCustomExercise = onSaveCustomExercise,
            onDeleteCustomExercise = onDeleteCustomExercise,
            onSelect = { definition ->
                draft.exercises.firstOrNull { it.id == exerciseId }?.let { exercise ->
                    onDraftChange(draft.updateExercise(selectExerciseDefinition(exercise, definition)))
                }
                exerciseBeingSelectedId = null
            }
        )
        return
    }

    RegisterSystemBackAction {
        when (editorSystemBackOutcome(draft.hasChangesFrom(originalDraft), showSaveDialog)) {
            SystemBackOutcome.CLOSE_DIALOG -> showSaveDialog = false
            SystemBackOutcome.SHOW_CONFIRMATION -> showSaveDialog = true
            SystemBackOutcome.NAVIGATE_BACK -> onCancel()
        }
    }
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Guardar cambios") },
            text = { Text("¿Deseas guardar los cambios antes de salir?") },
            dismissButton = {
                Row {
                    TextButton(onClick = { showSaveDialog = false }) { Text("CANCELAR") }
                    TextButton(onClick = {
                        showSaveDialog = false
                        onCancel()
                    }) { Text("DESCARTAR") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSaveDialog = false
                    onSave()
                }) { Text("GUARDAR") }
            }
        )
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Editar rutina") },
            navigationIcon = {
                IconButton(
                    onClick = requestExit,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = ArrowBackIcon,
                        contentDescription = "Volver",
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            actions = { TextButton(onClick = onSave) { Text("GUARDAR") } }
        )
    }) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                DraftTextField(
                    value = draft.warmupMinutes,
                    label = "Calentamiento (minutos)",
                    keyboardType = KeyboardType.Number,
                    onValueChange = { onDraftChange(draft.copy(warmupMinutes = it)) }
                )
            }
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
                    label = "Descanso entre ejercicios (minutos)",
                    keyboardType = KeyboardType.Number,
                    onValueChange = { onDraftChange(draft.copy(restBetweenExercisesSeconds = it)) }
                )
            }
            itemsIndexed(draft.exercises, key = { _, exercise -> exercise.id }) { index, exerciseDraft ->
                ExerciseEditor(
                    exercise = exerciseDraft,
                    expanded = expandedExerciseId == exerciseDraft.id,
                    onToggle = {
                        expandedExerciseId = toggledExpandedExercise(expandedExerciseId, exerciseDraft.id)
                    },
                    onSelectExercise = { exerciseBeingSelectedId = exerciseDraft.id },
                    onChange = { updatedExercise ->
                        onDraftChange(draft.updateExercise(updatedExercise))
                    }
                )
                if (expandedExerciseId == exerciseDraft.id) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                    TextButton(
                        modifier = Modifier.weight(1f),
                        enabled = index > 0,
                        onClick = { onDraftChange(draft.moveExercise(exerciseDraft.id, -1)) }
                    ) { Text("SUBIR") }
                    TextButton(
                        modifier = Modifier.weight(1f),
                        enabled = index < draft.exercises.lastIndex,
                        onClick = { onDraftChange(draft.moveExercise(exerciseDraft.id, 1)) }
                    ) { Text("BAJAR") }
                    }
                    TextButton(onClick = {
                        expandedExerciseId = null
                        onDraftChange(draft.removeExercise(exerciseDraft.id))
                    }) { Text("ELIMINAR EJERCICIO") }
                }
            }
            item(key = "add-exercise") {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        val id = "${draft.id}-exercise-${System.nanoTime()}"
                        onDraftChange(draft.addExercise(emptyCustomExercise(id).toDraft()))
                        expandedExerciseId = id
                    }
                ) { Text("AÑADIR EJERCICIO") }
            }
            validationMessage?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

private val ArrowBackIcon: ImageVector = ImageVector.Builder(
    name = "ArrowBack",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(20f, 11f)
        horizontalLineTo(7.83f)
        lineTo(13.42f, 5.41f)
        lineTo(12f, 4f)
        lineTo(4f, 12f)
        lineTo(12f, 20f)
        lineTo(13.41f, 18.59f)
        lineTo(7.83f, 13f)
        horizontalLineTo(20f)
        close()
    }
}.build()

@Composable
private fun ExerciseEditor(
    exercise: ExerciseDraft,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelectExercise: () -> Unit,
    onChange: (ExerciseDraft) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle)) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("✏ Editar", color = MaterialTheme.colorScheme.primary)
            if (expanded) {
                Text("Nombre del ejercicio", style = MaterialTheme.typography.labelLarge)
                OutlinedButton(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = onSelectExercise
                ) {
                    Text(
                        if (exercise.name == "Nuevo ejercicio" || exercise.name.isBlank()) {
                            "Seleccionar ejercicio"
                        } else {
                            exercise.name
                        }
                    )
                }
                DraftTextField(exercise.sets, "Series", KeyboardType.Number, { onChange(exercise.copy(sets = it)) })
                DraftTextField(exercise.repetitions, "Repeticiones", KeyboardType.Number, { onChange(exercise.copy(repetitions = it)) })
                DraftTextField(exercise.concentricSeconds, "Tiempo concéntrico (segundos)", KeyboardType.Number, { onChange(exercise.copy(concentricSeconds = it)) })
                DraftTextField(exercise.eccentricSeconds, "Tiempo excéntrico (segundos)", KeyboardType.Number, { onChange(exercise.copy(eccentricSeconds = it)) })
                DraftTextField(exercise.restSeconds, "Descanso entre series (minutos)", KeyboardType.Number, { onChange(exercise.copy(restSeconds = it)) })
                OutlinedTextField(
                    value = exercise.notes,
                    onValueChange = {
                        if (it.length <= MAX_EXERCISE_NOTES_LENGTH) {
                            onChange(exercise.copy(notes = it))
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notas (opcional)") },
                    minLines = 3,
                    maxLines = 5,
                    supportingText = {
                        Text("${exercise.notes.length}/$MAX_EXERCISE_NOTES_LENGTH")
                    }
                )
            }
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

    RegisterSystemBackAction {
        when (workoutSystemBackOutcome(showFinishConfirmation)) {
            SystemBackOutcome.CLOSE_DIALOG -> showFinishConfirmation = false
            SystemBackOutcome.SHOW_CONFIRMATION -> showFinishConfirmation = true
            SystemBackOutcome.NAVIGATE_BACK -> Unit
        }
    }

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
            if (state.phase == TrainingPhase.WARMUP) {
                Text(
                    text = state.routine.name,
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "Calentamiento",
                    style = MaterialTheme.typography.titleLarge
                )
            } else if (state.phase == TrainingPhase.REST_BETWEEN_EXERCISES) {
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
            if (state.phase != TrainingPhase.WARMUP && state.currentExerciseNotes.isNotBlank()) {
                Text(
                    text = state.currentExerciseNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (state.phase != TrainingPhase.WARMUP) {
                Text(stringResource(R.string.current_series, state.seriesNumber, exercise.sets))
                Text(stringResource(R.string.current_repetition, state.repetitionNumber, exercise.repetitions))
                state.phase.label?.let { phase ->
                    Text("Fase: $phase")
                }
            }
            Text("Tiempo restante")
            TrainingTimer(state)
            Spacer(modifier = Modifier.weight(1f))
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

@Composable
private fun TrainingTimer(state: TrainingUiState.Workout) {
    val timerText = state.secondsRemaining.toClockFormat()
    var frameTimeMillis by remember(state.phaseStartedAtMillis, state.phasePausedAtMillis) {
        mutableStateOf(state.phasePausedAtMillis ?: state.phaseStartedAtMillis)
    }
    LaunchedEffect(state.phaseStartedAtMillis, state.phasePausedAtMillis, state.isPaused) {
        if (state.isPaused) {
            frameTimeMillis = state.phasePausedAtMillis ?: frameTimeMillis
        } else {
            while (true) {
                frameTimeMillis = withFrameNanos { it / 1_000_000L }
            }
        }
    }
    val effectiveTimeMillis = state.phasePausedAtMillis ?: frameTimeMillis
    val durationMillis = state.phaseDurationSeconds.coerceAtLeast(0) * 1_000L
    val elapsedMillis = (effectiveTimeMillis - state.phaseStartedAtMillis).coerceAtLeast(0L)
    val progress = if (durationMillis > 0) {
        ((durationMillis - elapsedMillis).toFloat() / durationMillis).coerceIn(0f, 1f)
    } else 0f
    val targetProgressColor = trainingRingColor(state.phase, LocalTrainingRingColors.current)
    val progressColor by animateColorAsState(
        targetValue = targetProgressColor,
        animationSpec = tween(durationMillis = 250),
        label = "trainingRingColor"
    )
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(240.dp)) {
            val stroke = Stroke(width = 14.dp.toPx())
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = stroke
            )
        }
        Text(
            text = timerText,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp)
        )
    }
}

private val TrainingPhase.label: String?
    get() = when (this) {
        TrainingPhase.WARMUP -> "Calentamiento"
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
