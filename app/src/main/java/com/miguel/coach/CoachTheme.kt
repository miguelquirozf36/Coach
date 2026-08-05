package com.miguel.coach

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class TrainingRingColors(
    val warmupRing: Color,
    val concentricRing: Color,
    val eccentricRing: Color,
    val restSeriesRing: Color,
    val restExerciseRing: Color
)

private val ObsidianTrainingRingColors = TrainingRingColors(
    warmupRing = Color(0xFF7FA8C9),
    concentricRing = Color(0xFF88B59A),
    eccentricRing = Color(0xFFD0A06F),
    restSeriesRing = Color(0xFF8D9EAD),
    restExerciseRing = Color(0xFFA69ABA)
)

val LocalTrainingRingColors = staticCompositionLocalOf { ObsidianTrainingRingColors }

enum class CoachTheme(
    val id: String,
    val displayName: String,
    val description: String,
    val colorScheme: ColorScheme,
    val trainingRingColors: TrainingRingColors
) {
    OBSIDIAN(
        id = "obsidian",
        displayName = "Obsidian",
        description = "Oscuro, sobrio y de alto contraste.",
        colorScheme = darkColorScheme(
            primary = Color(0xFFAAB8C8),
            onPrimary = Color(0xFF17202A),
            primaryContainer = Color(0xFF303C49),
            onPrimaryContainer = Color(0xFFD8E3EF),
            secondary = Color(0xFFB7BBC5),
            onSecondary = Color(0xFF252830),
            secondaryContainer = Color(0xFF383B44),
            onSecondaryContainer = Color(0xFFE1E2E8),
            tertiary = Color(0xFFC2B8AA),
            onTertiary = Color(0xFF302A23),
            background = Color(0xFF111318),
            onBackground = Color(0xFFE3E5EA),
            surface = Color(0xFF181B21),
            onSurface = Color(0xFFE3E5EA),
            surfaceVariant = Color(0xFF292D35),
            onSurfaceVariant = Color(0xFFC5C8D0),
            outline = Color(0xFF8E929B),
            outlineVariant = Color(0xFF454951),
            error = Color(0xFFF2B8B5),
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF9DEDC)
        ),
        trainingRingColors = ObsidianTrainingRingColors
    ),
    OCEAN(
        id = "ocean",
        displayName = "Ocean",
        description = "Azules profundos y serenos.",
        colorScheme = darkColorScheme(
            primary = Color(0xFF91BAC9),
            onPrimary = Color(0xFF102D37),
            primaryContainer = Color(0xFF294A56),
            onPrimaryContainer = Color(0xFFC7E6F1),
            secondary = Color(0xFFAABDC4),
            onSecondary = Color(0xFF1D3036),
            secondaryContainer = Color(0xFF34474D),
            onSecondaryContainer = Color(0xFFD5E5EA),
            tertiary = Color(0xFFB6B8D0),
            onTertiary = Color(0xFF292B42),
            background = Color(0xFF0F171C),
            onBackground = Color(0xFFDDE5E8),
            surface = Color(0xFF162127),
            onSurface = Color(0xFFDDE5E8),
            surfaceVariant = Color(0xFF27343A),
            onSurfaceVariant = Color(0xFFBCC8CC),
            outline = Color(0xFF879499),
            outlineVariant = Color(0xFF3E4B50),
            error = Color(0xFFF2B8B5),
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF9DEDC)
        ),
        trainingRingColors = TrainingRingColors(
            warmupRing = Color(0xFF78AFC5),
            concentricRing = Color(0xFF7FB49F),
            eccentricRing = Color(0xFFC99B74),
            restSeriesRing = Color(0xFF849FAD),
            restExerciseRing = Color(0xFFA49FBE)
        )
    ),
    FOREST(
        id = "forest",
        displayName = "Forest",
        description = "Verdes naturales y descansados.",
        colorScheme = darkColorScheme(
            primary = Color(0xFFA8C2A5),
            onPrimary = Color(0xFF173219),
            primaryContainer = Color(0xFF304D32),
            onPrimaryContainer = Color(0xFFD3E8D0),
            secondary = Color(0xFFB5C0AE),
            onSecondary = Color(0xFF283127),
            secondaryContainer = Color(0xFF3E493C),
            onSecondaryContainer = Color(0xFFDCE7D6),
            tertiary = Color(0xFFA9C4C0),
            onTertiary = Color(0xFF173633),
            background = Color(0xFF121713),
            onBackground = Color(0xFFDFE6DE),
            surface = Color(0xFF19211B),
            onSurface = Color(0xFFDFE6DE),
            surfaceVariant = Color(0xFF2A342B),
            onSurfaceVariant = Color(0xFFC2CBC0),
            outline = Color(0xFF8C978A),
            outlineVariant = Color(0xFF444E43),
            error = Color(0xFFF2B8B5),
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF9DEDC)
        ),
        trainingRingColors = TrainingRingColors(
            warmupRing = Color(0xFF7FA8B8),
            concentricRing = Color(0xFF88B486),
            eccentricRing = Color(0xFFC1A06F),
            restSeriesRing = Color(0xFF8F9F95),
            restExerciseRing = Color(0xFFA39AAD)
        )
    ),
    LIGHT(
        id = "light",
        displayName = "Light",
        description = "Claro, limpio y neutral.",
        colorScheme = lightColorScheme(
            primary = Color(0xFF465F76),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD5E4F2),
            onPrimaryContainer = Color(0xFF162C3D),
            secondary = Color(0xFF59636E),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFDDE3EA),
            onSecondaryContainer = Color(0xFF1D252C),
            tertiary = Color(0xFF675F76),
            onTertiary = Color.White,
            background = Color(0xFFF6F7F8),
            onBackground = Color(0xFF1A1C1E),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1A1C1E),
            surfaceVariant = Color(0xFFE2E5E9),
            onSurfaceVariant = Color(0xFF45494E),
            outline = Color(0xFF75797E),
            outlineVariant = Color(0xFFC5C9CE),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        ),
        trainingRingColors = TrainingRingColors(
            warmupRing = Color(0xFF356B8C),
            concentricRing = Color(0xFF3E7752),
            eccentricRing = Color(0xFFA35E28),
            restSeriesRing = Color(0xFF5E7180),
            restExerciseRing = Color(0xFF6C5D8A)
        )
    );

    companion object {
        fun fromId(id: String?): CoachTheme = entries.firstOrNull { it.id == id } ?: OBSIDIAN
    }
}

fun trainingRingColor(
    phase: TrainingPhase,
    colors: TrainingRingColors
): Color = when (phase) {
    TrainingPhase.WARMUP,
    TrainingPhase.COUNTDOWN -> colors.warmupRing
    TrainingPhase.CONCENTRIC,
    TrainingPhase.REPETITION_ANNOUNCEMENT -> colors.concentricRing
    TrainingPhase.ECCENTRIC -> colors.eccentricRing
    TrainingPhase.REST -> colors.restSeriesRing
    TrainingPhase.REST_BETWEEN_EXERCISES -> colors.restExerciseRing
}

@Composable
fun CoachTheme(theme: CoachTheme, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalTrainingRingColors provides theme.trainingRingColors) {
        MaterialTheme(colorScheme = theme.colorScheme, content = content)
    }
}
