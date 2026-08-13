package com.miguel.coach

import kotlin.math.roundToInt

data class Routine(
    val id: String,
    val name: String,
    val isCustom: Boolean,
    val exercises: List<Exercise>,
    val restBetweenExercisesSeconds: Int,
    val warmupSeconds: Int = DEFAULT_WARMUP_SECONDS
)

data class Exercise(
    val id: String,
    val name: String,
    val sets: Int,
    val repetitions: Int,
    val concentricSeconds: Int,
    val eccentricSeconds: Int,
    val restSeconds: Int,
    val notes: String = "",
    val executionMode: ExerciseExecutionMode = ExerciseExecutionMode.SIMULTANEOUS,
    val isometricPauseMode: IsometricPauseMode = IsometricPauseMode.NONE,
    val isometricDurationSeconds: Int = 0
)

enum class ExerciseExecutionMode { SIMULTANEOUS, ONE_SIDE_AT_A_TIME }

enum class ExerciseSide { RIGHT, LEFT }

enum class IsometricPauseMode { NONE, SHORTENED, STRETCHED }

fun Routine.estimatedDurationMinutes(): Int {
    val exerciseSeconds = exercises.sumOf { exercise ->
        val repetitionSeconds = exercise.concentricSeconds + exercise.eccentricSeconds +
            exercise.isometricDurationSeconds.takeIf { exercise.isometricPauseMode != IsometricPauseMode.NONE }.orZero()
        val executions = exercise.sets * if (exercise.executionMode == ExerciseExecutionMode.ONE_SIDE_AT_A_TIME) 2 else 1
        val repetitionsSeconds = executions * exercise.repetitions * repetitionSeconds
        val seriesRestSeconds = (executions - 1).coerceAtLeast(0) * exercise.restSeconds
        repetitionsSeconds + seriesRestSeconds
    }
    val exerciseRestSeconds = (exercises.size - 1).coerceAtLeast(0) * restBetweenExercisesSeconds
    val startSeconds = if (warmupSeconds > 0) warmupSeconds else INITIAL_COUNTDOWN_SECONDS
    return ((startSeconds + exerciseSeconds + exerciseRestSeconds) / 60.0).roundToInt()
}

private const val INITIAL_COUNTDOWN_SECONDS = 10

data class RoutineDraft(
    val id: String,
    val name: String,
    val restBetweenExercisesSeconds: String,
    val exercises: List<ExerciseDraft>,
    val warmupMinutes: String = (DEFAULT_WARMUP_SECONDS / 60).toString()
)

data class ExerciseDraft(
    val id: String,
    val name: String,
    val sets: String,
    val repetitions: String,
    val concentricSeconds: String,
    val eccentricSeconds: String,
    val restSeconds: String,
    val notes: String = "",
    val executionMode: ExerciseExecutionMode = ExerciseExecutionMode.SIMULTANEOUS,
    val isometricPauseMode: IsometricPauseMode = IsometricPauseMode.NONE,
    val isometricDurationSeconds: String = "0"
)

data class RoutineDraftValidation(
    val routine: Routine?,
    val message: String?
)

enum class EditorExitChoice { CANCEL, DISCARD, SAVE }

enum class EditorExitResult { STAY, EXIT_WITHOUT_SAVING, SAVE_AND_EXIT }

fun RoutineDraft.hasChangesFrom(original: RoutineDraft): Boolean = this != original

fun editorExitResult(choice: EditorExitChoice): EditorExitResult = when (choice) {
    EditorExitChoice.CANCEL -> EditorExitResult.STAY
    EditorExitChoice.DISCARD -> EditorExitResult.EXIT_WITHOUT_SAVING
    EditorExitChoice.SAVE -> EditorExitResult.SAVE_AND_EXIT
}

fun toggledExpandedExercise(currentId: String?, selectedId: String): String? =
    selectedId.takeUnless { it == currentId }

fun RoutineDraft.addExercise(exercise: ExerciseDraft): RoutineDraft =
    copy(exercises = exercises + exercise)

fun RoutineDraft.removeExercise(exerciseId: String): RoutineDraft =
    copy(exercises = exercises.filterNot { it.id == exerciseId })

fun RoutineDraft.updateExercise(updated: ExerciseDraft): RoutineDraft {
    val index = exercises.indexOfFirst { it.id == updated.id }
    if (index < 0 || exercises[index] == updated) return this
    val changed = exercises.toMutableList()
    changed[index] = updated
    return copy(exercises = changed)
}

fun RoutineDraft.moveExercise(exerciseId: String, offset: Int): RoutineDraft {
    val fromIndex = exercises.indexOfFirst { it.id == exerciseId }
    val toIndex = fromIndex + offset
    if (fromIndex < 0 || toIndex !in exercises.indices) return this
    val reordered = exercises.toMutableList()
    val exercise = reordered.removeAt(fromIndex)
    reordered.add(toIndex, exercise)
    return copy(exercises = reordered)
}

fun Routine.toDraft() = RoutineDraft(
    id = id,
    name = name,
    restBetweenExercisesSeconds = restBetweenExercisesSeconds.toWholeMinutesString(),
    exercises = exercises.map(Exercise::toDraft),
    warmupMinutes = warmupSeconds.toWholeMinutesString()
)

fun Exercise.toDraft() = ExerciseDraft(
    id = id,
    name = name,
    sets = sets.toString(),
    repetitions = repetitions.toString(),
    concentricSeconds = concentricSeconds.toString(),
    eccentricSeconds = eccentricSeconds.toString(),
    restSeconds = restSeconds.toWholeMinutesString(),
    notes = notes,
    executionMode = executionMode,
    isometricPauseMode = isometricPauseMode,
    isometricDurationSeconds = isometricDurationSeconds.toString()
)

fun RoutineDraft.validate(isCustom: Boolean): RoutineDraftValidation {
    val errors = mutableListOf<String>()
    val identifiers = mutableSetOf<String>()
    val validatedName = name.trim()
    if (validatedName.isEmpty()) errors += "El nombre de la rutina es obligatorio."
    if (exercises.isEmpty()) errors += "La rutina debe tener al menos un ejercicio."
    if (id.isBlank() || !identifiers.add(id)) errors += "La rutina debe tener un identificador único."
    val routineRest = restBetweenExercisesSeconds.toNonNegativeMinutesInSeconds(
        "El descanso entre ejercicios",
        errors
    )
    val warmup = warmupMinutes.toNonNegativeMinutesInSeconds("El calentamiento", errors)
    val validatedExercises = exercises.map { exercise ->
        val exerciseName = exercise.name.trim()
        val exerciseNotes = exercise.notes.trim()
        if (exercise.id.isBlank() || !identifiers.add(exercise.id)) errors += "Los ejercicios deben tener identificadores únicos."
        if (exerciseName.isEmpty()) errors += "Cada ejercicio debe tener un nombre."
        if (exerciseNotes.length > MAX_EXERCISE_NOTES_LENGTH) {
            errors += "Las notas no pueden superar los $MAX_EXERCISE_NOTES_LENGTH caracteres."
        }
        val sets = exercise.sets.toPositiveInt("Las series", errors)
        val repetitions = exercise.repetitions.toPositiveInt("Las repeticiones", errors)
        val concentric = exercise.concentricSeconds.toNonNegativeInt("El tiempo concéntrico", errors)
        val eccentric = exercise.eccentricSeconds.toNonNegativeInt("El tiempo excéntrico", errors)
        val rest = exercise.restSeconds.toNonNegativeMinutesInSeconds(
            "El descanso entre series",
            errors
        )
        val isometricDuration = if (exercise.isometricPauseMode == IsometricPauseMode.NONE) {
            0
        } else {
            exercise.isometricDurationSeconds.toIntOrNull()?.takeIf { it > 0 }.also {
                if (it == null) errors += ISOMETRIC_DURATION_ERROR
            }
        }
        if (listOf(sets, repetitions, concentric, eccentric, rest, isometricDuration).any { it == null }) {
            null
        } else {
            Exercise(
                id = exercise.id,
                name = exerciseName,
                sets = sets!!,
                repetitions = repetitions!!,
                concentricSeconds = concentric!!,
                eccentricSeconds = eccentric!!,
                restSeconds = rest!!,
                notes = exerciseNotes,
                executionMode = exercise.executionMode,
                isometricPauseMode = exercise.isometricPauseMode,
                isometricDurationSeconds = isometricDuration!!
            )
        }
    }
    if (routineRest == null || warmup == null || validatedExercises.any { it == null } || errors.isNotEmpty()) {
        return RoutineDraftValidation(null, errors.joinToString("\n"))
    }
    return RoutineDraftValidation(
        routine = Routine(
            id = id,
            name = validatedName,
            isCustom = isCustom,
            exercises = validatedExercises.filterNotNull(),
            restBetweenExercisesSeconds = routineRest,
            warmupSeconds = warmup
        ),
        message = null
    )
}

const val MAX_EXERCISE_NOTES_LENGTH = 300
const val DEFAULT_WARMUP_SECONDS = 600
const val DEFAULT_ROUTINE_REST_SECONDS = 180
const val DEFAULT_ECCENTRIC_SECONDS = 2
const val DEFAULT_SERIES_REST_SECONDS = 120
const val ISOMETRIC_DURATION_ERROR = "Ingresa una duración mayor a 0 segundos."

private fun Int?.orZero(): Int = this ?: 0

fun emptyCustomRoutine(id: String): Routine = Routine(
    id = id,
    name = "Nueva rutina",
    isCustom = true,
    exercises = emptyList(),
    restBetweenExercisesSeconds = DEFAULT_ROUTINE_REST_SECONDS
)

fun emptyCustomExercise(id: String): Exercise = Exercise(
    id = id,
    name = "Nuevo ejercicio",
    sets = 1,
    repetitions = 1,
    concentricSeconds = 1,
    eccentricSeconds = DEFAULT_ECCENTRIC_SECONDS,
    restSeconds = DEFAULT_SERIES_REST_SECONDS
)

private fun String.toPositiveInt(label: String, errors: MutableList<String>): Int? {
    val value = toIntOrNull()
    if (value == null || value < 1) {
        errors += "$label debe ser al menos 1."
        return null
    }
    return value
}

private fun String.toNonNegativeInt(label: String, errors: MutableList<String>): Int? {
    val value = toIntOrNull()
    if (value == null || value < 0) {
        errors += "$label no puede ser negativo."
        return null
    }
    return value
}

private fun String.toNonNegativeMinutesInSeconds(
    label: String,
    errors: MutableList<String>
): Int? {
    val minutes = toLongOrNull()
    if (minutes == null || minutes < 0 || minutes > Int.MAX_VALUE / 60L) {
        errors += "$label debe ser una cantidad válida de minutos."
        return null
    }
    return (minutes * 60L).toInt()
}

private fun Int.toWholeMinutesString(): String = (coerceAtLeast(0) / 60).toString()

object Routines {
    private const val DEFAULT_CONCENTRIC_SECONDS = 1

    val all = listOf(
        routine(
            id = "day-1-chest-triceps",
            name = "DÍA 1 — PECHO Y TRÍCEPS",
            exercises = listOf(
                exercise("press-inclinado-mancuernas", "Press inclinado con mancuernas", 4, 12),
                exercise("fondos-triceps", "Fondos en paralelas", 4, 10, eccentricSeconds = 1),
                exercise("aperturas-maquina", "Aperturas en máquina", 4, 12),
                exercise("hombro-frontal", "Hombro frontal", 4, 12),
                exercise("extension-triceps-alta", "Extensión de tríceps en polea baja", 4, 12),
                exercise("extension-triceps-polea-alta", "Extensión de tríceps en polea alta", 3, 12)
            )
        ),
        routine(
            id = "day-2-quadriceps",
            name = "DÍA 2 — CUÁDRICEPS",
            exercises = listOf(
                exercise("prensa", "Prensa", 4, 10),
                exercise("bulgaras", "Búlgaras", 3, 10),
                exercise("extension-pierna", "Extensión de pierna", 3, 10),
                exercise("extension-cadera", "Extensión de cadera", 4, 10)
            )
        ),
        routine(
            id = "day-3-back",
            name = "DÍA 3 — ESPALDA",
            exercises = listOf(
                exercise("dominadas-polea", "Dominadas en polea", 3, 10),
                exercise("remo-polea-alta", "Remo con polea alta", 3, 10),
                exercise("jalon-unilateral", "Jalón unilateral polea alta", 3, 10),
                exercise("remo-sentado", "Remo sentado", 4, 10),
                exercise("hombro-posterior", "Hombro posterior", 3, 12)
            )
        ),
        routine(
            id = "day-4-shoulders-calves",
            name = "DÍA 4 — HOMBRO",
            exercises = listOf(
                exercise("press-militar-mancuernas", "Press militar con mancuernas", 4, 10),
                exercise("elevaciones-laterales", "Elevaciones laterales", 4, 10),
                exercise("elevaciones-laterales-ligas", "Elevaciones laterales con ligas", 3, 10),
                exercise("elevaciones-frontales", "Elevaciones frontales", 4, 10)
            )
        ),
        routine(
            id = "day-5-hamstrings",
            name = "DÍA 5 — ISQUIOS",
            exercises = listOf(
                exercise("peso-muerto-rumano", "Peso muerto rumano", 5, 8),
                exercise("curl-femoral", "Curl femoral", 5, 10),
                exercise("abdominales", "Abdominales", 5, 20)
            )
        ),
        routine(
            id = "day-6-biceps-forearm",
            name = "DÍA 6 — BÍCEPS Y ANTEBRAZO",
            exercises = listOf(
                exercise("curl-predicador", "Curl de bíceps predicador", 4, 10),
                exercise("curl-mancuernas", "Curl de bíceps con mancuernas", 4, 10),
                exercise("curl-martillo", "Curl de bíceps martillo", 4, 10),
                exercise("antebrazo", "Antebrazo", 4, 10)
            )
        ),
        routine(
            id = "day-7-calves",
            name = "DÍA 7 — PANTORRILLAS",
            exercises = listOf(
                exercise(
                    id = "pantorrillas-day-7",
                    name = "Pantorrillas",
                    sets = 5,
                    repetitions = 15,
                    eccentricSeconds = 1
                )
            ),
            warmupSeconds = 10 * 60
        )
    )

    private fun routine(
        id: String,
        name: String,
        exercises: List<Exercise>,
        warmupSeconds: Int = DEFAULT_WARMUP_SECONDS
    ) = Routine(
        id = id,
        name = name,
        isCustom = false,
        exercises = exercises,
        restBetweenExercisesSeconds = DEFAULT_ROUTINE_REST_SECONDS,
        warmupSeconds = warmupSeconds
    )

    private fun exercise(
        id: String,
        name: String,
        sets: Int,
        repetitions: Int,
        eccentricSeconds: Int = DEFAULT_ECCENTRIC_SECONDS
    ) = Exercise(
        id = id,
        name = name,
        sets = sets,
        repetitions = repetitions,
        concentricSeconds = DEFAULT_CONCENTRIC_SECONDS,
        eccentricSeconds = eccentricSeconds,
        restSeconds = DEFAULT_SERIES_REST_SECONDS
    )
}
