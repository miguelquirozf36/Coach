package com.miguel.coach

import android.media.AudioManager
import android.media.ToneGenerator

interface BeepSoundPlayer {
    fun play()
    fun stop()
}

class BeepPlayer : BeepSoundPlayer {
    private var toneGenerator: ToneGenerator? = ToneGenerator(AudioManager.STREAM_MUSIC, VOLUME)

    override fun play() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, BEEP_DURATION_MILLIS)
    }

    override fun stop() {
        toneGenerator?.stopTone()
    }

    fun release() {
        stop()
        toneGenerator?.release()
        toneGenerator = null
    }

    private companion object {
        const val VOLUME = 80
        const val BEEP_DURATION_MILLIS = 100
    }
}
