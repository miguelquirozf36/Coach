package com.miguel.coach

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun ProgramOnboardingScreen(
    programs: List<TrainingProgram>,
    onSelect: (TrainingProgram) -> Unit
) {
    RegisterSystemBackAction { }
    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("¿Cómo quieres entrenar?", style = MaterialTheme.typography.headlineMedium)
        Text("Elige un programa para comenzar. Podrás cambiarlo después.")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(programs.filter(TrainingProgram::builtIn), key = TrainingProgram::id) { program ->
                ProgramCard(program, active = false, onClick = { onSelect(program) })
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProgramsScreen(
    programs: List<TrainingProgram>,
    activeProgramId: String,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onOpen: (TrainingProgram) -> Unit,
    onCreate: (String) -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    if (showCreate) {
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("Crear programa") },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }) },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("CANCELAR") } },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = { onCreate(name.trim()); name = ""; showCreate = false }
                ) { Text("CREAR") }
            }
        )
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text("PROGRAMAS") }) },
        bottomBar = { MainNavigationBar(selectedTab, onTabSelected) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("PROGRAMAS PREDEFINIDOS", style = MaterialTheme.typography.titleMedium) }
            items(programs.filter(TrainingProgram::builtIn), key = TrainingProgram::id) { program ->
                ProgramCard(program, program.id == activeProgramId) { onOpen(program) }
            }
            item { Text("MIS PROGRAMAS", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.titleMedium) }
            items(programs.filterNot(TrainingProgram::builtIn), key = TrainingProgram::id) { program ->
                ProgramCard(program, program.id == activeProgramId) { onOpen(program) }
            }
            item { Button(modifier = Modifier.fillMaxWidth(), onClick = { showCreate = true }) { Text("CREAR PROGRAMA") } }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProgramDetailScreen(
    program: TrainingProgram,
    active: Boolean,
    onBack: () -> Unit,
    onUse: () -> Unit,
    onOpenRoutine: (Routine) -> Unit,
    onRename: ((String) -> Unit)?,
    onAddDay: (() -> Unit)?,
    onDeleteDay: ((Routine) -> Unit)?,
    onDeleteProgram: (() -> Unit)?
) {
    var showRename by remember { mutableStateOf(false) }
    var editedName by remember(program.id) { mutableStateOf(program.name) }
    if (showRename) {
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text("Nombre del programa") },
            text = { OutlinedTextField(value = editedName, onValueChange = { editedName = it }) },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text("CANCELAR") } },
            confirmButton = {
                TextButton(enabled = editedName.isNotBlank(), onClick = {
                    onRename?.invoke(editedName.trim())
                    showRename = false
                }) { Text("GUARDAR") }
            }
        )
    }
    RegisterSystemBackAction(onBack)
    Scaffold(topBar = { TopAppBar(title = { Text(program.name) }, navigationIcon = { CoachBackButton(onClick = onBack) }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text(program.frequency, style = MaterialTheme.typography.titleMedium) }
            item { Text(program.description) }
            if (onRename != null) item { TextButton(onClick = { showRename = true }) { Text("EDITAR NOMBRE") } }
            items(program.routines, key = Routine::id) { routine ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onOpenRoutine(routine) },
                    colors = contentCardColors()
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column { Text(routine.name, fontWeight = FontWeight.SemiBold); Text("${routine.exercises.size} ejercicios") }
                        if (onDeleteDay != null && program.routines.size > 1) {
                            TextButton(onClick = { onDeleteDay(routine) }) { Text("ELIMINAR") }
                        }
                    }
                }
            }
            if (onAddDay != null) item { TextButton(onClick = onAddDay) { Text("AÑADIR DÍA") } }
            item {
                Button(modifier = Modifier.fillMaxWidth(), enabled = !active, onClick = onUse) {
                    Text(if (active) "PROGRAMA ACTIVO" else "USAR ESTE PROGRAMA")
                }
            }
            if (onDeleteProgram != null && !active) item {
                TextButton(modifier = Modifier.fillMaxWidth(), onClick = onDeleteProgram) { Text("ELIMINAR PROGRAMA") }
            }
        }
    }
}

@Composable
private fun ProgramCard(program: TrainingProgram, active: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = contentCardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(program.name.uppercase(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (active) Text("ACTIVO", color = MaterialTheme.colorScheme.primary)
            }
            Text(program.frequency)
            Text(program.description, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
