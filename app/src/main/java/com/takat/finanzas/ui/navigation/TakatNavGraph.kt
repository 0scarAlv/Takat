package com.takat.finanzas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.takat.finanzas.ui.account.AccountDetailScreen
import com.takat.finanzas.ui.account.AddEditAccountScreen
import com.takat.finanzas.ui.fixedexpense.FixedExpenseFormScreen
import com.takat.finanzas.ui.fixedexpense.FixedExpensesScreen
import com.takat.finanzas.ui.home.HomeScreen
import com.takat.finanzas.ui.settings.SettingsScreen
import com.takat.finanzas.ui.stats.CategoryExpensesScreen
import com.takat.finanzas.ui.transaction.AddTransactionScreen
import com.takat.finanzas.ui.transfer.AddTransferScreen
import com.takat.finanzas.widget.WidgetActions

private object Routes {
    const val HOME = "home"
    const val ACCOUNT_NEW = "account/new"
    const val ACCOUNT_DETAIL = "account/{accountId}"
    const val ACCOUNT_EDIT = "account/{accountId}/edit"
    const val TRANSACTION_NEW = "transaction/new?accountId={accountId}&fixedExpenseId={fixedExpenseId}"
    const val TRANSFER_NEW = "transfer/new"
    const val CATEGORY_EXPENSES = "stats/category/{categoryId}?from={from}&to={to}"
    const val SETTINGS = "settings"
    const val FIXED_EXPENSES = "fixed-expenses"
    const val FIXED_EXPENSE_FORM = "fixed-expenses/form?id={id}"

    fun accountDetail(id: Long) = "account/$id"
    fun accountEdit(id: Long) = "account/$id/edit"
    fun transactionNew(accountId: Long? = null, fixedExpenseId: Long? = null) =
        "transaction/new?accountId=${accountId ?: -1L}&fixedExpenseId=${fixedExpenseId ?: -1L}"
    fun categoryExpenses(categoryId: Long?, from: Long, to: Long) = "stats/category/${categoryId ?: -1L}?from=$from&to=$to"
    fun fixedExpenseForm(id: Long? = null) = "fixed-expenses/form?id=${id ?: -1L}"
}

@Composable
fun TakatNavGraph(
    pendingWidgetAction: MutableState<String?> = remember { mutableStateOf(null) },
    pendingFixedExpenseId: MutableState<Long?> = remember { mutableStateOf(null) }
) {
    val navController = rememberNavController()
    val widgetAction by pendingWidgetAction
    val fixedExpenseId by pendingFixedExpenseId

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

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenAccount = { navController.navigate(Routes.accountDetail(it)) },
                onAddAccount = { navController.navigate(Routes.ACCOUNT_NEW) },
                onAddTransaction = { navController.navigate(Routes.transactionNew()) },
                onAddTransfer = { navController.navigate(Routes.TRANSFER_NEW) },
                onOpenCategoryExpenses = { categoryId, from, to ->
                    navController.navigate(Routes.categoryExpenses(categoryId, from, to))
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenFixedExpenses = { navController.navigate(Routes.FIXED_EXPENSES) },
                onPayFixedExpense = { navController.navigate(Routes.transactionNew(fixedExpenseId = it)) }
            )
        }

        composable(Routes.ACCOUNT_NEW) {
            AddEditAccountScreen(
                accountId = null,
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
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
                    onDone = { navController.popBackStack(Routes.HOME, false) },
                    onCancel = { navController.popBackStack() }
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
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate(Routes.accountEdit(accountId)) },
                    onAddTransaction = { navController.navigate(Routes.transactionNew(accountId)) }
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
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Routes.TRANSFER_NEW) {
            AddTransferScreen(
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
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
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenFixedExpenses = { navController.navigate(Routes.FIXED_EXPENSES) }
            )
        }

        composable(Routes.FIXED_EXPENSES) {
            FixedExpensesScreen(
                onBack = { navController.popBackStack() },
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
                onDone = { navController.popBackStack() },
                onCancel = { navController.popBackStack() }
            )
        }
    }
}
