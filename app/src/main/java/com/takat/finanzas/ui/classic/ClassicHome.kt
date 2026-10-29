package com.takat.finanzas.ui.classic

import com.takat.finanzas.ui.tutorial.TutorialAnchors
import com.takat.finanzas.ui.tutorial.tutorialAnchor
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takat.finanzas.data.model.AccountTotals
import com.takat.finanzas.data.model.AccountWithBalance
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.data.model.key
import com.takat.finanzas.network.UpdateState
import com.takat.finanzas.ui.charts.ChartsScreen
import com.takat.finanzas.ui.components.MovementDetailDialog
import com.takat.finanzas.ui.components.MovementRow
import com.takat.finanzas.ui.components.UpdateAvailableDialog
import com.takat.finanzas.ui.components.WhatsNewDialog
import com.takat.finanzas.ui.home.FixedExpensesSection
import com.takat.finanzas.ui.home.HomeUiState
import com.takat.finanzas.ui.stats.StatsScreen
import com.takat.finanzas.ui.theme.cardBorder
import com.takat.finanzas.ui.theme.financeColors
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.Changelog
import com.takat.finanzas.util.centsToDisplay
import java.time.LocalDate
import kotlinx.coroutines.launch

// Classic (pre-3.0) look, kept verbatim from v2.5.2 so it can be picked in Ajustes > Diseño.
// Only renamed/re-packaged; avoid restyling it, that is what the modern UI is for.
@Composable
fun ClassicHomeContent(
    uiState: HomeUiState,
    onOpenAccount: (Long) -> Unit,
    onAddTransactionForAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onMovementClick: (Movement) -> Unit,
    onOpenFixedExpenses: () -> Unit,
    onPayFixedExpense: (fixedExpenseId: Long) -> Unit,
    onToggleAmountsHidden: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            TotalsCard(
                totals = uiState.totals,
                sarcasticMessagesEnabled = uiState.sarcasticMessagesEnabled,
                amountsHidden = uiState.amountsHidden,
                onToggleAmountsHidden = onToggleAmountsHidden,
                modifier = Modifier.tutorialAnchor(TutorialAnchors.HOME_HERO)
            )
        }

        item {
            FixedExpensesSection(
                onManageClick = onOpenFixedExpenses,
                onPayClick = onPayFixedExpense
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cuentas", style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onAddAccount, modifier = Modifier.tutorialAnchor(TutorialAnchors.HOME_ADD_ACCOUNT)) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva cuenta")
                }
            }
        }
        if (uiState.accounts.isNotEmpty()) {
            items(uiState.accounts, key = { "acc_${it.account.id}" }) { accountWithBalance ->
                AccountCard(
                    item = accountWithBalance,
                    amountsHidden = uiState.amountsHidden,
                    onClick = { onOpenAccount(accountWithBalance.account.id) },
                    onLongClick = { onAddTransactionForAccount(accountWithBalance.account.id) }
                )
            }
        } else {
            item {
                Text(
                    "Todavía no tenés cuentas. Tocá + arriba para crear la primera.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Text(
                "Transacciones",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        if (uiState.movements.isEmpty()) {
            item {
                Text(
                    "Sin movimientos todavía.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.movements, key = { it.key }) { movement ->
                MovementRow(
                    movement = movement,
                    currentAccountId = null,
                    onClick = { onMovementClick(movement) }
                )
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun TotalsCard(
    totals: AccountTotals,
    sarcasticMessagesEnabled: Boolean,
    amountsHidden: Boolean,
    onToggleAmountsHidden: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mask = "••••••"
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = cardBorder,
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Disponible", color = MaterialTheme.colorScheme.onSurfaceVariant)
                // Full 48dp touch target, nudged into the card's padding so the header row stays compact.
                IconButton(onClick = onToggleAmountsHidden, modifier = Modifier.offset(x = 12.dp)) {
                    Icon(
                        if (amountsHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (amountsHidden) "Mostrar montos" else "Ocultar montos",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                when {
                    amountsHidden -> mask
                    totals.availableCents < 0 && sarcasticMessagesEnabled ->
                        "${totals.availableCents.centsToDisplay()} (Eres irresponsable financieramente)"
                    else -> totals.availableCents.centsToDisplay()
                },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                // While masked, a red/green color would still leak whether the balance is negative.
                color = when {
                    amountsHidden -> MaterialTheme.colorScheme.onSurface
                    totals.availableCents < 0 -> MaterialTheme.financeColors.negative
                    else -> MaterialTheme.financeColors.positive
                }
            )
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Capital total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (amountsHidden) mask else totals.capitalCents.centsToDisplay(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (totals.pendingFixedExpensesCents > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Gasto fijo este período", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            if (amountsHidden) mask else totals.pendingFixedExpensesCents.centsToDisplay(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.financeColors.warning
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Deuda total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (amountsHidden) mask else totals.debtCents.centsToDisplay(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (totals.debtCents > 0 && !amountsHidden) MaterialTheme.financeColors.negative else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            if (totals.financedCents > 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Deuda de crédito: " + (if (amountsHidden) mask else totals.financedCents.centsToDisplay()) +
                        if (totals.financedIncludedInAvailable) " (restado del disponible)" else " (no se resta del disponible)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AccountCard(item: AccountWithBalance, amountsHidden: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val haptics = LocalHapticFeedback.current
    val shape = RoundedCornerShape(18.dp)
    // Non-clickable Card + clip + combinedClickable (Card(onClick) has no long-press); ripple stays clipped to the shape.
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = cardBorder
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(item.account.colorArgb), CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Text(item.account.name, style = MaterialTheme.typography.titleMedium)
                if (!item.account.includeInTotal) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.VisibilityOff,
                        contentDescription = "No suma al resumen",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (amountsHidden) "••••••" else item.balanceCents.centsToDisplay(),
                    fontWeight = FontWeight.SemiBold,
                    color = if (item.balanceCents < 0 && !amountsHidden) MaterialTheme.financeColors.negative else MaterialTheme.colorScheme.onSurface
                )
                if (item.financedCents > 0) {
                    Text(
                        "Deuda de crédito: " + if (amountsHidden) "••••••" else item.financedCents.centsToDisplay(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
