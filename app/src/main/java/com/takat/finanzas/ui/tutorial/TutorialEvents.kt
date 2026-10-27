package com.takat.finanzas.ui.tutorial

/**
 * Domain events that interactive steps wait on ([Advance.Event]). ViewModels call [emit] right after the
 * real action succeeds. Delivery is synchronous on purpose: the step has to advance before the screen
 * reacts to the same save (e.g. popping back to Home), or the controller would see the route change first
 * and treat it as the user leaving the guide.
 */
object TutorialEvents {
    const val ACCOUNT_CREATED = "account.created"

    private val listeners = mutableSetOf<(String) -> Unit>()

    fun emit(name: String) = listeners.toList().forEach { it(name) }

    internal fun addListener(listener: (String) -> Unit) { listeners += listener }

    internal fun removeListener(listener: (String) -> Unit) { listeners -= listener }
}
