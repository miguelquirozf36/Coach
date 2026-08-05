package com.miguel.coach

import java.net.URLDecoder
import java.net.URLEncoder
import java.util.UUID

data class CustomExerciseOperationResult(
    val success: Boolean,
    val exercises: List<ExerciseDefinition>,
    val exercise: ExerciseDefinition? = null,
    val message: String? = null
)

class CustomExerciseRepository(
    private val storage: RoutineStorage,
    private val officialExercises: List<ExerciseDefinition> = ExerciseLibrary.official()
) {
    fun load(): List<ExerciseDefinition> =
        decode(storage.read(CUSTOM_EXERCISES))?.takeIf(::isValid).orEmpty()

    fun save(exercises: List<ExerciseDefinition>): Boolean {
        if (!isValid(exercises)) return false
        return storage.write(CUSTOM_EXERCISES, encode(exercises)) && load() == exercises
    }

    fun isValidForImport(exercises: List<ExerciseDefinition>): Boolean = isValid(exercises)

    fun create(name: String, category: String, notes: String = ""): CustomExerciseOperationResult {
        val current = load()
        val validated = validateDefinition(name, category, notes, current) ?: return failure(current)
        val exercise = validated.copy(id = "custom-exercise-${UUID.randomUUID()}")
        val updated = (current + exercise).sortedBy { normalizeExerciseText(it.name) }
        return if (save(updated)) {
            CustomExerciseOperationResult(true, updated, exercise)
        } else {
            CustomExerciseOperationResult(false, current, message = "No se pudo guardar el ejercicio.")
        }
    }

    fun edit(id: String, name: String, category: String, notes: String = ""): CustomExerciseOperationResult {
        val current = load()
        val existing = current.firstOrNull { it.id == id }
            ?: return CustomExerciseOperationResult(false, current, message = "El ejercicio no existe.")
        val validated = validateDefinition(name, category, notes, current.filterNot { it.id == id })
            ?: return failure(current)
        val edited = validated.copy(id = existing.id)
        val updated = current.map { if (it.id == id) edited else it }
            .sortedBy { normalizeExerciseText(it.name) }
        return if (save(updated)) {
            CustomExerciseOperationResult(true, updated, edited)
        } else {
            CustomExerciseOperationResult(false, current, message = "No se pudo guardar el ejercicio.")
        }
    }

    fun delete(id: String, routines: List<Routine>): CustomExerciseOperationResult {
        val current = load()
        val exercise = current.firstOrNull { it.id == id }
            ?: return CustomExerciseOperationResult(false, current, message = "El ejercicio no existe.")
        val normalizedName = normalizeExerciseText(exercise.name)
        val isInUse = routines.any { routine ->
            routine.exercises.any { normalizeExerciseText(it.name) == normalizedName }
        }
        if (isInUse) {
            return CustomExerciseOperationResult(
                false,
                current,
                message = "Este ejercicio está siendo utilizado por una rutina."
            )
        }
        val updated = current.filterNot { it.id == id }
        return if (save(updated)) {
            CustomExerciseOperationResult(true, updated)
        } else {
            CustomExerciseOperationResult(false, current, message = "No se pudo eliminar el ejercicio.")
        }
    }

    private var validationMessage: String? = null

    private fun validateDefinition(
        name: String,
        category: String,
        notes: String,
        otherCustomExercises: List<ExerciseDefinition>
    ): ExerciseDefinition? {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            validationMessage = "El nombre del ejercicio es obligatorio."
            return null
        }
        val canonicalCategory = ExerciseLibrary.categories().firstOrNull {
            normalizeExerciseText(it) == normalizeExerciseText(category.trim())
        }
        if (canonicalCategory == null) {
            validationMessage = "Selecciona una categoría válida."
            return null
        }
        val trimmedNotes = notes.trim()
        if (trimmedNotes.length > MAX_EXERCISE_NOTES_LENGTH) {
            validationMessage = "Las notas no pueden superar los $MAX_EXERCISE_NOTES_LENGTH caracteres."
            return null
        }
        val normalizedName = normalizeExerciseText(trimmedName)
        if ((officialExercises + otherCustomExercises).any {
                normalizeExerciseText(it.name) == normalizedName
            }
        ) {
            validationMessage = "Ya existe un ejercicio con ese nombre."
            return null
        }
        validationMessage = null
        return ExerciseDefinition("", trimmedName, canonicalCategory, trimmedNotes)
    }

    private fun failure(current: List<ExerciseDefinition>) =
        CustomExerciseOperationResult(false, current, message = validationMessage)

    private fun isValid(exercises: List<ExerciseDefinition>): Boolean {
        val ids = mutableSetOf<String>()
        val names = officialExercises.mapTo(mutableSetOf()) { normalizeExerciseText(it.name) }
        val categories = ExerciseLibrary.categories().toSet()
        return exercises.all { exercise ->
            exercise.id.startsWith("custom-exercise-") &&
                exercise.name.isNotBlank() &&
                exercise.category in categories &&
                exercise.notes.length <= MAX_EXERCISE_NOTES_LENGTH &&
                ids.add(exercise.id) &&
                names.add(normalizeExerciseText(exercise.name))
        }
    }

    private fun encode(exercises: List<ExerciseDefinition>): String = exercises.joinToString("\n") {
        listOf(it.id, it.name, it.category, it.notes).joinToString("\t", transform = ::encodePart)
    }

    private fun decode(value: String?): List<ExerciseDefinition>? {
        return try {
            if (value.isNullOrEmpty()) {
                emptyList()
            } else {
                val decoded = mutableListOf<ExerciseDefinition>()
                for (line in value.lineSequence()) {
                    val parts = line.split('\t')
                    if (parts.size !in 3..4) return null
                    decoded += ExerciseDefinition(
                        id = decodePart(parts[0]),
                        name = decodePart(parts[1]),
                        category = decodePart(parts[2]),
                        notes = parts.getOrNull(3)?.let(::decodePart).orEmpty()
                    )
                }
                decoded
            }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun encodePart(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())

    private fun decodePart(value: String): String = URLDecoder.decode(value, Charsets.UTF_8.name())

    private companion object {
        const val CUSTOM_EXERCISES = "custom_exercises"
    }
}
