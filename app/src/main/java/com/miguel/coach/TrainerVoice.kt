package com.miguel.coach

import java.util.Locale

const val DEFAULT_TRAINER_VOICE_ID = ""
const val TRAINER_VOICE_SAMPLE = "Preparado para entrenar."

data class TrainerVoiceDescriptor(
    val id: String,
    val locale: Locale,
    val requiresNetwork: Boolean
)

data class TrainerVoiceOption(
    val id: String,
    val label: String,
    val technicalName: String? = null
)

fun spanishOfflineVoiceOptions(voices: Collection<TrainerVoiceDescriptor>): List<TrainerVoiceOption> {
    val eligible = voices
        .filter { it.locale.language.equals("es", ignoreCase = true) && !it.requiresNetwork }
        .sortedWith(compareBy({ friendlySpanishLocale(it.locale) }, { it.id }))
    val localeCounts = eligible.groupingBy { it.locale.toLanguageTag() }.eachCount()
    val localeIndexes = mutableMapOf<String, Int>()
    return eligible.map { voice ->
        val tag = voice.locale.toLanguageTag()
        val index = (localeIndexes[tag] ?: 0) + 1
        localeIndexes[tag] = index
        TrainerVoiceOption(
            id = voice.id,
            label = buildString {
                append(friendlySpanishLocale(voice.locale))
                if (localeCounts[tag].orZero() > 1) append(" — Voz $index")
            }
        )
    }
}

fun resolveTrainerVoiceId(savedId: String?, availableVoiceIds: Collection<String>): String =
    savedId.orEmpty().takeIf { it in availableVoiceIds } ?: DEFAULT_TRAINER_VOICE_ID

fun storedTrainerVoiceToApply(savedId: String?, availableVoiceIds: Collection<String>): String? =
    resolveTrainerVoiceId(savedId, availableVoiceIds).takeIf { it != DEFAULT_TRAINER_VOICE_ID }

internal fun friendlySpanishLocale(locale: Locale): String {
    val language = locale.getDisplayLanguage(Locale.forLanguageTag("es"))
        .replaceFirstChar { it.titlecase(Locale.forLanguageTag("es")) }
        .ifBlank { "Español" }
    val country = locale.getDisplayCountry(Locale.forLanguageTag("es"))
    return if (country.isBlank()) language else "$language ($country)"
}

private fun Int?.orZero(): Int = this ?: 0

class TrainerVoiceSelection(initialVoiceId: String) {
    val originalVoiceId: String = initialVoiceId
    var temporaryVoiceId: String = initialVoiceId
        private set

    fun select(voiceId: String) {
        temporaryVoiceId = voiceId
    }

    fun cancel(): String = originalVoiceId

    fun save(): String = temporaryVoiceId

    fun preview(): Pair<String, String> = temporaryVoiceId to TRAINER_VOICE_SAMPLE
}
