package com.takat.finanzas.ui.tutorial

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val ScrimColor = Color.Black.copy(alpha = 0.62f)

/**
 * Draws the running guide over the whole app: a dimmed screen with a hole over the step's anchor and a
 * bubble explaining it. Touches inside the hole reach the real UI underneath (that's what makes the guides
 * interactive); touches anywhere else are swallowed so the user can't wander off mid-step.
 */
@Composable
fun TutorialOverlay(controller: TutorialController) {
    val guide = controller.activeGuide ?: return
    val step = controller.currentStep ?: return
    if (controller.paused) {
        PausedChip(guide = guide, onResume = controller::resume, onClose = controller::skip)
        return
    }
    if (!controller.isOnStepScreen) return

    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val stepKey = guide.id to controller.stepIndex
    // Each step starts with the keyboard closed, so its bubble and spotlight aren't hidden behind it.
    LaunchedEffect(stepKey) { focusManager.clearFocus() }

    // An anchor can be briefly missing while its screen animates in; only after a grace period fall back to a
    // centered bubble (e.g. the anchor is on a pager page that isn't showing).
    var anchorGraceOver by remember(stepKey) { mutableStateOf(step.anchor == null) }
    LaunchedEffect(stepKey) {
        if (!anchorGraceOver) {
            delay(600)
            anchorGraceOver = true
        }
    }

    var origin by remember { mutableStateOf(Offset.Zero) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }
    val padPx = with(density) { 6.dp.toPx() }
    val hole: Rect? = step.anchor
        ?.let(controller::anchorBounds)
        ?.takeIf { it.width > 0f && it.height > 0f }
        ?.translate(-origin)
        ?.inflate(padPx)

    val needsInteraction = step.advance != Advance.Next
    // Without a spotlight, an interactive step can't point at anything, so leave the screen usable instead.
    val dimAndBlock = hole != null || !needsInteraction

    val bubbleAlpha = remember(stepKey) { Animatable(0f) }
    LaunchedEffect(stepKey) { bubbleAlpha.animateTo(1f, tween(220)) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned {
                origin = it.positionInWindow()
                overlaySize = it.size
            }
    ) {
        if (hole == null && step.anchor != null && !anchorGraceOver) return@Box

        if (dimAndBlock) {
            Spotlight(hole)
            TouchBlockers(hole.takeIf { needsInteraction || step.touchable }, overlaySize)
        }

        val topInset = WindowInsets.safeDrawing.getTop(density)
        val bottomInset = max(WindowInsets.ime.getBottom(density), WindowInsets.navigationBars.getBottom(density))
        Layout(
            content = {
                TutorialBubble(
                    guide = guide,
                    step = step,
                    stepIndex = controller.stepIndex,
                    canGoBack = controller.canGoBack(),
                    onNext = controller::next,
                    onBack = controller::back,
                    onSkip = controller::skip,
                    onLater = controller::finishWithoutFollowUp,
                    modifier = Modifier.graphicsLayer { alpha = bubbleAlpha.value }
                )
            },
            modifier = Modifier.fillMaxSize()
        ) { measurables, constraints ->
            val margin = 16.dp.roundToPx()
            val gap = 12.dp.roundToPx()
            val maxWidth = min(constraints.maxWidth - 2 * margin, 380.dp.roundToPx())
            val bubble = measurables.first().measure(Constraints(maxWidth = maxWidth))
            val minY = topInset + margin
            val maxY = constraints.maxHeight - bottomInset - margin - bubble.height
            val y = if (hole == null) {
                (minY + maxY) / 2
            } else {
                val below = hole.bottom.roundToInt() + gap
                val above = hole.top.roundToInt() - gap - bubble.height
                when {
                    below <= maxY -> below
                    above >= minY -> above
                    else -> maxY
                }
            }
            layout(constraints.maxWidth, constraints.maxHeight) {
                bubble.place((constraints.maxWidth - bubble.width) / 2, y.coerceAtLeast(minY))
            }
        }
    }
}

@Composable
private fun Spotlight(hole: Rect?) {
    val pulse by rememberInfiniteTransition(label = "tutorialPulse").animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "ringAlpha"
    )
    val ringColor = MaterialTheme.colorScheme.primary
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            // Offscreen so BlendMode.Clear punches through the scrim only, not the app underneath.
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
    ) {
        drawRect(ScrimColor)
        if (hole != null) {
            val radius = CornerRadius(16.dp.toPx())
            drawRoundRect(Color.Black, hole.topLeft, hole.size, radius, blendMode = BlendMode.Clear)
            drawRoundRect(ringColor.copy(alpha = pulse), hole.topLeft, hole.size, radius, style = Stroke(3.dp.toPx()))
        }
    }
}

/**
 * Four touch-eating boxes around the hole (or one over everything). Nothing of the overlay covers the hole
 * itself, so hit-testing falls through to the real element there.
 */
@Composable
private fun TouchBlockers(hole: Rect?, size: IntSize) {
    val width = size.width.toFloat()
    val height = size.height.toFloat()
    if (hole == null) {
        Blocker(Rect(0f, 0f, width, height))
    } else {
        // The hole can hang off-screen (an anchor half scrolled out), so clamp it to the overlay.
        val top = hole.top.coerceIn(0f, height)
        val bottom = hole.bottom.coerceIn(0f, height)
        Blocker(Rect(0f, 0f, width, top))
        Blocker(Rect(0f, bottom, width, height))
        Blocker(Rect(0f, top, hole.left.coerceIn(0f, width), bottom))
        Blocker(Rect(hole.right.coerceIn(0f, width), top, width, bottom))
    }
}

@Composable
private fun Blocker(area: Rect) {
    if (area.width <= 0f || area.height <= 0f) return
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .offset { IntOffset(area.left.roundToInt(), area.top.roundToInt()) }
            .size(with(density) { area.width.toDp() }, with(density) { area.height.toDp() })
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            }
    )
}

@Composable
private fun TutorialBubble(
    guide: TutorialGuide,
    step: TutorialStep,
    stepIndex: Int,
    canGoBack: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSkip: () -> Unit,
    onLater: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLast = stepIndex == guide.steps.lastIndex
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "${guide.title} · ${stepIndex + 1} de ${guide.steps.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Text(step.title, style = MaterialTheme.typography.titleMedium)
            Text(step.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    isLast && guide.followUpId != null -> TextButton(onClick = onLater) { Text("Más tarde") }
                    !isLast -> TextButton(onClick = onSkip) { Text("Saltar guía") }
                }
                Spacer(Modifier.weight(1f))
                if (canGoBack) TextButton(onClick = onBack) { Text("Atrás") }
                if (step.advance == Advance.Next) {
                    Button(onClick = onNext) {
                        Text(step.primaryLabel ?: if (isLast) "Listo" else "Siguiente")
                    }
                } else {
                    Icon(
                        Icons.Filled.TouchApp,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Tocá lo resaltado",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
        }
    }
}

/** Shown while the user is on a screen the current step isn't on (e.g. they opened Ajustes mid-guide). */
@Composable
private fun PausedChip(guide: TutorialGuide, onResume: () -> Unit, onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(bottom = 96.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.inverseSurface, shadowElevation = 6.dp) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.School, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("${guide.title}: en pausa", style = MaterialTheme.typography.labelLarge)
                TextButton(
                    onClick = onResume,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.inversePrimary)
                ) { Text("Retomar") }
                IconButton(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "Cerrar guía", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
