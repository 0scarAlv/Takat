package com.takat.finanzas.ui.home

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
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.takat.finanzas.data.model.AccountTotals
import com.takat.finanzas.data.model.AccountWithBalance
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.data.model.key
import com.takat.finanzas.ui.components.MovementDetailDialog
import com.takat.finanzas.ui.components.MovementRow
import com.takat.finanzas.ui.stats.StatsScreen
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
    onAddTransfer: () -> Unit
) {
    val repository = rememberRepository()
    val viewModel: HomeViewModel = viewModel(factory = LambdaViewModelFactory { HomeViewModel(repository) })
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedMovement by remember { mutableStateOf<Movement?>(null) }
    var titleTapCount by remember { mutableStateOf(0) }
    var showEasterEgg by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(pageCount = { 2 })

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        if (pagerState.currentPage == 0) "Takat" else "Estadísticas",
                        modifier = Modifier.clickable {
                            titleTapCount++
                            if (titleTapCount >= 5) {
                                showEasterEgg = true
                                titleTapCount = 0
                            }
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = false,
                    onClick = onAddAccount,
                    icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = null) },
                    label = { Text("Cuenta") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onAddTransaction,
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    label = { Text("Movimiento") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onAddTransfer,
                    icon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                    label = { Text("Transferencia") }
                )
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            PagerDotsIndicator(
                pageCount = 2,
                currentPage = pagerState.currentPage,
                onDotClick = { page -> scope.launch { pagerState.animateScrollToPage(page) } },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                if (page == 0) {
                    HomeContent(
                        uiState = uiState,
                        onOpenAccount = onOpenAccount,
                        onMovementClick = { selectedMovement = it }
                    )
                } else {
                    StatsScreen()
                }
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
            title = { Text("¡Encontraste el secreto!") },
            text = { Text("Sip, esta app la hice yo — OscarAlv. Kotlin, Compose y bastante café.") },
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
    onMovementClick: (Movement) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { TotalsCard(uiState.totals) }

        if (uiState.accounts.isNotEmpty()) {
            item {
                Text(
                    "Cuentas",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(uiState.accounts, key = { "acc_${it.account.id}" }) { accountWithBalance ->
                AccountCard(accountWithBalance, onClick = { onOpenAccount(accountWithBalance.account.id) })
            }
        } else {
            item {
                Text(
                    "Todavía no tenés cuentas. Tocá \"Cuenta\" abajo para crear la primera.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            Text(
                "Movimientos",
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
private fun PagerDotsIndicator(
    pageCount: Int,
    currentPage: Int,
    onDotClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center
    ) {
        repeat(pageCount) { page ->
            val active = page == currentPage
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(if (active) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { onDotClick(page) }
            )
        }
    }
}

@Composable
private fun TotalsCard(totals: AccountTotals, modifier: Modifier = Modifier) {
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
                totals.availableCents.centsToDisplay(),
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
