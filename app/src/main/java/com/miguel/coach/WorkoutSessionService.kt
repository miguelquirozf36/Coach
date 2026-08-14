package com.miguel.coach

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

object WorkoutSessionController {
    var engine: TrainingEngine? by mutableStateOf(null)
        private set

    private var voiceCoach: VoiceCoach? = null
    private var beepPlayer: BeepPlayer? = null
    private var observer: ((TrainingUiState) -> Unit)? = null
    private var sessionActive = false

    fun ensureEngine(context: Context): TrainingEngine =
        if (engine != null && voiceCoach != null && beepPlayer != null) engine!! else createEngine(context)

    fun startWorkout(context: Context, routine: Routine) {
        if (routine.exercises.isEmpty()) return
        val activeEngine = ensureEngine(context)
        activeEngine.start(routine)
        startSessionService(context, activeEngine)
    }

    fun startWorkoutFromExercise(context: Context, routine: Routine, exerciseIndex: Int) {
        if (exerciseIndex !in routine.exercises.indices) return
        val activeEngine = ensureEngine(context)
        activeEngine.startFromExercise(routine, exerciseIndex)
        startSessionService(context, activeEngine)
    }

    private fun startSessionService(context: Context, activeEngine: TrainingEngine) {
        if (activeEngine.state !is TrainingUiState.Workout) return
        sessionActive = true
        try {
            ContextCompat.startForegroundService(
                context,
                Intent(context, WorkoutSessionService::class.java)
                    .setAction(WorkoutSessionService.ACTION_START)
            )
        } catch (_: RuntimeException) {
            handleUnexpectedServiceStop()
        }
    }

    fun finishWorkout() {
        val completed = engine?.state == TrainingUiState.Completed
        engine?.finish()
        if (completed) sessionActive = false
    }

    fun pauseWorkout(): Boolean {
        val activeEngine = engine ?: return false
        if (activeEngine.state !is TrainingUiState.Workout) return false
        activeEngine.pause()
        return true
    }

    fun resumeWorkout(): Boolean {
        val activeEngine = engine ?: return false
        if (activeEngine.state !is TrainingUiState.Workout) return false
        activeEngine.resume()
        return true
    }

    fun attachObserver(stateObserver: (TrainingUiState) -> Unit) {
        observer = stateObserver
        engine?.state?.let(stateObserver)
    }

    fun detachObserver(stateObserver: (TrainingUiState) -> Unit) {
        if (observer === stateObserver) observer = null
    }

    fun handleUnexpectedServiceStop() {
        if (!sessionActive) return
        engine?.finish()
        sessionActive = false
    }

    fun releaseFinishedSession() {
        sessionActive = false
    }

    fun releaseIdleResources() {
        if (sessionActive || engine?.state is TrainingUiState.Workout) return
        destroyInfrastructure()
    }

    private fun createEngine(context: Context): TrainingEngine {
        val preferences = UserPreferenceRepository(
            SharedPreferencesUserStorage(
                context.applicationContext.getSharedPreferences("coach_user", Context.MODE_PRIVATE)
            )
        )
        val beep = BeepPlayer(preferences::loadBeepVolumeLevel)
        val voice = VoiceCoach(context.applicationContext)
        beepPlayer = beep
        voiceCoach = voice
        return TrainingEngine(
            voiceSpeaker = voice,
            beepPlayer = beep,
            onStateChanged = { state -> observer?.invoke(state) }
        ).also { engine = it }
    }

    private fun destroyInfrastructure() {
        beepPlayer?.release()
        voiceCoach?.release()
        beepPlayer = null
        voiceCoach = null
    }
}

class WorkoutSessionService : Service() {
    private lateinit var workoutNotification: WorkoutNotification
    private lateinit var workoutWakeLock: WorkoutWakeLock
    private val notificationTracker = WorkoutNotificationTracker()
    private var intentionalStop = false
    private val stateObserver: (TrainingUiState) -> Unit = ::handleState

    override fun onCreate() {
        super.onCreate()
        workoutNotification = WorkoutNotification(this)
        workoutWakeLock = createWorkoutWakeLock(this)
        workoutNotification.createChannel()
        WorkoutSessionController.attachObserver(stateObserver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> stopSessionService()
            ACTION_PAUSE_WORKOUT -> handleNotificationAction(WorkoutSessionController::pauseWorkout)
            ACTION_RESUME_WORKOUT -> handleNotificationAction(WorkoutSessionController::resumeWorkout)
            else -> promoteCurrentWorkout()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        WorkoutSessionController.detachObserver(stateObserver)
        workoutWakeLock.release()
        workoutNotification.cancel()
        if (!intentionalStop) WorkoutSessionController.handleUnexpectedServiceStop()
        super.onDestroy()
    }

    private fun promoteCurrentWorkout() {
        val workout = WorkoutSessionController.engine?.state as? TrainingUiState.Workout
            ?: return stopSessionService()
        val content = workoutNotificationContent(workout)
        try {
            ServiceCompat.startForeground(
                this,
                WORKOUT_NOTIFICATION_ID,
                workoutNotification.build(content),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
            notificationTracker.next(workout)
        } catch (_: RuntimeException) {
            WorkoutSessionController.handleUnexpectedServiceStop()
            stopSessionService()
        }
    }

    private fun handleNotificationAction(action: () -> Boolean) {
        if (!action()) stopSessionService()
    }

    private fun handleState(state: TrainingUiState) {
        workoutWakeLock.update(state)
        when (val change = notificationTracker.next(state)) {
            is WorkoutNotificationChange.Show -> workoutNotification.notify(change.content)
            WorkoutNotificationChange.None -> Unit
            WorkoutNotificationChange.Remove -> workoutNotification.cancel()
        }
        if (state is TrainingUiState.Workout) return
        stopSessionService()
        WorkoutSessionController.releaseFinishedSession()
    }

    private fun stopSessionService() {
        intentionalStop = true
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        workoutNotification.cancel()
        stopSelf()
    }

    companion object {
        const val ACTION_START = "com.miguel.coach.action.START_WORKOUT"
        const val ACTION_STOP = "com.miguel.coach.action.STOP_WORKOUT"
        const val ACTION_PAUSE_WORKOUT = "com.miguel.coach.action.PAUSE_WORKOUT"
        const val ACTION_RESUME_WORKOUT = "com.miguel.coach.action.RESUME_WORKOUT"
    }
}
