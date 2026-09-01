package com.takat.finanzas.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import com.takat.finanzas.ui.charts.ChartsScreen
import com.takat.finanzas.ui.components.MovementDetailDialog
import com.takat.finanzas.ui.components.MovementRow
import com.takat.finanzas.ui.stats.StatsScreen
import com.takat.finanzas.ui.theme.AmberAccent
import com.takat.finanzas.ui.theme.NegativeRed
import com.takat.finanzas.ui.theme.PositiveGreen
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.centsToDisplay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onAddTransaction: () -> Unit,
    onAddTransfer: () -> Unit,
    onOpenCategoryExpenses: (categoryId: Long?, from: Long, to: Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFixedExpenses: () -> Unit,
    onPayFixedExpense: (fixedExpenseId: Long) -> Unit
) {
    val repository = rememberRepository()
    val viewModel: HomeViewModel = viewModel(factory = LambdaViewModelFactory { HomeViewModel(repository) })
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedMovement by remember { mutableStateOf<Movement?>(null) }
    var titleTapCount by remember { mutableStateOf(0) }
    var showEasterEgg by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (pagerState.currentPage) {
                            0 -> "Presupuesto"
                            1 -> "Takat"
                            else -> "Estadísticas"
                        },
                        modifier = Modifier.clickable {
                            titleTapCount++
                            if (titleTapCount >= 5) {
                                showEasterEgg = true
                                titleTapCount = 0
                            }
                        }
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pagerState.currentPage == 0,
                    onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                    icon = { Icon(Icons.Default.PieChart, contentDescription = null) },
                    label = { Text("Presupuesto") }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 1,
                    onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Inicio") }
                )
                NavigationBarItem(
                    selected = pagerState.currentPage == 2,
                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) },
                    label = { Text("Estadísticas") }
                )
            }
        },
        floatingActionButton = {
            AddActionFab(
                expanded = fabExpanded,
                onExpandedChange = { fabExpanded = it },
                onAddTransaction = { fabExpanded = false; onAddTransaction() },
                onAddTransfer = { fabExpanded = false; onAddTransfer() }
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> ChartsScreen()
                1 -> HomeContent(
                    uiState = uiState,
                    onOpenAccount = onOpenAccount,
                    onAddAccount = onAddAccount,
                    onMovementClick = { selectedMovement = it },
                    onOpenFixedExpenses = onOpenFixedExpenses,
                    onPayFixedExpense = onPayFixedExpense
                )
                else -> StatsScreen(onCategoryClick = onOpenCategoryExpenses)
            }
        }
    }

    selectedMovement?.let { movement ->
        MovementDetailDialog(
            movement = movement,
            onDismiss = { selectedMovement = null },
            onDelete = {
                scope.launch {
                    when (movement) {
                        is Movement.TransactionMovement -> repository.deleteTransaction(movement.transaction)
                        is Movement.TransferMovement -> repository.deleteTransfer(movement.transfer)
                    }
                    selectedMovement = null
                }
            }
        )
    }

    if (showEasterEgg) {
        AlertDialog(
            onDismissRequest = { showEasterEgg = false },
            title = { Text("Esteregg") },
            text = { Text("Esta app fue hecha en un hiperfoco con mucho café y aburrimiento.") },
            confirmButton = {
                TextButton(onClick = { showEasterEgg = false }) { Text("Cerrar") }
            }
        )
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onOpenAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onMovementClick: (Movement) -> Unit,
    onOpenFixedExpenses: () -> Unit,
    onPayFixedExpense: (fixedExpenseId: Long) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { TotalsCard(uiState.totals, uiState.sarcasticMessagesEnabled) }

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
                IconButton(onClick = onAddAccount) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva cuenta")
                }
            }
        }
        if (uiState.accounts.isNotEmpty()) {
            items(uiState.accounts, key = { "acc_${it.account.id}" }) { accountWithBalance ->
                AccountCard(accountWithBalance, onClick = { onOpenAccount(accountWithBalance.account.id) })
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

/** Speed-dial FAB: tap the main "+" to reveal Movimiento/Transferencia above it, tap again (or pick one) to collapse. */
@Composable
private fun AddActionFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddTransaction: () -> Unit,
    onAddTransfer: () -> Unit
) {
    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledMiniFab(label = "Transferencia", icon = Icons.Default.SwapHoriz, onClick = onAddTransfer)
                LabeledMiniFab(label = "Movimiento", icon = Icons.AutoMirrored.Filled.ReceiptLong, onClick = onAddTransaction)
                Spacer(Modifier.height(4.dp))
            }
        }
        FloatingActionButton(onClick = { onExpandedChange(!expanded) }) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (expanded) "Cerrar" else "Agregar"
            )
        }
    }
}

@Composable
private fun LabeledMiniFab(label: String, icon: ImageVector, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 2.dp
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        SmallFloatingActionButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
    }
}

@Composable
private fun TotalsCard(totals: AccountTotals, sarcasticMessagesEnabled: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Disponible", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text(
                if (totals.availableCents < 0 && sarcasticMessagesEnabled) {
                    "${totals.availableCents.centsToDisplay()} (Eres irresponsable financieramente)"
                } else {
                    totals.availableCents.centsToDisplay()
                },
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (totals.availableCents < 0) NegativeRed else PositiveGreen
            )
            Spacer(Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Capital total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        totals.capitalCents.centsToDisplay(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (totals.pendingFixedExpensesCents > 0) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Gasto fijo este período", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            totals.pendingFixedExpensesCents.centsToDisplay(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = AmberAccent
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Deuda total", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        totals.debtCents.centsToDisplay(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (totals.debtCents > 0) NegativeRed else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountCard(item: AccountWithBalance, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            Text(
                item.balanceCents.centsToDisplay(),
                fontWeight = FontWeight.SemiBold,
                color = if (item.balanceCents < 0) NegativeRed else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
