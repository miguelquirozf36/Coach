package com.miguel.coach

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExercisePickerTest {
    private val original = ExerciseDraft(
        id = "draft-exercise",
        name = "Ejercicio anterior",
        sets = "4",
        repetitions = "12",
        concentricSeconds = "2",
        eccentricSeconds = "3",
        restSeconds = "2"
    )
    private val selected = ExerciseLibrary.find("press-militar-mancuernas")!!

    @Test
    fun searchResultsUpdateImmediatelyWithTheCurrentText() {
        val pressResults = exercisePickerResults("press")
        val remoResults = exercisePickerResults("remo")

        assertTrue(pressResults.isNotEmpty())
        assertTrue(remoResults.isNotEmpty())
        assertNotEquals(pressResults, remoResults)
        assertTrue(remoResults.all { it.name.contains("remo", ignoreCase = true) })
    }

    @Test
    fun pickerUsesLibraryCategories() {
        assertEquals(9, ExerciseLibrary.categories().size)
        assertEquals("PECHO", ExerciseLibrary.categories().first())
        assertEquals("ABDOMINALES", ExerciseLibrary.categories().last())
    }

    @Test
    fun selectingACategoryExpandsItAndReplacesAnyPreviousExpansion() {
        assertEquals("PECHO", toggledExerciseCategory(null, "PECHO"))
        assertEquals("ESPALDA", toggledExerciseCategory("PECHO", "ESPALDA"))
    }

    @Test
    fun selectingTheExpandedCategoryCollapsesIt() {
        assertNull(toggledExerciseCategory("HOMBROS", "HOMBROS"))
    }

    @Test
    fun tappingALibraryRowOpensItsDetailWithoutSelectingItForTheRoutine() {
        val context = ExercisePickerNavigationContext()

        val opened = context.openDetail(selected.id)

        assertEquals(selected.id, opened.selectedExerciseId)
        assertEquals("Ejercicio anterior", original.name)
    }

    @Test
    fun returningFromDetailPreservesTheSearchText() {
        val context = ExercisePickerNavigationContext(query = "remo").openDetail(selected.id)

        val returned = context.closeDetail()

        assertEquals("remo", returned.query)
        assertNull(returned.selectedExerciseId)
    }

    @Test
    fun returningFromDetailPreservesTheExpandedCategory() {
        val context = ExercisePickerNavigationContext(expandedCategory = "ESPALDA")
            .openDetail(selected.id)

        val returned = context.closeDetail()

        assertEquals("ESPALDA", returned.expandedCategory)
        assertNull(returned.selectedExerciseId)
    }

    @Test
    fun selectingAnExerciseReturnsTheChosenLibraryName() {
        val result = selectExerciseDefinition(original, selected)

        assertEquals(selected.name, result.name)
    }

    @Test
    fun selectionReplacesThePreviousExerciseName() {
        val result = selectExerciseDefinition(original, selected)

        assertNotEquals(original.name, result.name)
        assertEquals("Press militar con mancuernas", result.name)
    }

    @Test
    fun selectionPreservesSets() {
        assertEquals(original.sets, selectExerciseDefinition(original, selected).sets)
    }

    @Test
    fun selectionPreservesRepetitions() {
        assertEquals(original.repetitions, selectExerciseDefinition(original, selected).repetitions)
    }

    @Test
    fun selectionPreservesAllTimesAndRest() {
        val result = selectExerciseDefinition(original, selected)

        assertEquals(original.concentricSeconds, result.concentricSeconds)
        assertEquals(original.eccentricSeconds, result.eccentricSeconds)
        assertEquals(original.restSeconds, result.restSeconds)
    }

    @Test
    fun selectionCopiesLibraryNoteAndPreservesEveryTrainingValue() {
        val definition = ExerciseDefinition(
            id = "custom-exercise-with-note",
            name = "Ejercicio seleccionado",
            category = "PECHO",
            notes = "Mantener los codos pegados."
        )

        val result = selectExerciseDefinition(original, definition)

        assertEquals(definition.name, result.name)
        assertEquals(definition.notes, result.notes)
        assertEquals(original.sets, result.sets)
        assertEquals(original.repetitions, result.repetitions)
        assertEquals(original.concentricSeconds, result.concentricSeconds)
        assertEquals(original.eccentricSeconds, result.eccentricSeconds)
        assertEquals(original.restSeconds, result.restSeconds)
    }

    @Test
    fun addingFromDetailReplacesOnlyNameAndNote() {
        val definition = ExerciseDefinition(
            id = "detail-selection",
            name = "Seleccionado desde ficha",
            category = "ESPALDA",
            notes = "Nota desde ficha"
        )

        val result = selectExerciseDefinition(original, definition)

        assertEquals(definition.name, result.name)
        assertEquals(definition.notes, result.notes)
        assertEquals(original.sets, result.sets)
        assertEquals(original.repetitions, result.repetitions)
        assertEquals(original.concentricSeconds, result.concentricSeconds)
        assertEquals(original.eccentricSeconds, result.eccentricSeconds)
        assertEquals(original.restSeconds, result.restSeconds)
    }

    @Test
    fun officialExerciseDetailIsReadOnly() {
        assertTrue(ExerciseLibrary.isOfficial(selected.id))
        assertFalse(selected.canBeManagedByUser())
    }

    @Test
    fun customExerciseDetailAllowsEditingAndDeletion() {
        val custom = ExerciseDefinition(
            id = "custom-exercise-manageable",
            name = "Personalizado",
            category = "PECHO"
        )

        assertTrue(custom.canBeManagedByUser())
    }

    @Test
    fun detailReflectsTheUpdatedCustomExerciseAfterEditing() {
        val originalDefinition = ExerciseDefinition(
            id = "custom-exercise-updated-detail",
            name = "Nombre anterior",
            category = "PECHO"
        )
        val edited = originalDefinition.copy(name = "Nombre actualizado", notes = "Nota actualizada")

        try {
            ExerciseLibrary.replaceCustom(listOf(originalDefinition))
            assertEquals("Nombre anterior", ExerciseLibrary.find(originalDefinition.id)?.name)
            ExerciseLibrary.replaceCustom(listOf(edited))

            assertEquals("Nombre actualizado", ExerciseLibrary.find(originalDefinition.id)?.name)
            assertEquals("Nota actualizada", ExerciseLibrary.find(originalDefinition.id)?.notes)
        } finally {
            ExerciseLibrary.replaceCustom(emptyList())
        }
    }

    @Test
    fun successfulDeletionReturnsToLibraryWithContextPreserved() {
        val context = ExercisePickerNavigationContext(
            query = "personal",
            expandedCategory = "PECHO"
        ).openDetail("custom-exercise-delete")

        val afterDeletion = context.afterDeletedExercise()

        assertNull(afterDeletion.selectedExerciseId)
        assertEquals("personal", afterDeletion.query)
        assertEquals("PECHO", afterDeletion.expandedCategory)
    }

    @Test
    fun routineExerciseCanOverrideTheCopiedLibraryNoteIndependently() {
        val definition = ExerciseDefinition(
            id = "definition-note",
            name = "Ejercicio con indicación",
            category = "HOMBROS",
            notes = "Nota general de biblioteca."
        )

        val selectedForRoutine = selectExerciseDefinition(original, definition)
            .copy(notes = "Nota propia de esta rutina.")

        assertEquals("Nota propia de esta rutina.", selectedForRoutine.notes)
        assertEquals("Nota general de biblioteca.", definition.notes)
    }

    @Test
    fun emptySearchDoesNotProduceSearchRows() {
        assertTrue(exercisePickerResults("").isEmpty())
        assertTrue(exercisePickerResults("   ").isEmpty())
    }

    @Test
    fun searchWithoutMatchesReturnsAnEmptyList() {
        assertTrue(exercisePickerResults("resultado inexistente xyz").isEmpty())
    }
}
