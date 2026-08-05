package com.miguel.coach

import java.time.Instant

const val COACH_BACKUP_VERSION = 1

data class CoachBackupDocument(
    val backupVersion: Int,
    val exportedAt: String,
    val routines: List<Routine>,
    val customExercises: List<ExerciseDefinition>,
    val userName: String,
    val themeId: String
)

data class CoachBackupResult(
    val success: Boolean,
    val message: String,
    val document: CoachBackupDocument? = null
)

object CoachBackupCodec {
    fun encode(document: CoachBackupDocument): String = buildString {
        append("{\n  \"backupVersion\": ").append(document.backupVersion)
        append(",\n  \"exportedAt\": ").appendJsonString(document.exportedAt)
        append(",\n  \"routines\": [")
        document.routines.forEachIndexed { index, routine ->
            if (index > 0) append(',')
            append("\n    ")
            appendRoutine(routine, "    ")
        }
        if (document.routines.isNotEmpty()) append('\n').append("  ")
        append("],\n  \"customExercises\": [")
        document.customExercises.forEachIndexed { index, exercise ->
            if (index > 0) append(',')
            append("\n    {\"id\": ").appendJsonString(exercise.id)
            append(", \"name\": ").appendJsonString(exercise.name)
            append(", \"category\": ").appendJsonString(exercise.category)
            append(", \"notes\": ").appendJsonString(exercise.notes).append('}')
        }
        if (document.customExercises.isNotEmpty()) append('\n').append("  ")
        append("],\n  \"userPreferences\": {\"user_name\": ")
        appendJsonString(document.userName)
        append("},\n  \"themePreference\": {\"selected_theme\": ")
        appendJsonString(document.themeId)
        append("}\n}")
    }

    fun decode(json: String): CoachBackupResult {
        return try {
            val root = JsonParser(json).parse() as? JsonValue.ObjectValue
                ?: return CoachBackupResult(false, "El archivo no contiene un documento JSON válido.")
            val version = root.requiredInt("backupVersion")
            if (version != COACH_BACKUP_VERSION) {
                return CoachBackupResult(false, "La versión de la copia no es compatible.")
            }
            val document = CoachBackupDocument(
                backupVersion = version,
                exportedAt = root.requiredString("exportedAt"),
                routines = root.requiredArray("routines").values.map(::decodeRoutine),
                customExercises = root.requiredArray("customExercises").values.map(::decodeCustomExercise),
                userName = root.requiredObject("userPreferences").requiredString("user_name"),
                themeId = root.requiredObject("themePreference").requiredString("selected_theme")
            )
            CoachBackupResult(true, "Copia válida.", document)
        } catch (_: IllegalArgumentException) {
            CoachBackupResult(false, "La copia está corrupta o incompleta.")
        }
    }

    private fun StringBuilder.appendRoutine(routine: Routine, indent: String) {
        append("{\"id\": ").appendJsonString(routine.id)
        append(", \"name\": ").appendJsonString(routine.name)
        append(", \"isCustom\": ").append(routine.isCustom)
        append(", \"restBetweenExercisesSeconds\": ").append(routine.restBetweenExercisesSeconds)
        append(", \"warmupSeconds\": ").append(routine.warmupSeconds)
        append(", \"exercises\": [")
        routine.exercises.forEachIndexed { index, exercise ->
            if (index > 0) append(',')
            append("\n").append(indent).append("  {\"id\": ").appendJsonString(exercise.id)
            append(", \"name\": ").appendJsonString(exercise.name)
            append(", \"sets\": ").append(exercise.sets)
            append(", \"repetitions\": ").append(exercise.repetitions)
            append(", \"concentricSeconds\": ").append(exercise.concentricSeconds)
            append(", \"eccentricSeconds\": ").append(exercise.eccentricSeconds)
            append(", \"restSeconds\": ").append(exercise.restSeconds)
            append(", \"notes\": ").appendJsonString(exercise.notes).append('}')
        }
        if (routine.exercises.isNotEmpty()) append('\n').append(indent)
        append("]}")
    }

    private fun StringBuilder.appendJsonString(value: String): StringBuilder = apply {
        append('"')
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\b' -> append("\\b")
                '\u000C' -> append("\\f")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 0x20) {
                    append("\\u%04x".format(character.code))
                } else {
                    append(character)
                }
            }
        }
        append('"')
    }

    private fun decodeRoutine(value: JsonValue): Routine {
        val fields = value.asObject()
        return Routine(
            id = fields.requiredString("id"),
            name = fields.requiredString("name"),
            isCustom = fields.requiredBoolean("isCustom"),
            exercises = fields.requiredArray("exercises").values.map(::decodeExercise),
            restBetweenExercisesSeconds = fields.requiredInt("restBetweenExercisesSeconds"),
            warmupSeconds = fields.requiredInt("warmupSeconds")
        )
    }

    private fun decodeExercise(value: JsonValue): Exercise {
        val fields = value.asObject()
        return Exercise(
            id = fields.requiredString("id"),
            name = fields.requiredString("name"),
            sets = fields.requiredInt("sets"),
            repetitions = fields.requiredInt("repetitions"),
            concentricSeconds = fields.requiredInt("concentricSeconds"),
            eccentricSeconds = fields.requiredInt("eccentricSeconds"),
            restSeconds = fields.requiredInt("restSeconds"),
            notes = fields.requiredString("notes")
        )
    }

    private fun decodeCustomExercise(value: JsonValue): ExerciseDefinition {
        val fields = value.asObject()
        return ExerciseDefinition(
            id = fields.requiredString("id"),
            name = fields.requiredString("name"),
            category = fields.requiredString("category"),
            notes = fields.requiredString("notes")
        )
    }

    private fun JsonValue.asObject(): JsonValue.ObjectValue =
        this as? JsonValue.ObjectValue ?: throw IllegalArgumentException()

    private fun JsonValue.ObjectValue.required(name: String): JsonValue =
        values[name] ?: throw IllegalArgumentException()

    private fun JsonValue.ObjectValue.requiredObject(name: String): JsonValue.ObjectValue =
        required(name) as? JsonValue.ObjectValue ?: throw IllegalArgumentException()

    private fun JsonValue.ObjectValue.requiredArray(name: String): JsonValue.ArrayValue =
        required(name) as? JsonValue.ArrayValue ?: throw IllegalArgumentException()

    private fun JsonValue.ObjectValue.requiredString(name: String): String =
        (required(name) as? JsonValue.StringValue)?.value ?: throw IllegalArgumentException()

    private fun JsonValue.ObjectValue.requiredInt(name: String): Int =
        (required(name) as? JsonValue.NumberValue)?.value?.toIntOrNull()
            ?: throw IllegalArgumentException()

    private fun JsonValue.ObjectValue.requiredBoolean(name: String): Boolean =
        (required(name) as? JsonValue.BooleanValue)?.value ?: throw IllegalArgumentException()
}

class CoachBackupManager(
    private val routineRepository: RoutineRepository,
    private val userPreferenceRepository: UserPreferenceRepository,
    private val themePreferenceRepository: ThemePreferenceRepository,
    private val now: () -> String = { Instant.now().toString() }
) {
    fun createDocument(): CoachBackupDocument = CoachBackupDocument(
        backupVersion = COACH_BACKUP_VERSION,
        exportedAt = now(),
        routines = routineRepository.load(),
        customExercises = routineRepository.loadCustomExercises(),
        userName = userPreferenceRepository.loadUserName(),
        themeId = themePreferenceRepository.load().id
    )

    fun restore(document: CoachBackupDocument?, workoutActive: Boolean): CoachBackupResult {
        if (document == null) return CoachBackupResult(false, "Importación cancelada.")
        if (workoutActive) {
            return CoachBackupResult(false, "No se puede importar durante un entrenamiento activo.")
        }
        val validated = validate(document)
        if (!validated.success) return validated
        val safeDocument = validated.document!!
        val previous = createDocument()
        val restored = routineRepository.save(safeDocument.routines) &&
            routineRepository.saveCustomExercises(safeDocument.customExercises) &&
            userPreferenceRepository.saveUserName(safeDocument.userName).saved &&
            themePreferenceRepository.save(CoachTheme.fromId(safeDocument.themeId))
        if (restored) return CoachBackupResult(true, "Copia restaurada correctamente.", safeDocument)

        val rolledBack = routineRepository.save(previous.routines) &&
            routineRepository.saveCustomExercises(previous.customExercises) &&
            userPreferenceRepository.saveUserName(previous.userName).saved &&
            themePreferenceRepository.save(CoachTheme.fromId(previous.themeId))
        val message = if (rolledBack) {
            "No se pudo restaurar la copia. Se recuperaron los datos anteriores."
        } else {
            "No se pudo restaurar la copia ni recuperar todos los datos anteriores."
        }
        return CoachBackupResult(false, message)
    }

    fun validate(document: CoachBackupDocument): CoachBackupResult {
        if (document.backupVersion != COACH_BACKUP_VERSION) {
            return CoachBackupResult(false, "La versión de la copia no es compatible.")
        }
        val hasValidExportDate = runCatching { Instant.parse(document.exportedAt) }.isSuccess
        val requiredRoutineIds = Routines.all.mapTo(mutableSetOf()) { it.id }
        val importedBaseRoutineIds = document.routines
            .asSequence()
            .filterNot(Routine::isCustom)
            .mapTo(mutableSetOf(), Routine::id)
        if (!hasValidExportDate ||
            importedBaseRoutineIds != requiredRoutineIds ||
            !routineRepository.acceptsRoutines(document.routines)
        ) {
            return CoachBackupResult(false, "La copia contiene rutinas inválidas o incompletas.")
        }
        if (!routineRepository.acceptsCustomExercises(document.customExercises)) {
            return CoachBackupResult(false, "La copia contiene ejercicios personalizados inválidos.")
        }
        val userName = validateUserName(document.userName).value
            ?: return CoachBackupResult(false, "La copia contiene un nombre de usuario inválido.")
        if (CoachTheme.entries.none { it.id == document.themeId }) {
            return CoachBackupResult(false, "La copia contiene un tema incompatible.")
        }
        return CoachBackupResult(true, "Copia válida.", document.copy(userName = userName))
    }
}
