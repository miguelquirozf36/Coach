package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class SystemBackNavigationTest {
    @Test
    fun backFromUnchangedEditorReturnsToRoutineDetail() {
        assertEquals(SystemBackOutcome.NAVIGATE_BACK, editorSystemBackOutcome(false, false))
    }

    @Test
    fun backFromChangedEditorShowsSaveConfirmation() {
        assertEquals(SystemBackOutcome.SHOW_CONFIRMATION, editorSystemBackOutcome(true, false))
    }

    @Test
    fun backWhileEditorExitDialogIsOpenCancelsExitFirst() {
        assertEquals(SystemBackOutcome.CLOSE_DIALOG, editorSystemBackOutcome(true, true))
    }

    @Test
    fun discardLeavesEditorWithoutSaving() {
        assertEquals(EditorExitResult.EXIT_WITHOUT_SAVING, editorExitResult(EditorExitChoice.DISCARD))
    }

    @Test
    fun saveLeavesEditorAfterSaving() {
        assertEquals(EditorExitResult.SAVE_AND_EXIT, editorExitResult(EditorExitChoice.SAVE))
    }

    @Test
    fun backFromLibraryReturnsToEditor() {
        assertEquals(
            ExercisePickerBackOutcome.RETURN_TO_EDITOR,
            exercisePickerSystemBackOutcome(false, false, false, false)
        )
    }

    @Test
    fun backFromDetailPreservesLibraryContext() {
        val context = ExercisePickerNavigationContext(
            query = "sentadilla",
            expandedCategory = "PIERNAS",
            selectedExerciseId = "squat"
        )

        assertEquals(
            ExercisePickerBackOutcome.CLOSE_DETAIL,
            exercisePickerSystemBackOutcome(false, false, false, true)
        )
        val returned = context.closeDetail()
        assertEquals("sentadilla", returned.query)
        assertEquals("PIERNAS", returned.expandedCategory)
        assertNull(returned.selectedExerciseId)
    }

    @Test
    fun backFromCustomExerciseFormClosesOnlyTheForm() {
        assertEquals(
            ExercisePickerBackOutcome.CLOSE_FORM,
            exercisePickerSystemBackOutcome(false, false, true, true)
        )
    }

    @Test
    fun openExerciseDialogHasPriorityOverDetailAndLibrary() {
        assertEquals(
            ExercisePickerBackOutcome.CLOSE_DELETE_CONFIRMATION,
            exercisePickerSystemBackOutcome(false, true, false, true)
        )
    }

    @Test
    fun backDuringWorkoutShowsFinishConfirmation() {
        assertEquals(SystemBackOutcome.SHOW_CONFIRMATION, workoutSystemBackOutcome(false))
    }

    @Test
    fun backDuringFinishConfirmationCancelsItWithoutEndingWorkout() {
        assertEquals(SystemBackOutcome.CLOSE_DIALOG, workoutSystemBackOutcome(true))
    }

    @Test
    fun rootNavigationContextHasNoPendingInternalScreen() {
        val context = ExercisePickerNavigationContext()
        assertNull(context.selectedExerciseId)
        assertFalse(context.query.isNotEmpty())
    }
}
