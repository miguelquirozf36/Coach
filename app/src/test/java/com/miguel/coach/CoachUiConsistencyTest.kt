package com.miguel.coach

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class CoachUiConsistencyTest {
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
}
