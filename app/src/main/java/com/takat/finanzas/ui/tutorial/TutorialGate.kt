package com.takat.finanzas.ui.tutorial

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import com.takat.finanzas.data.repository.FinanceRepository
import kotlinx.coroutines.flow.first

/**
 * Starts the welcome guide on a fresh install: no progress row for it means never seen (upgrades get one
 * from MIGRATION_24_25). [onSettled] fires once no guide is running anymore, so the update dialog that
 * follows in the chain never pops up over a guide.
 */
@Composable
fun TutorialGate(controller: TutorialController, repository: FinanceRepository, onSettled: () -> Unit) {
    LaunchedEffect(Unit) {
        val seen = repository.tutorialProgress().first().any { it.tutorialId == TutorialCatalog.WELCOME }
        if (!seen) TutorialCatalog.byId(TutorialCatalog.WELCOME)?.let(controller::start)
        snapshotFlow { controller.activeGuide }.first { it == null }
        onSettled()
    }
}
