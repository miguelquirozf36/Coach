package com.miguel.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.Image
import kotlin.math.roundToInt

private const val BUBBLE_MARGIN_DP = 24
private const val TARGET_GAP_DP = 16

internal data class TourArrowStyle(
    val halfWidthDp: Float = 7f,
    val headLengthDp: Float = 10f,
    val strokeWidthDp: Float = 3f,
    val targetGapDp: Float = 6f
)

internal val SHARED_TOUR_ARROW_STYLE = TourArrowStyle()

internal data class TourArrowGeometry(
    val lineStart: Offset,
    val tip: Offset,
    val headLeft: Offset,
    val headRight: Offset
)

internal enum class TourStep { TRAIN, EDIT, PROGRAMS, CUSTOM }

internal enum class TourTarget { TRAIN_ROUTINE, EDIT_BUTTON, PROGRAMS_TAB, CREATE_PROGRAM }

internal data class OnboardingBenefit(val title: String, val description: String)

internal val ONBOARDING_BENEFITS = listOf(
    OnboardingBenefit(
        title = "Rutinas personalizadas",
        description = "Crea o elige rutinas y adapta series, repeticiones y más."
    ),
    OnboardingBenefit(
        title = "Entrenamiento guiado",
        description = "Voz y temporizador para cada repetición y descanso."
    ),
    OnboardingBenefit(
        title = "Sigue tu progreso",
        description = "Registra tu avance y alcanza tus objetivos."
    )
)

internal fun onboardingGreeting(name: String): String = "Hola, $name"

internal fun tourTargetForStep(step: TourStep): TourTarget = when (step) {
    TourStep.TRAIN -> TourTarget.TRAIN_ROUTINE
    TourStep.EDIT -> TourTarget.EDIT_BUTTON
    TourStep.PROGRAMS -> TourTarget.PROGRAMS_TAB
    TourStep.CUSTOM -> TourTarget.CREATE_PROGRAM
}

internal fun hasValidTourBounds(bounds: Rect?): Boolean = bounds != null &&
    bounds.width > 0f && bounds.height > 0f &&
    bounds.left.isFinite() && bounds.top.isFinite() && bounds.right.isFinite() && bounds.bottom.isFinite()

internal fun registerTourTargetBounds(
    targets: Map<TourTarget, Rect>,
    target: TourTarget,
    bounds: Rect
): Map<TourTarget, Rect> = if (hasValidTourBounds(bounds)) targets + (target to bounds) else targets

internal fun tourBoundsForStep(targets: Map<TourTarget, Rect>, step: TourStep): Rect? =
    targets[tourTargetForStep(step)].takeIf(::hasValidTourBounds)

internal data class TourCopy(val counter: String, val title: String, val body: String, val button: String)

internal fun tourCopy(step: TourStep): TourCopy = when (step) {
    TourStep.TRAIN -> TourCopy("1/4", "Aquí comienza tu entrenamiento", "En esta sección ves tu rutina del día con los ejercicios que vas a realizar.", "SIGUIENTE")
    TourStep.EDIT -> TourCopy("2/4", "Edita los tiempos de cada ejercicio", "Aquí puedes ajustar la duración de la fase concéntrica (positiva), excéntrica (negativa) y descansos.", "SIGUIENTE")
    TourStep.PROGRAMS -> TourCopy("3/4", "Sigue o cambia tu programa", "Aquí puedes ver y cambiar tu programa de entrenamiento cuando lo necesites.", "SIGUIENTE")
    TourStep.CUSTOM -> TourCopy("4/4", "Crea tu rutina personalizada", "Aquí puedes crear tus propias rutinas con ejercicios, series, repeticiones y descansos.", "EMPEZAR")
}

internal fun nextTourStep(step: TourStep): TourStep? = when (step) {
    TourStep.TRAIN -> TourStep.EDIT
    TourStep.EDIT -> TourStep.PROGRAMS
    TourStep.PROGRAMS -> TourStep.CUSTOM
    TourStep.CUSTOM -> null
}

internal fun tourTargetLabel(step: TourStep): String = when (step) {
    TourStep.TRAIN -> "TARJETA DE RUTINA"
    TourStep.EDIT -> "EDITAR"
    TourStep.PROGRAMS -> "PROGRAMAS"
    TourStep.CUSTOM -> "CREAR PROGRAMA"
}

internal enum class TourBubbleSide { ABOVE, BELOW }

internal fun chooseTourBubbleSide(
    step: TourStep,
    target: Rect,
    viewportHeight: Float,
    bubbleHeight: Float,
    safeTop: Float,
    safeBottom: Float,
    margin: Float,
    gap: Float
): TourBubbleSide {
    if (step == TourStep.PROGRAMS || step == TourStep.CUSTOM) return TourBubbleSide.ABOVE
    val fitsAbove = safeTop + margin + bubbleHeight + gap <= target.top
    val fitsBelow = target.bottom + gap + bubbleHeight + margin <= viewportHeight - safeBottom
    val preferred = if (target.center.y >= viewportHeight / 2f) TourBubbleSide.ABOVE else TourBubbleSide.BELOW
    return when {
        preferred == TourBubbleSide.ABOVE && fitsAbove -> TourBubbleSide.ABOVE
        preferred == TourBubbleSide.BELOW && fitsBelow -> TourBubbleSide.BELOW
        fitsAbove -> TourBubbleSide.ABOVE
        fitsBelow -> TourBubbleSide.BELOW
        target.top - safeTop >= viewportHeight - safeBottom - target.bottom -> TourBubbleSide.ABOVE
        else -> TourBubbleSide.BELOW
    }
}

internal fun tourBubbleTop(
    side: TourBubbleSide,
    target: Rect,
    viewportHeight: Float,
    bubbleHeight: Float,
    safeTop: Float,
    safeBottom: Float,
    margin: Float,
    gap: Float
): Float = when (side) {
    TourBubbleSide.ABOVE -> (target.top - gap - bubbleHeight).coerceAtLeast(safeTop + margin)
    TourBubbleSide.BELOW -> (target.bottom + gap).coerceAtMost(viewportHeight - safeBottom - margin - bubbleHeight)
}

internal fun tourBubbleLeft(
    target: Rect,
    viewportWidth: Float,
    bubbleWidth: Float,
    safeLeft: Float,
    safeRight: Float,
    margin: Float
): Float = (target.center.x - bubbleWidth / 2f).coerceIn(
    safeLeft + margin,
    (viewportWidth - safeRight - margin - bubbleWidth).coerceAtLeast(safeLeft + margin)
)

internal fun tourArrowGeometry(
    side: TourBubbleSide,
    bubble: Rect,
    target: Rect,
    pixelsPerDp: Float,
    style: TourArrowStyle = SHARED_TOUR_ARROW_STYLE
): TourArrowGeometry {
    val halfWidth = style.halfWidthDp * pixelsPerDp
    val headLength = style.headLengthDp * pixelsPerDp
    val targetGap = style.targetGapDp * pixelsPerDp
    val anchorX = target.center.x.coerceIn(bubble.left + 16f * pixelsPerDp, bubble.right - 16f * pixelsPerDp)
    val bubbleAbove = side == TourBubbleSide.ABOVE
    val lineStart = Offset(anchorX, if (bubbleAbove) bubble.bottom else bubble.top)
    val tip = Offset(target.center.x, if (bubbleAbove) target.top - targetGap else target.bottom + targetGap)
    val headBaseY = tip.y + if (bubbleAbove) -headLength else headLength
    return TourArrowGeometry(
        lineStart = lineStart,
        tip = tip,
        headLeft = Offset(tip.x - halfWidth, headBaseY),
        headRight = Offset(tip.x + halfWidth, headBaseY)
    )
}

@Composable
internal fun WelcomeScreen(onContinue: (String) -> String?) {
    var name by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val focusManager = LocalFocusManager.current
    fun submit() {
        if (name.trim().isEmpty()) {
            error = if (name.isEmpty()) "Introduce tu nombre." else "El nombre no puede contener solo espacios."
            return
        }
        error = onContinue(name)
        if (error == null) focusManager.clearFocus()
    }
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Image(
                painter = painterResource(R.drawable.coach_logo_full),
                contentDescription = "Logo de Coach",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(0.84f).aspectRatio(460.34f / 332.73f)
            )
            Text(
                "Bienvenido a Coach",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "¿Cómo te llamas?",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Usaremos tu nombre para personalizar tu experiencia.",
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= MAX_USER_NAME_LENGTH) { name = it; error = null } },
                    modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                    label = { Text("Tu nombre") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = { error?.let { Text(it) } },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() })
                )
                Text(
                    "Puedes cambiarlo más adelante en Ajustes.",
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Button(onClick = { submit() }, enabled = name.trim().isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                Text("CONTINUAR")
            }
        }
    }
}

@Composable
internal fun GreetingScreen(name: String, onContinue: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
        val useHorizontalCards = maxWidth >= 300.dp
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.coach_logo_full),
                contentDescription = "Logo de Coach",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth(0.52f).aspectRatio(460.34f / 332.73f)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    onboardingGreeting(name),
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Te mostraremos rápidamente cómo funciona Coach.",
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            if (useHorizontalCards) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ONBOARDING_BENEFITS.forEachIndexed { index, benefit ->
                        OnboardingBenefitCard(
                            benefit = benefit,
                            icon = onboardingBenefitIcons[index],
                            modifier = Modifier.weight(1f).heightIn(min = 208.dp)
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ONBOARDING_BENEFITS.forEachIndexed { index, benefit ->
                        OnboardingBenefitCard(
                            benefit = benefit,
                            icon = onboardingBenefitIcons[index],
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
                Text("COMENZAR TOUR")
            }
        }
    }
}

@Composable
private fun OnboardingBenefitCard(
    benefit: OnboardingBenefit,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                benefit.title,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                benefit.description,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

private val onboardingBenefitIcons = listOf(
    simpleOnboardingIcon("Dumbbell") {
        path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(7f, 7f); lineTo(17f, 17f); moveTo(5f, 5f); lineTo(3f, 7f); lineTo(7f, 11f); lineTo(9f, 9f)
            moveTo(19f, 19f); lineTo(21f, 17f); lineTo(17f, 13f); lineTo(15f, 15f)
        }
    },
    simpleOnboardingIcon("Timer") {
        path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(9f, 2f); lineTo(15f, 2f); moveTo(12f, 6f); lineTo(12f, 13f); lineTo(16f, 15f)
            moveTo(19f, 6f); lineTo(17.5f, 7.5f); moveTo(12f, 6f); curveTo(7.6f, 6f, 4f, 9.6f, 4f, 14f); curveTo(4f, 18.4f, 7.6f, 22f, 12f, 22f); curveTo(16.4f, 22f, 20f, 18.4f, 20f, 14f); curveTo(20f, 9.6f, 16.4f, 6f, 12f, 6f)
        }
    },
    simpleOnboardingIcon("Progress") {
        path(fill = SolidColor(Color.Transparent), stroke = SolidColor(Color.Black), strokeLineWidth = 2f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round) {
            moveTo(4f, 20f); lineTo(4f, 14f); lineTo(8f, 14f); lineTo(8f, 20f)
            moveTo(10f, 20f); lineTo(10f, 10f); lineTo(14f, 10f); lineTo(14f, 20f)
            moveTo(16f, 20f); lineTo(16f, 5f); lineTo(20f, 5f); lineTo(20f, 20f)
        }
    }
)

private fun simpleOnboardingIcon(name: String, paths: ImageVector.Builder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply(paths).build()

@Composable
internal fun CoachMarkOverlay(target: Rect?, step: TourStep, onNext: () -> Unit, onSkip: () -> Unit) {
    val copy = tourCopy(step)
    val scrim = Color.Black.copy(alpha = 0.68f)
    val spotlightColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    var bubbleSize by remember { mutableStateOf(IntSize.Zero) }
    var bubbleBounds by remember { mutableStateOf<Rect?>(null) }
    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().clearAndSetSemantics {
            contentDescription = "Recorrido ${copy.counter}: ${copy.title}. ${copy.body}"
        }.clickable(enabled = true, onClick = {})
    ) {
        if (target == null) {
            Box(Modifier.fillMaxSize().background(scrim))
        } else {
            val pad = 6.dp
            Box(Modifier.fillMaxWidth().padding(bottom = 0.dp).background(Color.Transparent))
            Box(Modifier.fillMaxWidth().padding(bottom = 0.dp).background(Color.Transparent))
            androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                val p = pad.toPx()
                val left = (target.left - p).coerceAtLeast(0f)
                val top = (target.top - p).coerceAtLeast(0f)
                val right = (target.right + p).coerceAtMost(size.width)
                val bottom = (target.bottom + p).coerceAtMost(size.height)
                drawRect(scrim, androidx.compose.ui.geometry.Offset.Zero, androidx.compose.ui.geometry.Size(size.width, top))
                drawRect(scrim, androidx.compose.ui.geometry.Offset(0f, top), androidx.compose.ui.geometry.Size(left, bottom - top))
                drawRect(scrim, androidx.compose.ui.geometry.Offset(right, top), androidx.compose.ui.geometry.Size(size.width - right, bottom - top))
                drawRect(scrim, androidx.compose.ui.geometry.Offset(0f, bottom), androidx.compose.ui.geometry.Size(size.width, size.height - bottom))
                drawRoundRect(spotlightColor, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Size(right-left, bottom-top), cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                bubbleBounds?.let { bubble ->
                    val arrowGeometry = tourArrowGeometry(
                        side = if (bubble.bottom <= target.top) TourBubbleSide.ABOVE else TourBubbleSide.BELOW,
                        bubble = bubble,
                        target = target,
                        pixelsPerDp = density.density
                    )
                    drawLine(spotlightColor, arrowGeometry.lineStart, arrowGeometry.tip, strokeWidth = SHARED_TOUR_ARROW_STYLE.strokeWidthDp.dp.toPx())
                    val arrowHead = Path().apply {
                        moveTo(arrowGeometry.tip.x, arrowGeometry.tip.y)
                        lineTo(arrowGeometry.headLeft.x, arrowGeometry.headLeft.y)
                        lineTo(arrowGeometry.headRight.x, arrowGeometry.headRight.y)
                        close()
                    }
                    drawPath(arrowHead, spotlightColor)
                }
            }
        }
        val viewportWidth = with(density) { maxWidth.toPx() }
        val viewportHeight = with(density) { maxHeight.toPx() }
        val safeTop = WindowInsets.safeDrawing.getTop(density).toFloat()
        val safeBottom = WindowInsets.safeDrawing.getBottom(density).toFloat()
        val safeLeft = WindowInsets.safeDrawing.getLeft(density, layoutDirection).toFloat()
        val safeRight = WindowInsets.safeDrawing.getRight(density, layoutDirection).toFloat()
        val margin = with(density) { BUBBLE_MARGIN_DP.dp.toPx() }
        val gap = with(density) { TARGET_GAP_DP.dp.toPx() }
        val resolvedTarget = target ?: Rect.Zero
        val side = chooseTourBubbleSide(step, resolvedTarget, viewportHeight, bubbleSize.height.toFloat(), safeTop, safeBottom, margin, gap)
        val bubbleTop = if (target == null) safeTop + margin else
            tourBubbleTop(side, resolvedTarget, viewportHeight, bubbleSize.height.toFloat(), safeTop, safeBottom, margin, gap)
        val bubbleLeft = if (target == null) safeLeft + margin else
            tourBubbleLeft(resolvedTarget, viewportWidth, bubbleSize.width.toFloat(), safeLeft, safeRight, margin)
        val availableBubbleHeight = if (target == null) {
            viewportHeight - safeTop - safeBottom - margin * 2f
        } else when (side) {
            TourBubbleSide.ABOVE -> resolvedTarget.top - gap - safeTop - margin
            TourBubbleSide.BELOW -> viewportHeight - safeBottom - margin - resolvedTarget.bottom - gap
        }
        Card(
            modifier = Modifier.align(Alignment.TopStart)
                .offset { IntOffset(bubbleLeft.roundToInt(), bubbleTop.roundToInt()) }
                .widthIn(max = 520.dp)
                .heightIn(max = with(density) { availableBubbleHeight.coerceAtLeast(1f).toDp() })
                .onGloballyPositioned {
                    bubbleSize = it.size
                    bubbleBounds = it.boundsInWindow()
                },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(copy.counter, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(copy.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(copy.body, style = MaterialTheme.typography.bodyLarge)
                Button(onClick = onNext, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)) { Text(copy.button) }
                TextButton(onClick = onSkip, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("OMITIR RECORRIDO") }
            }
        }
    }
}
