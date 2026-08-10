package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTourTest {
    @Test
    fun greetingRemainsVisibleForThreeSeconds() {
        assertEquals(3_000L, ONBOARDING_GREETING_DURATION_MILLIS)
    }

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

    @Test
    fun existingTourCopyRemainsUnchanged() {
        assertEquals("Aquí comienza tu entrenamiento", tourCopy(TourStep.TRAIN).title)
        assertEquals("En esta sección ves tu rutina del día con los ejercicios que vas a realizar.", tourCopy(TourStep.TRAIN).body)
        assertEquals("Sigue o cambia tu programa", tourCopy(TourStep.PROGRAMS).title)
        assertEquals("Aquí puedes ver y cambiar tu programa de entrenamiento cuando lo necesites.", tourCopy(TourStep.PROGRAMS).body)
        assertEquals("Crea tu rutina personalizada", tourCopy(TourStep.CUSTOM).title)
        assertEquals("Aquí puedes crear tus propias rutinas con ejercicios, series, repeticiones y descansos.", tourCopy(TourStep.CUSTOM).body)
    }

    @Test
    fun lowerNavigationAndCreateProgramTargetsAlwaysPlaceBubbleAbove() {
        val lowerTarget = androidx.compose.ui.geometry.Rect(100f, 700f, 400f, 780f)
        listOf(TourStep.PROGRAMS, TourStep.CUSTOM).forEach { step ->
            assertEquals(
                TourBubbleSide.ABOVE,
                chooseTourBubbleSide(step, lowerTarget, 800f, 240f, 24f, 24f, 24f, 16f)
            )
        }
    }

    @Test
    fun bubblePlacementKeepsAGapFromTargetWhenSpaceIsAvailable() {
        val target = androidx.compose.ui.geometry.Rect(100f, 500f, 400f, 580f)
        val top = tourBubbleTop(TourBubbleSide.ABOVE, target, 800f, 220f, 20f, 20f, 24f, 16f)
        assertTrue(top + 220f <= target.top - 16f)
    }

    @Test
    fun routineStripeUsesEveryThemesPrimaryColor() {
        CoachTheme.entries.forEach { theme ->
            assertEquals(theme.colorScheme.primary, routineCardStripeColor(theme.colorScheme))
        }
    }
}
