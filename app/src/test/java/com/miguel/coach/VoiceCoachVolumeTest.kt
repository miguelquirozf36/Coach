package com.miguel.coach

import android.speech.tts.TextToSpeech
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCoachVolumeTest {
    @Test
    fun fiveLevelsMapToProgressiveRelativeVolumesEndingAtOne() {
        assertEquals(listOf(0.2f, 0.4f, 0.6f, 0.8f, 1.0f), (1..5).map(::relativeAudioVolume))
    }

    @Test
    fun speakAndEnqueueUseTheSameSelectedVolumeWithoutChangingQueueSemantics() {
        val speak = voiceUtteranceSettings(3, TextToSpeech.QUEUE_FLUSH)
        val enqueue = voiceUtteranceSettings(3, TextToSpeech.QUEUE_ADD)

        assertEquals(0.6f, speak.volume)
        assertEquals(0.6f, enqueue.volume)
        assertEquals(TextToSpeech.QUEUE_FLUSH, speak.queueMode)
        assertEquals(TextToSpeech.QUEUE_ADD, enqueue.queueMode)
    }

    @Test
    fun previewUsesSpeakPathWithSelectedVolume() {
        val preview = voiceUtteranceSettings(4, TextToSpeech.QUEUE_FLUSH)

        assertEquals(0.8f, preview.volume)
        assertEquals(TextToSpeech.QUEUE_FLUSH, preview.queueMode)
    }
}
