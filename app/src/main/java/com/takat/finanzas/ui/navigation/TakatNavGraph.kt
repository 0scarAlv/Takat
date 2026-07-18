package com.takat.finanzas.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.takat.finanzas.ui.account.AccountDetailScreen
import com.takat.finanzas.ui.account.AddEditAccountScreen
import com.takat.finanzas.ui.home.HomeScreen
import com.takat.finanzas.ui.transaction.AddTransactionScreen
import com.takat.finanzas.ui.transfer.AddTransferScreen

private object Routes {
    const val HOME = "home"
    const val ACCOUNT_NEW = "account/new"
    const val ACCOUNT_DETAIL = "account/{accountId}"
    const val ACCOUNT_EDIT = "account/{accountId}/edit"
    const val TRANSACTION_NEW = "transaction/new?accountId={accountId}"
    const val TRANSFER_NEW = "transfer/new"

    fun accountDetail(id: Long) = "account/$id"
    fun accountEdit(id: Long) = "account/$id/edit"
    fun transactionNew(accountId: Long? = null) = "transaction/new?accountId=${accountId ?: -1L}"
}

@Composable
fun TakatNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenAccount = { navController.navigate(Routes.accountDetail(it)) },
                onAddAccount = { navController.navigate(Routes.ACCOUNT_NEW) },
                onAddTransaction = { navController.navigate(Routes.transactionNew()) },
                onAddTransfer = { navController.navigate(Routes.TRANSFER_NEW) }
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
            arguments = listOf(navArgument("accountId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val rawId = backStackEntry.arguments?.getLong("accountId") ?: -1L
            AddTransactionScreen(
                preselectedAccountId = rawId.takeIf { it >= 0 },
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
    }
}
