package com.miguel.coach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    private lateinit var beepPlayer: BeepPlayer
    private lateinit var voiceCoach: VoiceCoach
    private lateinit var trainingEngine: TrainingEngine
    private lateinit var routineRepository: RoutineRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        beepPlayer = BeepPlayer()
        voiceCoach = VoiceCoach(applicationContext)
        trainingEngine = TrainingEngine(voiceCoach, beepPlayer)
        routineRepository = RoutineRepository(
            SharedPreferencesRoutineStorage(
                getSharedPreferences("coach_routines", MODE_PRIVATE)
            )
        )
        enableEdgeToEdge()
        setContent {
            CoachApp(
                trainingEngine = trainingEngine,
                routineRepository = routineRepository
            )
        }
    }

    override fun onDestroy() {
        trainingEngine.finish()
        beepPlayer.release()
        voiceCoach.release()
        super.onDestroy()
    }
}
