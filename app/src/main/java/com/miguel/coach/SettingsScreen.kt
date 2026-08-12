package com.miguel.coach

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
    onBeepVolumeLevelChanged: (Int) -> Unit,
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
internal fun BeepVolumeControl(level: Int, onLevelChanged: (Int) -> Unit) {
    val normalizedLevel = normalizeBeepVolumeLevel(level)
    Card(modifier = Modifier.fillMaxWidth(), colors = contentCardColors()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Volumen del pitido", style = MaterialTheme.typography.titleMedium)
            Text("Nivel $normalizedLevel", style = MaterialTheme.typography.bodyMedium)
            Slider(
                value = normalizedLevel.toFloat(),
                onValueChange = { onLevelChanged(it.roundToInt().coerceIn(1, 5)) },
                valueRange = 1f..5f,
                steps = 3,
                modifier = Modifier.fillMaxWidth()
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
