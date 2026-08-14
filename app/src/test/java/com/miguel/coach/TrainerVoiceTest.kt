package com.miguel.coach

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainerVoiceTest {
    @Test
    fun defaultSelectionUsesDeviceDefault() {
        assertEquals(DEFAULT_TRAINER_VOICE_ID, resolveTrainerVoiceId(null, emptyList()))
        assertEquals(null, storedTrainerVoiceToApply(null, emptyList()))
    }

    @Test
    fun existingInstallWithoutPreferenceDoesNotOverrideCurrentDefaultVoice() {
        assertEquals(null, storedTrainerVoiceToApply(null, listOf("available")))
        assertEquals(null, storedTrainerVoiceToApply("missing", listOf("available")))
        assertEquals("available", storedTrainerVoiceToApply("available", listOf("available")))
    }

    @Test
    fun filtersNonSpanishAndNetworkRequiredVoices() {
        val options = spanishOfflineVoiceOptions(
            listOf(
                voice("spanish-local", "es-MX"),
                voice("english-local", "en-US"),
                voice("spanish-network", "es-ES", requiresNetwork = true)
            )
        )
        assertEquals(listOf("spanish-local"), options.map { it.id })
    }

    @Test
    fun acceptsSpanishVoicesFromEveryCountry() {
        val options = spanishOfflineVoiceOptions(
            listOf(voice("mx", "es-MX"), voice("pe", "es-PE"), voice("es", "es-ES"), voice("ar", "es-AR"))
        )
        assertEquals(setOf("mx", "pe", "es", "ar"), options.mapTo(mutableSetOf()) { it.id })
    }

    @Test
    fun sameLocaleVoicesRemainDistinct() {
        val options = spanishOfflineVoiceOptions(listOf(voice("voice-a", "es-MX"), voice("voice-b", "es-MX")))
        assertEquals(2, options.size)
        assertTrue(options[0].label.endsWith("Voz 1"))
        assertTrue(options[1].label.endsWith("Voz 2"))
        assertEquals(listOf("voice-a", "voice-b"), options.map { it.id })
    }

    @Test
    fun labelsUseLocaleAndNeverClassifyGender() {
        val option = spanishOfflineVoiceOptions(listOf(voice("female-looking-id", "es-PE"))).single()
        assertTrue(option.label.startsWith("Español (Perú)"))
        assertFalse(option.label.contains("Masculina", ignoreCase = true))
        assertFalse(option.label.contains("Femenina", ignoreCase = true))
    }

    @Test
    fun missingSavedVoiceFallsBackAfterVoiceOrEngineChange() {
        assertEquals(DEFAULT_TRAINER_VOICE_ID, resolveTrainerVoiceId("old-engine-voice", listOf("new-engine-voice")))
    }

    @Test
    fun oneOrNoAdditionalSpanishVoiceIsSupported() {
        assertEquals(1, spanishOfflineVoiceOptions(listOf(voice("only", "es-CO"))).size)
        assertTrue(spanishOfflineVoiceOptions(emptyList()).isEmpty())
    }

    @Test
    fun cancelRestoresOriginalAndSaveUsesTemporarySelection() {
        val selection = TrainerVoiceSelection("original")
        selection.select("temporary")
        assertEquals("original", selection.cancel())
        assertEquals("temporary", selection.save())
    }

    @Test
    fun previewUsesTemporaryVoiceAndExactPhrase() {
        val selection = TrainerVoiceSelection(DEFAULT_TRAINER_VOICE_ID)
        selection.select("preview-voice")
        assertEquals("preview-voice" to "Preparado para entrenar.", selection.preview("preview-voice"))
    }

    @Test
    fun previewingAnyRowDoesNotChangeTemporarySelection() {
        val selection = TrainerVoiceSelection("saved")
        selection.select("selected-row")
        assertEquals("other-row" to TRAINER_VOICE_SAMPLE, selection.preview("other-row"))
        assertEquals("selected-row", selection.temporaryVoiceId)
        assertEquals(DEFAULT_TRAINER_VOICE_ID to TRAINER_VOICE_SAMPLE, selection.preview(DEFAULT_TRAINER_VOICE_ID))
        assertEquals("selected-row", selection.temporaryVoiceId)
    }

    @Test
    fun previewButtonsHaveAccessibleDescriptions() {
        assertEquals(
            "Probar voz predeterminada del dispositivo",
            trainerVoicePreviewDescription("Predeterminada del dispositivo")
        )
        assertEquals("Probar Español (España) — Voz 2", trainerVoicePreviewDescription("Español (España) — Voz 2"))
        assertEquals(24f, TRAINER_VOICE_PREVIEW_ICON_SIZE.value, 0f)
    }

    private fun voice(id: String, languageTag: String, requiresNetwork: Boolean = false) =
        TrainerVoiceDescriptor(id, Locale.forLanguageTag(languageTag), requiresNetwork)
}
