package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTourTest {
    @Test
    fun newUserGoesDirectlyToWelcomeWithoutBrandedLaunch() {
        assertEquals(LaunchStage.WELCOME, launchStageFor(onboardingPending = true))
    }

    @Test
    fun returningUserSeesBrandedLaunchBeforeNormalContent() {
        assertEquals(LaunchStage.BRANDED, launchStageFor(onboardingPending = false))
        assertEquals(LaunchStage.CONTENT, launchStageAfterBranded())
    }

    @Test
    fun brandedLaunchUsesTheApprovedDuration() {
        assertEquals(1_000L, BRANDED_LAUNCH_DURATION_MILLIS)
    }

    @Test
    fun introductionGreetingUsesTheDynamicNameWithoutEmoji() {
        val greeting = onboardingGreeting("Miguel")

        assertEquals("Hola, Miguel", greeting)
        assertTrue("👋" !in greeting)
    }

    @Test
    fun introductionShowsTheThreeApprovedBenefits() {
        assertEquals(
            listOf("Rutinas personalizadas", "Entrenamiento guiado", "Sigue tu progreso"),
            ONBOARDING_BENEFITS.map { it.title }
        )
        assertEquals(
            listOf(
                "Crea o elige rutinas y adapta series, repeticiones y más.",
                "Voz y temporizador para cada repetición y descanso.",
                "Registra tu avance y alcanza tus objetivos."
            ),
            ONBOARDING_BENEFITS.map { it.description }
        )
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
    fun sharedRoutineAndProgramStripeUsesEveryThemesPrimaryColor() {
        CoachTheme.entries.forEach { theme ->
            assertEquals(theme.colorScheme.primary, routineCardStripeColor(theme.colorScheme))
        }
    }

    @Test
    fun everyStepUsesTheSingleSharedArrowDefinition() {
        assertEquals(TourArrowStyle(), SHARED_TOUR_ARROW_STYLE)
    }

    @Test
    fun everyStepMapsToItsOwnExplicitTarget() {
        assertEquals(TourTarget.TRAIN_ROUTINE, tourTargetForStep(TourStep.TRAIN))
        assertEquals(TourTarget.EDIT_BUTTON, tourTargetForStep(TourStep.EDIT))
        assertEquals(TourTarget.PROGRAMS_TAB, tourTargetForStep(TourStep.PROGRAMS))
        assertEquals(TourTarget.CREATE_PROGRAM, tourTargetForStep(TourStep.CUSTOM))
    }

    @Test
    fun createProgramBoundsAreRegisteredBeforeStepFourAndThenDeliveredToOverlay() {
        val createProgramBounds = androidx.compose.ui.geometry.Rect(120f, 620f, 960f, 700f)
        val targetsMeasuredDuringProgramsStep = registerTourTargetBounds(
            emptyMap(),
            TourTarget.CREATE_PROGRAM,
            createProgramBounds
        )

        assertEquals(createProgramBounds, tourBoundsForStep(targetsMeasuredDuringProgramsStep, TourStep.CUSTOM))
        assertTrue(hasValidTourBounds(tourBoundsForStep(targetsMeasuredDuringProgramsStep, TourStep.CUSTOM)))
    }

    @Test
    fun invalidCreateProgramBoundsDoNotReplaceAValidMeasurement() {
        val valid = androidx.compose.ui.geometry.Rect(120f, 620f, 960f, 700f)
        val registered = registerTourTargetBounds(emptyMap(), TourTarget.CREATE_PROGRAM, valid)
        val afterEmptyMeasurement = registerTourTargetBounds(
            registered,
            TourTarget.CREATE_PROGRAM,
            androidx.compose.ui.geometry.Rect.Zero
        )

        assertEquals(valid, tourBoundsForStep(afterEmptyMeasurement, TourStep.CUSTOM))
    }

    @Test
    fun arrowBelowBubblePointsDownTowardTarget() {
        val bubble = androidx.compose.ui.geometry.Rect(80f, 200f, 420f, 420f)
        val target = androidx.compose.ui.geometry.Rect(150f, 460f, 350f, 520f)
        val arrow = tourArrowGeometry(TourBubbleSide.ABOVE, bubble, target, pixelsPerDp = 1f)

        assertEquals(bubble.bottom, arrow.lineStart.y)
        assertTrue(arrow.tip.y < target.top)
        assertTrue(arrow.tip.y > arrow.headLeft.y)
        assertEquals(target.center.x, arrow.tip.x)
    }

    @Test
    fun customBubbleIsCenteredAboveCreateProgramWithoutIntersection() {
        val target = androidx.compose.ui.geometry.Rect(120f, 620f, 960f, 700f)
        val bubbleWidth = 520f
        val bubbleHeight = 280f
        val left = tourBubbleLeft(target, 1080f, bubbleWidth, 0f, 0f, 24f)
        val top = tourBubbleTop(TourBubbleSide.ABOVE, target, 800f, bubbleHeight, 24f, 24f, 24f, 16f)
        val bubble = androidx.compose.ui.geometry.Rect(left, top, left + bubbleWidth, top + bubbleHeight)

        assertTrue(bubble.bottom <= target.top - 16f)
        assertEquals(target.center.x, bubble.center.x)
        assertEquals("CREAR PROGRAMA", tourTargetLabel(TourStep.CUSTOM))
        assertTrue(tourTargetLabel(TourStep.CUSTOM) != "PROGRAMAS")
    }

    @Test
    fun bubbleAlignmentRespectsViewportInsets() {
        val targetNearRightEdge = androidx.compose.ui.geometry.Rect(900f, 600f, 1060f, 680f)
        val left = tourBubbleLeft(targetNearRightEdge, 1080f, 520f, 16f, 32f, 24f)

        assertTrue(left >= 40f)
        assertTrue(left + 520f <= 1024f)
    }
}
