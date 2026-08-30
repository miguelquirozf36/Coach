package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutTimingInfrastructureTest {
    @Test
    fun oneSecondProgressRingUsesElapsedMonotonicTimeFromZeroToOne() {
        val workout = workout(durationSeconds = 1, startedAtMillis = 10_000L)

        assertEquals(0f, workoutElapsedFraction(workout, 10_000L), 0f)
        assertEquals(0.25f, workoutElapsedFraction(workout, 10_250L), 0f)
        assertEquals(0.5f, workoutElapsedFraction(workout, 10_500L), 0f)
        assertEquals(0.75f, workoutElapsedFraction(workout, 10_750L), 0f)
        assertEquals(1f, workoutElapsedFraction(workout, 11_000L), 0f)
    }

    @Test
    fun progressRingClampsBeforeStartAndAfterDeadline() {
        val workout = workout(durationSeconds = 1, startedAtMillis = 10_000L)

        assertEquals(0f, workoutElapsedFraction(workout, 9_500L), 0f)
        assertEquals(1f, workoutElapsedFraction(workout, 11_500L), 0f)
    }

    @Test
    fun lateFirstRenderReflectsConsumedTimeWithoutMovingTheDeadline() {
        val workout = workout(durationSeconds = 1, startedAtMillis = 10_000L)

        assertEquals(0.2f, workoutElapsedFraction(workout, 10_200L), 0.0001f)
        assertEquals(11_000L, workout.phaseStartedAtMillis + workout.phaseDurationSeconds * 1_000L)
    }

    @Test
    fun pausedProgressRingStaysFrozenAtThePausedMonotonicInstant() {
        val workout = workout(durationSeconds = 1, startedAtMillis = 10_000L, paused = true)
            .copy(phasePausedAtMillis = 10_400L)
        val progressAcrossLaterFrames = listOf(10_400L, 20_000L).map { frameTimeMillis ->
            workoutElapsedFraction(workout, workout.phasePausedAtMillis ?: frameTimeMillis)
        }

        assertEquals(listOf(0.4f, 0.4f), progressAcrossLaterFrames)
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
