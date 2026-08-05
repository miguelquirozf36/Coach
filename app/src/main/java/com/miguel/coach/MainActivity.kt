package com.miguel.coach

import android.app.AlertDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.content.ContextCompat
import androidx.core.content.edit

class MainActivity : ComponentActivity() {
    private lateinit var routineRepository: RoutineRepository
    private var pendingRoutine: Routine? = null
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) {
        pendingRoutine?.let { WorkoutSessionController.startWorkout(applicationContext, it) }
        pendingRoutine = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WorkoutSessionController.ensureEngine(applicationContext)
        routineRepository = RoutineRepository(
            SharedPreferencesRoutineStorage(
                getSharedPreferences("coach_routines", MODE_PRIVATE)
            )
        )
        enableEdgeToEdge()
        setContent {
            val trainingEngine = WorkoutSessionController.engine ?: return@setContent
            CoachApp(
                trainingEngine = trainingEngine,
                routineRepository = routineRepository,
                onStartWorkout = ::startWorkoutWithNotificationPermission,
                onFinishWorkout = WorkoutSessionController::finishWorkout
            )
        }
    }

    override fun onDestroy() {
        if (!isChangingConfigurations) WorkoutSessionController.releaseIdleResources()
        super.onDestroy()
    }

    private fun startWorkoutWithNotificationPermission(routine: Routine) {
        val preferences = getSharedPreferences("coach_notifications", MODE_PRIVATE)
        val action = notificationPermissionAction(
            sdkInt = Build.VERSION.SDK_INT,
            permissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS_PERMISSION) ==
                PackageManager.PERMISSION_GRANTED,
            previouslyAsked = preferences.getBoolean(NOTIFICATION_PERMISSION_ASKED, false)
        )
        if (action != NotificationPermissionAction.REQUEST_WITH_EXPLANATION) {
            WorkoutSessionController.startWorkout(applicationContext, routine)
            return
        }
        pendingRoutine = routine
        AlertDialog.Builder(this)
            .setMessage("Coach necesita mostrar el entrenamiento activo en la pantalla de bloqueo.")
            .setNegativeButton("CANCELAR") { _, _ ->
                preferences.edit { putBoolean(NOTIFICATION_PERMISSION_ASKED, true) }
                pendingRoutine?.let { WorkoutSessionController.startWorkout(applicationContext, it) }
                pendingRoutine = null
            }
            .setPositiveButton("PERMITIR") { _, _ ->
                preferences.edit { putBoolean(NOTIFICATION_PERMISSION_ASKED, true) }
                notificationPermissionLauncher.launch(POST_NOTIFICATIONS_PERMISSION)
            }
            .setOnCancelListener {
                preferences.edit { putBoolean(NOTIFICATION_PERMISSION_ASKED, true) }
                pendingRoutine?.let { WorkoutSessionController.startWorkout(applicationContext, it) }
                pendingRoutine = null
            }
            .show()
    }

    private companion object {
        const val NOTIFICATION_PERMISSION_ASKED = "notification_permission_asked"
        const val POST_NOTIFICATIONS_PERMISSION = "android.permission.POST_NOTIFICATIONS"
    }
}
