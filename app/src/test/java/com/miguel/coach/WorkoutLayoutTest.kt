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
}
