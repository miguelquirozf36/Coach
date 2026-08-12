package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutTimingInfrastructureTest {
    @Test
    fun progressRingUsesTheSameElapsedTimelineAsTheWorkout() {
        val workout = workout(durationSeconds = 600, startedAtMillis = 10_000L)

        assertEquals(1f, workoutRemainingFraction(workout, 10_000L), 0f)
        assertEquals(500f / 600f, workoutRemainingFraction(workout, 110_000L), 0.0001f)
        assertEquals(0.5f, workoutRemainingFraction(workout, 310_000L), 0f)
        assertEquals(0f, workoutRemainingFraction(workout, 700_000L), 0f)
    }

    @Test
    fun wakeLockIsHeldOnlyWhileWorkoutIsActivelyRunning() {
        val handle = FakeWakeLockHandle()
        val wakeLock = WorkoutWakeLock(handle)

        wakeLock.update(workout())
        assertTrue(handle.isHeld)
        assertEquals(1, handle.acquireCalls)

        wakeLock.update(workout())
        assertEquals(1, handle.acquireCalls)

        wakeLock.update(workout(paused = true))
        assertFalse(handle.isHeld)
        assertEquals(1, handle.releaseCalls)

        wakeLock.update(workout())
        assertTrue(handle.isHeld)
        assertEquals(2, handle.acquireCalls)

        wakeLock.update(TrainingUiState.Completed)
        assertFalse(handle.isHeld)
        assertEquals(2, handle.releaseCalls)
    }

    @Test
    fun explicitServiceCleanupReleasesTheWakeLockOnlyOnce() {
        val handle = FakeWakeLockHandle()
        val wakeLock = WorkoutWakeLock(handle)
        wakeLock.update(workout())

        wakeLock.release()
        wakeLock.release()

        assertFalse(handle.isHeld)
        assertEquals(1, handle.releaseCalls)
    }

    private class FakeWakeLockHandle : WakeLockHandle {
        override var isHeld = false
        var acquireCalls = 0
        var releaseCalls = 0

        override fun acquire() {
            isHeld = true
            acquireCalls += 1
        }

        override fun release() {
            isHeld = false
            releaseCalls += 1
        }
    }

    private companion object {
        fun workout(
            durationSeconds: Int = 600,
            startedAtMillis: Long = 0L,
            paused: Boolean = false
        ) = TrainingUiState.Workout(
            routine = Routine(
                id = "timing-test",
                name = "Timing test",
                isCustom = false,
                exercises = listOf(Exercise("exercise", "Exercise", 1, 1, 1, 1, 30)),
                restBetweenExercisesSeconds = 30,
                warmupSeconds = durationSeconds
            ),
            exerciseIndex = 0,
            seriesNumber = 1,
            repetitionNumber = 1,
            phase = TrainingPhase.WARMUP,
            secondsRemaining = durationSeconds,
            phaseDurationSeconds = durationSeconds,
            phaseStartedAtMillis = startedAtMillis,
            phasePausedAtMillis = if (paused) startedAtMillis else null,
            isPaused = paused,
            currentExerciseNotes = ""
        )
    }
}
