package com.miguel.coach

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

const val WORKOUT_NOTIFICATION_ID = 1308
const val WORKOUT_NOTIFICATION_CHANNEL_ID = "active_workout"

data class WorkoutNotificationContent(
    val title: String,
    val text: String,
    val phase: String
)

sealed interface WorkoutNotificationChange {
    data class Show(val content: WorkoutNotificationContent) : WorkoutNotificationChange
    data object Remove : WorkoutNotificationChange
    data object None : WorkoutNotificationChange
}

class WorkoutNotificationTracker {
    private var lastContent: WorkoutNotificationContent? = null

    fun next(state: TrainingUiState): WorkoutNotificationChange {
        val workout = state as? TrainingUiState.Workout
        if (workout == null) {
            val hadContent = lastContent != null
            lastContent = null
            return if (hadContent) WorkoutNotificationChange.Remove else WorkoutNotificationChange.None
        }
        val content = workoutNotificationContent(workout)
        if (content == lastContent) return WorkoutNotificationChange.None
        lastContent = content
        return WorkoutNotificationChange.Show(content)
    }
}

fun workoutNotificationContent(state: TrainingUiState.Workout): WorkoutNotificationContent {
    if (state.phase == TrainingPhase.WARMUP) {
        return WorkoutNotificationContent(
            title = state.routine.name,
            text = "Calentamiento",
            phase = if (state.isPaused) "Pausa" else "Calentamiento"
        )
    }
    val exercise = if (state.phase == TrainingPhase.REST_BETWEEN_EXERCISES && state.exerciseIndex > 0) {
        state.routine.exercises[state.exerciseIndex - 1]
    } else {
        state.routine.exercises[state.exerciseIndex]
    }
    val series = if (state.phase == TrainingPhase.REST_BETWEEN_EXERCISES) exercise.sets else state.seriesNumber
    val repetition = if (state.phase == TrainingPhase.REST_BETWEEN_EXERCISES) {
        exercise.repetitions
    } else {
        state.repetitionNumber
    }
    return WorkoutNotificationContent(
        title = exercise.name,
        text = "Serie $series de ${exercise.sets} · Repetición $repetition de ${exercise.repetitions}",
        phase = if (state.isPaused) "Pausa" else state.phase.notificationLabel
    )
}

private val TrainingPhase.notificationLabel: String
    get() = when (this) {
        TrainingPhase.WARMUP -> "Calentamiento"
        TrainingPhase.COUNTDOWN -> "Cuenta inicial"
        TrainingPhase.CONCENTRIC,
        TrainingPhase.REPETITION_ANNOUNCEMENT -> "Concéntrica"
        TrainingPhase.ECCENTRIC -> "Excéntrica"
        TrainingPhase.REST,
        TrainingPhase.REST_BETWEEN_EXERCISES -> "Descanso"
    }

enum class NotificationPermissionAction { REQUEST_WITH_EXPLANATION, START_WITHOUT_REQUEST, START_ALLOWED }

fun notificationPermissionAction(
    sdkInt: Int,
    permissionGranted: Boolean,
    previouslyAsked: Boolean
): NotificationPermissionAction = when {
    sdkInt < Build.VERSION_CODES.TIRAMISU || permissionGranted -> NotificationPermissionAction.START_ALLOWED
    previouslyAsked -> NotificationPermissionAction.START_WITHOUT_REQUEST
    else -> NotificationPermissionAction.REQUEST_WITH_EXPLANATION
}

class WorkoutNotification(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)

    fun createChannel() {
        val channel = NotificationChannel(
            WORKOUT_NOTIFICATION_CHANNEL_ID,
            "Entrenamiento activo",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Muestra el progreso de la rutina mientras Coach está en uso."
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun build(content: WorkoutNotificationContent): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Notification.CATEGORY_WORKOUT
        } else {
            NotificationCompat.CATEGORY_SERVICE
        }
        return NotificationCompat.Builder(context, WORKOUT_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${content.text}\nFase: ${content.phase}"))
            .setSubText("Fase: ${content.phase}")
            .setContentIntent(pendingIntent)
            .setCategory(category)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    fun notify(content: WorkoutNotificationContent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, "android.permission.POST_NOTIFICATIONS") !=
            PackageManager.PERMISSION_GRANTED
        ) return
        manager.notify(WORKOUT_NOTIFICATION_ID, build(content))
    }

    fun cancel() = manager.cancel(WORKOUT_NOTIFICATION_ID)
}
