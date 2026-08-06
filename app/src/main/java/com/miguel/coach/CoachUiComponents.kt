package com.miguel.coach

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val COACH_BACK_TOUCH_TARGET = 48.dp
internal val COACH_BACK_ICON_SIZE = 28.dp
internal const val COACH_BACK_CONTENT_DESCRIPTION = "Volver"

@Composable
fun CoachBackButton(onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(COACH_BACK_TOUCH_TARGET)
    ) {
        Icon(
            imageVector = CoachArrowBackIcon,
            contentDescription = COACH_BACK_CONTENT_DESCRIPTION,
            modifier = Modifier.size(COACH_BACK_ICON_SIZE)
        )
    }
}

private val CoachArrowBackIcon: ImageVector = ImageVector.Builder(
    name = "CoachArrowBack",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(20f, 11f)
        horizontalLineTo(7.83f)
        lineTo(13.42f, 5.41f)
        lineTo(12f, 4f)
        lineTo(4f, 12f)
        lineTo(12f, 20f)
        lineTo(13.41f, 18.59f)
        lineTo(7.83f, 13f)
        horizontalLineTo(20f)
        close()
    }
}.build()
