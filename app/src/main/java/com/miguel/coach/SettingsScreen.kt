package com.miguel.coach

import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

internal enum class SettingsDestination { ROOT, APPEARANCE }

internal fun openAppearanceFromSettings(): SettingsDestination = SettingsDestination.APPEARANCE

internal fun backFromSettingsAppearance(): SettingsDestination = SettingsDestination.ROOT

internal const val EXPORT_BACKUP_DESCRIPTION = "Guardar los datos en un archivo JSON."

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    userName: String,
    currentTheme: CoachTheme,
    beepVolumeLevel: Int,
    trainerVoiceVolumeLevel: Int,
    onBeepVolumeLevelChanged: (Int) -> Unit,
    onTrainerVoiceVolumeLevelChanged: (Int) -> Unit,
    onSaveUserName: (String) -> String?,
    onAppearance: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    pendingBackupImport: Boolean,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    backupMessage: String?,
    onDismissBackupMessage: () -> Unit,
    onReplayTour: () -> Unit,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    var showNameEditor by rememberSaveable { mutableStateOf(false) }
    var showTrainerVoiceSelector by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val trainerVoiceCoach = remember(context) { WorkoutSessionController.trainerVoiceCoach(context) }
    var selectedTrainerVoiceId by rememberSaveable { mutableStateOf(trainerVoiceCoach.selectedVoiceId()) }
    var trainerVoiceOptions by remember { mutableStateOf(emptyList<TrainerVoiceOption>()) }

    LaunchedEffect(trainerVoiceCoach.isReady, showTrainerVoiceSelector) {
        if (trainerVoiceCoach.isReady && showTrainerVoiceSelector) {
            trainerVoiceOptions = trainerVoiceCoach.availableSpanishVoices()
            selectedTrainerVoiceId = trainerVoiceCoach.selectedVoiceId()
        }
    }

    if (showNameEditor) {
        UserNameDialog(
            currentName = userName,
            onDismiss = { showNameEditor = false },
            onSave = { input ->
                onSaveUserName(input).also { message ->
                    if (message == null) showNameEditor = false
                }
            }
        )
    }
    if (showTrainerVoiceSelector) {
        TrainerVoiceDialog(
            currentVoiceId = selectedTrainerVoiceId,
            voices = trainerVoiceOptions,
            onPreview = trainerVoiceCoach::previewVoice,
            onCancel = { originalVoiceId ->
                trainerVoiceCoach.applyVoice(originalVoiceId)
                showTrainerVoiceSelector = false
            },
            onSave = { voiceId ->
                if (trainerVoiceCoach.saveVoice(voiceId)) {
                    selectedTrainerVoiceId = trainerVoiceCoach.selectedVoiceId()
                    showTrainerVoiceSelector = false
                }
            }
        )
    }
    if (pendingBackupImport) {
        AlertDialog(
            onDismissRequest = onCancelImport,
            containerColor = LocalDialogContainerColor.current,
            title = { Text("¿Restaurar copia de seguridad?") },
            text = { Text("Los datos configurables actuales serán reemplazados.") },
            dismissButton = { TextButton(onClick = onCancelImport) { Text("CANCELAR") } },
            confirmButton = { TextButton(onClick = onConfirmImport) { Text("RESTAURAR") } }
        )
    }
    backupMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onDismissBackupMessage,
            containerColor = LocalDialogContainerColor.current,
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = onDismissBackupMessage) { Text("ACEPTAR") }
            }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("AJUSTES") }) },
        bottomBar = { MainNavigationBar(selectedTab, onTabSelected) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("PERFIL", style = MaterialTheme.typography.titleMedium)
            SettingsCard(
                title = "Nombre",
                value = userName.ifEmpty { "No configurado" },
                onClick = { showNameEditor = true }
            )
            Text(
                "APARIENCIA",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium
            )
            SettingsCard(
                title = "Tema actual",
                value = currentTheme.displayName,
                onClick = {
                    openAppearanceFromSettings()
                    onAppearance()
                }
            )
            SettingsCard(
                title = "Ver recorrido de nuevo",
                value = "Repasa las funciones principales de Coach.",
                onClick = onReplayTour
            )
            Text(
                "ENTRENAMIENTO",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium
            )
            BeepVolumeControl(beepVolumeLevel, onBeepVolumeLevelChanged)
            TrainerVoiceVolumeControl(trainerVoiceVolumeLevel, onTrainerVoiceVolumeLevelChanged)
            SettingsCard(
                title = "Voz del entrenador",
                value = "Selecciona una de las voces en español disponibles en tu dispositivo.",
                onClick = {
                    trainerVoiceOptions = trainerVoiceCoach.availableSpanishVoices()
                    selectedTrainerVoiceId = trainerVoiceCoach.selectedVoiceId()
                    showTrainerVoiceSelector = true
                }
            )
            Text(
                "DATOS",
                modifier = Modifier.padding(top = 12.dp),
                style = MaterialTheme.typography.titleMedium
            )
            SettingsCard(
                title = "EXPORTAR COPIA",
                value = EXPORT_BACKUP_DESCRIPTION,
                onClick = onExportBackup
            )
            SettingsCard(
                title = "IMPORTAR COPIA",
                value = "Restaurar los datos desde un archivo JSON.",
                onClick = onImportBackup
            )
        }
    }
}

@Composable
private fun TrainerVoiceDialog(
    currentVoiceId: String,
    voices: List<TrainerVoiceOption>,
    onPreview: (String) -> Unit,
    onCancel: (String) -> Unit,
    onSave: (String) -> Unit
) {
    val selection = remember(currentVoiceId) { TrainerVoiceSelection(currentVoiceId) }
    var temporaryVoiceId by rememberSaveable(currentVoiceId) { mutableStateOf(currentVoiceId) }
    val cancel = { onCancel(selection.cancel()) }
    AlertDialog(
        onDismissRequest = cancel,
        containerColor = LocalDialogContainerColor.current,
        title = { Text("Voz del entrenador") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TrainerVoiceRow(
                    label = "Predeterminada del dispositivo",
                    selected = temporaryVoiceId == DEFAULT_TRAINER_VOICE_ID,
                    onClick = {
                        selection.select(DEFAULT_TRAINER_VOICE_ID)
                        temporaryVoiceId = DEFAULT_TRAINER_VOICE_ID
                    },
                    onPreview = { onPreview(selection.preview(DEFAULT_TRAINER_VOICE_ID).first) }
                )
                voices.forEach { voice ->
                    TrainerVoiceRow(
                        label = voice.label,
                        selected = temporaryVoiceId == voice.id,
                        onClick = {
                            selection.select(voice.id)
                            temporaryVoiceId = voice.id
                        },
                        onPreview = { onPreview(selection.preview(voice.id).first) }
                    )
                }
                if (voices.isEmpty()) {
                    Text(
                        "No se encontraron otras voces españolas disponibles sin conexión.",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        dismissButton = { TextButton(onClick = cancel) { Text("CANCELAR") } },
        confirmButton = {
            TextButton(onClick = { onSave(selection.save()) }) { Text("GUARDAR") }
        }
    )
}

@Composable
private fun TrainerVoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onPreview: () -> Unit
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onPreview) {
                Icon(
                    imageVector = TrainerVoicePreviewIcon,
                    contentDescription = trainerVoicePreviewDescription(label),
                    modifier = Modifier.size(TRAINER_VOICE_PREVIEW_ICON_SIZE),
                    tint = if (selected) contentColor else MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

internal val TRAINER_VOICE_PREVIEW_ICON_SIZE = 24.dp

internal fun trainerVoicePreviewDescription(label: String): String =
    if (label == "Predeterminada del dispositivo") {
        "Probar voz predeterminada del dispositivo"
    } else {
        "Probar $label"
    }

private val TrainerVoicePreviewIcon: ImageVector = ImageVector.Builder(
    name = "TrainerVoicePreview",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 9f)
        verticalLineTo(15f)
        horizontalLineTo(7f)
        lineTo(12f, 20f)
        verticalLineTo(4f)
        lineTo(7f, 9f)
        close()
        moveTo(16.5f, 12f)
        curveTo(16.5f, 10.23f, 15.48f, 8.71f, 14f, 7.97f)
        verticalLineTo(16.02f)
        curveTo(15.48f, 15.29f, 16.5f, 13.77f, 16.5f, 12f)
        close()
        moveTo(14f, 3.23f)
        verticalLineTo(5.29f)
        curveTo(16.89f, 6.15f, 19f, 8.83f, 19f, 12f)
        curveTo(19f, 15.17f, 16.89f, 17.85f, 14f, 18.71f)
        verticalLineTo(20.77f)
        curveTo(18.01f, 19.86f, 21f, 16.28f, 21f, 12f)
        curveTo(21f, 7.72f, 18.01f, 4.14f, 14f, 3.23f)
        close()
    }
}.build()

@Composable
internal fun BeepVolumeControl(level: Int, onLevelChanged: (Int) -> Unit) {
    FiveLevelVolumeControl("Volumen del pitido", level, onLevelChanged)
}

@Composable
internal fun TrainerVoiceVolumeControl(level: Int, onLevelChanged: (Int) -> Unit) {
    FiveLevelVolumeControl("Volumen de la voz", level, onLevelChanged)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun FiveLevelVolumeControl(title: String, level: Int, onLevelChanged: (Int) -> Unit) {
    val normalizedLevel = normalizeAudioVolumeLevel(level)
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.secondaryContainer
    Card(modifier = Modifier.fillMaxWidth(), colors = contentCardColors()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text("Nivel $normalizedLevel", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = normalizedLevel.toFloat(),
                onValueChange = { onLevelChanged(it.roundToInt().coerceIn(1, 5)) },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                thumb = {
                    Box(
                        modifier = Modifier.fillMaxHeight(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .size(12.dp)
                                .background(activeColor, CircleShape)
                        )
                    }
                },
                track = {
                    Canvas(Modifier.fillMaxWidth().height(4.dp)) {
                        val cornerRadius = size.height / 2f
                        drawRoundRect(
                            color = inactiveColor,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                        )
                        val activeWidth = size.width * (normalizedLevel - 1) / 4f
                        if (activeWidth > 0f) {
                            drawRoundRect(
                                color = activeColor,
                                size = androidx.compose.ui.geometry.Size(activeWidth, size.height),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius)
                            )
                        }
                        repeat(5) { index ->
                            val x = size.width * index / 4f
                            drawCircle(
                                color = if (index < normalizedLevel) activeColor else inactiveColor,
                                radius = cornerRadius,
                                center = androidx.compose.ui.geometry.Offset(x, center.y)
                            )
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, value: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .clickable(onClick = onClick),
        colors = contentCardColors()
    ) {
        ListItem(
            headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
            supportingContent = { Text(value, style = MaterialTheme.typography.bodyLarge) },
            trailingContent = {
                Icon(
                    imageVector = RoutineChevronRightIcon,
                    contentDescription = null,
                    modifier = Modifier.size(22.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )
    }
}

@Composable
private fun UserNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> String?
) {
    var input by rememberSaveable(currentName) { mutableStateOf(currentName) }
    var validationMessage by rememberSaveable(currentName) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalDialogContainerColor.current,
        title = { Text("Editar nombre") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { if (it.length <= MAX_USER_NAME_LENGTH) input = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre") },
                    supportingText = { Text("${input.length}/$MAX_USER_NAME_LENGTH") },
                    singleLine = true
                )
                validationMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
        confirmButton = {
            TextButton(onClick = { validationMessage = onSave(input) }) { Text("GUARDAR") }
        }
    )
}
