package com.takat.finanzas.ui.tutorial

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.takat.finanzas.data.entity.TutorialProgressEntity
import com.takat.finanzas.data.repository.FinanceRepository
import com.takat.finanzas.ui.navigation.Routes
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Null outside the app's NavGraph (widgets, previews), where [tutorialAnchor] is a no-op. */
val LocalTutorialController = staticCompositionLocalOf<TutorialController?> { null }

/**
 * Runs at most one guide at a time. Activity-scoped (a ViewModel) so a guide survives rotation and the
 * screens it walks through. Screens never talk to it directly: they mark elements with [tutorialAnchor]
 * and ViewModels report actions through [TutorialEvents]; the NavGraph feeds it the current route.
 */
class TutorialController(private val repository: FinanceRepository) : ViewModel() {

    var activeGuide by mutableStateOf<TutorialGuide?>(null)
        private set
    var stepIndex by mutableIntStateOf(0)
        private set
    /** True while the user is on a screen the current step doesn't belong to. */
    var paused by mutableStateOf(false)
        private set
    private var currentRoute by mutableStateOf<String?>(null)

    private val anchors = mutableStateMapOf<String, Rect>()

    val progress: StateFlow<Map<String, TutorialProgressEntity>> = repository.tutorialProgress()
        .map { rows -> rows.associateBy { it.tutorialId } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val accountCount: StateFlow<Int> = repository.accountsWithBalance()
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun isAvailable(guide: TutorialGuide): Boolean = accountCount.value >= guide.minAccounts

    private val _homeRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Asks the NavGraph to go back to Home, e.g. when a guide is started from Ajustes. */
    val homeRequests: SharedFlow<Unit> = _homeRequests

    val currentStep: TutorialStep?
        get() = activeGuide?.steps?.getOrNull(stepIndex)

    /** False in the gap between a step advancing and its navigation landing; the overlay waits it out. */
    val isOnStepScreen: Boolean
        get() = currentStep?.screen == currentRoute

    private val eventListener: (String) -> Unit = { name ->
        val advance = currentStep?.advance
        if (advance is Advance.Event && advance.name == name) next()
    }

    init {
        TutorialEvents.addListener(eventListener)
    }

    override fun onCleared() {
        TutorialEvents.removeListener(eventListener)
    }

    fun start(guide: TutorialGuide) {
        activeGuide = guide
        stepIndex = 0
        if (currentRoute != guide.steps.first().screen) _homeRequests.tryEmit(Unit)
        reconcile()
    }

    fun next() {
        val guide = activeGuide ?: return
        if (stepIndex >= guide.steps.lastIndex) {
            finish(startFollowUp = true)
        } else {
            // No reconcile here: a tap or save advances before its navigation lands, and reconciling against
            // the old route would rewind the step. The route change that follows does the syncing.
            stepIndex++
        }
    }

    fun back() {
        previousStepIndex()?.let { stepIndex = it }
    }

    /** Back only stays on the same screen; going back across a navigation would need the user to undo it. */
    fun canGoBack(): Boolean = previousStepIndex() != null

    /** Previous step on this screen, passing over optional ones that were skipped (or they'd skip forward again). */
    private fun previousStepIndex(): Int? {
        val steps = activeGuide?.steps ?: return null
        val screen = steps[stepIndex].screen
        var i = stepIndex - 1
        while (i >= 0 && steps[i].screen == screen) {
            val step = steps[i]
            if (!(step.optional && step.anchor != null && anchors[step.anchor] == null)) return i
            i--
        }
        return null
    }

    /** Called by the overlay when an [TutorialStep.optional] step's anchor never showed up. */
    fun skipMissingOptionalStep() {
        val step = currentStep ?: return
        if (step.optional && step.anchor != null && anchors[step.anchor] == null) next()
    }

    /** Ends the guide early; it won't auto-start again but stays in Ajustes > Guías. */
    fun skip() {
        val guide = activeGuide ?: return
        viewModelScope.launch { repository.markTutorialSkipped(guide.id) }
        clear()
    }

    /** Last step's secondary button: counts as finished, but doesn't chain into the follow-up guide. */
    fun finishWithoutFollowUp() = finish(startFollowUp = false)

    private fun finish(startFollowUp: Boolean) {
        val guide = activeGuide ?: return
        viewModelScope.launch { repository.markTutorialCompleted(guide.id) }
        // Skip chained guides the user can't do yet (transfers with a single account) instead of dead-ending.
        var followUp = guide.followUpId?.let(TutorialCatalog::byId)
        while (followUp != null && !isAvailable(followUp)) followUp = followUp.followUpId?.let(TutorialCatalog::byId)
        clear()
        if (startFollowUp && followUp != null) start(followUp)
    }

    /** From the "guía en pausa" chip: rewind to the last step on Home and go there. */
    fun resume() {
        val steps = activeGuide?.steps ?: return
        stepIndex = (stepIndex downTo 0).firstOrNull { steps[it].screen == Routes.HOME } ?: 0
        _homeRequests.tryEmit(Unit)
        reconcile()
    }

    fun onRouteChanged(route: String?) {
        currentRoute = route
        reconcile()
    }

    fun onAnchorTapped(id: String) {
        val step = currentStep ?: return
        if (!paused && step.advance == Advance.TapAnchor && step.anchor == id) next()
    }

    fun registerAnchor(id: String, bounds: Rect) {
        if (anchors[id] != bounds) anchors[id] = bounds
    }

    fun unregisterAnchor(id: String) {
        anchors.remove(id)
    }

    fun anchorBounds(id: String): Rect? = anchors[id]

    /**
     * Keeps the step in sync with where the user actually is. A tap step whose button navigated moves on
     * (covers the case where the route changes before the tap is reported); landing on a screen an earlier
     * step lives on rewinds there (e.g. backing out of the account form returns to "tocá el +"); anywhere
     * else pauses the guide.
     */
    private fun reconcile() {
        val guide = activeGuide ?: return
        val route = currentRoute ?: return
        val steps = guide.steps
        val step = steps[stepIndex]
        if (step.screen == route) {
            paused = false
            return
        }
        val nextStep = steps.getOrNull(stepIndex + 1)
        if (step.advance == Advance.TapAnchor && nextStep?.screen == route) {
            stepIndex++
            paused = false
            return
        }
        val earlier = (stepIndex - 1 downTo 0).firstOrNull { steps[it].screen == route }
        if (earlier != null) {
            stepIndex = earlier
            paused = false
        } else {
            paused = true
        }
    }

    private fun clear() {
        activeGuide = null
        stepIndex = 0
        paused = false
    }
}
