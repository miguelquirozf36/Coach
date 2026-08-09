package com.miguel.coach

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class WorkoutLayoutTest {
    @Test
    fun workoutMetricsKeepTheExistingDynamicValues() {
        assertEquals(
            WorkoutMetricTexts("2 de 4", "7 de 12", "Concéntrica"),
            workoutMetricTexts(2, 4, 7, 12, "Concéntrica")
        )
        assertEquals(
            "Excéntrica",
            workoutMetricTexts(2, 4, 7, 12, "Excéntrica").phase
        )
    }

    @Test
    fun usefulCenterExcludesSystemInsets() {
        assertEquals(544f, usefulAreaCenter(1080, 2400, 24, 80, 16, 120).first)
        assertEquals(1180f, usefulAreaCenter(1080, 2400, 24, 80, 16, 120).second)
    }

    @Test
    fun usefulCenterChangesWithAsymmetricInsets() {
        assertEquals(490f, usefulAreaCenter(1000, 2000, 20, 100, 40, 200).first)
        assertEquals(950f, usefulAreaCenter(1000, 2000, 20, 100, 40, 200).second)
    }

    @Test
    fun ringCenterDoesNotDependOnHeaderHeight() {
        val center = usefulAreaCenter(1080, 2200, 0, 100, 0, 100)
        assertEquals(center, usefulAreaCenter(1080, 2200, 0, 100, 0, 100))
    }

    @Test
    fun ringCenterDoesNotDependOnButtonHeight() {
        val center = usefulAreaCenter(1080, 2200, 0, 100, 0, 100)
        assertEquals(1100f, center.second)
    }

    @Test
    fun ringDiameterIsLimitedOnSmallAndLargeScreens() {
        assertEquals(160.dp, workoutRingDiameter(320.dp, 560.dp))
        assertEquals(240.dp, workoutRingDiameter(500.dp, 900.dp))
    }

    @Test
    fun portraitAndLandscapeSelectTheirDedicatedLayouts() {
        assertEquals(WorkoutLayout.PORTRAIT, workoutLayoutFor(400.dp, 800.dp))
        assertEquals(WorkoutLayout.PORTRAIT, workoutLayoutFor(600.dp, 600.dp))
        assertEquals(WorkoutLayout.LANDSCAPE, workoutLayoutFor(800.dp, 400.dp))
    }

    @Test
    fun landscapeRingFitsItsCenterColumnAndShortScreens() {
        assertEquals(240.dp, landscapeWorkoutRingDiameter(320.dp, 400.dp))
        assertEquals(148.dp, landscapeWorkoutRingDiameter(240.dp, 200.dp))
    }

    @Test
    fun timerProgressAlwaysUsesThemePrimary() {
        CoachTheme.entries.forEach { theme ->
            assertEquals(theme.colorScheme.primary, timerProgressColor(theme.colorScheme))
        }
    }

    @Test
    fun pauseButtonKeepsTheExistingPauseAndResumeCallbacks() {
        var paused = 0
        var resumed = 0
        val onPause: () -> Unit = { paused++ }
        val onResume: () -> Unit = { resumed++ }

        workoutPauseAction(isPaused = false, onPause, onResume).invoke()
        workoutPauseAction(isPaused = true, onPause, onResume).invoke()

        assertEquals(1, paused)
        assertEquals(1, resumed)
    }
}
