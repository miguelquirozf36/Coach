package com.miguel.coach

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.min
import java.time.LocalDate
import java.util.Locale

internal const val BRANDED_LAUNCH_DURATION_MILLIS = 1_000L

internal enum class LaunchStage { INITIALIZING, WELCOME, BRANDED, CONTENT }

internal fun launchStageFor(
    onboardingPending: Boolean,
    restoredStage: LaunchStage = LaunchStage.INITIALIZING
): LaunchStage = when {
    restoredStage == LaunchStage.BRANDED -> LaunchStage.CONTENT
    restoredStage != LaunchStage.INITIALIZING -> restoredStage
    onboardingPending -> LaunchStage.WELCOME
    else -> LaunchStage.BRANDED
}

internal fun launchStageAfterBranded(): LaunchStage = LaunchStage.CONTENT

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
fun CoachApp(
    trainingEngine: TrainingEngine,
    routineRepository: RoutineRepository,
    onStartWorkout: (Routine) -> Unit = trainingEngine::start,
    onStartWorkoutFromExercise: (Routine, Int) -> Unit = trainingEngine::startFromExercise,
    onFinishWorkout: () -> Unit = trainingEngine::finish
) {
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
    val programRepository = remember(applicationContext) {
        TrainingProgramRepository(
            SharedPreferencesTrainingProgramStorage(
                applicationContext.getSharedPreferences("coach_programs", Context.MODE_PRIVATE)
            )
        )
    }
    var selectedThemeId by rememberSaveable { mutableStateOf(themeRepository.load().id) }
    var userName by rememberSaveable { mutableStateOf(userPreferenceRepository.loadUserName()) }
    var beepVolumeLevel by rememberSaveable { mutableStateOf(userPreferenceRepository.loadBeepVolumeLevel()) }
    var launchStage by rememberSaveable { mutableStateOf(LaunchStage.INITIALIZING) }
    var showGreeting by rememberSaveable { mutableStateOf(false) }
    var tourStep by rememberSaveable { mutableStateOf<TourStep?>(null) }
    var tourTargets by remember { mutableStateOf<Map<TourTarget, Rect>>(emptyMap()) }
    val selectedTheme = remember(selectedThemeId) { CoachTheme.fromId(selectedThemeId) }
    var routines by remember { mutableStateOf<List<Routine>>(emptyList()) }
    var programs by remember { mutableStateOf<List<TrainingProgram>>(emptyList()) }
    var selectedProgramId by rememberSaveable { mutableStateOf<String?>(null) }
    var openedProgramId by rememberSaveable { mutableStateOf<String?>(null) }
    var customExercises by remember { mutableStateOf<List<ExerciseDefinition>>(emptyList()) }
    var isLoadingRoutines by remember { mutableStateOf(true) }
    var selectedRoutineId by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var showAppearance by rememberSaveable { mutableStateOf(false) }
    var backupMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingBackupImport by remember { mutableStateOf<CoachBackupDocument?>(null) }
    val backupManager = remember(routineRepository, userPreferenceRepository, themeRepository, programRepository) {
        CoachBackupManager(routineRepository, userPreferenceRepository, themeRepository, programRepository)
    }
    val exportBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) {
            backupMessage = "Exportación cancelada."
        } else {
            backupMessage = runCatching {
                val json = CoachBackupCodec.encode(backupManager.createDocument())
                applicationContext.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                    it.write(json)
                } ?: error("No se pudo abrir el documento.")
                "Copia exportada correctamente."
            }.getOrElse { "No se pudo exportar la copia." }
        }
    }
    val importBackupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) {
            backupMessage = "Importación cancelada."
        } else {
            val parsed = runCatching {
                val json = applicationContext.contentResolver.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: error("No se pudo abrir el documento.")
                CoachBackupCodec.decode(json)
            }.getOrElse { CoachBackupResult(false, "No se pudo leer la copia seleccionada.") }
            val validated = parsed.document?.let(backupManager::validate) ?: parsed
            if (validated.success) {
                pendingBackupImport = validated.document
            } else {
                backupMessage = validated.message
            }
        }
    }

    LaunchedEffect(routineRepository, programRepository) {
        val existingInstallation = routineRepository.hasStoredRoutines() ||
            programRepository.hasStoredPrograms() || programRepository.loadSelectedProgramId() != null
        val onboardingPending = userPreferenceRepository.initializeOnboarding(existingInstallation) &&
            userPreferenceRepository.loadUserName().isBlank()
        launchStage = launchStageFor(onboardingPending, launchStage)
        val legacyRoutines = routineRepository.load()
        programs = programRepository.loadPrograms(legacyRoutines, existingInstallation)
        selectedProgramId = programRepository.loadSelectedProgramId()
        routines = programs.firstOrNull { it.id == selectedProgramId }?.routines.orEmpty()
        customExercises = routineRepository.loadCustomExercises()
        ExerciseLibrary.replaceCustom(customExercises)
        isLoadingRoutines = false
    }

    CoachTheme(selectedTheme) {
        SystemBackHost {
            Surface(modifier = Modifier.fillMaxSize()) {
                when (launchStage) {
                    LaunchStage.INITIALIZING -> return@Surface
                    LaunchStage.WELCOME -> {
                        WelcomeScreen { input ->
                            val validation = userPreferenceRepository.saveUserName(input)
                            validation.value?.let {
                                userName = it
                                launchStage = LaunchStage.CONTENT
                                showGreeting = true
                            }
                            validation.message
                        }
                        return@Surface
                    }
                    LaunchStage.BRANDED -> {
                        BrandedLaunchScreen()
                        LaunchedEffect(launchStage) {
                            kotlinx.coroutines.delay(BRANDED_LAUNCH_DURATION_MILLIS)
                            launchStage = launchStageAfterBranded()
                        }
                        return@Surface
                    }
                    LaunchStage.CONTENT -> Unit
                }
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
                    if (showGreeting) {
                        GreetingScreen(userName) { showGreeting = false }
                        return@Surface
                    }
                    if (shouldShowProgramOnboarding(selectedProgramId)) {
                        ProgramOnboardingScreen(programs) { program ->
                            if (programRepository.selectProgram(program.id)) {
                                selectedProgramId = program.id
                                routines = program.routines
                                selectedTab = 0
                                if (!userPreferenceRepository.isTourCompleted()) tourStep = TourStep.TRAIN
                            }
                        }
                        return@Surface
                    }
                    val activeProgram = activeTrainingProgram(programs, selectedProgramId) ?: return@Surface
                    val selectedRoutine = selectedRoutineId?.let { id ->
                        programs.asSequence().flatMap { it.routines.asSequence() }.firstOrNull { it.id == id }
                    }
                    if (selectedRoutine == null) {
                        val openedProgram = openedProgramId?.let { id -> programs.firstOrNull { it.id == id } }
                        if (openedProgram != null) {
                            ProgramDetailScreen(
                                program = openedProgram,
                                active = openedProgram.id == selectedProgramId,
                                onBack = { openedProgramId = null },
                                onUse = {
                                    if (programRepository.selectProgram(openedProgram.id)) {
                                        selectedProgramId = openedProgram.id
                                        routines = openedProgram.routines
                                        openedProgramId = null
                                        selectedTab = 0
                                    }
                                },
                                onOpenRoutine = { selectedRoutineId = it.id },
                                onRename = if (openedProgram.builtIn) null else {{ name ->
                                    val updated = openedProgram.copy(name = name)
                                    val next = programs.map { if (it.id == updated.id) updated else it }
                                    if (programRepository.savePrograms(next)) programs = next
                                }},
                                onAddDay = if (openedProgram.builtIn) null else {{
                                    val newDay = emptyCustomRoutine("${openedProgram.id}-day-${System.nanoTime()}")
                                    val updated = openedProgram.copy(
                                        routines = openedProgram.routines + newDay,
                                        frequency = "${openedProgram.routines.size + 1} días"
                                    )
                                    val next = programs.map { if (it.id == updated.id) updated else it }
                                    if (programRepository.savePrograms(next)) { programs = next; openedProgramId = updated.id }
                                }},
                                onDeleteDay = if (openedProgram.builtIn) null else {{ day ->
                                    val updated = openedProgram.copy(
                                        routines = openedProgram.routines.filterNot { it.id == day.id },
                                        frequency = "${openedProgram.routines.size - 1} días"
                                    )
                                    val next = programs.map { if (it.id == updated.id) updated else it }
                                    if (programRepository.savePrograms(next)) programs = next
                                }},
                                onDeleteProgram = if (openedProgram.builtIn) null else {{
                                    val next = programs.filterNot { it.id == openedProgram.id }
                                    if (programRepository.savePrograms(next)) { programs = next; openedProgramId = null }
                                }}
                            )
                        } else if (selectedTab == 2) {
                            SettingsScreen(
                                userName = userName,
                                currentTheme = selectedTheme,
                                beepVolumeLevel = beepVolumeLevel,
                                onBeepVolumeLevelChanged = { level ->
                                    if (userPreferenceRepository.saveBeepVolumeLevel(level)) {
                                        beepVolumeLevel = normalizeBeepVolumeLevel(level)
                                    }
                                },
                                onSaveUserName = { input ->
                                    val validation = userPreferenceRepository.saveUserName(input)
                                    validation.value?.let { userName = it }
                                    validation.message
                                },
                                onAppearance = { showAppearance = true },
                                onExportBackup = {
                                    backupMessage = null
                                    exportBackupLauncher.launch("Coach-backup-${LocalDate.now()}.json")
                                },
                                onImportBackup = {
                                    backupMessage = null
                                    if (trainingEngine.state is TrainingUiState.Workout) {
                                        backupMessage = "No se puede importar durante un entrenamiento activo."
                                    } else {
                                        importBackupLauncher.launch(arrayOf("application/json"))
                                    }
                                },
                                pendingBackupImport = pendingBackupImport != null,
                                onConfirmImport = {
                                    val result = backupManager.restore(
                                        pendingBackupImport,
                                        workoutActive = trainingEngine.state is TrainingUiState.Workout
                                    )
                                    pendingBackupImport = null
                                    backupMessage = result.message
                                    if (result.success) {
                                        programs = programRepository.loadPrograms(
                                            routineRepository.load(),
                                            routineRepository.hasStoredRoutines()
                                        )
                                        selectedProgramId = programRepository.loadSelectedProgramId()
                                        routines = programs.firstOrNull { it.id == selectedProgramId }?.routines.orEmpty()
                                        customExercises = routineRepository.loadCustomExercises()
                                        ExerciseLibrary.replaceCustom(customExercises)
                                        userName = userPreferenceRepository.loadUserName()
                                        selectedThemeId = themeRepository.load().id
                                        selectedRoutineId = null
                                    }
                                },
                                onCancelImport = { pendingBackupImport = null },
                                backupMessage = backupMessage,
                                onDismissBackupMessage = { backupMessage = null },
                                onReplayTour = {
                                    selectedTab = 0
                                    selectedRoutineId = null
                                    openedProgramId = null
                                    tourStep = TourStep.TRAIN
                                },
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it }
                            )
                        } else if (selectedTab == 1) {
                            ProgramsScreen(
                                programs = programs,
                                activeProgramId = activeProgram.id,
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it },
                                onOpen = { openedProgramId = it.id },
                                onCreate = { name ->
                                    val id = "custom-program-${System.nanoTime()}"
                                    val program = TrainingProgram(
                                        id = id,
                                        name = name,
                                        description = "Programa personalizado.",
                                        frequency = "1 día",
                                        routines = listOf(emptyCustomRoutine("$id-day-1")),
                                        builtIn = false
                                    )
                                    val next = programs + program
                                    if (programRepository.savePrograms(next)) { programs = next; openedProgramId = id }
                                },
                                onProgramsTabPositioned = { tourTargets = registerTourTargetBounds(tourTargets, TourTarget.PROGRAMS_TAB, it) },
                                onCreateProgramPositioned = { tourTargets = registerTourTargetBounds(tourTargets, TourTarget.CREATE_PROGRAM, it) }
                            )
                        } else {
                            HomeScreen(
                                routines = activeProgram.routines,
                                isCustomTab = false,
                                userName = userName,
                                activeProgramName = activeProgram.name,
                                activeProgramFrequency = activeProgram.frequency,
                                onOpen = { selectedRoutineId = it.id },
                                onCreate = {},
                                onDelete = {},
                                selectedTab = selectedTab,
                                onTabSelected = { selectedTab = it }
                                , onRoutinePositioned = { tourTargets = registerTourTargetBounds(tourTargets, TourTarget.TRAIN_ROUTINE, it) }
                            )
                        }
                    } else {
                        RoutineDetailScreen(
                            routine = selectedRoutine,
                            onBack = { selectedRoutineId = null },
                            onStart = onStartWorkout,
                            onStartFromExercise = onStartWorkoutFromExercise,
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
                                val result = routineRepository.deleteCustomExercise(
                                    definition.id,
                                    programs.flatMap(TrainingProgram::routines)
                                )
                                if (result.success) {
                                    customExercises = result.exercises
                                    ExerciseLibrary.replaceCustom(result.exercises)
                                }
                                result.message
                            },
                            onSave = { updatedRoutine ->
                                val updatedPrograms = programs.map { program ->
                                    if (program.routines.none { it.id == updatedRoutine.id }) program else program.copy(
                                        routines = program.routines.map { routine ->
                                            if (routine.id == updatedRoutine.id) updatedRoutine else routine
                                        }
                                    )
                                }
                                if (programRepository.savePrograms(updatedPrograms)) {
                                    programs = updatedPrograms
                                    routines = updatedPrograms.first { it.id == selectedProgramId }.routines
                                    true
                                } else {
                                    false
                                }
                            },
                            onEditPositioned = { tourTargets = registerTourTargetBounds(tourTargets, TourTarget.EDIT_BUTTON, it) }
                        )
                    }
                    tourStep?.let { currentStep ->
                        CoachMarkOverlay(
                            target = tourBoundsForStep(tourTargets, currentStep),
                            step = currentStep,
                            onNext = {
                                when (currentStep) {
                                    TourStep.TRAIN -> {
                                        selectedRoutineId = activeProgram.routines.firstOrNull()?.id
                                        tourStep = TourStep.EDIT
                                    }
                                    TourStep.EDIT -> {
                                        selectedRoutineId = null
                                        selectedTab = 1
                                        tourStep = TourStep.PROGRAMS
                                    }
                                    TourStep.PROGRAMS -> tourStep = TourStep.CUSTOM
                                    TourStep.CUSTOM -> {
                                        userPreferenceRepository.completeTour()
                                        tourStep = null
                                    }
                                }
                            },
                            onSkip = {
                                userPreferenceRepository.completeTour()
                                tourStep = null
                            }
                        )
                    }
                }

                is TrainingUiState.Workout -> WorkoutScreen(
                    state = state,
                    onPause = trainingEngine::pause,
                    onResume = trainingEngine::resume,
                    onSkip = trainingEngine::skip,
                    onFinish = onFinishWorkout
                )

                TrainingUiState.Completed -> CompletionScreen(onFinish = onFinishWorkout)
                }
            }
        }
    }
}

@Composable
private fun BrandedLaunchScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.coach_logo_full),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(0.72f).aspectRatio(460.34f / 332.73f)
        )
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
                Text("IR AL INICIO")
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
    activeProgramName: String = "",
    activeProgramFrequency: String = "",
    onOpen: (Routine) -> Unit,
    onCreate: () -> Unit,
    onDelete: (Routine) -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onRoutinePositioned: (Rect) -> Unit = {}
) {
    var routinePendingDeletion by remember { mutableStateOf<Routine?>(null) }
    val routineListState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }
    val customRoutineListState = rememberLazyListState()
    routinePendingDeletion?.let { routine ->
        AlertDialog(
            onDismissRequest = { routinePendingDeletion = null },
            containerColor = LocalDialogContainerColor.current,
            title = { Text("¿Eliminar rutina?") },
            text = { Text("Esta acción no se puede deshacer.") },
            dismissButton = { TextButton(onClick = { routinePendingDeletion = null }) { Text("CANCELAR") } },
            confirmButton = { TextButton(onClick = { onDelete(routine); routinePendingDeletion = null }) { Text("ELIMINAR") } }
        )
    }
    Scaffold(
        topBar = {
            if (isCustomTab) TopAppBar(title = { Text("PERSONALIZADO") })
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
                Column(
                    modifier = Modifier.padding(top = 24.dp, bottom = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (userName.isNotBlank()) {
                        Text(
                            text = "Hola, $userName",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "¿Qué quieres entrenar hoy?",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Selecciona una rutina para comenzar.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (activeProgramName.isNotBlank()) {
                        Text(
                            text = "$activeProgramName · $activeProgramFrequency",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            if (isCustomTab && routines.isEmpty()) {
                Text("Crea tu primera rutina personalizada.")
                Button(modifier = Modifier.fillMaxWidth(), onClick = onCreate) { Text("+ CREAR RUTINA") }
            } else LazyColumn(
                state = if (isCustomTab) customRoutineListState else routineListState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(top = 4.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(if (isCustomTab) 8.dp else 10.dp)
            ) {
                if (isCustomTab) {
                    items(routines, key = Routine::id) { routine ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(routine) },
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            colors = routineCardColors()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .routineCardStripe()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        RoutineCardTitle(routine.name, maxLines = 1)
                                        RoutineCardMetadata(
                                            "${routine.exercises.size} ejercicios · " +
                                                "${routine.estimatedDurationMinutes()} min"
                                        )
                                    }
                                    TextButton(onClick = { routinePendingDeletion = routine }) {
                                        Text("ELIMINAR")
                                    }
                                    Icon(
                                        imageVector = RoutineChevronRightIcon,
                                        contentDescription = null,
                                        modifier = Modifier.size(22.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                } else {
                    itemsIndexed(routines, key = { _, routine -> routine.id }) { index, routine ->
                        val cardContent = remember(routine) {
                            routineCardContent(
                                exerciseCount = routine.exercises.size,
                                durationMinutes = routine.estimatedDurationMinutes()
                            )
                        }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(if (index == 0) Modifier.onGloballyPositioned { onRoutinePositioned(it.boundsInWindow()) } else Modifier)
                                .clickable { onOpen(routine) },
                            shape = RoundedCornerShape(20.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            colors = routineCardColors()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .routineCardStripe()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        RoutineCardTitle(routine.name, maxLines = 2)
                                        RoutineCardMetadata(cardContent.exerciseMetadata)
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        RoutineDuration(cardContent.duration)
                                        Icon(
                                            imageVector = RoutineChevronRightIcon,
                                            contentDescription = null,
                                            modifier = Modifier.size(22.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
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

internal val ROUTINE_CARD_TITLE_FONT_SIZE = 18.sp

internal fun contentCardContainerColor(colorScheme: ColorScheme) = colorScheme.surfaceVariant

@Composable
internal fun contentCardColors() = CardDefaults.cardColors(
    containerColor = contentCardContainerColor(MaterialTheme.colorScheme)
)

@Composable
private fun routineCardColors() = contentCardColors()

@Composable
internal fun Modifier.routineCardStripe(): Modifier {
    val stripeColor = routineCardStripeColor(MaterialTheme.colorScheme)
    return drawBehind {
        val inset = 4.dp.toPx()
        val strokeWidth = 4.dp.toPx()
        drawLine(
            color = stripeColor,
            start = Offset(strokeWidth / 2f, inset),
            end = Offset(strokeWidth / 2f, (size.height - inset).coerceAtLeast(inset)),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}

internal fun routineCardStripeColor(colorScheme: ColorScheme): Color = colorScheme.primary

internal data class RoutineCardContent(
    val exerciseMetadata: String,
    val duration: String
)

internal fun routineCardContent(exerciseCount: Int, durationMinutes: Int) = RoutineCardContent(
    exerciseMetadata = "$exerciseCount ejercicios",
    duration = "$durationMinutes min"
)

@Composable
private fun RoutineCardTitle(name: String, maxLines: Int) {
    Text(
        text = name,
        style = MaterialTheme.typography.titleLarge.copy(fontSize = ROUTINE_CARD_TITLE_FONT_SIZE),
        fontWeight = FontWeight.SemiBold,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun RoutineCardMetadata(summary: String) {
    Text(
        text = summary,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun RoutineDuration(duration: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            imageVector = ScheduleIcon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = duration,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

internal fun isNavigationTabOutlined(selectedTab: Int, tabIndex: Int): Boolean =
    selectedTab == tabIndex

@Composable
internal fun MainNavigationBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onProgramsPositioned: (Rect) -> Unit = {}
) {
    NavigationBar(containerColor = LocalNavigationBarContainerColor.current) {
        listOf("ENTRENAR", "PROGRAMAS", "AJUSTES").forEachIndexed { index, label ->
            val selected = selectedTab == index
            NavigationBarItem(
                selected = selected,
                onClick = { onTabSelected(index) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.Transparent
                ),
                icon = {
                    Text(
                        text = label,
                        modifier = Modifier
                            .then(if (index == 1) Modifier.onGloballyPositioned { onProgramsPositioned(it.boundsInWindow()) } else Modifier)
                            .then(
                                if (isNavigationTabOutlined(selectedTab, index)) {
                                    Modifier.border(
                                        width = 2.dp,
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = RoundedCornerShape(50)
                                    )
                                } else Modifier
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RoutineDetailScreen(
    routine: Routine,
    onBack: () -> Unit,
    onStart: (Routine) -> Unit,
    onStartFromExercise: (Routine, Int) -> Unit,
    isVoiceReady: Boolean,
    customExercises: List<ExerciseDefinition>,
    onSaveCustomExercise: (String?, String, String, String) -> String?,
    onDeleteCustomExercise: (ExerciseDefinition) -> String?,
    onSave: (Routine) -> Boolean,
    onEditPositioned: (Rect) -> Unit = {}
) {
    var isEditing by rememberSaveable(routine.id) { mutableStateOf(false) }
    var draft by remember(routine.id) { mutableStateOf(routine.toDraft()) }
    var validationMessage by remember(routine.id) { mutableStateOf<String?>(null) }
    var startValidationMessage by remember(routine.id) { mutableStateOf<String?>(null) }
    var pendingStartExerciseIndex by rememberSaveable(routine.id) { mutableStateOf<Int?>(null) }

    pendingStartExerciseIndex?.let { exerciseIndex ->
        val exercise = routine.exercises.getOrNull(exerciseIndex)
        if (exercise == null) {
            pendingStartExerciseIndex = null
        } else {
            AlertDialog(
                onDismissRequest = { pendingStartExerciseIndex = null },
                containerColor = LocalDialogContainerColor.current,
                title = { Text(startFromExerciseDialogTitle(exercise.name)) },
                text = { Text(START_FROM_EXERCISE_DIALOG_MESSAGE) },
                dismissButton = {
                    TextButton(onClick = { pendingStartExerciseIndex = null }) { Text("CANCELAR") }
                },
                confirmButton = {
                    TextButton(onClick = {
                        pendingStartExerciseIndex = null
                        onStartFromExercise(routine, exerciseIndex)
                    }) { Text("INICIAR") }
                }
            )
        }
    }

    startValidationMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { startValidationMessage = null },
            containerColor = LocalDialogContainerColor.current,
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { startValidationMessage = null }) { Text("ACEPTAR") }
            }
        )
    }

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
                navigationIcon = {
                    CoachBackButton(onClick = onBack)
                },
                actions = {
                    TextButton(modifier = Modifier.onGloballyPositioned { onEditPositioned(it.boundsInWindow()) }, onClick = {
                        draft = routine.toDraft()
                        validationMessage = null
                        isEditing = true
                    }) { Text("EDITAR") }
                }
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
            Text(
                routine.name,
                style = MaterialTheme.typography.headlineSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = ScheduleIcon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    buildAnnotatedString {
                        append("Descanso entre ejercicios: ")
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                            append("${routine.restBetweenExercisesSeconds.toClockFormat()} min")
                        }
                    }
                )
            }
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(routine.exercises, key = { _, exercise -> exercise.id }) { index, exercise ->
                    ExerciseSummary(
                        exercise = exercise,
                        startEnabled = isVoiceReady,
                        onStartFromHere = { pendingStartExerciseIndex = index }
                    )
                }
                item(key = "routine-summary") {
                    RoutineSummaryCard(routine)
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = isVoiceReady,
                onClick = {
                    startValidationMessage = attemptRoutineStart(routine, onStart)
                }
            ) {
                Icon(
                    imageVector = StartWorkoutPlayIcon,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(START_WORKOUT_LABEL)
            }
            if (!isVoiceReady) Text("Inicializando voz")
        }
    }
}

const val EMPTY_ROUTINE_START_MESSAGE = "Agrega al menos un ejercicio antes de comenzar."
const val START_WORKOUT_LABEL = "INICIAR"
const val START_FROM_EXERCISE_DIALOG_MESSAGE =
    "El entrenamiento comenzará desde este ejercicio y continuará con los siguientes."

internal fun startFromExerciseDialogTitle(exerciseName: String) = "¿Iniciar desde $exerciseName?"

internal fun startFromExerciseContentDescription(exerciseName: String) = "Iniciar desde $exerciseName"

private val StartWorkoutPlayIcon: ImageVector = ImageVector.Builder(
    name = "PlayArrow",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(8f, 5f)
        lineTo(8f, 19f)
        lineTo(19f, 12f)
        close()
    }
}.build()

private val PauseIcon = coachIcon("Pause") {
    path(fill = SolidColor(Color.Black)) {
        moveTo(6f, 5f); lineTo(10f, 5f); lineTo(10f, 19f); lineTo(6f, 19f); close()
        moveTo(14f, 5f); lineTo(18f, 5f); lineTo(18f, 19f); lineTo(14f, 19f); close()
    }
}

private val SkipNextIcon = coachIcon("SkipNext") {
    path(fill = SolidColor(Color.Black)) {
        moveTo(6f, 18f); lineTo(14.5f, 12f); lineTo(6f, 6f); close()
        moveTo(16f, 6f); lineTo(19f, 6f); lineTo(19f, 18f); lineTo(16f, 18f); close()
    }
}

private val StopIcon = coachIcon("Stop") {
    path(fill = SolidColor(Color.Black)) {
        moveTo(6f, 6f); lineTo(18f, 6f); lineTo(18f, 18f); lineTo(6f, 18f); close()
    }
}

private val ScheduleIcon = coachIcon("Schedule") {
    path(fill = SolidColor(Color.Black)) {
        moveTo(11.99f, 2f)
        curveTo(6.47f, 2f, 2f, 6.48f, 2f, 12f)
        curveTo(2f, 17.52f, 6.47f, 22f, 11.99f, 22f)
        curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
        curveTo(22f, 6.48f, 17.52f, 2f, 11.99f, 2f)
        close()
        moveTo(12f, 20f)
        curveTo(7.58f, 20f, 4f, 16.42f, 4f, 12f)
        curveTo(4f, 7.58f, 7.58f, 4f, 12f, 4f)
        curveTo(16.42f, 4f, 20f, 7.58f, 20f, 12f)
        curveTo(20f, 16.42f, 16.42f, 20f, 12f, 20f)
        close()
        moveTo(12.5f, 7f)
        lineTo(11f, 7f)
        lineTo(11f, 13f)
        lineTo(16.25f, 16.15f)
        lineTo(17f, 14.92f)
        lineTo(12.5f, 12.25f)
        close()
    }
}

internal val RoutineChevronRightIcon = coachIcon("ChevronRight") {
    path(fill = SolidColor(Color.Black)) {
        moveTo(9.29f, 6.71f)
        lineTo(13.58f, 11f)
        lineTo(9.29f, 15.29f)
        lineTo(10.7f, 16.7f)
        lineTo(16.4f, 11f)
        lineTo(10.7f, 5.3f)
        close()
    }
}

private val SeriesIcon = coachIcon("Series") {
    path(fill = SolidColor(Color.Black)) {
        moveTo(4f, 6f); lineTo(20f, 6f); lineTo(20f, 8f); lineTo(4f, 8f); close()
        moveTo(4f, 11f); lineTo(20f, 11f); lineTo(20f, 13f); lineTo(4f, 13f); close()
        moveTo(4f, 16f); lineTo(20f, 16f); lineTo(20f, 18f); lineTo(4f, 18f); close()
    }
}

private val RepeatIcon = coachIcon("Repeat") {
    path(fill = SolidColor(Color.Black)) {
        moveTo(7f, 7f); lineTo(17f, 7f); lineTo(17f, 10f); lineTo(22f, 6f)
        lineTo(17f, 2f); lineTo(17f, 5f); lineTo(7f, 5f)
        curveTo(4.24f, 5f, 2f, 7.24f, 2f, 10f); lineTo(4f, 10f)
        curveTo(4f, 8.34f, 5.34f, 7f, 7f, 7f); close()
        moveTo(17f, 17f); lineTo(7f, 17f); lineTo(7f, 14f); lineTo(2f, 18f)
        lineTo(7f, 22f); lineTo(7f, 19f); lineTo(17f, 19f)
        curveTo(19.76f, 19f, 22f, 16.76f, 22f, 14f); lineTo(20f, 14f)
        curveTo(20f, 15.66f, 18.66f, 17f, 17f, 17f); close()
    }
}

private val PhaseIcon = coachIcon("Activity") {
    path(fill = SolidColor(Color.Black)) {
        moveTo(2f, 13f); lineTo(7f, 13f); lineTo(9.5f, 6f); lineTo(13f, 18f)
        lineTo(16f, 10f); lineTo(18f, 13f); lineTo(22f, 13f); lineTo(22f, 15f)
        lineTo(17f, 15f); lineTo(16.5f, 14.5f); lineTo(12.8f, 23f); lineTo(9.3f, 11f)
        lineTo(8.4f, 15f); lineTo(2f, 15f); close()
    }
}

private fun coachIcon(name: String, paths: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(paths).build()

fun attemptRoutineStart(routine: Routine, onStart: (Routine) -> Unit): String? {
    if (routine.exercises.isEmpty()) return EMPTY_ROUTINE_START_MESSAGE
    onStart(routine)
    return null
}

@Composable
private fun RoutineSummaryCard(routine: Routine) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = contentCardColors()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DurationSummaryMetric(
                modifier = Modifier.weight(1.35f),
                value = "${routine.estimatedDurationMinutes()} min"
            )
            SummaryDivider()
            RoutineSummaryMetric(
                modifier = Modifier.weight(1f),
                icon = PhaseIcon,
                value = routine.exercises.size.toString(),
                label = "Ejercicios"
            )
            SummaryDivider()
            RoutineSummaryMetric(
                modifier = Modifier.weight(1f),
                icon = SeriesIcon,
                value = routine.totalExecutionSets().toString(),
                label = "Series totales"
            )
            SummaryDivider()
            RoutineSummaryMetric(
                modifier = Modifier.weight(1f),
                icon = RepeatIcon,
                value = routine.totalRepetitions().toString(),
                label = "Repeticiones totales"
            )
        }
    }
}

@Composable
private fun DurationSummaryMetric(modifier: Modifier, value: String) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = ScheduleIcon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Duración estimada del día",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RoutineSummaryMetric(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SummaryDivider() {
    VerticalDivider(
        modifier = Modifier.height(56.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun ExerciseSummary(
    exercise: Exercise,
    startEnabled: Boolean,
    onStartFromHere: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = contentCardColors()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = exerciseCardTitle(exercise.name),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
            IconButton(enabled = startEnabled, onClick = onStartFromHere) {
                Icon(
                    imageVector = StartWorkoutPlayIcon,
                    contentDescription = startFromExerciseContentDescription(exercise.name),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

internal fun exerciseCardTitle(name: String, locale: Locale = Locale.getDefault()): String =
    name.uppercase(locale)

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
                val existing = draft.exercises.firstOrNull { it.id == exerciseId }
                val selected = selectExerciseDefinition(
                    existing ?: emptyCustomExercise(exerciseId).toDraft(),
                    definition
                )
                onDraftChange(if (existing == null) draft.addExercise(selected) else draft.updateExercise(selected))
                expandedExerciseId = exerciseId
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
            containerColor = LocalDialogContainerColor.current,
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
                CoachBackButton(onClick = requestExit)
            },
            actions = { TextButton(onClick = onSave) { Text("GUARDAR") } }
        )
    }) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
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
                        exerciseBeingSelectedId = id
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

@Composable
private fun ExerciseEditor(
    exercise: ExerciseDraft,
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelectExercise: () -> Unit,
    onChange: (ExerciseDraft) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        colors = contentCardColors()
    ) {
        Column(
            modifier = Modifier.padding(if (expanded) 16.dp else 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = exercise.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text("EDITAR", color = MaterialTheme.colorScheme.primary)
            }
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
                Text("Modo de ejecución", style = MaterialTheme.typography.labelLarge)
                ExecutionModeOption(
                    title = "Simultáneo",
                    description = "Ambos lados se entrenan en la misma serie.",
                    selected = exercise.executionMode == ExerciseExecutionMode.SIMULTANEOUS,
                    onClick = { onChange(exercise.copy(executionMode = ExerciseExecutionMode.SIMULTANEOUS)) }
                )
                ExecutionModeOption(
                    title = "Un lado a la vez",
                    description = "Alterna entre lado derecho e izquierdo en cada serie.",
                    selected = exercise.executionMode == ExerciseExecutionMode.ONE_SIDE_AT_A_TIME,
                    onClick = { onChange(exercise.copy(executionMode = ExerciseExecutionMode.ONE_SIDE_AT_A_TIME)) }
                )
                Text("Pausa isométrica", style = MaterialTheme.typography.labelLarge)
                IsometricPauseMode.entries.forEach { mode ->
                    ExecutionModeOption(
                        title = mode.editorLabel(),
                        description = "",
                        selected = exercise.isometricPauseMode == mode,
                        onClick = { onChange(exercise.copy(isometricPauseMode = mode)) }
                    )
                }
                if (exercise.isometricPauseMode != IsometricPauseMode.NONE) {
                    val invalidDuration = exercise.isometricDurationSeconds.toIntOrNull()?.let { it <= 0 } ?: true
                    OutlinedTextField(
                        value = exercise.isometricDurationSeconds,
                        onValueChange = { onChange(exercise.copy(isometricDurationSeconds = it)) },
                        modifier = rememberImeAwareFieldModifier(),
                        label = { Text("Duración (s)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = invalidDuration,
                        supportingText = { if (invalidDuration) Text(ISOMETRIC_DURATION_ERROR) }
                    )
                }
                OutlinedTextField(
                    value = exercise.notes,
                    onValueChange = {
                        if (it.length <= MAX_EXERCISE_NOTES_LENGTH) {
                            onChange(exercise.copy(notes = it))
                        }
                    },
                    modifier = rememberImeAwareFieldModifier(),
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
private fun ExecutionModeOption(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (description.isNotEmpty()) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun IsometricPauseMode.editorLabel(): String = when (this) {
    IsometricPauseMode.NONE -> "Sin pausa isométrica"
    IsometricPauseMode.SHORTENED -> "Músculo acortado"
    IsometricPauseMode.STRETCHED -> "Músculo estirado"
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
        modifier = rememberImeAwareFieldModifier(),
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType)
    )
}

@Composable
private fun rememberImeAwareFieldModifier(): Modifier {
    val bringIntoViewRequester = remember { BringIntoViewRequester() }
    val coroutineScope = rememberCoroutineScope()
    return Modifier
        .fillMaxWidth()
        .bringIntoViewRequester(bringIntoViewRequester)
        .onFocusChanged { focusState ->
            if (focusState.isFocused) {
                coroutineScope.launch { bringIntoViewRequester.bringIntoView() }
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
            containerColor = LocalDialogContainerColor.current,
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.safeDrawing.asPaddingValues())
    ) {
        val metrics = workoutMetricTexts(
            seriesNumber = state.seriesNumber,
            seriesTotal = exercise.sets,
            repetitionNumber = state.repetitionNumber,
            repetitionTotal = exercise.repetitions,
            phase = workoutMetricPhaseLabel(state.phase)
        )
        when (workoutLayoutFor(maxWidth, maxHeight)) {
            WorkoutLayout.PORTRAIT -> WorkoutPortraitLayout(
                state = state,
                exercise = exercise,
                metrics = metrics,
                ringDiameter = workoutRingDiameter(maxWidth, maxHeight),
                onPause = onPause,
                onResume = onResume,
                onSkip = onSkip,
                onRequestFinish = { showFinishConfirmation = true }
            )
            WorkoutLayout.LANDSCAPE -> WorkoutLandscapeLayout(
                state = state,
                exercise = exercise,
                metrics = metrics,
                ringDiameter = landscapeWorkoutRingDiameter(maxWidth * 0.4f, maxHeight),
                onPause = onPause,
                onResume = onResume,
                onSkip = onSkip,
                onRequestFinish = { showFinishConfirmation = true }
            )
        }
    }
}

internal enum class WorkoutLayout { PORTRAIT, LANDSCAPE }

internal fun workoutLayoutFor(width: Dp, height: Dp): WorkoutLayout =
    if (width > height) WorkoutLayout.LANDSCAPE else WorkoutLayout.PORTRAIT

@Composable
private fun WorkoutPortraitLayout(
    state: TrainingUiState.Workout,
    exercise: Exercise,
    metrics: WorkoutMetricTexts,
    ringDiameter: Dp,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onRequestFinish: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WorkoutHeader(state, exercise, nameMaxLines = Int.MAX_VALUE)
            if (state.phase != TrainingPhase.WARMUP) WorkoutMetricsCard(metrics)
        }
        Box(modifier = Modifier.align(Alignment.Center), contentAlignment = Alignment.Center) {
            Text(
                text = "Tiempo restante",
                modifier = Modifier.align(Alignment.TopCenter).offset(y = (-32).dp),
                style = MaterialTheme.typography.labelLarge
            )
            TrainingTimer(state, ringDiameter, showPhaseLabel = false)
        }
        WorkoutControls(
            state = state,
            compact = false,
            onPause = onPause,
            onResume = onResume,
            onSkip = onSkip,
            onRequestFinish = onRequestFinish,
            modifier = Modifier.align(Alignment.BottomCenter).padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
private fun WorkoutLandscapeLayout(
    state: TrainingUiState.Workout,
    exercise: Exercise,
    metrics: WorkoutMetricTexts,
    ringDiameter: Dp,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onRequestFinish: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp)) {
        val sideWidth = landscapeWorkoutSideWidth(maxWidth)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.6f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            WorkoutHeader(state, exercise, nameMaxLines = 1)
        }
        if (state.phase != TrainingPhase.WARMUP) {
            LandscapeWorkoutMetrics(
                metrics = metrics,
                modifier = Modifier.align(Alignment.CenterStart).width(sideWidth)
            )
        }
        Column(modifier = Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Tiempo restante", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            TrainingTimer(state, ringDiameter, showPhaseLabel = true)
        }
        WorkoutControls(
            state = state,
            compact = true,
            onPause = onPause,
            onResume = onResume,
            onSkip = onSkip,
            onRequestFinish = onRequestFinish,
            modifier = Modifier.align(Alignment.CenterEnd).width(sideWidth)
        )
    }
}

@Composable
private fun WorkoutHeader(
    state: TrainingUiState.Workout,
    exercise: Exercise,
    nameMaxLines: Int
) {
    Text(stringResource(R.string.workout_title), style = MaterialTheme.typography.titleLarge)
    if (state.phase == TrainingPhase.WARMUP) {
        Text(
            state.routine.name,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = nameMaxLines,
            overflow = TextOverflow.Ellipsis
        )
        Text("Calentamiento", style = MaterialTheme.typography.titleLarge)
    } else if (state.phase == TrainingPhase.REST_BETWEEN_EXERCISES) {
        Text("Descanso entre ejercicios", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Siguiente: ${exercise.name}",
            maxLines = nameMaxLines,
            overflow = TextOverflow.Ellipsis
        )
    } else {
        Text(
            exercise.name,
            style = MaterialTheme.typography.headlineMedium,
            maxLines = nameMaxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun LandscapeWorkoutMetrics(metrics: WorkoutMetricTexts, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = workoutSupportingContainerColor(MaterialTheme.colorScheme)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            LandscapeWorkoutMetric(SeriesIcon, "SERIE", metrics.series)
            WorkoutMetricHorizontalDivider()
            LandscapeWorkoutMetric(RepeatIcon, WORKOUT_REPETITION_LABEL, metrics.repetition)
            WorkoutMetricHorizontalDivider()
            LandscapeWorkoutMetric(PhaseIcon, "FASE", metrics.phase)
        }
    }
}

@Composable
private fun LandscapeWorkoutMetric(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        WorkoutMetricIcon(icon)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1)
        }
    }
}

@Composable
private fun WorkoutMetricHorizontalDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
        thickness = 1.dp,
        color = workoutMetricDividerColor(MaterialTheme.colorScheme)
    )
}

@Composable
private fun WorkoutControls(
    state: TrainingUiState.Workout,
    compact: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onSkip: () -> Unit,
    onRequestFinish: () -> Unit,
    modifier: Modifier = Modifier
) {
    val buttonHeight = if (compact) 44.dp else 56.dp
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 8.dp)) {
        if (state.phase != TrainingPhase.WARMUP) {
            workoutNoteText(state.currentExerciseNotes)?.let { note ->
                WorkoutNote(note)
            }
        }
        Button(
            modifier = Modifier.fillMaxWidth().height(buttonHeight),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            onClick = workoutPauseAction(state.isPaused, onPause, onResume)
        ) {
            WorkoutButtonContent(if (state.isPaused) StartWorkoutPlayIcon else PauseIcon, if (state.isPaused) "REANUDAR" else "PAUSA")
        }
        Button(
            modifier = Modifier.fillMaxWidth().height(buttonHeight),
            shape = RoundedCornerShape(16.dp),
            colors = workoutNeutralButtonColors(),
            enabled = !state.isPaused && state.phase != TrainingPhase.COUNTDOWN,
            onClick = onSkip
        ) { WorkoutButtonContent(SkipNextIcon, "OMITIR") }
        Button(
            modifier = Modifier.fillMaxWidth().height(buttonHeight),
            shape = RoundedCornerShape(16.dp),
            colors = workoutNeutralButtonColors(),
            onClick = onRequestFinish
        ) { WorkoutButtonContent(StopIcon, stringResource(R.string.finish_workout)) }
    }
}

@Composable
private fun WorkoutNote(note: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "NOTA:",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.Center
        )
        Text(
            text = note,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

internal fun workoutNoteText(notes: String): String? {
    val firstVisibleCharacterIndex = notes.indexOfFirst { !it.isWhitespace() }
    if (firstVisibleCharacterIndex == -1) return null
    return notes.replaceRange(
        firstVisibleCharacterIndex,
        firstVisibleCharacterIndex + 1,
        notes[firstVisibleCharacterIndex].titlecase()
    )
}

internal fun workoutPauseAction(isPaused: Boolean, onPause: () -> Unit, onResume: () -> Unit): () -> Unit =
    if (isPaused) onResume else onPause

internal data class WorkoutMetricTexts(
    val series: String,
    val repetition: String,
    val phase: String
)

internal fun workoutMetricTexts(
    seriesNumber: Int,
    seriesTotal: Int,
    repetitionNumber: Int,
    repetitionTotal: Int,
    phase: String
) = WorkoutMetricTexts(
    series = "$seriesNumber de $seriesTotal",
    repetition = "$repetitionNumber de $repetitionTotal",
    phase = phase
)

@Composable
private fun WorkoutMetricsCard(metrics: WorkoutMetricTexts) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = workoutSupportingContainerColor(MaterialTheme.colorScheme)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WorkoutMetricColumn(SeriesIcon, "SERIE", metrics.series, Modifier.weight(1f))
            WorkoutMetricDivider()
            WorkoutMetricColumn(RepeatIcon, WORKOUT_REPETITION_LABEL, metrics.repetition, Modifier.weight(1f))
            WorkoutMetricDivider()
            WorkoutMetricColumn(PhaseIcon, "FASE", metrics.phase, Modifier.weight(1f))
        }
    }
}

@Composable
private fun WorkoutMetricColumn(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        WorkoutMetricIcon(icon)
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun WorkoutMetricIcon(icon: ImageVector) {
    Box(
        modifier = Modifier.size(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(if (icon == RepeatIcon) 20.dp else 24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun WorkoutMetricDivider() {
    VerticalDivider(
        modifier = Modifier.height(72.dp),
        thickness = 1.dp,
        color = workoutMetricDividerColor(MaterialTheme.colorScheme)
    )
}

@Composable
private fun workoutNeutralButtonColors() = ButtonDefaults.buttonColors(
    containerColor = workoutSupportingContainerColor(MaterialTheme.colorScheme),
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledContainerColor = workoutSupportingContainerColor(MaterialTheme.colorScheme).copy(alpha = 0.5f),
    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
)

@Composable
private fun WorkoutButtonContent(icon: ImageVector, label: String) {
    Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp))
    Spacer(modifier = Modifier.size(8.dp))
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge.copy(
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    )
}

internal fun workoutRingDiameter(containerWidth: Dp, containerHeight: Dp): Dp {
    val widthLimited = containerWidth - 32.dp
    val verticallyClearDiameter = containerHeight - 440.dp
    return min(widthLimited.value, verticallyClearDiameter.value)
        .coerceIn(160f, 240f)
        .dp
}

internal fun landscapeWorkoutRingDiameter(columnWidth: Dp, containerHeight: Dp): Dp =
    min((columnWidth - 24.dp).value, (containerHeight - 52.dp).value)
        .coerceIn(120f, 240f)
        .dp

internal const val LANDSCAPE_WORKOUT_SIDE_FRACTION = 0.28f
internal const val WORKOUT_METRIC_SEPARATOR_COUNT = 2
internal const val WORKOUT_REPETITION_LABEL = "REPETICIÓN"
internal const val DARK_WORKOUT_DIVIDER_ALPHA = 0.28f
internal const val LIGHT_WORKOUT_DIVIDER_ALPHA = 0.55f

internal fun landscapeWorkoutSideWidth(containerWidth: Dp): Dp =
    containerWidth * LANDSCAPE_WORKOUT_SIDE_FRACTION

internal fun workoutMetricSeparatorCount(layout: WorkoutLayout): Int = when (layout) {
    WorkoutLayout.PORTRAIT,
    WorkoutLayout.LANDSCAPE -> WORKOUT_METRIC_SEPARATOR_COUNT
}

internal fun workoutSupportingContainerColor(colorScheme: ColorScheme): Color =
    if (colorScheme.background.luminance() > 0.5f) {
        lerp(colorScheme.surfaceVariant, colorScheme.surface, 0.35f)
    } else {
        colorScheme.surfaceVariant
    }

internal fun workoutMetricDividerColor(colorScheme: ColorScheme): Color =
    colorScheme.outline.copy(
        alpha = if (colorScheme.background.luminance() > 0.5f) {
            LIGHT_WORKOUT_DIVIDER_ALPHA
        } else {
            DARK_WORKOUT_DIVIDER_ALPHA
        }
    )

internal fun usefulAreaCenter(
    screenWidth: Int,
    screenHeight: Int,
    leftInset: Int,
    topInset: Int,
    rightInset: Int,
    bottomInset: Int
): Pair<Float, Float> = Pair(
    leftInset + (screenWidth - leftInset - rightInset) / 2f,
    topInset + (screenHeight - topInset - bottomInset) / 2f
)

@Composable
private fun TrainingTimer(state: TrainingUiState.Workout, diameter: Dp, showPhaseLabel: Boolean) {
    val timerText = state.secondsRemaining.toClockFormat()
    val visibleSide = workoutTimerSide(
        phase = state.phase,
        secondsRemaining = state.secondsRemaining,
        currentSide = state.currentSide,
        isStartingExecution = state.isStartingExecution
    )
    val sideLabel = visibleSide.displayLabel()
    val supportingText = workoutTimerSupportingText(
        sideLabel = sideLabel,
        phaseLabel = state.phase.label,
        showPhaseLabel = showPhaseLabel,
        hasUnilateralContext = state.currentSide != null
    )
    var frameTimeMillis by remember(state.phaseStartedAtMillis, state.phasePausedAtMillis) {
        mutableStateOf(state.phasePausedAtMillis ?: state.phaseStartedAtMillis)
    }
    LaunchedEffect(state.phaseStartedAtMillis, state.phasePausedAtMillis, state.isPaused) {
        if (state.isPaused) {
            frameTimeMillis = state.phasePausedAtMillis ?: frameTimeMillis
        } else {
            while (true) {
                withFrameNanos { }
                frameTimeMillis = android.os.SystemClock.elapsedRealtime()
            }
        }
    }
    val effectiveTimeMillis = state.phasePausedAtMillis ?: frameTimeMillis
    val progress = workoutRemainingFraction(state, effectiveTimeMillis)
    val progressColor = timerProgressColor(MaterialTheme.colorScheme)
    val trackColor = MaterialTheme.colorScheme.outlineVariant

    Box(modifier = Modifier.size(diameter), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 14.dp.toPx())
            val progressStroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
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
                style = progressStroke
            )
        }
        if (showPhaseLabel) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                supportingText?.let { text ->
                    Text(
                        text = text,
                        style = if (sideLabel != null) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge,
                        color = if (sideLabel != null) MaterialTheme.colorScheme.primary else Color.Unspecified,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
                Text(
                    text = timerText,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = (diameter.value / 3f).coerceIn(52f, 80f).sp
                    )
                )
            }
        } else {
            sideLabel?.let { side ->
                Text(
                    text = side,
                    modifier = Modifier.align(Alignment.Center).offset(y = -(diameter.value / 5f).dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
            Text(
                text = timerText,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = (diameter.value / 3f).coerceIn(52f, 80f).sp
                )
            )
        }
    }
}

internal fun workoutTimerSupportingText(
    sideLabel: String?,
    phaseLabel: String?,
    showPhaseLabel: Boolean,
    hasUnilateralContext: Boolean
): String? = sideLabel ?: if (showPhaseLabel && !hasUnilateralContext) phaseLabel.orEmpty().uppercase() else null

internal fun workoutTimerSide(
    phase: TrainingPhase,
    secondsRemaining: Int,
    currentSide: ExerciseSide?,
    isStartingExecution: Boolean = false
): ExerciseSide? = when (phase) {
    TrainingPhase.WARMUP,
    TrainingPhase.COUNTDOWN,
    TrainingPhase.REST_BETWEEN_EXERCISES -> currentSide.takeIf { secondsRemaining <= 10 }
    TrainingPhase.REST -> {
        val upcomingSide = if (isStartingExecution) currentSide else currentSide.nextSide()
        upcomingSide.takeIf { secondsRemaining <= 10 }
    }
    else -> currentSide
}

internal fun workoutRemainingFraction(state: TrainingUiState.Workout, nowMillis: Long): Float {
    val durationMillis = state.phaseDurationSeconds.coerceAtLeast(0) * 1_000L
    if (durationMillis == 0L) return 0f
    val endMillis = state.phaseStartedAtMillis + durationMillis
    val remainingMillis = (endMillis - nowMillis).coerceIn(0L, durationMillis)
    return remainingMillis.toFloat() / durationMillis
}

internal fun timerProgressColor(colorScheme: ColorScheme): Color = colorScheme.primary

private val TrainingPhase.label: String?
    get() = when (this) {
        TrainingPhase.WARMUP -> "Calentamiento"
        TrainingPhase.COUNTDOWN -> null
        TrainingPhase.CONCENTRIC -> "Concéntrica"
        TrainingPhase.REPETITION_ANNOUNCEMENT -> "Concéntrica"
        TrainingPhase.ECCENTRIC -> "Excéntrica"
        TrainingPhase.ISOMETRIC -> "Isométrica"
        TrainingPhase.REST -> "Descanso"
        TrainingPhase.REST_BETWEEN_EXERCISES -> "Descanso entre ejercicios"
    }

internal fun workoutMetricPhaseLabel(phase: TrainingPhase): String =
    if (phase == TrainingPhase.REST_BETWEEN_EXERCISES) "Descanso" else phase.label.orEmpty()

private fun Int.toClockFormat(): String {
    val safeSeconds = coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}
