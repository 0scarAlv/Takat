package com.takat.finanzas.data.repository

import com.takat.finanzas.data.AppDatabase
import com.takat.finanzas.data.entity.AccountEntity
import com.takat.finanzas.data.entity.CategoryEntity
import com.takat.finanzas.data.entity.TransactionEntity
import com.takat.finanzas.data.entity.TransferEntity
import com.takat.finanzas.data.model.AccountTotals
import com.takat.finanzas.data.model.AccountWithBalance
import com.takat.finanzas.data.model.CategoryExpense
import com.takat.finanzas.data.model.IncomeExpenseSummary
import com.takat.finanzas.data.model.Movement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FinanceRepository(db: AppDatabase) {

    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()
    private val transactionDao = db.transactionDao()
    private val transferDao = db.transferDao()

    val categories: Flow<List<CategoryEntity>> = categoryDao.getAll()

    fun accountsWithBalance(): Flow<List<AccountWithBalance>> =
        combine(accountDao.getAll(), transactionDao.getAll(), transferDao.getAll()) { accounts, transactions, transfers ->
            accounts.map { account -> AccountWithBalance(account, balanceFor(account.id, account.initialBalanceCents, transactions, transfers)) }
        }

    fun accountTotals(): Flow<AccountTotals> =
        accountsWithBalance().map { list ->
            val included = list.filter { it.account.includeInTotal }
            val capital = included.filter { !it.account.isDebt }.sumOf { it.balanceCents }
            val debtRaw = included.filter { it.account.isDebt }.sumOf { it.balanceCents }
            AccountTotals(availableCents = capital + debtRaw, capitalCents = capital, debtCents = -debtRaw)
        }

    fun expensesByCategory(fromInclusive: Long, toExclusive: Long): Flow<List<CategoryExpense>> =
        combine(transactionDao.getAll(), categoryDao.getAll()) { transactions, categories ->
            val categoryById = categories.associateBy { it.id }
            transactions
                .filter { it.amountCents < 0 && it.date >= fromInclusive && it.date < toExclusive }
                .groupBy { it.categoryId }
                .map { (categoryId, txs) -> CategoryExpense(categoryById[categoryId], txs.sumOf { -it.amountCents }) }
                .sortedByDescending { it.totalCents }
        }

    fun expenseTransactionsForCategory(categoryId: Long?, fromInclusive: Long, toExclusive: Long): Flow<List<Movement.TransactionMovement>> =
        combine(transactionDao.getAll(), categoryDao.getAll(), accountDao.getAll()) { transactions, categories, accounts ->
            val categoryById = categories.associateBy { it.id }
            val accountById = accounts.associateBy { it.id }
            transactions
                .filter { it.amountCents < 0 && it.date >= fromInclusive && it.date < toExclusive && it.categoryId == categoryId }
                .sortedByDescending { it.date }
                .map { Movement.TransactionMovement(it, categoryById[it.categoryId], accountById[it.accountId]) }
        }

    fun incomeExpenseSummary(fromInclusive: Long, toExclusive: Long): Flow<IncomeExpenseSummary> =
        transactionDao.getAll().map { transactions ->
            val inRange = transactions.filter { it.date >= fromInclusive && it.date < toExclusive }
            IncomeExpenseSummary(
                incomeCents = inRange.filter { it.amountCents > 0 }.sumOf { it.amountCents },
                expenseCents = inRange.filter { it.amountCents < 0 }.sumOf { -it.amountCents }
            )
        }

    fun accountWithBalance(accountId: Long): Flow<AccountWithBalance?> =
        accountsWithBalance().map { list -> list.find { it.account.id == accountId } }

    private fun balanceFor(
        accountId: Long,
        initialBalanceCents: Long,
        transactions: List<TransactionEntity>,
        transfers: List<TransferEntity>
    ): Long {
        val txSum = transactions.filter { it.accountId == accountId }.sumOf { it.amountCents }
        val transferIn = transfers.filter { it.toAccountId == accountId }.sumOf { it.amountCents }
        val transferOut = transfers.filter { it.fromAccountId == accountId }.sumOf { it.amountCents }
        return initialBalanceCents + txSum + transferIn - transferOut
    }

    fun movementsForAccount(accountId: Long): Flow<List<Movement>> =
        combine(
            transactionDao.getForAccount(accountId),
            transferDao.getForAccount(accountId),
            categoryDao.getAll(),
            accountDao.getAll()
        ) { transactions, transfers, categories, accounts ->
            buildMovements(transactions, transfers, categories, accounts)
        }

    fun allMovements(): Flow<List<Movement>> =
        combine(
            transactionDao.getAll(),
            transferDao.getAll(),
            categoryDao.getAll(),
            accountDao.getAll()
        ) { transactions, transfers, categories, accounts ->
            buildMovements(transactions, transfers, categories, accounts)
        }

    private fun buildMovements(
        transactions: List<TransactionEntity>,
        transfers: List<TransferEntity>,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>
    ): List<Movement> {
        val categoryById = categories.associateBy { it.id }
        val accountById = accounts.associateBy { it.id }
        val txMovements = transactions.map {
            Movement.TransactionMovement(it, categoryById[it.categoryId], accountById[it.accountId])
        }
        val transferMovements = transfers.map {
            Movement.TransferMovement(it, accountById[it.fromAccountId], accountById[it.toAccountId], categoryById[it.categoryId])
        }
        return (txMovements + transferMovements).sortedByDescending { it.date }
    }

    suspend fun addAccount(account: AccountEntity): Long = accountDao.insert(account)
    suspend fun updateAccount(account: AccountEntity) = accountDao.update(account)
    suspend fun deleteAccount(account: AccountEntity) = accountDao.delete(account)

    suspend fun addCategory(category: CategoryEntity): Long = categoryDao.insert(category)

    suspend fun addTransaction(transaction: TransactionEntity): Long = transactionDao.insert(transaction)
    suspend fun deleteTransaction(transaction: TransactionEntity) = transactionDao.delete(transaction)

    suspend fun addTransfer(transfer: TransferEntity): Long = transferDao.insert(transfer)
    suspend fun deleteTransfer(transfer: TransferEntity) = transferDao.delete(transfer)
}
