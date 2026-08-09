package com.miguel.coach

import android.content.SharedPreferences

data class TrainingProgram(
    val id: String,
    val name: String,
    val description: String,
    val frequency: String,
    val routines: List<Routine>,
    val builtIn: Boolean
)

interface TrainingProgramStorage {
    fun readPrograms(): String?
    fun writePrograms(value: String): Boolean
    fun readSelectedProgramId(): String?
    fun writeSelectedProgramId(id: String?): Boolean
}

class SharedPreferencesTrainingProgramStorage(
    private val preferences: SharedPreferences
) : TrainingProgramStorage {
    override fun readPrograms(): String? = preferences.getString(PROGRAMS_KEY, null)
    override fun writePrograms(value: String): Boolean = preferences.edit().putString(PROGRAMS_KEY, value).commit()
    override fun readSelectedProgramId(): String? = preferences.getString(SELECTED_KEY, null)
    override fun writeSelectedProgramId(id: String?): Boolean = preferences.edit().run {
        if (id == null) remove(SELECTED_KEY) else putString(SELECTED_KEY, id)
    }.commit()

    private companion object {
        const val PROGRAMS_KEY = "training_programs_json"
        const val SELECTED_KEY = "selectedProgramId"
    }
}

class TrainingProgramRepository(
    private val storage: TrainingProgramStorage
) {
    fun loadPrograms(legacyRoutines: List<Routine>, existingInstallation: Boolean): List<TrainingProgram> {
        TrainingProgramCodec.decode(storage.readPrograms())?.takeIf { validPrograms(it) }?.let { return it }
        val legacyWeider = legacyRoutines.filterNot(Routine::isCustom)
        val official = OfficialTrainingPrograms.create(
            weiderRoutines = legacyWeider.takeIf { existingInstallation && it.isNotEmpty() } ?: Routines.all
        )
        val customRoutines = legacyRoutines.filter(Routine::isCustom)
        val migrated = if (customRoutines.isEmpty()) official else official + TrainingProgram(
            id = "my-routines",
            name = "Mis rutinas",
            description = "Rutinas personalizadas migradas.",
            frequency = "${customRoutines.size} días",
            routines = customRoutines,
            builtIn = false
        )
        savePrograms(migrated)
        if (existingInstallation && loadSelectedProgramId() == null) selectProgram(OfficialTrainingPrograms.WEIDER_ID)
        return migrated
    }

    fun savePrograms(programs: List<TrainingProgram>): Boolean =
        validPrograms(programs) && storage.writePrograms(TrainingProgramCodec.encode(programs))

    fun loadSelectedProgramId(): String? = storage.readSelectedProgramId()

    fun selectProgram(id: String): Boolean = storage.writeSelectedProgramId(id)

    fun clearSelectedProgram(): Boolean = storage.writeSelectedProgramId(null)

    fun acceptsPrograms(programs: List<TrainingProgram>): Boolean = validPrograms(programs)

    private fun validPrograms(programs: List<TrainingProgram>): Boolean {
        if (programs.isEmpty() || programs.map(TrainingProgram::id).toSet().size != programs.size) return false
        return programs.all { program ->
            program.id.isNotBlank() && program.name.isNotBlank() && program.description.isNotBlank() &&
                program.frequency.isNotBlank() && program.routines.isNotEmpty() &&
                RoutineJsonCodec.decode(RoutineJsonCodec.encode(program.routines)) == program.routines
        }
    }
}

internal fun shouldShowProgramOnboarding(selectedProgramId: String?): Boolean = selectedProgramId == null

internal fun activeTrainingProgram(
    programs: List<TrainingProgram>,
    selectedProgramId: String?
): TrainingProgram? = programs.firstOrNull { it.id == selectedProgramId }

object OfficialTrainingPrograms {
    const val FULL_BODY_ID = "full-body"
    const val PPL_ID = "push-pull-legs"
    const val UPPER_LOWER_ID = "upper-lower"
    const val WEIDER_ID = "weider"

    fun create(weiderRoutines: List<Routine> = Routines.all): List<TrainingProgram> = listOf(
        TrainingProgram(
            FULL_BODY_ID,
            "Full Body",
            "Entrena todo el cuerpo en cada sesión.",
            "3 días por semana",
            listOf(
                programRoutine("full-body-a", "FULL BODY A", "press-banca-plana-mancuernas", "remo-sentado", "prensa", "peso-muerto-rumano", "press-militar-mancuernas", "curl-mancuernas"),
                programRoutine("full-body-b", "FULL BODY B", "press-inclinado-mancuernas", "dominadas-polea", "bulgaras", "curl-femoral", "elevaciones-laterales", "extension-triceps-alta"),
                programRoutine("full-body-c", "FULL BODY C", "aperturas", "jalon-unilateral", "extension-pierna", "extension-cadera", "hombro-posterior", "pantorrillas")
            ),
            true
        ),
        TrainingProgram(
            PPL_ID,
            "Push / Pull / Legs",
            "Empuje, tirón y piernas en una división de seis sesiones.",
            "6 días por semana",
            listOf(
                programRoutine("ppl-push-a", "PUSH A", "press-banca-plana-mancuernas", "press-inclinado-mancuernas", "press-militar-mancuernas", "extension-triceps-alta"),
                programRoutine("ppl-pull-a", "PULL A", "dominadas-polea", "remo-sentado", "hombro-posterior", "curl-predicador"),
                programRoutine("ppl-legs-a", "LEGS A", "prensa", "bulgaras", "curl-femoral", "pantorrillas"),
                programRoutine("ppl-push-b", "PUSH B", "press-inclinado-mancuernas", "aperturas", "elevaciones-laterales", "extension-triceps-polea-alta"),
                programRoutine("ppl-pull-b", "PULL B", "remo-polea-alta", "jalon-unilateral", "curl-mancuernas", "antebrazo"),
                programRoutine("ppl-legs-b", "LEGS B", "extension-pierna", "peso-muerto-rumano", "extension-cadera", "pantorrillas")
            ),
            true
        ),
        TrainingProgram(
            UPPER_LOWER_ID,
            "Torso / Pierna",
            "Alterna tren superior y tren inferior.",
            "4 días por semana",
            listOf(
                programRoutine("upper-lower-upper-a", "TORSO A", "press-banca-plana-mancuernas", "dominadas-polea", "press-militar-mancuernas", "curl-predicador", "extension-triceps-alta"),
                programRoutine("upper-lower-lower-a", "PIERNA A", "prensa", "bulgaras", "curl-femoral", "pantorrillas"),
                programRoutine("upper-lower-upper-b", "TORSO B", "press-inclinado-mancuernas", "remo-sentado", "elevaciones-laterales", "curl-mancuernas", "extension-triceps-polea-alta"),
                programRoutine("upper-lower-lower-b", "PIERNA B", "extension-pierna", "peso-muerto-rumano", "extension-cadera", "pantorrillas")
            ),
            true
        ),
        TrainingProgram(
            WEIDER_ID,
            "Weider / División por grupos musculares",
            "División por grupos musculares.",
            "7 días por semana",
            weiderRoutines,
            true
        )
    )

    private fun programRoutine(id: String, name: String, vararg exerciseIds: String): Routine = Routine(
        id = id,
        name = name,
        isCustom = false,
        exercises = exerciseIds.map { exerciseId ->
            val definition = ExerciseLibrary.find(exerciseId) ?: error("Ejercicio oficial inexistente: $exerciseId")
            Exercise(
                id = "$id-$exerciseId",
                name = definition.name,
                sets = 4,
                repetitions = 10,
                concentricSeconds = 1,
                eccentricSeconds = 2,
                restSeconds = 120
            )
        },
        restBetweenExercisesSeconds = 180,
        warmupSeconds = 600
    )
}

internal object TrainingProgramCodec {
    fun encode(programs: List<TrainingProgram>): String = buildString {
        append("{\"programs\":[")
        programs.forEachIndexed { index, program ->
            if (index > 0) append(',')
            append("{\"id\":").appendQuoted(program.id)
            append(",\"name\":").appendQuoted(program.name)
            append(",\"description\":").appendQuoted(program.description)
            append(",\"frequency\":").appendQuoted(program.frequency)
            append(",\"builtIn\":").append(program.builtIn)
            append(",\"routinesJson\":").appendQuoted(RoutineJsonCodec.encode(program.routines))
            append('}')
        }
        append("]}")
    }

    fun decode(json: String?): List<TrainingProgram>? {
        if (json == null) return null
        return try {
            val root = JsonParser(json).parse() as? JsonValue.ObjectValue ?: return null
            val values = (root.values["programs"] as? JsonValue.ArrayValue)?.values ?: return null
            values.map { value ->
                val fields = (value as? JsonValue.ObjectValue)?.values ?: throw IllegalArgumentException()
                TrainingProgram(
                    id = fields.string("id"),
                    name = fields.string("name"),
                    description = fields.string("description"),
                    frequency = fields.string("frequency"),
                    routines = RoutineJsonCodec.decode(fields.string("routinesJson")) ?: throw IllegalArgumentException(),
                    builtIn = (fields["builtIn"] as? JsonValue.BooleanValue)?.value ?: throw IllegalArgumentException()
                )
            }
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    private fun MutableMap<String, JsonValue>.string(name: String): String =
        (this[name] as? JsonValue.StringValue)?.value ?: throw IllegalArgumentException()

    private fun StringBuilder.appendQuoted(value: String): StringBuilder = apply {
        append('"')
        value.forEach { character ->
            when (character) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(character)
            }
        }
        append('"')
    }
}
