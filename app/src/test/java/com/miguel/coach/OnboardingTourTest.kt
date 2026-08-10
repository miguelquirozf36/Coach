package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnboardingTourTest {
    @Test
    fun tourUsesTheExactRequiredOrder() {
        assertEquals(TourStep.EDIT, nextTourStep(TourStep.TRAIN))
        assertEquals(TourStep.PROGRAMS, nextTourStep(TourStep.EDIT))
        assertEquals(TourStep.CUSTOM, nextTourStep(TourStep.PROGRAMS))
        assertNull(nextTourStep(TourStep.CUSTOM))
    }

    @Test
    fun editCopyIsExact() {
        assertEquals(
            "Aquí puedes ajustar la duración de la fase concéntrica (positiva), excéntrica (negativa) y descansos.",
            tourCopy(TourStep.EDIT).body
        )
    }

    @Test
    fun fourthStepTargetsCreateProgramAndUsesFinalAction() {
        assertEquals("CREAR PROGRAMA", tourTargetLabel(TourStep.CUSTOM))
        assertEquals("EMPEZAR", tourCopy(TourStep.CUSTOM).button)
    }

    @Test
    fun eachStepHasSkipCopyAndExpectedCounters() {
        assertEquals(listOf("1/4", "2/4", "3/4", "4/4"), TourStep.entries.map { tourCopy(it).counter })
    }
}
