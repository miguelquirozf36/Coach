package com.miguel.coach

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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

internal enum class SettingsDestination { ROOT, APPEARANCE }

internal fun openAppearanceFromSettings(): SettingsDestination = SettingsDestination.APPEARANCE

internal fun backFromSettingsAppearance(): SettingsDestination = SettingsDestination.ROOT

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    userName: String,
    currentTheme: CoachTheme,
    onSaveUserName: (String) -> String?,
    onAppearance: () -> Unit,
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("AJUSTES") }) },
        bottomBar = { MainNavigationBar(selectedTab, onTabSelected) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
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
        }
    }
}

@Composable
private fun SettingsCard(title: String, value: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .sizeIn(minHeight = 48.dp)
            .clickable(onClick = onClick)
    ) {
        ListItem(
            headlineContent = { Text(title, style = MaterialTheme.typography.titleMedium) },
            supportingContent = { Text(value, style = MaterialTheme.typography.bodyLarge) },
            trailingContent = { Text("ABRIR") }
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
