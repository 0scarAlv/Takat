package com.takat.finanzas.ui.tutorial

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import kotlinx.coroutines.delay

/**
 * Marks an element a guide can spotlight. Reports its on-screen bounds to the [TutorialController], scrolls
 * it into view when its step comes up, and reports taps on it (observed without consuming them, so the
 * element's own click handler still runs).
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.tutorialAnchor(id: String): Modifier = composed {
    val controller = LocalTutorialController.current ?: return@composed this
    val requester = remember { BringIntoViewRequester() }
    val isActive = controller.currentStep?.anchor == id && !controller.paused

    LaunchedEffect(isActive) {
        if (isActive) {
            // Let the screen finish its enter transition (and the keyboard settle) before scrolling.
            delay(250)
            requester.bringIntoView()
        }
    }
    DisposableEffect(controller, id) {
        onDispose { controller.unregisterAnchor(id) }
    }

    this
        .bringIntoViewRequester(requester)
        .onGloballyPositioned { controller.registerAnchor(id, it.boundsInWindow()) }
        .pointerInput(controller, id) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                if (waitForUpOrCancellation(PointerEventPass.Initial) != null) controller.onAnchorTapped(id)
            }
        }
}
