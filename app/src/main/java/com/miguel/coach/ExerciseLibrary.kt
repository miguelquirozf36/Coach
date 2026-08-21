package com.miguel.coach

import java.text.Normalizer
import java.util.Collections

data class ExerciseDefinition(
    val id: String,
    val name: String,
    val category: String,
    val notes: String = ""
)

object ExerciseLibrary {
    const val CHEST = "PECHO"
    const val BACK = "ESPALDA"
    const val LEGS = "PIERNAS"
    const val GLUTES = "GLÚTEOS"
    const val SHOULDERS = "HOMBROS"
    const val BICEPS = "BÍCEPS"
    const val TRICEPS = "TRÍCEPS"
    const val FOREARMS = "ANTEBRAZO"
    const val CALVES = "PANTORRILLAS"
    const val ABS = "ABDOMINALES"

    private val categoryOrder = Collections.unmodifiableList(
        listOf(CHEST, BACK, LEGS, GLUTES, SHOULDERS, BICEPS, TRICEPS, FOREARMS, CALVES, ABS)
    )

    private val officialDefinitions = Collections.unmodifiableList(
        listOf(
            definition("aperturas", "Aperturas", CHEST),
            definition("aperturas-cables", "Aperturas con cables", CHEST),
            definition("aperturas-maquina", "Aperturas en máquina", CHEST),
            definition("fondos-pecho", "Fondos para pecho", CHEST),
            definition("press-banca-barra", "Press banca con barra", CHEST),
            definition("press-banca-plana-mancuernas", "Press banca plana mancuernas", CHEST),
            definition("press-declinado", "Press declinado", CHEST),
            definition("press-inclinado-barra", "Press inclinado con barra", CHEST),
            definition("press-inclinado-mancuernas", "Press inclinado mancuernas", CHEST),
            definition("pullover-mancuerna", "Pullover con mancuerna", CHEST),

            definition("dominadas", "Dominadas", BACK),
            definition("dominadas-polea", "Dominadas en polea", BACK),
            definition("jalon-al-pecho", "Jalón al pecho", BACK),
            definition("jalon-unilateral", "Jalón unilateral polea alta", BACK),
            definition("peso-muerto-convencional", "Peso muerto convencional", BACK),
            definition("remo-barra", "Remo con barra", BACK),
            definition("remo-mancuerna", "Remo con mancuerna", BACK),
            definition("remo-polea-alta", "Remo con polea alta", BACK),
            definition("remo-sentado", "Remo sentado", BACK),
            definition("remo-t", "Remo T", BACK),

            definition("aductores-maquina", "Aductores en máquina", LEGS),
            definition("curl-femoral", "Curl femoral", LEGS),
            definition("extension-pierna", "Extensión de pierna", LEGS),
            definition("prensa", "Prensa", LEGS),
            definition("sentadilla-barra", "Sentadilla con barra", LEGS),
            definition("sentadilla-goblet", "Sentadilla goblet", LEGS),
            definition("sentadilla-hack", "Sentadilla hack", LEGS),
            definition("zancadas", "Zancadas", LEGS),

            definition("abduccion-cadera-acostado", "Abducción de cadera acostado", GLUTES),
            definition("abduccion-cadera-maquina", "Abducción de cadera en máquina", GLUTES),
            definition("abduccion-cadera-polea", "Abducción de cadera en polea", GLUTES),
            definition("bulgaras", "Búlgaras", GLUTES),
            definition("caminata-lateral-banda", "Caminata lateral con banda", GLUTES),
            definition("extension-cadera", "Extensión de cadera", GLUTES),
            definition("extension-cadera-maquina", "Extensión de cadera en máquina", GLUTES),
            definition("extension-cadera-polea", "Extensión de cadera en polea", GLUTES),
            definition("hip-thrust", "Hip thrust", GLUTES),
            definition("hip-thrust-unilateral", "Hip thrust unilateral", GLUTES),
            definition("patada-gluteo-maquina", "Patada de glúteo en máquina", GLUTES),
            definition("patada-gluteo-polea", "Patada de glúteo en polea", GLUTES),
            definition("peso-muerto-rumano", "Peso muerto rumano", GLUTES),
            definition("peso-muerto-rumano-unilateral", "Peso muerto rumano unilateral", GLUTES),
            definition("puente-gluteos", "Puente de glúteos", GLUTES),
            definition("puente-gluteos-unilateral", "Puente de glúteos unilateral", GLUTES),
            definition("sentadilla-sumo", "Sentadilla sumo", GLUTES),
            definition("step-up", "Step-up", GLUTES),
            definition("zancada-atras", "Zancada hacia atrás", GLUTES),

            definition("elevaciones-frontales", "Elevaciones frontales", SHOULDERS),
            definition("elevaciones-laterales", "Elevaciones laterales", SHOULDERS),
            definition("elevaciones-laterales-ligas", "Elevaciones laterales con ligas", SHOULDERS),
            definition("face-pull", "Face pull", SHOULDERS),
            definition("hombro-frontal", "Hombro frontal", SHOULDERS),
            definition("hombro-posterior", "Hombro posterior", SHOULDERS),
            definition("press-arnold", "Press Arnold", SHOULDERS),
            definition("press-militar-barra", "Press militar con barra", SHOULDERS),
            definition("press-militar-mancuernas", "Press militar con mancuernas", SHOULDERS),
            definition("pajaros-mancuernas", "Pájaros con mancuernas", SHOULDERS),

            definition("curl-barra", "Curl con barra", BICEPS),
            definition("curl-concentrado", "Curl concentrado", BICEPS),
            definition("curl-mancuernas", "Curl de bíceps con mancuernas", BICEPS),
            definition("curl-martillo", "Curl de bíceps martillo", BICEPS),
            definition("curl-inclinado", "Curl inclinado con mancuernas", BICEPS),
            definition("curl-polea", "Curl en polea", BICEPS),
            definition("curl-predicador", "Curl de bíceps predicador", BICEPS),
            definition("curl-spider", "Curl spider", BICEPS),

            definition("extension-triceps-alta", "Extensión de tríceps alta", TRICEPS),
            definition("extension-triceps-polea-alta", "Extensión de tríceps polea alta", TRICEPS),
            definition("fondos-triceps", "Fondos para tríceps", TRICEPS),
            definition("patada-triceps", "Patada de tríceps", TRICEPS),
            definition("press-frances", "Press francés", TRICEPS),
            definition("press-cerrado", "Press cerrado", TRICEPS),
            definition("rompecraneos", "Rompecráneos", TRICEPS),
            definition("triceps-cuerda", "Tríceps con cuerda", TRICEPS),

            definition("antebrazo", "Antebrazo", FOREARMS),
            definition("curl-muneca-barra", "Curl de muñeca con barra", FOREARMS),
            definition("curl-muneca-mancuernas", "Curl de muñeca con mancuernas", FOREARMS),
            definition("curl-inverso", "Curl inverso", FOREARMS),
            definition("extension-muneca", "Extensión de muñeca", FOREARMS),
            definition("paseo-granjero", "Paseo del granjero", FOREARMS),
            definition("pronacion-supinacion", "Pronación y supinación", FOREARMS),

            definition("elevacion-talones-prensa", "Elevación de talones en prensa", CALVES),
            definition("pantorrillas", "Pantorrillas", CALVES),
            definition("pantorrillas-burro", "Pantorrillas estilo burro", CALVES),
            definition("pantorrillas-escalon", "Pantorrillas en escalón", CALVES),
            definition("pantorrillas-maquina", "Pantorrillas en máquina", CALVES),
            definition("pantorrillas-sentado", "Pantorrillas sentado", CALVES),
            definition("saltos-puntillas", "Saltos de puntillas", CALVES),

            definition("abdominales", "Abdominales", ABS),
            definition("abdominales-bicicleta", "Abdominales bicicleta", ABS),
            definition("crunch", "Crunch", ABS),
            definition("crunch-polea", "Crunch en polea", ABS),
            definition("elevacion-piernas", "Elevación de piernas", ABS),
            definition("escaladores", "Escaladores", ABS),
            definition("plancha", "Plancha", ABS),
            definition("plancha-lateral", "Plancha lateral", ABS),
            definition("rueda-abdominal", "Rueda abdominal", ABS)
        ).sortedWith(
            compareBy<ExerciseDefinition> { categoryOrder.indexOf(it.category) }
                .thenBy { normalizeExerciseText(it.name) }
        )
    )

    @Volatile
    private var customDefinitions: List<ExerciseDefinition> = emptyList()

    @Volatile
    private var mergedDefinitions: List<ExerciseDefinition> = officialDefinitions

    fun all(): List<ExerciseDefinition> = mergedDefinitions

    fun official(): List<ExerciseDefinition> = officialDefinitions

    fun custom(): List<ExerciseDefinition> = customDefinitions

    fun replaceCustom(exercises: List<ExerciseDefinition>) {
        customDefinitions = immutableSorted(exercises)
        mergedDefinitions = immutableSorted(officialDefinitions + customDefinitions)
    }

    fun categories(): List<String> = categoryOrder

    fun byCategory(category: String): List<ExerciseDefinition> {
        val normalizedCategory = normalizeExerciseText(category)
        return mergedDefinitions.filter { normalizeExerciseText(it.category) == normalizedCategory }
    }

    fun search(text: String): List<ExerciseDefinition> {
        val query = normalizeExerciseText(text.trim())
        return if (query.isEmpty()) mergedDefinitions else immutableAlphabetical(
            mergedDefinitions.filter { normalizeExerciseText(it.name).contains(query) }
        )
    }

    fun find(id: String): ExerciseDefinition? = mergedDefinitions.firstOrNull { it.id == id }

    fun isOfficial(id: String): Boolean = officialDefinitions.any { it.id == id }

    private fun definition(id: String, name: String, category: String) =
        ExerciseDefinition(id = id, name = name, category = category)

    private fun immutableSorted(exercises: List<ExerciseDefinition>): List<ExerciseDefinition> =
        Collections.unmodifiableList(
            exercises.sortedWith(
                compareBy<ExerciseDefinition> { categoryOrder.indexOf(it.category) }
                    .thenBy { normalizeExerciseText(it.name) }
            )
        )

    private fun immutableAlphabetical(exercises: List<ExerciseDefinition>): List<ExerciseDefinition> =
        Collections.unmodifiableList(exercises.sortedBy { normalizeExerciseText(it.name) })
}

fun normalizeExerciseText(value: String): String = Normalizer
    .normalize(value, Normalizer.Form.NFD)
    .replace("\\p{Mn}+".toRegex(), "")
    .lowercase()
