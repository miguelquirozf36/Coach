package com.miguel.coach

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.foundation.Image
import kotlin.math.roundToInt

internal const val ONBOARDING_GREETING_DURATION_MILLIS = 3_000L
private const val BUBBLE_MARGIN_DP = 24
private const val TARGET_GAP_DP = 16

internal enum class TourStep { TRAIN, EDIT, PROGRAMS, CUSTOM }

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
    Column(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.coach_splash_icon),
            contentDescription = "Logo de Coach",
            modifier = Modifier.size(96.dp).align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.padding(12.dp))
        Text("Bienvenido a Coach", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.padding(12.dp))
        Text("¿Cómo te llamas?", style = MaterialTheme.typography.headlineSmall)
        Text("Usaremos tu nombre para personalizar tu experiencia.", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Button(onClick = { submit() }, enabled = name.trim().isNotEmpty(), modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("CONTINUAR")
        }
    }
}

@Composable
internal fun GreetingScreen(name: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Hola, $name 👋", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Text("Te mostraremos rápidamente cómo funciona Coach.", modifier = Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun CoachMarkOverlay(target: Rect?, step: TourStep, onNext: () -> Unit, onSkip: () -> Unit) {
    val copy = tourCopy(step)
    val scrim = Color.Black.copy(alpha = 0.68f)
    val spotlightColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
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
                    val bubbleAbove = bubble.bottom <= target.top
                    val start = Offset(target.center.x.coerceIn(bubble.left + 16.dp.toPx(), bubble.right - 16.dp.toPx()), if (bubbleAbove) bubble.bottom else bubble.top)
                    val end = Offset(target.center.x, if (bubbleAbove) target.top - p else target.bottom + p)
                    drawLine(spotlightColor, start, end, strokeWidth = 3.dp.toPx())
                    val direction = if (bubbleAbove) -1f else 1f
                    val arrow = Path().apply {
                        moveTo(end.x, end.y)
                        lineTo(end.x - 7.dp.toPx(), end.y + direction * 10.dp.toPx())
                        lineTo(end.x + 7.dp.toPx(), end.y + direction * 10.dp.toPx())
                        close()
                    }
                    drawPath(arrow, spotlightColor)
                }
            }
        }
        val viewportHeight = with(density) { maxHeight.toPx() }
        val safeTop = WindowInsets.safeDrawing.getTop(density).toFloat()
        val safeBottom = WindowInsets.safeDrawing.getBottom(density).toFloat()
        val margin = with(density) { BUBBLE_MARGIN_DP.dp.toPx() }
        val gap = with(density) { TARGET_GAP_DP.dp.toPx() }
        val resolvedTarget = target ?: Rect.Zero
        val side = chooseTourBubbleSide(step, resolvedTarget, viewportHeight, bubbleSize.height.toFloat(), safeTop, safeBottom, margin, gap)
        val bubbleTop = if (target == null) safeTop + margin else
            tourBubbleTop(side, resolvedTarget, viewportHeight, bubbleSize.height.toFloat(), safeTop, safeBottom, margin, gap)
        val availableBubbleHeight = if (target == null) {
            viewportHeight - safeTop - safeBottom - margin * 2f
        } else when (side) {
            TourBubbleSide.ABOVE -> resolvedTarget.top - gap - safeTop - margin
            TourBubbleSide.BELOW -> viewportHeight - safeBottom - margin - resolvedTarget.bottom - gap
        }
        Card(
            modifier = Modifier.align(Alignment.TopCenter)
                .offset { IntOffset(0, bubbleTop.roundToInt()) }
                .padding(horizontal = 20.dp)
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
