package com.takat.finanzas.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.takat.finanzas.data.AppDatabase
import com.takat.finanzas.data.attachment.AttachmentStorage
import com.takat.finanzas.data.csv.BackupCsv
import com.takat.finanzas.data.csv.BackupZip
import com.takat.finanzas.data.csv.ParsedBackup
import com.takat.finanzas.data.entity.AccountEntity
import com.takat.finanzas.data.entity.AppSettingsEntity
import com.takat.finanzas.data.entity.AttachmentEntity
import com.takat.finanzas.data.entity.AttachmentType
import com.takat.finanzas.data.entity.BudgetSettingsEntity
import com.takat.finanzas.data.entity.CategoryEntity
import com.takat.finanzas.data.entity.FixedExpenseEntity
import com.takat.finanzas.data.entity.FixedExpensePeriodStateEntity
import com.takat.finanzas.data.entity.TransactionEntity
import com.takat.finanzas.data.entity.TransferEntity
import com.takat.finanzas.data.model.AccountTotals
import com.takat.finanzas.data.model.AccountWithBalance
import com.takat.finanzas.data.model.CategoryExpense
import com.takat.finanzas.data.model.FixedExpensePaymentRecord
import com.takat.finanzas.data.model.FixedExpensePeriod
import com.takat.finanzas.data.model.ImportResult
import com.takat.finanzas.data.model.IncomeExpenseSummary
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.data.model.PendingFixedExpense
import com.takat.finanzas.data.model.ReminderStage
import com.takat.finanzas.util.DebugLog
import com.takat.finanzas.util.computeBudgetFreeze
import com.takat.finanzas.util.computeLiveBudget
import com.takat.finanzas.util.dayRange
import com.takat.finanzas.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class FinanceRepository(
    db: AppDatabase,
    private val attachmentStorage: AttachmentStorage,
    private val context: Context
) {

    private suspend fun refreshWidget() = WidgetUpdater.refresh(context)

    private val accountDao = db.accountDao()
    private val categoryDao = db.categoryDao()
    private val transactionDao = db.transactionDao()
    private val transferDao = db.transferDao()
    private val attachmentDao = db.attachmentDao()
    private val budgetSettingsDao = db.budgetSettingsDao()
    private val fixedExpenseDao = db.fixedExpenseDao()
    private val fixedExpensePeriodStateDao = db.fixedExpensePeriodStateDao()
    private val appSettingsDao = db.appSettingsDao()

    val categories: Flow<List<CategoryEntity>> = categoryDao.getAll()

    suspend fun updateCategory(category: CategoryEntity) = categoryDao.update(category)

    /**
     * Calendar date of the most recent income tagged with a "salary" category — the anchor that lets a
     * salary income jump the current quincena forward as soon as it's logged, instead of waiting for
     * the calendar to reach day 16/1. See [FixedExpensePeriod.currentPeriodKey]/[FixedExpensePeriod.hasPeriodStarted].
     * Derived from the transactions table itself (not separately stored), so editing or deleting the
     * salary transaction automatically updates or removes the anchor.
     */
    fun lastSalaryDate(zone: ZoneId = ZoneId.systemDefault()): Flow<LocalDate?> =
        combine(categoryDao.getAll(), transactionDao.getAll()) { categories, transactions ->
            val salaryCategoryIds = categories.filter { it.isSalary }.map { it.id }.toSet()
            transactions
                .filter { it.amountCents > 0 && it.categoryId in salaryCategoryIds }
                .maxByOrNull { it.date }
                ?.let { Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
        }

    fun budgetSettings(): Flow<BudgetSettingsEntity?> = budgetSettingsDao.get()

    suspend fun updateBudgetSettings(settings: BudgetSettingsEntity) =
        budgetSettingsDao.upsert(settings.copy(id = 0))

    fun appSettings(): Flow<AppSettingsEntity?> = appSettingsDao.get()

    suspend fun updateAppSettings(settings: AppSettingsEntity) =
        appSettingsDao.upsert(settings.copy(id = 0))

    fun accountsWithBalance(): Flow<List<AccountWithBalance>> =
        combine(accountDao.getAll(), transactionDao.getAll(), transferDao.getAll()) { accounts, transactions, transfers ->
            accounts.map { account -> AccountWithBalance(account, balanceFor(account.id, account.initialBalanceCents, transactions, transfers)) }
        }

    fun accountTotals(): Flow<AccountTotals> =
        combine(accountsWithBalance(), pendingFixedExpenses()) { list, pending ->
            val included = list.filter { it.account.includeInTotal }
            val capital = included.filter { !it.account.isDebt }.sumOf { it.balanceCents }
            val debtRaw = included.filter { it.account.isDebt }.sumOf { it.balanceCents }
            val pendingCents = pending.filter { it.isPending && it.countsTowardTotal }.sumOf { it.remainingCents }
            AccountTotals(
                availableCents = capital + debtRaw - pendingCents,
                capitalCents = capital,
                debtCents = -debtRaw,
                pendingFixedExpensesCents = pendingCents
            )
        }

    /**
     * Excludes fixed-expense payments (transactions tagged with [TransactionEntity.fixedExpenseId]):
     * that money was already reserved out of the daily-budget balance while the expense was pending
     * ([accountTotals]'s [AccountTotals.pendingFixedExpensesCents]), so paying it shouldn't also count
     * against today's discretionary spending.
     */
    fun spentTodayCents(zone: ZoneId = ZoneId.systemDefault()): Flow<Long> =
        combine(transactionDao.getAll(), accountDao.getAll()) { transactions, accounts ->
            val (start, end) = dayRange(LocalDate.now(zone), zone)
            val includedAccountIds = accounts.filter { it.includeInTotal }.map { it.id }.toSet()
            transactions
                .filter {
                    it.accountId in includedAccountIds && it.amountCents < 0 &&
                        it.fixedExpenseId == null && it.date >= start && it.date < end
                }
                .sumOf { -it.amountCents }
        }

    /**
     * Lazily rolls the static "presupuesto diario" over to [today] if it's stale, freezing whatever live
     * value was last observed on a prior day. No-op when the daily budget feature is disabled or has never
     * been configured. Called both at app startup and reactively from [DailyBudgetViewModel] so the freeze
     * applies as soon as possible without needing an exact-midnight background job.
     */
    suspend fun ensureDailyBudgetFrozen(today: LocalDate = LocalDate.now(ZoneId.systemDefault())) {
        val settings = budgetSettingsDao.get().first() ?: return
        if (!settings.enabled) return
        val totals = accountTotals().first()
        val live = computeLiveBudget(settings, totals, today, lastSalaryDate().first())
        val freeze = computeBudgetFreeze(settings, live.liveValueCents, today)
        DebugLog.log(
            "ensureDailyBudgetFrozen: today=$today changed=${freeze.changed} " +
                "frozenBudgetCents=${freeze.frozenBudgetCents} frozenBudgetEpochDay=${freeze.frozenBudgetEpochDay}"
        )
        if (freeze.changed) {
            updateBudgetSettings(
                settings.copy(
                    lastLiveValueCents = freeze.lastLiveValueCents,
                    lastLiveValueEpochDay = freeze.lastLiveValueEpochDay,
                    frozenBudgetCents = freeze.frozenBudgetCents,
                    frozenBudgetEpochDay = freeze.frozenBudgetEpochDay
                )
            )
        }
    }

    /** Every enabled fixed expense resolved against its current period (stored exception, or the implicit active/unpaid default). */
    fun pendingFixedExpenses(): Flow<List<PendingFixedExpense>> =
        combine(
            fixedExpenseDao.getAll(),
            fixedExpensePeriodStateDao.getAll(),
            accountDao.getAll(),
            transactionDao.getAll(),
            lastSalaryDate()
        ) { rules, states, accounts, transactions, lastSalaryDate ->
            val accountById = accounts.associateBy { it.id }
            val stateByKey = states.associateBy { it.fixedExpenseId to it.periodKey }
            val paymentsByKey = transactions
                .filter { it.fixedExpenseId != null && it.fixedExpensePeriodKey != null }
                .groupBy { it.fixedExpenseId!! to it.fixedExpensePeriodKey!! }
            rules.filter { it.enabled }.map { rule ->
                val periodKey = FixedExpensePeriod.currentPeriodKey(rule.frequency, lastSalaryDate = lastSalaryDate)
                val state = stateByKey[rule.id to periodKey]
                val payments = paymentsByKey[rule.id to periodKey].orEmpty()
                PendingFixedExpense(
                    fixedExpense = rule,
                    periodKey = periodKey,
                    active = state?.active ?: true,
                    started = FixedExpensePeriod.hasPeriodStarted(rule.frequency, rule.dayOfMonth, rule.quincenaOnly, lastSalaryDate = lastSalaryDate),
                    paidCents = payments.sumOf { -it.amountCents },
                    lastPaymentTransactionId = payments.maxByOrNull { it.date }?.id,
                    countsTowardTotal = accountById[rule.accountId]?.includeInTotal ?: false
                )
            }
        }

    /** Remaining unpaid amount for an arbitrary (possibly past) period, e.g. for a follow-up reminder. */
    suspend fun remainingCentsForPeriod(rule: FixedExpenseEntity, periodKey: String): Long {
        val active = fixedExpensePeriodStateDao.find(rule.id, periodKey)?.active ?: true
        if (!active) return 0
        val paidCents = transactionDao.getAll().first()
            .filter { it.fixedExpenseId == rule.id && it.fixedExpensePeriodKey == periodKey }
            .sumOf { -it.amountCents }
        return (rule.amountCents - paidCents).coerceAtLeast(0)
    }

    fun fixedExpenses(): Flow<List<FixedExpenseEntity>> = fixedExpenseDao.getAll()

    suspend fun addFixedExpense(entity: FixedExpenseEntity): Long = fixedExpenseDao.insert(entity)
    suspend fun updateFixedExpense(entity: FixedExpenseEntity) = fixedExpenseDao.update(entity)
    suspend fun deleteFixedExpense(entity: FixedExpenseEntity) = fixedExpenseDao.delete(entity)

    suspend fun setFixedExpensePeriodActive(fixedExpenseId: Long, periodKey: String, active: Boolean) {
        upsertPeriodState(fixedExpenseId, periodKey) { it.copy(active = active) }
    }

    suspend fun markFixedExpenseNotified(fixedExpenseId: Long, periodKey: String, stage: ReminderStage, notifiedAt: Long) {
        upsertPeriodState(fixedExpenseId, periodKey) {
            when (stage) {
                ReminderStage.PRE_DUE -> it.copy(preNotifiedAt = notifiedAt)
                ReminderStage.DUE -> it.copy(dueNotifiedAt = notifiedAt)
                ReminderStage.FOLLOW_UP -> it.copy(followUpNotifiedAt = notifiedAt)
            }
        }
    }

    suspend fun wasFixedExpenseNotified(fixedExpenseId: Long, periodKey: String, stage: ReminderStage): Boolean {
        val state = fixedExpensePeriodStateDao.find(fixedExpenseId, periodKey) ?: return false
        return when (stage) {
            ReminderStage.PRE_DUE -> state.preNotifiedAt != null
            ReminderStage.DUE -> state.dueNotifiedAt != null
            ReminderStage.FOLLOW_UP -> state.followUpNotifiedAt != null
        }
    }

    private suspend fun upsertPeriodState(
        fixedExpenseId: Long,
        periodKey: String,
        mutate: (FixedExpensePeriodStateEntity) -> FixedExpensePeriodStateEntity
    ) {
        val existing = fixedExpensePeriodStateDao.find(fixedExpenseId, periodKey)
        val base = existing ?: FixedExpensePeriodStateEntity(fixedExpenseId = fixedExpenseId, periodKey = periodKey)
        val updated = mutate(base)
        if (existing == null) fixedExpensePeriodStateDao.insert(updated) else fixedExpensePeriodStateDao.update(updated)
    }

    /** Periods with at least one payment toward them, across all fixed expenses, newest first, for the "Historial" report. */
    fun paidHistory(): Flow<List<FixedExpensePaymentRecord>> =
        combine(transactionDao.getAll(), fixedExpenseDao.getAll()) { transactions, rules ->
            val ruleById = rules.associateBy { it.id }
            transactions
                .filter { it.fixedExpenseId != null && it.fixedExpensePeriodKey != null }
                .groupBy { it.fixedExpenseId!! to it.fixedExpensePeriodKey!! }
                .mapNotNull { (key, payments) ->
                    val rule = ruleById[key.first] ?: return@mapNotNull null
                    val lastPayment = payments.maxByOrNull { it.date } ?: return@mapNotNull null
                    FixedExpensePaymentRecord(rule, key.second, payments.sumOf { -it.amountCents }, lastPayment)
                }
                .sortedByDescending { it.lastPayment.date }
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
        combine(
            transactionDao.getAll(),
            categoryDao.getAll(),
            accountDao.getAll(),
            attachmentDao.getAll()
        ) { transactions, categories, accounts, attachments ->
            val categoryById = categories.associateBy { it.id }
            val accountById = accounts.associateBy { it.id }
            val attachmentsByTransaction = attachments.groupBy { it.transactionId }
            transactions
                .filter { it.amountCents < 0 && it.date >= fromInclusive && it.date < toExclusive && it.categoryId == categoryId }
                .sortedByDescending { it.date }
                .map {
                    Movement.TransactionMovement(
                        it,
                        categoryById[it.categoryId],
                        accountById[it.accountId],
                        attachmentsByTransaction[it.id].orEmpty()
                    )
                }
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
            accountDao.getAll(),
            attachmentDao.getAll()
        ) { transactions, transfers, categories, accounts, attachments ->
            buildMovements(transactions, transfers, categories, accounts, attachments)
        }

    fun allMovements(): Flow<List<Movement>> =
        combine(
            transactionDao.getAll(),
            transferDao.getAll(),
            categoryDao.getAll(),
            accountDao.getAll(),
            attachmentDao.getAll()
        ) { transactions, transfers, categories, accounts, attachments ->
            buildMovements(transactions, transfers, categories, accounts, attachments)
        }

    private fun buildMovements(
        transactions: List<TransactionEntity>,
        transfers: List<TransferEntity>,
        categories: List<CategoryEntity>,
        accounts: List<AccountEntity>,
        attachments: List<AttachmentEntity>
    ): List<Movement> {
        val categoryById = categories.associateBy { it.id }
        val accountById = accounts.associateBy { it.id }
        val attachmentsByTransaction = attachments.groupBy { it.transactionId }
        val txMovements = transactions.map {
            Movement.TransactionMovement(
                it,
                categoryById[it.categoryId],
                accountById[it.accountId],
                attachmentsByTransaction[it.id].orEmpty()
            )
        }
        val transferMovements = transfers.map {
            Movement.TransferMovement(it, accountById[it.fromAccountId], accountById[it.toAccountId], categoryById[it.categoryId])
        }
        return (txMovements + transferMovements).sortedByDescending { it.date }
    }

    suspend fun addAccount(account: AccountEntity): Long = accountDao.insert(account).also { refreshWidget() }
    suspend fun updateAccount(account: AccountEntity) {
        accountDao.update(account)
        refreshWidget()
    }
    suspend fun deleteAccount(account: AccountEntity) {
        accountDao.delete(account)
        refreshWidget()
    }

    suspend fun addCategory(category: CategoryEntity): Long = categoryDao.insert(category)

    suspend fun addTransaction(transaction: TransactionEntity): Long {
        val id = transactionDao.insert(transaction)
        if (isSalaryIncome(transaction)) unfreezeDailyBudget()
        refreshWidget()
        return id
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        attachmentDao.getForTransactionOnce(transaction.id).forEach { attachmentStorage.deleteFiles(attachmentDao, it) }
        transactionDao.delete(transaction)
        if (isSalaryIncome(transaction)) unfreezeDailyBudget()
        refreshWidget()
    }

    private suspend fun isSalaryIncome(transaction: TransactionEntity): Boolean {
        if (transaction.amountCents <= 0 || transaction.categoryId == null) return false
        return categoryDao.getAll().first().any { it.id == transaction.categoryId && it.isSalary }
    }

    /**
     * Forces the next [ensureDailyBudgetFrozen] call to re-freeze "presupuesto diario" from the current
     * live value instead of keeping today's already-frozen number. Needed because a salary income can
     * jump [lastSalaryDate]'s quincena forward mid-day (see [FixedExpensePeriod.currentPeriodKey]), and
     * the freeze otherwise only rolls over once per calendar day — without this, both the in-app "Disponible
     * hoy" and the home screen widget would keep showing the pre-salary number for the rest of the day.
     */
    private suspend fun unfreezeDailyBudget() {
        val settings = budgetSettingsDao.get().first() ?: return
        if (settings.frozenBudgetEpochDay != 0L) {
            updateBudgetSettings(settings.copy(frozenBudgetEpochDay = 0))
        }
    }

    fun attachmentsForTransaction(transactionId: Long): Flow<List<AttachmentEntity>> =
        attachmentDao.getForTransaction(transactionId)

    suspend fun addImageAttachment(transactionId: Long, bytes: ByteArray): AttachmentEntity =
        attachmentStorage.saveImage(attachmentDao, transactionId, bytes)

    suspend fun addDocumentAttachment(transactionId: Long, type: AttachmentType, bytes: ByteArray): AttachmentEntity =
        attachmentStorage.saveDocument(attachmentDao, transactionId, type, bytes)

    suspend fun readAttachment(attachment: AttachmentEntity): ByteArray =
        attachmentStorage.readDecrypted(attachment)

    fun createCaptureFile() = attachmentStorage.createCaptureFile()

    fun writeAttachmentForExternalView(bytes: ByteArray, suffix: String) =
        attachmentStorage.writeTempForView(bytes, suffix)

    fun readFromUri(resolver: ContentResolver, uri: Uri): ByteArray =
        attachmentStorage.readFromUri(resolver, uri)

    suspend fun addTransfer(transfer: TransferEntity): Long = transferDao.insert(transfer).also { refreshWidget() }
    suspend fun deleteTransfer(transfer: TransferEntity) {
        transferDao.delete(transfer)
        refreshWidget()
    }

    /** Writes a single .zip backup: backup.csv plus every transaction's receipts, decrypted, under adjuntos/. */
    suspend fun exportBackup(output: OutputStream) {
        val accounts = accountDao.getAll().first()
        val categories = categoryDao.getAll().first()
        val transactions = transactionDao.getAll().first()
        val transfers = transferDao.getAll().first()
        val attachmentsByTransaction = attachmentDao.getAll().first().groupBy { it.transactionId }

        val files = mutableMapOf<String, ByteArray>()
        val entryNamesByTransaction = mutableMapOf<Long, List<String>>()
        transactions.forEach { tx ->
            val entries = attachmentsByTransaction[tx.id].orEmpty().mapIndexed { index, attachment ->
                val entryName = "tx${tx.id}_$index.${extensionFor(attachment.type)}"
                files[entryName] = attachmentStorage.readDecrypted(attachment)
                entryName
            }
            if (entries.isNotEmpty()) entryNamesByTransaction[tx.id] = entries
        }

        val csv = BackupCsv.encode(accounts, categories, transactions, transfers, entryNamesByTransaction)
        BackupZip.write(output, csv, files)
    }

    suspend fun commitImport(parsed: ParsedBackup, attachmentFiles: Map<String, ByteArray> = emptyMap()): ImportResult {
        val accountIdByName = accountDao.getAll().first().associateTo(mutableMapOf()) { it.name to it.id }
        val categoryIdByName = categoryDao.getAll().first().associateTo(mutableMapOf()) { it.name to it.id }

        var accountsAdded = 0
        var categoriesAdded = 0
        var skipped = 0

        parsed.accounts.forEach { row ->
            if (!accountIdByName.containsKey(row.name)) {
                val id = accountDao.insert(
                    AccountEntity(
                        name = row.name,
                        initialBalanceCents = row.initialBalanceCents,
                        colorArgb = row.colorArgb,
                        isDebt = row.isDebt,
                        includeInTotal = row.includeInTotal
                    )
                )
                accountIdByName[row.name] = id
                accountsAdded++
            }
        }

        parsed.categories.forEach { row ->
            if (!categoryIdByName.containsKey(row.name)) {
                val id = categoryDao.insert(CategoryEntity(name = row.name, emoji = row.emoji, kind = row.kind))
                categoryIdByName[row.name] = id
                categoriesAdded++
            }
        }

        var transactionsAdded = 0
        var attachmentsAdded = 0
        parsed.transactions.forEach { row ->
            val accountId = accountIdByName[row.accountName]
            if (accountId == null) {
                skipped++
                return@forEach
            }
            val transactionId = transactionDao.insert(
                TransactionEntity(
                    accountId = accountId,
                    categoryId = row.categoryName?.let { categoryIdByName[it] },
                    amountCents = row.amountCents,
                    note = row.note,
                    date = row.date
                )
            )
            transactionsAdded++

            row.attachmentEntries.forEach { entryName ->
                val bytes = attachmentFiles[entryName] ?: return@forEach
                val type = typeForEntryName(entryName)
                if (type == AttachmentType.IMAGE) {
                    attachmentStorage.saveImage(attachmentDao, transactionId, bytes)
                } else {
                    attachmentStorage.saveDocument(attachmentDao, transactionId, type, bytes)
                }
                attachmentsAdded++
            }
        }

        var transfersAdded = 0
        parsed.transfers.forEach { row ->
            val fromId = accountIdByName[row.fromAccountName]
            val toId = accountIdByName[row.toAccountName]
            if (fromId == null || toId == null) {
                skipped++
                return@forEach
            }
            transferDao.insert(
                TransferEntity(
                    fromAccountId = fromId,
                    toAccountId = toId,
                    categoryId = row.categoryName?.let { categoryIdByName[it] },
                    amountCents = row.amountCents,
                    note = row.note,
                    date = row.date
                )
            )
            transfersAdded++
        }

        refreshWidget()
        return ImportResult(accountsAdded, categoriesAdded, transactionsAdded, transfersAdded, skipped, attachmentsAdded)
    }

    private fun extensionFor(type: AttachmentType): String = when (type) {
        AttachmentType.IMAGE -> "jpg"
        AttachmentType.JSON -> "json"
        AttachmentType.PDF -> "pdf"
    }

    private fun typeForEntryName(name: String): AttachmentType = when (name.substringAfterLast('.', "").lowercase()) {
        "json" -> AttachmentType.JSON
        "pdf" -> AttachmentType.PDF
        else -> AttachmentType.IMAGE
    }
}
