package com.miguel.coach

import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Handler
import android.os.Looper
import kotlin.math.roundToInt

interface BeepSoundPlayer {
    fun play()
    fun playTick(onCompleted: () -> Unit) = onCompleted()
    fun stop()
}

class BeepPlayer(
    initialVolumeLevel: Int = DEFAULT_BEEP_VOLUME_LEVEL,
    private val toneFactory: BeepToneFactory = AndroidBeepToneFactory,
    private val tickScheduler: BeepTickScheduler = AndroidBeepTickScheduler
) : BeepSoundPlayer {
    private var beepTone: BeepTone? = null
    private var tickTone: BeepTone? = null
    private var activeVolume: Int? = null
    private var volumeLevel = normalizeBeepVolumeLevel(initialVolumeLevel)
    private var tickGeneration = 0L

    override fun play() {
        val volume = beepToneVolume(volumeLevel)
        ensureVolume(volume)
        if (beepTone == null) beepTone = toneFactory.create(volume)
        beepTone?.play(BEEP_DURATION_MILLIS)
    }

    override fun playTick(onCompleted: () -> Unit) {
        cancelTickCompletion()
        val volume = beepToneVolume(volumeLevel)
        ensureVolume(volume)
        if (tickTone == null) tickTone = toneFactory.createTick(volume)
        tickTone?.play(TICK_DURATION_MILLIS)
        val generation = tickGeneration
        tickScheduler.schedule(TICK_DURATION_MILLIS.toLong()) {
            if (generation == tickGeneration) onCompleted()
        }
    }

    fun updateVolumeLevel(level: Int) {
        volumeLevel = normalizeBeepVolumeLevel(level)
    }

    override fun stop() {
        cancelTickCompletion()
        beepTone?.stop()
        tickTone?.stop()
    }

    fun release() {
        stop()
        beepTone?.release()
        tickTone?.release()
        beepTone = null
        tickTone = null
        activeVolume = null
    }

    private fun ensureVolume(volume: Int) {
        if (volume == activeVolume) return
        beepTone?.release()
        tickTone?.release()
        beepTone = null
        tickTone = null
        activeVolume = volume
    }

    private fun cancelTickCompletion() {
        tickGeneration += 1
        tickScheduler.cancel()
    }

    private companion object {
        const val BEEP_DURATION_MILLIS = 100
        const val TICK_DURATION_MILLIS = 50
    }
}

internal fun beepToneVolume(level: Int): Int =
    (relativeAudioVolume(level) * ToneGenerator.MAX_VOLUME).roundToInt()

interface BeepToneFactory {
    fun create(volume: Int): BeepTone
    fun createTick(volume: Int): BeepTone = create(volume)
}

interface BeepTone {
    fun play(durationMillis: Int)
    fun stop()
    fun release()
}

private object AndroidBeepToneFactory : BeepToneFactory {
    override fun create(volume: Int): BeepTone = AndroidBeepTone(
        ToneGenerator(AudioManager.STREAM_MUSIC, volume),
        ToneGenerator.TONE_PROP_BEEP
    )

    override fun createTick(volume: Int): BeepTone = AndroidBeepTone(
        ToneGenerator(AudioManager.STREAM_MUSIC, volume),
        ToneGenerator.TONE_PROP_ACK
    )
}

private class AndroidBeepTone(
    private val generator: ToneGenerator,
    private val toneType: Int
) : BeepTone {
    override fun play(durationMillis: Int) {
        generator.startTone(toneType, durationMillis)
    }
    override fun stop() = generator.stopTone()
    override fun release() = generator.release()
}

interface BeepTickScheduler {
    fun schedule(delayMillis: Long, action: () -> Unit)
    fun cancel()
}

private object AndroidBeepTickScheduler : BeepTickScheduler {
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private var pendingAction: Runnable? = null

    override fun schedule(delayMillis: Long, action: () -> Unit) {
        cancel()
        Runnable {
            pendingAction = null
            action()
        }.also { runnable ->
            pendingAction = runnable
            handler.postDelayed(runnable, delayMillis)
        }
    }

    override fun cancel() {
        val action = pendingAction ?: return
        handler.removeCallbacks(action)
        pendingAction = null
    }
}
