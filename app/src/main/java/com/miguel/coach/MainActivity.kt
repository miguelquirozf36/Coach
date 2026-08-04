package com.miguel.coach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    private lateinit var voiceCoach: VoiceCoach
    private lateinit var trainingEngine: TrainingEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        voiceCoach = VoiceCoach(applicationContext)
        trainingEngine = TrainingEngine(voiceCoach)
        enableEdgeToEdge()
        setContent {
            CoachApp(trainingEngine = trainingEngine)
        }
    }

    override fun onDestroy() {
        trainingEngine.finish()
        voiceCoach.release()
        super.onDestroy()
    }
}
