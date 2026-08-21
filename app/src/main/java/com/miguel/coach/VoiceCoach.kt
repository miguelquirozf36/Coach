package com.miguel.coach

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

interface VoiceSpeaker {
    val isReady: Boolean
    fun speak(phrase: String, onCompleted: (() -> Unit)? = null)
    fun enqueue(phrase: String)
    fun stop()
}

class VoiceCoach(
    context: Context,
    private val preferences: UserPreferenceRepository
) : VoiceSpeaker {
    override var isReady by mutableStateOf(false)
        private set

    private val callbackLock = Any()
    private val mainHandler = Handler(Looper.getMainLooper())
    private val pendingCallbacks = mutableMapOf<String, PendingCallback>()
    private var isReleased = false
    private var utteranceSequence = 0L
    private var voiceGeneration = 0L
    private var textToSpeech: TextToSpeech? = null

    init {
        textToSpeech = TextToSpeech(context.applicationContext) { status ->
            if (isReleased || status != TextToSpeech.SUCCESS) {
                isReady = false
                return@TextToSpeech
            }
            val languageStatus = textToSpeech?.setLanguage(Locale.forLanguageTag("es-ES"))
            isReady = languageStatus != null &&
                languageStatus != TextToSpeech.LANG_MISSING_DATA &&
                languageStatus != TextToSpeech.LANG_NOT_SUPPORTED
            if (isReady) applyStoredVoice()
        }
        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit

            override fun onDone(utteranceId: String) {
                completeUtterance(utteranceId)
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) {
                removeUtterance(utteranceId)
            }

            override fun onError(utteranceId: String, errorCode: Int) {
                removeUtterance(utteranceId)
            }
        })
    }

    override fun speak(phrase: String, onCompleted: (() -> Unit)?) {
        speak(phrase, TextToSpeech.QUEUE_FLUSH, onCompleted)
    }

    override fun enqueue(phrase: String) {
        speak(phrase, TextToSpeech.QUEUE_ADD, null)
    }

    private fun speak(phrase: String, queueMode: Int, onCompleted: (() -> Unit)?) {
        if (!isReady || isReleased) return

        val utteranceSettings = voiceUtteranceSettings(
            preferences.loadTrainerVoiceVolumeLevel(),
            queueMode
        )

        val utteranceId: String
        synchronized(callbackLock) {
            utteranceSequence += 1
            utteranceId = "voice-coach-$utteranceSequence"
            onCompleted?.let { pendingCallbacks[utteranceId] = PendingCallback(voiceGeneration, it) }
        }
        val parameters = Bundle().apply {
            putFloat(
                TextToSpeech.Engine.KEY_PARAM_VOLUME,
                utteranceSettings.volume
            )
        }
        val result = textToSpeech?.speak(phrase, utteranceSettings.queueMode, parameters, utteranceId)
        if (result != TextToSpeech.SUCCESS) removeUtterance(utteranceId)
    }

    override fun stop() {
        synchronized(callbackLock) {
            voiceGeneration += 1
            pendingCallbacks.clear()
        }
        textToSpeech?.stop()
    }

    fun availableSpanishVoices(): List<TrainerVoiceOption> =
        currentOfflineSpanishVoices().let { voices ->
            spanishOfflineVoiceOptions(voices.map { it.toDescriptor() })
        }

    fun selectedVoiceId(): String {
        val voices = currentOfflineSpanishVoices()
        return resolveTrainerVoiceId(preferences.loadTrainerVoiceId(), voices.map(Voice::getName))
    }

    fun applyVoice(voiceId: String): Boolean {
        if (!isReady || isReleased) return false
        if (voiceId == DEFAULT_TRAINER_VOICE_ID) {
            val status = textToSpeech?.setLanguage(Locale.forLanguageTag("es-ES"))
            return status != null && status != TextToSpeech.LANG_MISSING_DATA &&
                status != TextToSpeech.LANG_NOT_SUPPORTED
        }
        val voice = currentOfflineSpanishVoices().firstOrNull { it.name == voiceId }
            ?: return applyVoice(DEFAULT_TRAINER_VOICE_ID)
        return textToSpeech?.setVoice(voice) == TextToSpeech.SUCCESS
    }

    fun previewVoice(voiceId: String) {
        if (applyVoice(voiceId)) speak(TRAINER_VOICE_SAMPLE)
    }

    fun saveVoice(voiceId: String): Boolean {
        val resolved = resolveTrainerVoiceId(voiceId, currentOfflineSpanishVoices().map(Voice::getName))
        if (!preferences.saveTrainerVoiceId(resolved)) return false
        return !isReady || applyVoice(resolved)
    }

    fun release() {
        isReleased = true
        isReady = false
        stop()
        textToSpeech?.shutdown()
        textToSpeech = null
    }

    private fun completeUtterance(utteranceId: String) {
        val pendingCallback = synchronized(callbackLock) {
            pendingCallbacks.remove(utteranceId)
        } ?: return
        mainHandler.post {
            val shouldRun = synchronized(callbackLock) {
                !isReleased && pendingCallback.generation == voiceGeneration
            }
            if (shouldRun) pendingCallback.onCompleted()
        }
    }

    private fun removeUtterance(utteranceId: String) {
        synchronized(callbackLock) { pendingCallbacks.remove(utteranceId) }
    }

    private fun applyStoredVoice() {
        val voices = currentOfflineSpanishVoices()
        storedTrainerVoiceToApply(preferences.loadTrainerVoiceId(), voices.map(Voice::getName))
            ?.let(::applyVoice)
    }

    private fun currentOfflineSpanishVoices(): List<Voice> = textToSpeech?.voices.orEmpty()
        .filter { it.locale.language.equals("es", ignoreCase = true) && !it.isNetworkConnectionRequired }

    private fun Voice.toDescriptor() = TrainerVoiceDescriptor(name, locale, isNetworkConnectionRequired)

    private data class PendingCallback(val generation: Long, val onCompleted: () -> Unit)
}

internal data class VoiceUtteranceSettings(val queueMode: Int, val volume: Float)

internal fun voiceUtteranceSettings(level: Int, queueMode: Int): VoiceUtteranceSettings =
    VoiceUtteranceSettings(queueMode, relativeAudioVolume(level))
