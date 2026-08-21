package com.miguel.coach

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

fun exercisePickerResults(query: String): List<ExerciseDefinition> =
    if (query.isBlank()) emptyList() else ExerciseLibrary.search(query)

fun toggledExerciseCategory(current: String?, selected: String): String? =
    selected.takeUnless { it == current }

internal enum class CategoryExpansionIcon { CHEVRON_RIGHT, EXPAND_MORE }

internal fun categoryExpansionIcon(expanded: Boolean): CategoryExpansionIcon =
    if (expanded) CategoryExpansionIcon.EXPAND_MORE else CategoryExpansionIcon.CHEVRON_RIGHT

internal const val EXERCISE_SEARCH_LABEL = "Buscar ejercicios"

data class ExercisePickerNavigationContext(
    val query: String = "",
    val expandedCategory: String? = null,
    val selectedExerciseId: String? = null
)

fun ExercisePickerNavigationContext.openDetail(id: String): ExercisePickerNavigationContext =
    copy(selectedExerciseId = id)

fun ExercisePickerNavigationContext.closeDetail(): ExercisePickerNavigationContext =
    copy(selectedExerciseId = null)

fun ExercisePickerNavigationContext.afterDeletedExercise(): ExercisePickerNavigationContext =
    copy(selectedExerciseId = null)

internal enum class ExercisePickerBackOutcome {
    CLOSE_MESSAGE, CLOSE_DELETE_CONFIRMATION, CLOSE_FORM, CLOSE_DETAIL, RETURN_TO_EDITOR
}

internal fun exercisePickerSystemBackOutcome(
    messageOpen: Boolean,
    deleteConfirmationOpen: Boolean,
    formOpen: Boolean,
    detailOpen: Boolean
): ExercisePickerBackOutcome = when {
    messageOpen -> ExercisePickerBackOutcome.CLOSE_MESSAGE
    deleteConfirmationOpen -> ExercisePickerBackOutcome.CLOSE_DELETE_CONFIRMATION
    formOpen -> ExercisePickerBackOutcome.CLOSE_FORM
    detailOpen -> ExercisePickerBackOutcome.CLOSE_DETAIL
    else -> ExercisePickerBackOutcome.RETURN_TO_EDITOR
}

fun ExerciseDefinition.canBeManagedByUser(): Boolean = !ExerciseLibrary.isOfficial(id)

fun selectExerciseDefinition(
    exercise: ExerciseDraft,
    definition: ExerciseDefinition
): ExerciseDraft = exercise.copy(name = definition.name, notes = definition.notes)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ExercisePickerScreen(
    customExercises: List<ExerciseDefinition>,
    onBack: () -> Unit,
    onSaveCustomExercise: (String?, String, String, String) -> String?,
    onDeleteCustomExercise: (ExerciseDefinition) -> String?,
    onSelect: (ExerciseDefinition) -> Unit
) {
    var query by rememberSaveable { mutableStateOf("") }
    var expandedCategory by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    val categories = remember { ExerciseLibrary.categories() }
    val customExerciseIds = remember(customExercises) { customExercises.mapTo(hashSetOf()) { it.id } }
    val listState = rememberLazyListState()
    var editingExerciseId by rememberSaveable { mutableStateOf<String?>(null) }
    var showExerciseForm by rememberSaveable { mutableStateOf(false) }
    var pendingDeletionId by rememberSaveable { mutableStateOf<String?>(null) }
    var operationMessage by rememberSaveable { mutableStateOf<String?>(null) }
    val results by remember(query, customExercises) {
        derivedStateOf { exercisePickerResults(query) }
    }
    val editingExercise = customExercises.firstOrNull { it.id == editingExerciseId }
    val selectedExercise = selectedExerciseId?.let(ExerciseLibrary::find)

    if (showExerciseForm) {
        CustomExerciseDialog(
            exercise = editingExercise,
            onDismiss = {
                showExerciseForm = false
                editingExerciseId = null
            },
            onSave = { name, category, notes ->
                val message = onSaveCustomExercise(editingExerciseId, name, category, notes)
                if (message == null) {
                    showExerciseForm = false
                    editingExerciseId = null
                }
                message
            }
        )
    }
    pendingDeletionId?.let { id ->
        customExercises.firstOrNull { it.id == id }?.let { definition ->
            AlertDialog(
                onDismissRequest = { pendingDeletionId = null },
                containerColor = LocalDialogContainerColor.current,
                title = { Text("¿Eliminar ejercicio?") },
                text = { Text("Esta acción no se puede deshacer.") },
                dismissButton = {
                    TextButton(onClick = { pendingDeletionId = null }) { Text("CANCELAR") }
                },
                confirmButton = {
                    TextButton(onClick = {
                        pendingDeletionId = null
                        val message = onDeleteCustomExercise(definition)
                        operationMessage = message
                        if (message == null) selectedExerciseId = null
                    }) { Text("ELIMINAR") }
                }
            )
        }
    }
    operationMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { operationMessage = null },
            containerColor = LocalDialogContainerColor.current,
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { operationMessage = null }) { Text("ACEPTAR") }
            }
        )
    }

    RegisterSystemBackAction {
        when (exercisePickerSystemBackOutcome(
            messageOpen = operationMessage != null,
            deleteConfirmationOpen = pendingDeletionId != null,
            formOpen = showExerciseForm,
            detailOpen = selectedExerciseId != null
        )) {
            ExercisePickerBackOutcome.CLOSE_MESSAGE -> operationMessage = null
            ExercisePickerBackOutcome.CLOSE_DELETE_CONFIRMATION -> pendingDeletionId = null
            ExercisePickerBackOutcome.CLOSE_FORM -> {
                showExerciseForm = false
                editingExerciseId = null
            }
            ExercisePickerBackOutcome.CLOSE_DETAIL -> selectedExerciseId = null
            ExercisePickerBackOutcome.RETURN_TO_EDITOR -> onBack()
        }
    }

    if (selectedExercise != null) {
        ExerciseDetailScreen(
            definition = selectedExercise,
            isCustom = selectedExercise.id in customExerciseIds,
            onBack = { selectedExerciseId = null },
            onAdd = { onSelect(selectedExercise) },
            onEdit = {
                editingExerciseId = selectedExercise.id
                showExerciseForm = true
            },
            onDelete = { pendingDeletionId = selectedExercise.id }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Biblioteca de ejercicios") },
                navigationIcon = {
                    CoachBackButton(onClick = onBack)
                }
            )
        },
        bottomBar = {
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(16.dp),
                onClick = {
                    editingExerciseId = null
                    showExerciseForm = true
                }
            ) { Text("+ NUEVO EJERCICIO") }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(EXERCISE_SEARCH_LABEL) },
                placeholder = { Text("Nombre del ejercicio") },
                singleLine = true
            )
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (query.isBlank()) {
                    categories.forEach { category ->
                        item(key = "category-$category") {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedCategory = toggledExerciseCategory(expandedCategory, category)
                                    },
                                colors = contentCardColors()
                            ) {
                                val expanded = expandedCategory == category
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = when (categoryExpansionIcon(expanded)) {
                                            CategoryExpansionIcon.EXPAND_MORE -> ExpandMoreIcon
                                            CategoryExpansionIcon.CHEVRON_RIGHT -> ChevronRightIcon
                                        },
                                        contentDescription = if (expanded) {
                                            "Contraer categoría"
                                        } else {
                                            "Expandir categoría"
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Text(category, style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }
                        if (expandedCategory == category) {
                            items(
                                items = ExerciseLibrary.byCategory(category),
                                key = { "category-exercise-${it.id}" }
                            ) { definition ->
                                ExerciseDefinitionRow(
                                    definition = definition,
                                    isCustom = definition.id in customExerciseIds,
                                    onOpen = { selectedExerciseId = definition.id }
                                )
                            }
                        }
                    }
                } else {
                    items(results, key = { "search-${it.id}" }) { definition ->
                        ExerciseDefinitionRow(
                            definition = definition,
                            isCustom = definition.id in customExerciseIds,
                            onOpen = { selectedExerciseId = definition.id }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseDefinitionRow(
    definition: ExerciseDefinition,
    isCustom: Boolean,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = contentCardColors()
    ) {
        ListItem(
            headlineContent = {
                Text(
                    definition.name,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(definition.category)
                    if (isCustom) {
                        Text(
                            "Personalizado",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun ExerciseDetailScreen(
    definition: ExerciseDefinition,
    isCustom: Boolean,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detalle del ejercicio") },
                navigationIcon = {
                    CoachBackButton(onClick = onBack)
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "detail-name") {
                Text(
                    definition.name,
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            item(key = "detail-category") {
                Text(definition.category, style = MaterialTheme.typography.titleMedium)
            }
            item(key = "detail-kind") {
                Text(
                    if (isCustom) "Ejercicio personalizado" else "Ejercicio oficial",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (definition.notes.isNotBlank()) {
                item(key = "detail-notes") {
                    Text(
                        definition.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item(key = "detail-add") {
                Button(modifier = Modifier.fillMaxWidth(), onClick = onAdd) {
                    Text("AGREGAR A LA RUTINA")
                }
            }
            if (isCustom) {
                item(key = "detail-edit") {
                    TextButton(modifier = Modifier.fillMaxWidth(), onClick = onEdit) {
                        Text("EDITAR")
                    }
                }
                item(key = "detail-delete") {
                    TextButton(modifier = Modifier.fillMaxWidth(), onClick = onDelete) {
                        Text("ELIMINAR")
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun CustomExerciseDialog(
    exercise: ExerciseDefinition?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> String?
) {
    var name by rememberSaveable(exercise?.id) { mutableStateOf(exercise?.name.orEmpty()) }
    var category by rememberSaveable(exercise?.id) { mutableStateOf(exercise?.category.orEmpty()) }
    var notes by rememberSaveable(exercise?.id) { mutableStateOf(exercise?.notes.orEmpty()) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var validationMessage by rememberSaveable(exercise?.id) { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = LocalDialogContainerColor.current,
        title = { Text(if (exercise == null) "Nuevo ejercicio" else "Editar ejercicio") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState())
                    .imePadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre *") },
                    singleLine = true
                )
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded }
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        readOnly = true,
                        label = { Text("Categoría *") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded)
                        }
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        ExerciseLibrary.categories().forEach { availableCategory ->
                            DropdownMenuItem(
                                text = { Text(availableCategory) },
                                onClick = {
                                    category = availableCategory
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        if (it.length <= MAX_EXERCISE_NOTES_LENGTH) notes = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Notas (opcional)") },
                    minLines = 3,
                    maxLines = 5,
                    supportingText = {
                        Text("${notes.length}/$MAX_EXERCISE_NOTES_LENGTH")
                    }
                )
                validationMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("CANCELAR") } },
        confirmButton = {
            TextButton(onClick = { validationMessage = onSave(name, category, notes) }) {
                Text("GUARDAR")
            }
        }
    )
}

private val ChevronRightIcon: ImageVector = ImageVector.Builder(
    name = "ChevronRight",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(9.29f, 6.71f)
        lineTo(13.58f, 11f)
        lineTo(9.29f, 15.29f)
        lineTo(10.7f, 16.7f)
        lineTo(16.4f, 11f)
        lineTo(10.7f, 5.3f)
        close()
    }
}.build()

private val ExpandMoreIcon: ImageVector = ImageVector.Builder(
    name = "ExpandMore",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(7.41f, 8.59f)
        lineTo(12f, 13.17f)
        lineTo(16.59f, 8.59f)
        lineTo(18f, 10f)
        lineTo(12f, 16f)
        lineTo(6f, 10f)
        close()
    }
}.build()
