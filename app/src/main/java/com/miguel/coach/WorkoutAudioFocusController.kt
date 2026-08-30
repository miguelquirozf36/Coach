package com.miguel.coach

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager

class WorkoutAudioFocusController internal constructor(
    private val gateway: WorkoutAudioFocusGateway,
    enabled: Boolean = false
) {
    private val transientTokens = mutableSetOf<String>()
    private var continuousDesired = false
    private var focusDesired = false
    private var ownsFocus = false

    var isEnabled: Boolean = enabled
        private set

    fun setEnabled(enabled: Boolean) {
        if (isEnabled == enabled) return
        isEnabled = enabled
        if (!enabled) transientTokens.clear()
        reconcileFocus()
    }

    fun setContinuousDuckingDesired(desired: Boolean) {
        if (continuousDesired == desired) return
        continuousDesired = desired
        reconcileFocus()
    }

    fun acquireTransient(token: String) {
        if (!isEnabled || !transientTokens.add(token)) return
        reconcileFocus()
    }

    fun releaseTransient(token: String) {
        if (!transientTokens.remove(token)) return
        reconcileFocus()
    }

    fun cleanup() {
        continuousDesired = false
        transientTokens.clear()
        isEnabled = false
        reconcileFocus()
    }

    private fun reconcileFocus() {
        val nextFocusDesired = isEnabled && (continuousDesired || transientTokens.isNotEmpty())
        if (focusDesired == nextFocusDesired) return
        focusDesired = nextFocusDesired
        when {
            focusDesired && !ownsFocus -> {
                ownsFocus = gateway.requestFocus(::handlePermanentFocusLoss)
            }
            !focusDesired && ownsFocus -> {
                gateway.abandonFocus()
                ownsFocus = false
            }
        }
    }

    private fun handlePermanentFocusLoss() {
        ownsFocus = false
    }

    companion object {
        fun create(audioManager: AudioManager, enabled: Boolean = false): WorkoutAudioFocusController =
            WorkoutAudioFocusController(AndroidWorkoutAudioFocusGateway(audioManager), enabled)
    }
}

internal interface WorkoutAudioFocusGateway {
    fun requestFocus(onPermanentFocusLoss: () -> Unit): Boolean
    fun abandonFocus()
}

private class AndroidWorkoutAudioFocusGateway(
    private val audioManager: AudioManager
) : WorkoutAudioFocusGateway {
    private var request: AudioFocusRequest? = null

    override fun requestFocus(onPermanentFocusLoss: () -> Unit): Boolean {
        val focusRequest = request ?: AudioFocusRequest.Builder(
            AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
        ).setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        ).setOnAudioFocusChangeListener { change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS) onPermanentFocusLoss()
        }.build().also { request = it }

        return audioManager.requestAudioFocus(focusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
    }

    override fun abandonFocus() {
        request?.let(audioManager::abandonAudioFocusRequest)
        request = null
    }
}
