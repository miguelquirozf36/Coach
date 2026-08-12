package com.miguel.coach

import android.content.Context
import android.os.PowerManager

interface WakeLockHandle {
    val isHeld: Boolean
    fun acquire()
    fun release()
}

class WorkoutWakeLock(private val handle: WakeLockHandle) {
    fun update(state: TrainingUiState) {
        val shouldBeHeld = (state as? TrainingUiState.Workout)?.isPaused == false
        if (shouldBeHeld && !handle.isHeld) handle.acquire()
        if (!shouldBeHeld && handle.isHeld) handle.release()
    }

    fun release() {
        if (handle.isHeld) handle.release()
    }
}

fun createWorkoutWakeLock(context: Context): WorkoutWakeLock {
    val powerManager = context.getSystemService(PowerManager::class.java)
    val wakeLock = powerManager.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK,
        "Coach:ActiveWorkout"
    ).apply { setReferenceCounted(false) }
    return WorkoutWakeLock(AndroidWakeLockHandle(wakeLock))
}

private class AndroidWakeLockHandle(
    private val wakeLock: PowerManager.WakeLock
) : WakeLockHandle {
    override val isHeld: Boolean get() = wakeLock.isHeld
    override fun acquire() = wakeLock.acquire()
    override fun release() = wakeLock.release()
}
