package com.takat.finanzas.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.takat.finanzas.ui.account.AccountDetailScreen
import com.takat.finanzas.ui.account.AddEditAccountScreen
import com.takat.finanzas.ui.category.CategoriesScreen
import com.takat.finanzas.ui.fixedexpense.FixedExpenseFormScreen
import com.takat.finanzas.ui.fixedexpense.FixedExpensesScreen
import com.takat.finanzas.ui.home.HomeScreen
import com.takat.finanzas.ui.pairing.PairScanScreen
import com.takat.finanzas.ui.settings.SettingsScreen
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.ui.components.LocalOpenBalanceSnapshot
import com.takat.finanzas.ui.stats.BalanceSnapshotScreen
import com.takat.finanzas.ui.stats.CategoryExpensesScreen
import com.takat.finanzas.ui.stats.DayExpensesScreen
import com.takat.finanzas.ui.transaction.AddTransactionScreen
import com.takat.finanzas.ui.transfer.AddTransferScreen
import com.takat.finanzas.widget.WidgetActions
import java.time.LocalDate

private object Routes {
    const val HOME = "home"
    const val ACCOUNT_NEW = "account/new"
    const val ACCOUNT_DETAIL = "account/{accountId}"
    const val ACCOUNT_EDIT = "account/{accountId}/edit"
    const val TRANSACTION_NEW = "transaction/new?accountId={accountId}&fixedExpenseId={fixedExpenseId}"
    const val TRANSACTION_EDIT = "transaction/{transactionId}/edit"
    const val TRANSFER_NEW = "transfer/new"
    const val CATEGORY_EXPENSES = "stats/category/{categoryId}?from={from}&to={to}"
    const val DAY_EXPENSES = "stats/day/{epochDay}"
    const val SETTINGS = "settings"
    const val CATEGORIES = "categories"
    const val FIXED_EXPENSES = "fixed-expenses"
    const val FIXED_EXPENSE_FORM = "fixed-expenses/form?id={id}"
    const val PAIR_SCAN = "pair-scan"
    const val BALANCE_SNAPSHOT = "balance/{kind}/{id}"

    fun accountDetail(id: Long) = "account/$id"
    fun accountEdit(id: Long) = "account/$id/edit"
    fun transactionNew(accountId: Long? = null, fixedExpenseId: Long? = null) =
        "transaction/new?accountId=${accountId ?: -1L}&fixedExpenseId=${fixedExpenseId ?: -1L}"
    fun transactionEdit(id: Long) = "transaction/$id/edit"
    fun categoryExpenses(categoryId: Long?, from: Long, to: Long) = "stats/category/${categoryId ?: -1L}?from=$from&to=$to"
    fun dayExpenses(date: LocalDate) = "stats/day/${date.toEpochDay()}"
    fun fixedExpenseForm(id: Long? = null) = "fixed-expenses/form?id=${id ?: -1L}"
    fun balanceSnapshot(movement: Movement) = when (movement) {
        is Movement.TransactionMovement -> "balance/t/${movement.transaction.id}"
        is Movement.TransferMovement -> "balance/x/${movement.transfer.id}"
    }
}

@Composable
fun TakatNavGraph(
    pendingWidgetAction: MutableState<String?> = remember { mutableStateOf(null) },
    pendingFixedExpenseId: MutableState<Long?> = remember { mutableStateOf(null) },
    pendingShareUris: MutableState<List<Uri>> = remember { mutableStateOf(emptyList()) }
) {
    val navController = rememberNavController()
    val widgetAction by pendingWidgetAction
    val fixedExpenseId by pendingFixedExpenseId
    val shareUris by pendingShareUris
    // Snapshot of the share URIs consumed by the currently-open "new transaction" screen.
    // Kept separate from pendingShareUris so navigating there again later (e.g. via the FAB)
    // doesn't re-attach stale files from a previous share.
    var consumedShareUris by remember { mutableStateOf<List<Uri>>(emptyList()) }

    LaunchedEffect(widgetAction) {
        when (widgetAction) {
            WidgetActions.ACTION_NEW_TRANSACTION -> navController.navigate(Routes.transactionNew())
            WidgetActions.ACTION_NEW_TRANSFER -> navController.navigate(Routes.TRANSFER_NEW)
        }
        if (widgetAction != null) pendingWidgetAction.value = null
    }

    LaunchedEffect(fixedExpenseId) {
        if (fixedExpenseId != null) {
            navController.navigate(Routes.transactionNew(fixedExpenseId = fixedExpenseId))
            pendingFixedExpenseId.value = null
        }
    }

    LaunchedEffect(shareUris) {
        if (shareUris.isNotEmpty()) {
            consumedShareUris = shareUris
            pendingShareUris.value = emptyList()
            navController.navigate(Routes.transactionNew())
        }
    }

    val openBalanceSnapshot: (Movement) -> Unit = remember(navController) {
        { movement -> navController.navigate(Routes.balanceSnapshot(movement)) }
    }
    CompositionLocalProvider(LocalOpenBalanceSnapshot provides openBalanceSnapshot) {
        NavHost(navController = navController, startDestination = Routes.HOME) {
            composable(
                Routes.BALANCE_SNAPSHOT,
                arguments = listOf(
                    navArgument("kind") { type = NavType.StringType },
                    navArgument("id") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                BalanceSnapshotScreen(
                    isTransfer = backStackEntry.arguments?.getString("kind") == "x",
                    movementId = backStackEntry.arguments?.getLong("id") ?: -1L,
                    onBack = { navController.popBackStackFrom(backStackEntry) }
                )
            }

            composable(Routes.HOME) {
                HomeScreen(
                    onOpenAccount = { navController.navigate(Routes.accountDetail(it)) },
                    onAddAccount = { navController.navigate(Routes.ACCOUNT_NEW) },
                    onAddTransaction = { navController.navigate(Routes.transactionNew()) },
                    onAddTransfer = { navController.navigate(Routes.TRANSFER_NEW) },
                    onOpenCategoryExpenses = { categoryId, from, to ->
                        navController.navigate(Routes.categoryExpenses(categoryId, from, to))
                    },
                    onOpenDayExpenses = { date -> navController.navigate(Routes.dayExpenses(date)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenFixedExpenses = { navController.navigate(Routes.FIXED_EXPENSES) },
                    onPayFixedExpense = { navController.navigate(Routes.transactionNew(fixedExpenseId = it)) },
                    onEditTransaction = { navController.navigate(Routes.transactionEdit(it)) }
                )
            }

            composable(Routes.ACCOUNT_NEW) { backStackEntry ->
                AddEditAccountScreen(
                    accountId = null,
                    onDone = { navController.popBackStackFrom(backStackEntry) },
                    onCancel = { navController.popBackStackFrom(backStackEntry) }
                )
            }

            composable(
                Routes.ACCOUNT_EDIT,
                arguments = listOf(navArgument("accountId") { type = NavType.LongType })
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getLong("accountId")
                if (accountId != null) {
                    AddEditAccountScreen(
                        accountId = accountId,
                        onDone = { navController.popBackStackFrom(backStackEntry, toRoute = Routes.HOME) },
                        onCancel = { navController.popBackStackFrom(backStackEntry) }
                    )
                }
            }

            composable(
                Routes.ACCOUNT_DETAIL,
                arguments = listOf(navArgument("accountId") { type = NavType.LongType })
            ) { backStackEntry ->
                val accountId = backStackEntry.arguments?.getLong("accountId")
                if (accountId != null) {
                    AccountDetailScreen(
                        accountId = accountId,
                        onBack = { navController.popBackStackFrom(backStackEntry) },
                        onEdit = { navController.navigate(Routes.accountEdit(accountId)) },
                        onAddTransaction = { navController.navigate(Routes.transactionNew(accountId)) },
                        onEditTransaction = { navController.navigate(Routes.transactionEdit(it)) }
                    )
                }
            }

            composable(
                Routes.TRANSACTION_NEW,
                arguments = listOf(
                    navArgument("accountId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("fixedExpenseId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStackEntry ->
                val rawId = backStackEntry.arguments?.getLong("accountId") ?: -1L
                val rawFixedExpenseId = backStackEntry.arguments?.getLong("fixedExpenseId") ?: -1L
                AddTransactionScreen(
                    preselectedAccountId = rawId.takeIf { it >= 0 },
                    preselectedFixedExpenseId = rawFixedExpenseId.takeIf { it >= 0 },
                    initialShareUris = consumedShareUris,
                    onShareUrisConsumed = { consumedShareUris = emptyList() },
                    onDone = { navController.popBackStackFrom(backStackEntry) },
                    onCancel = { navController.popBackStackFrom(backStackEntry) }
                )
            }

            composable(
                Routes.TRANSACTION_EDIT,
                arguments = listOf(navArgument("transactionId") { type = NavType.LongType })
            ) { backStackEntry ->
                val transactionId = backStackEntry.arguments?.getLong("transactionId")
                if (transactionId != null) {
                    AddTransactionScreen(
                        preselectedAccountId = null,
                        editTransactionId = transactionId,
                        onDone = { navController.popBackStackFrom(backStackEntry) },
                        onCancel = { navController.popBackStackFrom(backStackEntry) }
                    )
                }
            }

            composable(Routes.TRANSFER_NEW) { backStackEntry ->
                AddTransferScreen(
                    onDone = { navController.popBackStackFrom(backStackEntry) },
                    onCancel = { navController.popBackStackFrom(backStackEntry) }
                )
            }

            composable(
                Routes.CATEGORY_EXPENSES,
                arguments = listOf(
                    navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L },
                    navArgument("from") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("to") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { backStackEntry ->
                val args = backStackEntry.arguments
                val rawCategoryId = args?.getLong("categoryId") ?: -1L
                CategoryExpensesScreen(
                    categoryId = rawCategoryId.takeIf { it >= 0 },
                    fromMillis = args?.getLong("from") ?: 0L,
                    toMillis = args?.getLong("to") ?: 0L,
                    onBack = { navController.popBackStackFrom(backStackEntry) },
                    onEditTransaction = { navController.navigate(Routes.transactionEdit(it)) }
                )
            }

            composable(
                Routes.DAY_EXPENSES,
                arguments = listOf(navArgument("epochDay") { type = NavType.LongType })
            ) { backStackEntry ->
                val epochDay = backStackEntry.arguments?.getLong("epochDay")
                if (epochDay != null) {
                    DayExpensesScreen(
                        date = LocalDate.ofEpochDay(epochDay),
                        onBack = { navController.popBackStackFrom(backStackEntry) },
                        onEditTransaction = { navController.navigate(Routes.transactionEdit(it)) }
                    )
                }
            }

            composable(Routes.SETTINGS) { backStackEntry ->
                SettingsScreen(
                    onBack = { navController.popBackStackFrom(backStackEntry) },
                    onOpenFixedExpenses = { navController.navigate(Routes.FIXED_EXPENSES) },
                    onOpenCategories = { navController.navigate(Routes.CATEGORIES) },
                    onOpenPairScan = { navController.navigate(Routes.PAIR_SCAN) }
                )
            }

            composable(Routes.PAIR_SCAN) { backStackEntry ->
                PairScanScreen(
                    onDone = { navController.popBackStackFrom(backStackEntry) },
                    onCancel = { navController.popBackStackFrom(backStackEntry) }
                )
            }

            composable(Routes.CATEGORIES) { backStackEntry ->
                CategoriesScreen(onBack = { navController.popBackStackFrom(backStackEntry) })
            }

            composable(Routes.FIXED_EXPENSES) { backStackEntry ->
                FixedExpensesScreen(
                    onBack = { navController.popBackStackFrom(backStackEntry) },
                    onAddNew = { navController.navigate(Routes.fixedExpenseForm()) },
                    onEdit = { id -> navController.navigate(Routes.fixedExpenseForm(id)) }
                )
            }

            composable(
                Routes.FIXED_EXPENSE_FORM,
                arguments = listOf(navArgument("id") { type = NavType.LongType; defaultValue = -1L })
            ) { backStackEntry ->
                val rawId = backStackEntry.arguments?.getLong("id") ?: -1L
                FixedExpenseFormScreen(
                    fixedExpenseId = rawId.takeIf { it >= 0 },
                    onDone = { navController.popBackStackFrom(backStackEntry) },
                    onCancel = { navController.popBackStackFrom(backStackEntry) }
                )
            }
        }
    }
}

/**
 * Pops only while [entry] is still the screen on top. Tapping a back arrow twice quickly (or again during the
 * exit animation) would otherwise pop a second time and remove Home too, leaving a blank screen.
 */
private fun NavController.popBackStackFrom(entry: NavBackStackEntry, toRoute: String? = null) {
    if (currentBackStackEntry?.id != entry.id) return
    if (toRoute != null) popBackStack(toRoute, inclusive = false) else popBackStack()
}
