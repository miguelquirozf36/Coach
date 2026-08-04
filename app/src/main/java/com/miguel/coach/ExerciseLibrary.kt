package com.miguel.coach

import java.text.Normalizer
import java.util.Collections

data class ExerciseDefinition(
    val id: String,
    val name: String,
    val category: String
)

object ExerciseLibrary {
    const val CHEST = "PECHO"
    const val BACK = "ESPALDA"
    const val LEGS = "PIERNAS"
    const val SHOULDERS = "HOMBROS"
    const val BICEPS = "BÍCEPS"
    const val TRICEPS = "TRÍCEPS"
    const val FOREARMS = "ANTEBRAZO"
    const val CALVES = "PANTORRILLAS"
    const val ABS = "ABDOMINALES"

    private val categoryOrder = Collections.unmodifiableList(
        listOf(CHEST, BACK, LEGS, SHOULDERS, BICEPS, TRICEPS, FOREARMS, CALVES, ABS)
    )

    private val definitions = Collections.unmodifiableList(
        listOf(
            definition("aperturas", "Aperturas", CHEST),
            definition("aperturas-cables", "Aperturas con cables", CHEST),
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
            definition("bulgaras", "Búlgaras", LEGS),
            definition("curl-femoral", "Curl femoral", LEGS),
            definition("extension-cadera", "Extensión de cadera", LEGS),
            definition("extension-pierna", "Extensión de pierna", LEGS),
            definition("hip-thrust", "Hip thrust", LEGS),
            definition("peso-muerto-rumano", "Peso muerto rumano", LEGS),
            definition("prensa", "Prensa", LEGS),
            definition("sentadilla-barra", "Sentadilla con barra", LEGS),
            definition("sentadilla-goblet", "Sentadilla goblet", LEGS),
            definition("sentadilla-hack", "Sentadilla hack", LEGS),
            definition("zancadas", "Zancadas", LEGS),

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
                .thenBy { normalize(it.name) }
        )
    )

    fun all(): List<ExerciseDefinition> = definitions

    fun categories(): List<String> = categoryOrder

    fun byCategory(category: String): List<ExerciseDefinition> {
        val normalizedCategory = normalize(category)
        return definitions.filter { normalize(it.category) == normalizedCategory }
    }

    fun search(text: String): List<ExerciseDefinition> {
        val query = normalize(text.trim())
        return if (query.isEmpty()) definitions else definitions.filter { normalize(it.name).contains(query) }
    }

    fun find(id: String): ExerciseDefinition? = definitions.firstOrNull { it.id == id }

    private fun definition(id: String, name: String, category: String) =
        ExerciseDefinition(id = id, name = name, category = category)

    private fun normalize(value: String): String = Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace("\\p{Mn}+".toRegex(), "")
        .lowercase()
}
