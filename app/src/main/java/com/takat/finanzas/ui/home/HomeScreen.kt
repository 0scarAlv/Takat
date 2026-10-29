package com.takat.finanzas.ui.home

import com.takat.finanzas.ui.components.CreditLimitBar
import com.takat.finanzas.ui.tutorial.LocalTutorialController
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
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.LaunchedEffect
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
import com.takat.finanzas.network.UpdateState
import com.takat.finanzas.ui.components.MovementDetailDialog
import com.takat.finanzas.ui.components.MovementRow
import com.takat.finanzas.ui.components.UpdateAvailableDialog
import com.takat.finanzas.ui.components.WhatsNewDialog
import com.takat.finanzas.util.Changelog
import com.takat.finanzas.ui.stats.StatsScreen
import com.takat.finanzas.ui.util.LambdaViewModelFactory
import com.takat.finanzas.ui.util.rememberRepository
import com.takat.finanzas.util.centsToDisplay
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.takat.finanzas.ui.theme.financeColors
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.takat.finanzas.ui.components.IconBadge
import com.takat.finanzas.ui.components.SectionHeader
import com.takat.finanzas.ui.components.TakatCard
import com.takat.finanzas.util.toDayHeader
import com.takat.finanzas.util.toLocalDate
import com.takat.finanzas.data.entity.UiStyle
import com.takat.finanzas.ui.classic.ClassicHomeContent
import com.takat.finanzas.ui.theme.LocalUiStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenAccount: (Long) -> Unit,
    onTransferFromAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onAddTransaction: () -> Unit,
    onAddTransfer: () -> Unit,
    onOpenCategoryExpenses: (categoryId: Long?, from: Long, to: Long) -> Unit,
    onOpenDayExpenses: (date: LocalDate) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenFixedExpenses: () -> Unit,
    onPayFixedExpense: (fixedExpenseId: Long) -> Unit,
    onEditTransaction: (Long) -> Unit
) {
    val repository = rememberRepository()
    val viewModel: HomeViewModel = viewModel(factory = LambdaViewModelFactory { HomeViewModel(repository) })
    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    var selectedMovement by remember { mutableStateOf<Movement?>(null) }
    var titleTapCount by remember { mutableStateOf(0) }
    var showEasterEgg by remember { mutableStateOf(false) }
    var fabExpanded by remember { mutableStateOf(false) }
    var showChangelog by remember { mutableStateOf(false) }
    var showUpdateDialog by remember { mutableStateOf(false) }
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    // System back from Presupuesto or Estadísticas returns to Inicio instead of leaving the app.
    BackHandler(enabled = pagerState.currentPage != 1) {
        scope.launch { pagerState.animateScrollToPage(1) }
    }
    // Guides point at things on Inicio (balance card, accounts), so bring that page up when one starts.
    val activeGuide = LocalTutorialController.current?.activeGuide
    LaunchedEffect(activeGuide) {
        if (activeGuide != null && pagerState.currentPage != 1) pagerState.animateScrollToPage(1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (pagerState.currentPage == 1) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Takat",
                                modifier = Modifier.clickable {
                                    titleTapCount++
                                    if (titleTapCount >= 5) {
                                        showEasterEgg = true
                                        titleTapCount = 0
                                    }
                                }
                            )
                            val updateInfo by UpdateState.available
                            if (updateInfo != null) {
                                Spacer(Modifier.width(10.dp))
                                Button(
                                    onClick = { showUpdateDialog = true },
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(32.dp)
                                ) {
                                    Text("Actualizar", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    } else {
                        Text(if (pagerState.currentPage == 0) "Presupuesto" else "Estadísticas")
                    }
                },
                actions = {
                    IconButton(onClick = { showChangelog = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Novedades")
                    }
                    IconButton(onClick = onOpenSettings, modifier = Modifier.tutorialAnchor(TutorialAnchors.HOME_SETTINGS)) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            NavigationBar(modifier = Modifier.tutorialAnchor(TutorialAnchors.HOME_NAV)) {
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
                onAddTransfer = { fabExpanded = false; onAddTransfer() },
                modifier = Modifier.tutorialAnchor(TutorialAnchors.HOME_FAB)
            )
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().padding(innerPadding)
        ) { page ->
            when (page) {
                0 -> ChartsScreen()
                1 -> if (LocalUiStyle.current == UiStyle.CLASSIC) ClassicHomeContent(
                    uiState = uiState,
                    onOpenAccount = onOpenAccount,
                    onTransferFromAccount = onTransferFromAccount,
                    onAddAccount = onAddAccount,
                    onMovementClick = { selectedMovement = it },
                    onOpenFixedExpenses = onOpenFixedExpenses,
                    onPayFixedExpense = onPayFixedExpense,
                    onToggleAmountsHidden = viewModel::toggleAmountsHidden
                ) else HomeContent(
                    uiState = uiState,
                    onOpenAccount = onOpenAccount,
                    onTransferFromAccount = onTransferFromAccount,
                    onAddAccount = onAddAccount,
                    onMovementClick = { selectedMovement = it },
                    onOpenFixedExpenses = onOpenFixedExpenses,
                    onPayFixedExpense = onPayFixedExpense,
                    onToggleAmountsHidden = viewModel::toggleAmountsHidden
                )
                else -> StatsScreen(onCategoryClick = onOpenCategoryExpenses, onDayClick = onOpenDayExpenses)
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
            },
            onEdit = (movement as? Movement.TransactionMovement)?.let { tx ->
                { selectedMovement = null; onEditTransaction(tx.transaction.id) }
            }
        )
    }

    if (showChangelog) {
        WhatsNewDialog(
            entries = Changelog.entries,
            onDismiss = { showChangelog = false }
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

    if (showUpdateDialog) {
        UpdateState.available.value?.let { info ->
            UpdateAvailableDialog(info = info, onDismiss = { showUpdateDialog = false })
        }
    }
}

@Composable
private fun HomeContent(
    uiState: HomeUiState,
    onOpenAccount: (Long) -> Unit,
    onTransferFromAccount: (Long) -> Unit,
    onAddAccount: () -> Unit,
    onMovementClick: (Movement) -> Unit,
    onOpenFixedExpenses: () -> Unit,
    onPayFixedExpense: (fixedExpenseId: Long) -> Unit,
    onToggleAmountsHidden: () -> Unit
) {
    val today = remember { LocalDate.now() }
    // Movements arrive newest first; groupBy keeps that order for both the days and the rows within each day.
    val movementsByDay = remember(uiState.movements) { uiState.movements.groupBy { it.date.toLocalDate() } }
    val firstMovementKey = uiState.movements.firstOrNull()?.key

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        item {
            BalanceHero(
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
                onPayClick = onPayFixedExpense,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        item {
            SectionHeader("Cuentas", modifier = Modifier.padding(top = 12.dp)) {
                IconButton(onClick = onAddAccount, modifier = Modifier.tutorialAnchor(TutorialAnchors.HOME_ADD_ACCOUNT)) {
                    Icon(Icons.Default.Add, contentDescription = "Nueva cuenta")
                }
            }
        }
        item {
            if (uiState.accounts.isEmpty()) {
                EmptyHint("Todavía no tenés cuentas. Tocá + para crear la primera.")
            } else {
                AccountsGroup(uiState.accounts, uiState.amountsHidden, onOpenAccount, onTransferFromAccount)
            }
        }

        item { SectionHeader("Movimientos", modifier = Modifier.padding(top = 12.dp)) }

        if (movementsByDay.isEmpty()) {
            item { EmptyHint("Sin movimientos todavía. Tocá + abajo para registrar el primero.") }
        } else {
            movementsByDay.forEach { (day, movements) ->
                item(key = "day_$day") {
                    Text(
                        day.toDayHeader(today),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(start = 4.dp, top = 8.dp, bottom = 2.dp)
                            .semantics { heading() }
                    )
                }
                items(movements, key = { it.key }) { movement ->
                    MovementRow(
                        movement = movement,
                        currentAccountId = null,
                        modifier = if (movement.key == firstMovementKey) {
                            Modifier.tutorialAnchor(TutorialAnchors.HOME_FIRST_MOVEMENT)
                        } else Modifier,
                        showDate = false,
                        onClick = { onMovementClick(movement) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    TakatCard {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}

/** Speed-dial FAB: tap the main "+" to reveal Movimiento/Transferencia above it, tap again (or pick one) to collapse. */
@Composable
private fun AddActionFab(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAddTransaction: () -> Unit,
    onAddTransfer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.End) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                LabeledMiniFab(
                    label = "Transferencia",
                    icon = Icons.Default.SwapHoriz,
                    onClick = onAddTransfer,
                    modifier = Modifier.tutorialAnchor(TutorialAnchors.HOME_FAB_TRANSFER)
                )
                LabeledMiniFab(
                    label = "Movimiento",
                    icon = Icons.AutoMirrored.Filled.ReceiptLong,
                    onClick = onAddTransaction,
                    modifier = Modifier.tutorialAnchor(TutorialAnchors.HOME_FAB_MOVEMENT)
                )
                Spacer(Modifier.height(4.dp))
            }
        }
        FloatingActionButton(onClick = { onExpandedChange(!expanded) }, modifier = modifier) {
            Icon(
                imageVector = if (expanded) Icons.Default.Close else Icons.Default.Add,
                contentDescription = if (expanded) "Cerrar" else "Agregar"
            )
        }
    }
}

@Composable
private fun LabeledMiniFab(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
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
        SmallFloatingActionButton(onClick = onClick, modifier = modifier) {
            Icon(icon, contentDescription = label)
        }
    }
}

/**
 * Home's headline: the available balance on a brand gradient, with capital / fixed expenses / debt underneath.
 * The whole card turns red when the balance is negative, unless amounts are hidden: there any color cue
 * would leak what the mask is hiding.
 */
@Composable
private fun BalanceHero(
    totals: AccountTotals,
    sarcasticMessagesEnabled: Boolean,
    amountsHidden: Boolean,
    onToggleAmountsHidden: () -> Unit,
    modifier: Modifier = Modifier
) {
    val mask = "••••••"
    val negative = totals.availableCents < 0 && !amountsHidden
    val gradient = if (negative) MaterialTheme.financeColors.heroNegative else MaterialTheme.financeColors.heroPositive
    val muted = Color.White.copy(alpha = 0.82f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.extraLarge)
            .background(Brush.verticalGradient(gradient))
            .animateContentSize()
            .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 20.dp)
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Disponible", style = MaterialTheme.typography.labelLarge, color = muted, modifier = Modifier.weight(1f))
                    IconButton(onClick = onToggleAmountsHidden, modifier = Modifier.offset(x = 12.dp)) {
                        Icon(
                            if (amountsHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (amountsHidden) "Mostrar montos" else "Ocultar montos",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    if (amountsHidden) mask else totals.availableCents.centsToDisplay(),
                    style = MaterialTheme.typography.displaySmall,
                    maxLines = 1
                )
                if (negative && sarcasticMessagesEnabled) {
                    Text(
                        "Eres irresponsable financieramente",
                        style = MaterialTheme.typography.bodySmall,
                        color = muted,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(Color.White.copy(alpha = 0.12f))
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                ) {
                    HeroLine(
                        "Capital total",
                        if (amountsHidden) mask else totals.capitalCents.centsToDisplay(),
                        muted,
                        Modifier.tutorialAnchor(TutorialAnchors.HOME_CAPITAL)
                    )
                    if (totals.pendingFixedExpensesCents > 0) {
                        HeroLine(
                            "Gasto fijo este período",
                            if (amountsHidden) mask else totals.pendingFixedExpensesCents.centsToDisplay(),
                            muted,
                            Modifier.tutorialAnchor(TutorialAnchors.HOME_FIXED)
                        )
                    }
                    HeroLine(
                        "Deuda total",
                        if (amountsHidden) mask else totals.debtCents.centsToDisplay(),
                        muted,
                        Modifier.tutorialAnchor(TutorialAnchors.HOME_DEBT)
                    )
                    if (totals.financedCents > 0) {
                        HeroLine(
                            if (totals.financedIncludedInAvailable) "Crédito (restado)" else "Crédito (no se resta)",
                            if (amountsHidden) mask else totals.financedCents.centsToDisplay(),
                            muted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroLine(label: String, value: String, labelColor: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = labelColor, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleSmall)
    }
}

/** All accounts in one card, separated by inset dividers, instead of one floating card per account. */
@Composable
private fun AccountsGroup(accounts: List<AccountWithBalance>, amountsHidden: Boolean,
    onOpenAccount: (Long) -> Unit,
    onTransferFromAccount: (Long) -> Unit
) {
    TakatCard(modifier = Modifier.tutorialAnchor(TutorialAnchors.HOME_ACCOUNTS)) {
        accounts.forEachIndexed { index, item ->
            AccountRow(
                item,
                amountsHidden,
                onClick = { onOpenAccount(item.account.id) },
                onLongClick = { onTransferFromAccount(item.account.id) }
            )
            if (index < accounts.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 68.dp),
                    color = MaterialTheme.colorScheme.outlineVariant
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun AccountRow(item: AccountWithBalance, amountsHidden: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    val account = item.account
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            )
            .heightIn(min = 64.dp)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // The initial is decorative (the name sits right next to it), so it's hidden from TalkBack.
        IconBadge(
            containerColor = Color(account.colorArgb),
            contentColor = Color.White,
            modifier = Modifier.clearAndSetSemantics {}
        ) {
            Text(account.name.take(1).uppercase(), style = MaterialTheme.typography.titleSmall)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 14.dp)
        ) {
            Text(account.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val subtitle = when {
                item.financedCents > 0 -> "Crédito: " + if (amountsHidden) "••••••" else item.financedCents.centsToDisplay()
                !account.includeInTotal -> "No suma al resumen"
                else -> null
            }
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            CreditLimitBar(item, amountsHidden, compact = true, modifier = Modifier.padding(top = 4.dp))
        }
        Text(
            if (amountsHidden) "••••••" else item.balanceCents.centsToDisplay(),
            style = MaterialTheme.typography.titleSmall,
            color = if (item.balanceCents < 0 && !amountsHidden) MaterialTheme.financeColors.negative else MaterialTheme.colorScheme.onSurface
        )
    }
}
