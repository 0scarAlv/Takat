package com.takat.finanzas.data.dao

import androidx.room.Dao
import androidx.room.Query

/**
 * Bulk deletes for "Reemplazar todo" restores. Only ever called inside a Room transaction
 * (see FinanceRepository.commitImport), so a failed restore rolls back instead of leaving an empty app.
 * Order matters for the foreign keys: children first.
 */
@Dao
interface WipeDao {
    @Query("DELETE FROM attachments")
    suspend fun deleteAttachments()

    @Query("DELETE FROM transactions")
    suspend fun deleteTransactions()

    @Query("DELETE FROM transfers")
    suspend fun deleteTransfers()

    @Query("DELETE FROM fixed_expense_period_state")
    suspend fun deleteFixedExpensePeriodStates()

    @Query("DELETE FROM fixed_expenses")
    suspend fun deleteFixedExpenses()

    @Query("DELETE FROM accounts")
    suspend fun deleteAccounts()

    @Query("DELETE FROM categories")
    suspend fun deleteCategories()
}
