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
        val factory = FakeToneFactory()
        val player = BeepPlayer(1, factory)

        player.play()
        player.updateVolumeLevel(4)
        player.play()

        assertEquals(listOf(20, 80), factory.volumes)
        assertEquals(listOf(100), factory.tones[0].durations)
        assertEquals(listOf(100), factory.tones[1].durations)
        assertTrue(factory.tones[0].released)
    }

    @Test
    fun unchangedCachedLevelReusesTheExistingTone() {
        val factory = FakeToneFactory()
        val player = BeepPlayer(3, factory)

        player.play()
        player.updateVolumeLevel(3)
        player.play()

        assertEquals(listOf(60), factory.volumes)
        assertEquals(listOf(100, 100), factory.tones.single().durations)
    }

    @Test
    fun shortenedPointTickUsesItsOwnToneForFiftyMillisAndCompletesAfterIt() {
        val factory = FakeToneFactory()
        val scheduler = FakeTickScheduler()
        val player = BeepPlayer(3, factory, scheduler)
        var completed = false

        player.playTick { completed = true }

        assertEquals(listOf(60), factory.tickVolumes)
        assertEquals(listOf(50), factory.tickTones.single().durations)
        assertEquals(false, completed)
        scheduler.complete()
        assertEquals(true, completed)
    }

    @Test
    fun stopCancelsPendingTickCompletion() {
        val scheduler = FakeTickScheduler()
        val player = BeepPlayer(3, FakeToneFactory(), scheduler)
        var completed = false

        player.playTick { completed = true }
        player.stop()
        scheduler.completeCancelled()

        assertEquals(false, completed)
    }

    private class FakeToneFactory : BeepToneFactory {
        val volumes = mutableListOf<Int>()
        val tones = mutableListOf<FakeTone>()
        val tickVolumes = mutableListOf<Int>()
        val tickTones = mutableListOf<FakeTone>()
        override fun create(volume: Int): BeepTone {
            volumes += volume
            return FakeTone().also(tones::add)
        }

        override fun createTick(volume: Int): BeepTone {
            tickVolumes += volume
            return FakeTone().also(tickTones::add)
        }
    }

    private class FakeTone : BeepTone {
        val durations = mutableListOf<Int>()
        var released = false
        override fun play(durationMillis: Int) { durations += durationMillis }
        override fun stop() = Unit
        override fun release() { released = true }
    }

    private class FakeTickScheduler : BeepTickScheduler {
        private var pending: (() -> Unit)? = null
        private var cancelled: (() -> Unit)? = null

        override fun schedule(delayMillis: Long, action: () -> Unit) {
            assertEquals(50L, delayMillis)
            pending = action
        }

        override fun cancel() {
            cancelled = pending
            pending = null
        }

        fun complete() {
            pending?.invoke()
            pending = null
        }

        fun completeCancelled() {
            cancelled?.invoke()
            cancelled = null
        }
    }
}
