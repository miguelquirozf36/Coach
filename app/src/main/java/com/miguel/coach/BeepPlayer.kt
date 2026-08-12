package com.miguel.coach

import android.media.AudioManager
import android.media.ToneGenerator

interface BeepSoundPlayer {
    fun play()
    fun stop()
}

class BeepPlayer(
    private val volumeLevelProvider: () -> Int = { DEFAULT_BEEP_VOLUME_LEVEL },
    private val toneFactory: BeepToneFactory = AndroidBeepToneFactory
) : BeepSoundPlayer {
    private var tone: BeepTone? = null
    private var activeVolume: Int? = null

    override fun play() {
        val volume = beepToneVolume(volumeLevelProvider())
        if (volume != activeVolume) {
            tone?.release()
            tone = toneFactory.create(volume)
            activeVolume = volume
        }
        tone?.play(BEEP_DURATION_MILLIS)
    }

    override fun stop() {
        tone?.stop()
    }

    fun release() {
        stop()
        tone?.release()
        tone = null
        activeVolume = null
    }

    private companion object {
        const val BEEP_DURATION_MILLIS = 100
    }
}

internal fun beepToneVolume(level: Int): Int = when (normalizeBeepVolumeLevel(level)) {
    1 -> 80
    2 -> 85
    3 -> 90
    4 -> 95
    else -> 100
}

interface BeepToneFactory {
    fun create(volume: Int): BeepTone
}

interface BeepTone {
    fun play(durationMillis: Int)
    fun stop()
    fun release()
}

private object AndroidBeepToneFactory : BeepToneFactory {
    override fun create(volume: Int): BeepTone = AndroidBeepTone(
        ToneGenerator(AudioManager.STREAM_MUSIC, volume)
    )
}

private class AndroidBeepTone(private val generator: ToneGenerator) : BeepTone {
    override fun play(durationMillis: Int) {
        generator.startTone(ToneGenerator.TONE_PROP_BEEP, durationMillis)
    }
    override fun stop() = generator.stopTone()
    override fun release() = generator.release()
}
