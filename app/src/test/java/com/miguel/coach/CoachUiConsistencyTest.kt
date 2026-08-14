package com.miguel.coach

import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachUiConsistencyTest {
    @Test
    fun startFromExerciseDialogAndAccessibilityCopyUseTheSelectedExercise() {
        assertEquals("¿Iniciar desde Aperturas?", startFromExerciseDialogTitle("Aperturas"))
        assertEquals("Iniciar desde Aperturas", startFromExerciseContentDescription("Aperturas"))
        assertEquals(
            "El entrenamiento comenzará desde este ejercicio y continuará con los siguientes.",
            START_FROM_EXERCISE_DIALOG_MESSAGE
        )
    }

    @Test
    fun exerciseCardTitleIsUppercaseWithoutChangingTheStoredExerciseName() {
        val exercise = Exercise("press-inclinado", "Press inclinado mancuernas", 3, 10, 1, 2, 60)

        val visibleTitle = exerciseCardTitle(exercise.name, Locale.forLanguageTag("es-PE"))

        assertEquals("PRESS INCLINADO MANCUERNAS", visibleTitle)
        assertEquals("Press inclinado mancuernas", exercise.name)
    }

    @Test
    fun routineCardTitleUsesTheApprovedCompactSize() {
        assertEquals(18.sp, ROUTINE_CARD_TITLE_FONT_SIZE)
    }

    @Test
    fun mainRoutineCardSeparatesExercisesFromItsDynamicDuration() {
        val content = routineCardContent(exerciseCount = 6, durationMinutes = 71)

        assertEquals("6 ejercicios", content.exerciseMetadata)
        assertEquals("71 min", content.duration)
        assertFalse(content.exerciseMetadata.contains("min"))
    }

    @Test
    fun onlyTheSelectedNavigationTabUsesTheSharedOutlineTreatment() {
        repeat(3) { selectedTab ->
            repeat(3) { tabIndex ->
                assertEquals(selectedTab == tabIndex, isNavigationTabOutlined(selectedTab, tabIndex))
            }
        }
    }

    @Test
    fun workoutStartActionUsesTheInitiateLabel() {
        assertEquals("INICIAR", START_WORKOUT_LABEL)
    }

    @Test
    fun emptyRoutineStartProducesValidationMessageWithoutStarting() {
        val routine = routineWithExercises(emptyList())
        var startedRoutine: Routine? = null

        val message = attemptRoutineStart(routine) { startedRoutine = it }

        assertEquals(EMPTY_ROUTINE_START_MESSAGE, message)
        assertEquals("Agrega al menos un ejercicio antes de comenzar.", message)
        assertNull(startedRoutine)
    }

    @Test
    fun routineWithOneExerciseStartsNormallyWithoutValidationMessage() {
        val routine = routineWithExercises(
            listOf(Exercise("exercise", "Ejercicio", 1, 1, 1, 1, 0))
        )
        var startedRoutine: Routine? = null

        val message = attemptRoutineStart(routine) { startedRoutine = it }

        assertNull(message)
        assertEquals(routine, startedRoutine)
    }

    @Test
    fun sharedBackButtonKeepsApprovedAccessibilityMeasurements() {
        assertEquals(48.dp, COACH_BACK_TOUCH_TARGET)
        assertEquals(28.dp, COACH_BACK_ICON_SIZE)
        assertEquals("Volver", COACH_BACK_CONTENT_DESCRIPTION)
    }

    @Test
    fun categoryExpansionUsesTheIconThatMatchesItsState() {
        assertEquals(CategoryExpansionIcon.CHEVRON_RIGHT, categoryExpansionIcon(expanded = false))
        assertEquals(CategoryExpansionIcon.EXPAND_MORE, categoryExpansionIcon(expanded = true))
    }

    @Test
    fun exerciseSearchHasAStableAccessibleLabel() {
        assertEquals("Buscar ejercicios", EXERCISE_SEARCH_LABEL)
    }

    private fun routineWithExercises(exercises: List<Exercise>) = Routine(
        id = "start-validation",
        name = "Rutina",
        isCustom = true,
        exercises = exercises,
        restBetweenExercisesSeconds = 0
    )
}
