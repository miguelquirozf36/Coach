package com.miguel.coach

data class Routine(
    val id: String,
    val name: String,
    val isCustom: Boolean,
    val exercises: List<Exercise>,
    val restBetweenExercisesSeconds: Int
)

data class Exercise(
    val id: String,
    val name: String,
    val sets: Int,
    val repetitions: Int,
    val concentricSeconds: Int,
    val eccentricSeconds: Int,
    val restSeconds: Int
)

data class RoutineDraft(
    val id: String,
    val name: String,
    val restBetweenExercisesSeconds: String,
    val exercises: List<ExerciseDraft>
)

data class ExerciseDraft(
    val id: String,
    val name: String,
    val sets: String,
    val repetitions: String,
    val concentricSeconds: String,
    val eccentricSeconds: String,
    val restSeconds: String
)

data class RoutineDraftValidation(
    val routine: Routine?,
    val message: String?
)

fun Routine.toDraft() = RoutineDraft(
    id = id,
    name = name,
    restBetweenExercisesSeconds = restBetweenExercisesSeconds.toString(),
    exercises = exercises.map(Exercise::toDraft)
)

fun Exercise.toDraft() = ExerciseDraft(
    id = id,
    name = name,
    sets = sets.toString(),
    repetitions = repetitions.toString(),
    concentricSeconds = concentricSeconds.toString(),
    eccentricSeconds = eccentricSeconds.toString(),
    restSeconds = restSeconds.toString()
)

fun RoutineDraft.validate(isCustom: Boolean): RoutineDraftValidation {
    val errors = mutableListOf<String>()
    val validatedName = name.trim()
    if (validatedName.isEmpty()) errors += "El nombre de la rutina es obligatorio."
    val routineRest = restBetweenExercisesSeconds.toNonNegativeInt("El descanso entre ejercicios", errors)
    val validatedExercises = exercises.map { exercise ->
        val exerciseName = exercise.name.trim()
        if (exerciseName.isEmpty()) errors += "Cada ejercicio debe tener un nombre."
        val sets = exercise.sets.toPositiveInt("Las series", errors)
        val repetitions = exercise.repetitions.toPositiveInt("Las repeticiones", errors)
        val concentric = exercise.concentricSeconds.toNonNegativeInt("El tiempo concéntrico", errors)
        val eccentric = exercise.eccentricSeconds.toNonNegativeInt("El tiempo excéntrico", errors)
        val rest = exercise.restSeconds.toNonNegativeInt("El descanso entre series", errors)
        if (listOf(sets, repetitions, concentric, eccentric, rest).any { it == null }) {
            null
        } else {
            Exercise(
                id = exercise.id,
                name = exerciseName,
                sets = sets!!,
                repetitions = repetitions!!,
                concentricSeconds = concentric!!,
                eccentricSeconds = eccentric!!,
                restSeconds = rest!!
            )
        }
    }
    if (routineRest == null || validatedExercises.any { it == null } || errors.isNotEmpty()) {
        return RoutineDraftValidation(null, errors.joinToString("\n"))
    }
    return RoutineDraftValidation(
        routine = Routine(
            id = id,
            name = validatedName,
            isCustom = isCustom,
            exercises = validatedExercises.filterNotNull(),
            restBetweenExercisesSeconds = routineRest
        ),
        message = null
    )
}

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

object Routines {
    private const val DEFAULT_CONCENTRIC_SECONDS = 1
    private const val DEFAULT_ECCENTRIC_SECONDS = 3
    private const val DEFAULT_REST_SECONDS = 60

    val all = listOf(
        routine(
            id = "day-1-chest-triceps",
            name = "DÍA 1 — PECHO Y TRÍCEPS",
            exercises = listOf(
                exercise("press-banca-plana-mancuernas", "Press banca plana mancuernas", 3, 10),
                exercise("press-inclinado-mancuernas", "Press inclinado mancuernas", 4, 10),
                exercise("aperturas", "Aperturas", 4, 10),
                exercise("hombro-frontal", "Hombro frontal", 4, 12),
                exercise("extension-triceps-alta", "Extensión de tríceps alta", 4, 10),
                exercise("extension-triceps-polea-alta", "Extensión de tríceps polea alta", 4, 10)
            )
        ),
        routine(
            id = "day-2-quadriceps",
            name = "DÍA 2 — CUÁDRICEPS",
            exercises = listOf(
                exercise("pantorrillas-day-2", "Pantorrillas", 4, 15),
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
            name = "DÍA 4 — HOMBRO Y PANTORRILLAS",
            exercises = listOf(
                exercise("press-militar-mancuernas", "Press militar con mancuernas", 4, 10),
                exercise("elevaciones-laterales", "Elevaciones laterales", 4, 10),
                exercise("elevaciones-laterales-ligas", "Elevaciones laterales con ligas", 3, 10),
                exercise("elevaciones-frontales", "Elevaciones frontales", 4, 10),
                exercise("pantorrillas-day-4", "Pantorrillas", 4, 10)
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
                exercise("pantorrillas-day-6", "Pantorrillas", 4, 10),
                exercise("curl-predicador", "Curl de bíceps predicador", 4, 10),
                exercise("curl-mancuernas", "Curl de bíceps con mancuernas", 4, 10),
                exercise("curl-martillo", "Curl de bíceps martillo", 4, 10),
                exercise("antebrazo", "Antebrazo", 4, 10)
            )
        )
    )

    private fun routine(id: String, name: String, exercises: List<Exercise>) = Routine(
        id = id,
        name = name,
        isCustom = false,
        exercises = exercises,
        restBetweenExercisesSeconds = DEFAULT_REST_SECONDS
    )

    private fun exercise(id: String, name: String, sets: Int, repetitions: Int) = Exercise(
        id = id,
        name = name,
        sets = sets,
        repetitions = repetitions,
        concentricSeconds = DEFAULT_CONCENTRIC_SECONDS,
        eccentricSeconds = DEFAULT_ECCENTRIC_SECONDS,
        restSeconds = DEFAULT_REST_SECONDS
    )
}
