package com.miguel.coach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    private lateinit var beepPlayer: BeepPlayer
    private lateinit var voiceCoach: VoiceCoach
    private lateinit var trainingEngine: TrainingEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        beepPlayer = BeepPlayer()
        voiceCoach = VoiceCoach(applicationContext)
        trainingEngine = TrainingEngine(voiceCoach, beepPlayer)
        enableEdgeToEdge()
        setContent {
            CoachApp(trainingEngine = trainingEngine)
        }
    }

    override fun onDestroy() {
        trainingEngine.finish()
        beepPlayer.release()
        voiceCoach.release()
        super.onDestroy()
    }
}
