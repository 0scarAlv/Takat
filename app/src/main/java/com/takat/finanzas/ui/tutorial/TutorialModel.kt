package com.takat.finanzas.ui.tutorial

import androidx.compose.ui.graphics.vector.ImageVector

/** What moves a step forward. */
sealed interface Advance {
    /** Purely explanatory: the bubble shows a Siguiente button. */
    data object Next : Advance

    /** The user taps the highlighted element itself (usually a button that navigates somewhere). */
    data object TapAnchor : Advance

    /** Waits for the app to report a real action, e.g. [TutorialEvents.ACCOUNT_CREATED] after saving. */
    data class Event(val name: String) : Advance
}

data class TutorialStep(
    /** NavGraph route pattern the step lives on; the guide pauses while the user is anywhere else. */
    val screen: String,
    val title: String,
    val body: String,
    /** [tutorialAnchor] id to spotlight. Null shows the bubble centered over a dimmed screen. */
    val anchor: String? = null,
    val advance: Advance = Advance.Next,
    /** Overrides the primary button text (only for [Advance.Next] steps). */
    val primaryLabel: String? = null,
    /**
     * Lets touches through the spotlight on an [Advance.Next] step, for form fields the user fills in before
     * tapping Siguiente. Off for purely explanatory steps, so tapping the highlighted button can't wander off.
     * Interactive steps ([Advance.TapAnchor], [Advance.Event]) always let touches through.
     */
    val touchable: Boolean = false
)

data class TutorialGuide(
    val id: String,
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val steps: List<TutorialStep>,
    /** Guide offered by the last step's primary button, so guides can chain (welcome -> first account). */
    val followUpId: String? = null
)
