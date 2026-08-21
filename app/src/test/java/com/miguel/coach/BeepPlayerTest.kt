package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeepPlayerTest {
    @Test
    fun fiveLevelsUseTheSharedRelativeMappingAndFiveIsTheInternalMaximum() {
        assertEquals(20, beepToneVolume(1))
        assertEquals(listOf(20, 40, 60, 80, 100), (1..5).map(::beepToneVolume))
        assertEquals(20, beepToneVolume(-1))
        assertEquals(100, beepToneVolume(10))
    }

    @Test
    fun nextBeepUsesAChangedLevelWithoutChangingToneOrDuration() {
        var level = 1
        val factory = FakeToneFactory()
        val player = BeepPlayer({ level }, factory)

        player.play()
        level = 4
        player.play()

        assertEquals(listOf(20, 80), factory.volumes)
        assertEquals(listOf(100), factory.tones[0].durations)
        assertEquals(listOf(100), factory.tones[1].durations)
        assertTrue(factory.tones[0].released)
    }

    private class FakeToneFactory : BeepToneFactory {
        val volumes = mutableListOf<Int>()
        val tones = mutableListOf<FakeTone>()
        override fun create(volume: Int): BeepTone {
            volumes += volume
            return FakeTone().also(tones::add)
        }
    }

    private class FakeTone : BeepTone {
        val durations = mutableListOf<Int>()
        var released = false
        override fun play(durationMillis: Int) { durations += durationMillis }
        override fun stop() = Unit
        override fun release() { released = true }
    }
}
