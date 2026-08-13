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
val LocalNavigationBarContainerColor = staticCompositionLocalOf { Color.Unspecified }
val LocalDialogContainerColor = staticCompositionLocalOf { Color.Unspecified }

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
            surfaceVariant = Color(0xFFE8EBEF),
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
    ),
    GREEN(
        id = "green",
        displayName = "Green",
        description = "Enérgico, enfocado y equilibrado.",
        colorScheme = darkColorScheme(
            primary = Color(0xFF86DF86),
            onPrimary = Color(0xFF08390E),
            primaryContainer = Color(0xFF235126),
            onPrimaryContainer = Color(0xFFB7F5B5),
            secondary = Color(0xFFAEB4BE),
            onSecondary = Color(0xFF292E35),
            secondaryContainer = Color(0xFF3A4048),
            onSecondaryContainer = Color(0xFFD9DFE9),
            tertiary = Color(0xFF91D5B0),
            onTertiary = Color(0xFF0D3522),
            background = Color(0xFF0D1014),
            onBackground = Color(0xFFF5F5F5),
            surface = Color(0xFF1C222B),
            onSurface = Color(0xFFF5F5F5),
            surfaceVariant = Color(0xFF292F38),
            onSurfaceVariant = Color(0xFFAEB4BE),
            outline = Color(0xFF89919B),
            outlineVariant = Color(0xFF424952),
            error = Color(0xFFF2B8B5),
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF9DEDC)
        ),
        trainingRingColors = TrainingRingColors(
            warmupRing = Color(0xFF74BCA5),
            concentricRing = Color(0xFF86DF86),
            eccentricRing = Color(0xFFB4D979),
            restSeriesRing = Color(0xFF72A884),
            restExerciseRing = Color(0xFF91BFA4)
        )
    ),
    ORANGE(
        id = "orange",
        displayName = "Orange",
        description = "Enérgico, intenso y deportivo.",
        colorScheme = darkColorScheme(
            primary = Color(0xFFEA571C),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFF71300F),
            onPrimaryContainer = Color(0xFFFFDBCC),
            secondary = Color(0xFFAEB4BE),
            onSecondary = Color(0xFF292E35),
            secondaryContainer = Color(0xFF3A4048),
            onSecondaryContainer = Color(0xFFD9DFE9),
            tertiary = Color(0xFFE6A06F),
            onTertiary = Color(0xFF452B12),
            background = Color(0xFF0D1014),
            onBackground = Color(0xFFF5F5F5),
            surface = Color(0xFF1C222B),
            onSurface = Color(0xFFF5F5F5),
            surfaceVariant = Color(0xFF292F38),
            onSurfaceVariant = Color(0xFFAEB4BE),
            outline = Color(0xFF89919B),
            outlineVariant = Color(0xFF424952),
            error = Color(0xFFF2B8B5),
            onError = Color(0xFF601410),
            errorContainer = Color(0xFF8C1D18),
            onErrorContainer = Color(0xFFF9DEDC)
        ),
        trainingRingColors = TrainingRingColors(
            warmupRing = Color(0xFFE29A63),
            concentricRing = Color(0xFFEA571C),
            eccentricRing = Color(0xFFF08A36),
            restSeriesRing = Color(0xFFC36A42),
            restExerciseRing = Color(0xFFD98B60)
        )
    ),
    SAND(
        id = "sand",
        displayName = "Sand",
        description = "Claro, cálido y natural.",
        colorScheme = lightColorScheme(
            primary = Color(0xFFD56A32),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFFFDBCA),
            onPrimaryContainer = Color(0xFF4B1F08),
            secondary = Color(0xFF68645F),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFECE5DD),
            onSecondaryContainer = Color(0xFF292521),
            tertiary = Color(0xFF7A6047),
            onTertiary = Color.White,
            background = Color(0xFFF7F4EF),
            onBackground = Color(0xFF24211F),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF24211F),
            surfaceVariant = Color(0xFFE9E4DE),
            onSurfaceVariant = Color(0xFF68645F),
            outline = Color(0xFFDED8D0),
            outlineVariant = Color(0xFFE8E2DA),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        ),
        trainingRingColors = TrainingRingColors(
            warmupRing = Color(0xFF527A91),
            concentricRing = Color(0xFF477856),
            eccentricRing = Color(0xFFD56A32),
            restSeriesRing = Color(0xFF77685C),
            restExerciseRing = Color(0xFF765D82)
        )
    ),
    ICE(
        id = "ice",
        displayName = "Ice",
        description = "Claro, fresco y sereno.",
        colorScheme = lightColorScheme(
            primary = Color(0xFF4F7898),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD1E8F8),
            onPrimaryContainer = Color(0xFF102F44),
            secondary = Color(0xFF65717C),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFDDE6ED),
            onSecondaryContainer = Color(0xFF202A32),
            tertiary = Color(0xFF5F6F91),
            onTertiary = Color.White,
            background = Color(0xFFF3F7FA),
            onBackground = Color(0xFF20262C),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF20262C),
            surfaceVariant = Color(0xFFE3EAF0),
            onSurfaceVariant = Color(0xFF65717C),
            outline = Color(0xFFD9E1E7),
            outlineVariant = Color(0xFFE2E9EE),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        ),
        trainingRingColors = TrainingRingColors(
            warmupRing = Color(0xFF4F7898),
            concentricRing = Color(0xFF3F7668),
            eccentricRing = Color(0xFF9A633E),
            restSeriesRing = Color(0xFF607887),
            restExerciseRing = Color(0xFF626E91)
        )
    ),
    MINT(
        id = "mint",
        displayName = "Mint",
        description = "Claro, verde y relajante.",
        colorScheme = lightColorScheme(
            primary = Color(0xFF4F8A5B),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFD1E8D4),
            onPrimaryContainer = Color(0xFF173B1E),
            secondary = Color(0xFF637267),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFDDE8DE),
            onSecondaryContainer = Color(0xFF202A22),
            tertiary = Color(0xFF56706A),
            onTertiary = Color.White,
            background = Color(0xFFF3F8F3),
            onBackground = Color(0xFF20251F),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF20251F),
            surfaceVariant = Color(0xFFE2EDE3),
            onSurfaceVariant = Color(0xFF5F6960),
            outline = Color(0xFFC9D5CA),
            outlineVariant = Color(0xFFDCE6DD),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        ),
        trainingRingColors = TrainingRingColors(
            warmupRing = Color(0xFF4F7898),
            concentricRing = Color(0xFF4F8A5B),
            eccentricRing = Color(0xFF9A6B3F),
            restSeriesRing = Color(0xFF62786A),
            restExerciseRing = Color(0xFF6D688A)
        )
    ),
    SKY(
        id = "sky",
        displayName = "Sky",
        description = "Claro, fresco y luminoso.",
        colorScheme = lightColorScheme(
            primary = Color(0xFF3F8198),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFCBE8F1),
            onPrimaryContainer = Color(0xFF103641),
            secondary = Color(0xFF60747B),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFD9E8EC),
            onSecondaryContainer = Color(0xFF1D292D),
            tertiary = Color(0xFF626E91),
            onTertiary = Color.White,
            background = Color(0xFFF2F8FA),
            onBackground = Color(0xFF1E2528),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1E2528),
            surfaceVariant = Color(0xFFE0EDF1),
            onSurfaceVariant = Color(0xFF5D6D73),
            outline = Color(0xFFCAD8DC),
            outlineVariant = Color(0xFFDCE8EB),
            error = Color(0xFFBA1A1A),
            onError = Color.White,
            errorContainer = Color(0xFFFFDAD6),
            onErrorContainer = Color(0xFF410002)
        ),
        trainingRingColors = TrainingRingColors(
            warmupRing = Color(0xFF3F8198),
            concentricRing = Color(0xFF467C6C),
            eccentricRing = Color(0xFF9A6845),
            restSeriesRing = Color(0xFF607A84),
            restExerciseRing = Color(0xFF626E91)
        )
    );

    companion object {
        fun fromId(id: String?): CoachTheme = entries.firstOrNull { it.id == id } ?: OBSIDIAN
    }
}

internal fun navigationBarContainerColor(theme: CoachTheme): Color = when (theme) {
    CoachTheme.LIGHT -> Color(0xFFF1F3F5)
    CoachTheme.SAND -> Color(0xFFF3EFEA)
    CoachTheme.ICE -> Color(0xFFEEF3F6)
    CoachTheme.MINT -> Color(0xFFF0F6F0)
    CoachTheme.SKY -> Color(0xFFEEF6F8)
    else -> theme.colorScheme.surfaceContainer
}

internal fun dialogContainerColor(theme: CoachTheme): Color = when (theme) {
    CoachTheme.LIGHT,
    CoachTheme.SAND,
    CoachTheme.ICE,
    CoachTheme.MINT,
    CoachTheme.SKY -> theme.colorScheme.surface
    else -> theme.colorScheme.surfaceContainerHigh
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
    TrainingPhase.ISOMETRIC -> colors.concentricRing
    TrainingPhase.REST -> colors.restSeriesRing
    TrainingPhase.REST_BETWEEN_EXERCISES -> colors.restExerciseRing
}

@Composable
fun CoachTheme(theme: CoachTheme, content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalTrainingRingColors provides theme.trainingRingColors,
        LocalNavigationBarContainerColor provides navigationBarContainerColor(theme),
        LocalDialogContainerColor provides dialogContainerColor(theme)
    ) {
        MaterialTheme(colorScheme = theme.colorScheme, content = content)
    }
}
