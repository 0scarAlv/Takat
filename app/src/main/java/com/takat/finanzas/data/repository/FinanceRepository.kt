package com.takat.finanzas.data.repository

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.takat.finanzas.data.AppDatabase
import com.takat.finanzas.data.attachment.AttachmentStorage
import com.takat.finanzas.data.csv.BackupCsv
import com.takat.finanzas.data.csv.BackupZip
import com.takat.finanzas.data.csv.ImportMode
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
import com.takat.finanzas.data.model.DailyExpense
import com.takat.finanzas.data.model.FixedExpensePaymentRecord
import com.takat.finanzas.data.model.FixedExpensePeriod
import com.takat.finanzas.data.model.ImportResult
import com.takat.finanzas.data.model.IncomeExpenseSummary
import com.takat.finanzas.data.model.MonthExpense
import com.takat.finanzas.data.model.Movement
import com.takat.finanzas.data.model.PendingFixedExpense
import com.takat.finanzas.data.model.ReminderStage
import com.takat.finanzas.util.DebugLog
import com.takat.finanzas.util.computeBudgetFreeze
import com.takat.finanzas.util.computeLiveBudget
import com.takat.finanzas.util.dayRange
import com.takat.finanzas.util.monthRange
import com.takat.finanzas.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.OutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

class FinanceRepository(
    private val db: AppDatabase,
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

    fun accountsWithBalance(includeArchived: Boolean = false): Flow<List<AccountWithBalance>> =
        combine(accountDao.getAll(), transactionDao.getAll(), transferDao.getAll(), fixedExpenseDao.getAll()) { accounts, transactions, transfers, rules ->
            val financedByAccount = financedByAccount(rules, transactions)
            accounts
                .filter { includeArchived || !it.isArchived }
                .map { account ->
                    AccountWithBalance(
                        account,
                        balanceFor(account.id, account.initialBalanceCents, transactions, transfers),
                        financedByAccount[account.id] ?: 0
                    )
                }
        }

    /**
     * Principal still owed per credit account from "compra a crédito" plans. Derived rather than stored as a
     * purchase transaction so it never counts as spending (reports, "gastado hoy"), and deleting the plan
     * removes the debt with it. Disabled plans still count: pausing reminders doesn't cancel what's owed.
     */
    private fun financedByAccount(rules: List<FixedExpenseEntity>, transactions: List<TransactionEntity>): Map<Long, Long> {
        val creditRules = rules.filter { it.isCreditPurchase }
        if (creditRules.isEmpty()) return emptyMap()
        val totalPaidByRule = transactions
            .filter { it.fixedExpenseId != null }
            .groupBy { it.fixedExpenseId!! }
            .mapValues { (_, txs) -> txs.sumOf { -it.amountCents } }
        return creditRules
            .groupBy { it.creditAccountId!! }
            .mapValues { (_, plans) -> plans.sumOf { it.financedRemainingCents(totalPaidByRule[it.id] ?: 0) } }
    }

    fun accountTotals(): Flow<AccountTotals> =
        combine(accountsWithBalance(), pendingFixedExpenses(), appSettingsDao.get()) { list, pending, settings ->
            val included = list.filter { it.account.includeInTotal }
            val capital = included.filter { !it.account.isDebt }.sumOf { it.balanceCents }
            val debtRaw = included.filter { it.account.isDebt }.sumOf { it.balanceCents }
            // Financed principal is shown apart ("Deuda de crédito"): by default only each period's cuota — already in
            // pendingCents or paid — reduces what's available, unless the user opts into subtracting all of it.
            val financed = included.sumOf { it.financedCents }
            val includeFinanced = settings?.includeFinancedInAvailable ?: false
            val pendingCents = pending.filter { it.isPending && it.countsTowardTotal }.sumOf { it.remainingCents }
            AccountTotals(
                availableCents = capital + debtRaw - pendingCents - (if (includeFinanced) financed else 0),
                capitalCents = capital,
                debtCents = -debtRaw,
                pendingFixedExpensesCents = pendingCents,
                financedCents = financed,
                financedIncludedInAvailable = includeFinanced
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
            val includedAccountIds = accounts.filter { it.includeInTotal && !it.isArchived }.map { it.id }.toSet()
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
            val totalPaidByRule = transactions
                .filter { it.fixedExpenseId != null }
                .groupBy { it.fixedExpenseId!! }
                .mapValues { (_, txs) -> txs.sumOf { -it.amountCents } }
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
                    totalPaidCents = totalPaidByRule[rule.id] ?: 0,
                    lastPaymentTransactionId = payments.maxByOrNull { it.date }?.id,
                    countsTowardTotal = accountById[rule.accountId]?.includeInTotal ?: false
                )
            }
        }

    /** Remaining unpaid amount for an arbitrary (possibly past) period, e.g. for a follow-up reminder. */
    suspend fun remainingCentsForPeriod(rule: FixedExpenseEntity, periodKey: String): Long {
        val active = fixedExpensePeriodStateDao.find(rule.id, periodKey)?.active ?: true
        if (!active) return 0
        val allForRule = transactionDao.getAll().first().filter { it.fixedExpenseId == rule.id }
        val paidCents = allForRule.filter { it.fixedExpensePeriodKey == periodKey }.sumOf { -it.amountCents }
        val periodRemaining = (rule.amountCents - paidCents).coerceAtLeast(0)
        val totalDebtCents = rule.totalDebtCents ?: return periodRemaining
        val debtRemaining = (totalDebtCents - allForRule.sumOf { -it.amountCents }).coerceAtLeast(0)
        return minOf(periodRemaining, debtRemaining)
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

    /** One entry per calendar month in the [monthsBack]-month window ending at [endMonth] (including zero-expense months), for the "últimos meses" trend chart. */
    fun expensesByMonthRange(endMonth: YearMonth, monthsBack: Int, zone: ZoneId = ZoneId.systemDefault()): Flow<List<MonthExpense>> =
        transactionDao.getAll().map { transactions ->
            val startMonth = endMonth.minusMonths((monthsBack - 1).toLong())
            val (start, _) = monthRange(startMonth, zone)
            val (_, end) = monthRange(endMonth, zone)
            val totalsByMonth = transactions
                .filter { it.amountCents < 0 && it.date >= start && it.date < end }
                .groupBy { YearMonth.from(Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate()) }
                .mapValues { (_, txs) -> txs.sumOf { -it.amountCents } }
            (0 until monthsBack).map { offset ->
                val month = startMonth.plusMonths(offset.toLong())
                MonthExpense(month, totalsByMonth[month] ?: 0L)
            }
        }

    /** One entry per calendar day of [month] (including zero-expense days), for the "día que más gastaste" bar chart. */
    fun expensesByDay(month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): Flow<List<DailyExpense>> =
        transactionDao.getAll().map { transactions ->
            val (start, end) = monthRange(month, zone)
            val totalsByDate = transactions
                .filter { it.amountCents < 0 && it.date >= start && it.date < end }
                .groupBy { Instant.ofEpochMilli(it.date).atZone(zone).toLocalDate() }
                .mapValues { (_, txs) -> txs.sumOf { -it.amountCents } }
            (1..month.lengthOfMonth()).map { day ->
                val date = month.atDay(day)
                DailyExpense(date, totalsByDate[date] ?: 0L)
            }
        }

    /** Every expense on [date], regardless of category, for the day-detail screen off the "gasto por día" chart. */
    fun expenseTransactionsForDay(date: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Flow<List<Movement.TransactionMovement>> =
        combine(
            transactionDao.getAll(),
            categoryDao.getAll(),
            accountDao.getAll(),
            attachmentDao.getAll()
        ) { transactions, categories, accounts, attachments ->
            val (start, end) = dayRange(date, zone)
            val categoryById = categories.associateBy { it.id }
            val accountById = accounts.associateBy { it.id }
            val attachmentsByTransaction = attachments.groupBy { it.transactionId }
            transactions
                .filter { it.amountCents < 0 && it.date >= start && it.date < end }
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

    /**
     * Same "day <=15 funds this Q2, day >15 funds next Q1" logic as
     * [com.takat.finanzas.data.model.FixedExpensePeriod], applied to the Ingresos vs Gastos report: a
     * salary logged in the back half of a month (e.g. paid on the 31st) isn't really that month's
     * income — it's what covers the first half of next month — so it's reattributed there instead.
     * Only salary income shifts; regular income and all expenses stay on their literal calendar date.
     */
    fun incomeExpenseSummary(month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): Flow<IncomeExpenseSummary> =
        combine(transactionDao.getAll(), categoryDao.getAll()) { transactions, categories ->
            val salaryCategoryIds = categories.filter { it.isSalary }.map { it.id }.toSet()
            fun isLateSalary(t: TransactionEntity): Boolean {
                if (t.amountCents <= 0 || t.categoryId !in salaryCategoryIds) return false
                return Instant.ofEpochMilli(t.date).atZone(zone).toLocalDate().dayOfMonth > 15
            }

            val (start, end) = monthRange(month, zone)
            val (prevStart, _) = monthRange(month.minusMonths(1), zone)
            val thisMonth = transactions.filter { it.date >= start && it.date < end }
            val reattributedFromPrevMonth = transactions.filter { it.date >= prevStart && it.date < start && isLateSalary(it) }

            IncomeExpenseSummary(
                incomeCents = thisMonth.filter { it.amountCents > 0 && !isLateSalary(it) }.sumOf { it.amountCents } +
                    reattributedFromPrevMonth.sumOf { it.amountCents },
                expenseCents = thisMonth.filter { it.amountCents < 0 }.sumOf { -it.amountCents }
            )
        }

    fun accountWithBalance(accountId: Long): Flow<AccountWithBalance?> =
        accountsWithBalance(includeArchived = true).map { list -> list.find { it.account.id == accountId } }

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

    suspend fun movementCountForAccount(accountId: Long): Int =
        transactionDao.countForAccount(accountId) + transferDao.countForAccount(accountId)

    /**
     * Hides the account from pickers/totals for good (no way back from the UI) while keeping its
     * transactions/transfers pointing at a valid accountId, so their history stays intact.
     */
    suspend fun archiveAccount(account: AccountEntity) {
        accountDao.update(account.copy(isArchived = true))
        refreshWidget()
    }

    suspend fun addCategory(category: CategoryEntity): Long = categoryDao.insert(category)

    suspend fun addTransaction(transaction: TransactionEntity): Long {
        val id = transactionDao.insert(transaction)
        if (isSalaryIncome(transaction)) unfreezeDailyBudget()
        refreshWidget()
        return id
    }

    suspend fun transactionById(id: Long): TransactionEntity? = transactionDao.getById(id)

    suspend fun updateTransaction(transaction: TransactionEntity) {
        val previous = transactionDao.getById(transaction.id)
        transactionDao.update(transaction)
        if ((previous != null && isSalaryIncome(previous)) || isSalaryIncome(transaction)) unfreezeDailyBudget()
        refreshWidget()
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

    /** Plain backup.csv, no attachments — for the PC-access panel's "Exportar CSV" (opening in Excel etc). */
    suspend fun exportCsv(): String {
        val accounts = accountDao.getAll().first()
        val categories = categoryDao.getAll().first()
        val transactions = transactionDao.getAll().first()
        val transfers = transferDao.getAll().first()
        val fixedExpenses = fixedExpenseDao.getAll().first()
        val fixedExpensePeriodStates = fixedExpensePeriodStateDao.getAll().first()
        return BackupCsv.encode(accounts, categories, transactions, transfers, emptyMap(), fixedExpenses, fixedExpensePeriodStates)
    }

    /** Writes a single .zip backup: backup.csv plus every transaction's receipts, decrypted, under adjuntos/. */
    suspend fun exportBackup(output: OutputStream) {
        val accounts = accountDao.getAll().first()
        val categories = categoryDao.getAll().first()
        val transactions = transactionDao.getAll().first()
        val transfers = transferDao.getAll().first()
        val fixedExpenses = fixedExpenseDao.getAll().first()
        val fixedExpensePeriodStates = fixedExpensePeriodStateDao.getAll().first()
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

        val csv = BackupCsv.encode(
            accounts, categories, transactions, transfers, entryNamesByTransaction,
            fixedExpenses, fixedExpensePeriodStates
        )
        BackupZip.write(output, csv, files)
    }

    /**
     * Restores [parsed] according to [mode] (see [ImportMode]). Runs in a single Room transaction, so a
     * restore that fails halfway — including a REPLACE_ALL that already wiped everything — rolls back to
     * exactly what was there before instead of leaving the app half-empty.
     *
     * Names are matched trimmed and case-insensitively, and the backup's own duplicates collapse into one
     * (a backup taken after a previous double import won't recreate "Comida" twice).
     */
    suspend fun commitImport(
        parsed: ParsedBackup,
        attachmentFiles: Map<String, ByteArray> = emptyMap(),
        mode: ImportMode = ImportMode.ONLY_MISSING
    ): ImportResult {
        // Snapshot read up front, outside the transaction (Flow queries aren't meant to run inside one).
        val replace = mode == ImportMode.REPLACE_ALL
        val existing = ExistingData(
            accounts = if (replace) emptyList() else accountDao.getAll().first(),
            categories = categoryDao.getAll().first(),
            fixedExpenses = if (replace) emptyList() else fixedExpenseDao.getAll().first(),
            transactions = if (mode == ImportMode.ONLY_MISSING) transactionDao.getAll().first() else emptyList(),
            transfers = if (mode == ImportMode.ONLY_MISSING) transferDao.getAll().first() else emptyList()
        )
        val replacedAttachments = if (replace) attachmentDao.getAll().first() else emptyList()
        val result = db.withTransaction {
            if (replace) {
                val wipe = db.wipeDao()
                wipe.deleteAttachments()
                wipe.deleteTransactions()
                wipe.deleteTransfers()
                wipe.deleteFixedExpensePeriodStates()
                wipe.deleteFixedExpenses()
                wipe.deleteAccounts()
                wipe.deleteCategories()
            }
            importRows(parsed, attachmentFiles, existing, replace)
        }
        // Old receipt files only go once the new data is safely committed.
        replacedAttachments.forEach { attachmentStorage.deleteFiles(attachmentDao, it) }
        unfreezeDailyBudget()
        refreshWidget()
        return result
    }

    private suspend fun importRows(
        parsed: ParsedBackup,
        attachmentFiles: Map<String, ByteArray>,
        existing: ExistingData,
        replace: Boolean
    ): ImportResult {
        fun key(name: String) = name.trim().lowercase()

        val accountIdByName = existing.accounts.associateTo(mutableMapOf()) { key(it.name) to it.id }
        val categoryIdByName =
            if (replace) mutableMapOf() else existing.categories.associateTo(mutableMapOf()) { key(it.name) to it.id }
        // The backup format doesn't carry these flags, so a REPLACE_ALL keeps them from the categories it replaced.
        val previousCategoryByName = existing.categories.associateBy { key(it.name) }

        var accountsAdded = 0
        var categoriesAdded = 0
        var skipped = 0
        var duplicatesSkipped = 0

        parsed.accounts.forEach { row ->
            if (accountIdByName.containsKey(key(row.name))) return@forEach
            val id = accountDao.insert(
                AccountEntity(
                    name = row.name.trim(),
                    initialBalanceCents = row.initialBalanceCents,
                    colorArgb = row.colorArgb,
                    isDebt = row.isDebt,
                    includeInTotal = row.includeInTotal
                )
            )
            accountIdByName[key(row.name)] = id
            accountsAdded++
        }

        parsed.categories.forEach { row ->
            if (categoryIdByName.containsKey(key(row.name))) return@forEach
            val previous = previousCategoryByName[key(row.name)]
            val id = categoryDao.insert(
                CategoryEntity(
                    name = row.name.trim(),
                    emoji = row.emoji,
                    kind = row.kind,
                    isDefault = previous?.isDefault ?: false,
                    isSalary = previous?.isSalary ?: false
                )
            )
            categoryIdByName[key(row.name)] = id
            categoriesAdded++
        }

        // Gastos fijos go before transactions so payments can be re-linked to them.
        val fixedExpenseIdByName = existing.fixedExpenses.associateTo(mutableMapOf()) { key(it.name) to it.id }
        var fixedExpensesAdded = 0
        parsed.fixedExpenses.forEach { row ->
            if (fixedExpenseIdByName.containsKey(key(row.name))) return@forEach
            val accountId = accountIdByName[key(row.accountName)]
            if (accountId == null) {
                skipped++
                return@forEach
            }
            val id = fixedExpenseDao.insert(
                FixedExpenseEntity(
                    name = row.name.trim(),
                    amountCents = row.amountCents,
                    accountId = accountId,
                    categoryId = row.categoryName?.let { categoryIdByName[key(it)] },
                    frequency = row.frequency,
                    dayOfMonth = row.dayOfMonth,
                    quincenaOnly = row.quincenaOnly,
                    notifyEnabled = row.notifyEnabled,
                    enabled = row.enabled,
                    totalDebtCents = row.totalDebtCents,
                    installmentsCount = row.installmentsCount,
                    creditAccountId = row.creditAccountName?.let { accountIdByName[key(it)] },
                    financedPrincipalCents = row.financedPrincipalCents,
                    interestRateBps = row.interestRateBps
                )
            )
            fixedExpenseIdByName[key(row.name)] = id
            fixedExpensesAdded++
        }

        parsed.fixedExpensePeriodStates.forEach { row ->
            val fixedExpenseId = fixedExpenseIdByName[key(row.fixedExpenseName)]
            if (fixedExpenseId == null) {
                skipped++
                return@forEach
            }
            if (fixedExpensePeriodStateDao.find(fixedExpenseId, row.periodKey) == null) {
                fixedExpensePeriodStateDao.insert(
                    FixedExpensePeriodStateEntity(
                        fixedExpenseId = fixedExpenseId,
                        periodKey = row.periodKey,
                        active = row.active
                    )
                )
            }
        }

        // Existing movements as a multiset, so a backup holding two identical real movements (two coffees
        // of the same price on the same minute) still restores both when the app has neither.
        val existingTransactionKeys = mutableMapOf<List<Any?>, Int>()
        existing.transactions.forEach {
            existingTransactionKeys.merge(listOf(it.accountId, it.date, it.amountCents, it.categoryId, it.note), 1, Int::plus)
        }
        val existingTransferKeys = mutableMapOf<List<Any?>, Int>()
        existing.transfers.forEach {
            existingTransferKeys.merge(listOf(it.fromAccountId, it.toAccountId, it.date, it.amountCents, it.note), 1, Int::plus)
        }
        fun MutableMap<List<Any?>, Int>.consume(key: List<Any?>): Boolean {
            val count = this[key] ?: return false
            if (count <= 1) remove(key) else this[key] = count - 1
            return true
        }

        var transactionsAdded = 0
        var attachmentsAdded = 0
        parsed.transactions.forEach { row ->
            val accountId = accountIdByName[key(row.accountName)]
            if (accountId == null) {
                skipped++
                return@forEach
            }
            val categoryId = row.categoryName?.let { categoryIdByName[key(it)] }
            if (existingTransactionKeys.consume(listOf(accountId, row.date, row.amountCents, categoryId, row.note))) {
                duplicatesSkipped++
                return@forEach
            }
            val fixedExpenseId = row.fixedExpenseName?.let { fixedExpenseIdByName[key(it)] }
            val transactionId = transactionDao.insert(
                TransactionEntity(
                    accountId = accountId,
                    categoryId = categoryId,
                    amountCents = row.amountCents,
                    note = row.note,
                    date = row.date,
                    fixedExpenseId = fixedExpenseId,
                    fixedExpensePeriodKey = row.fixedExpensePeriodKey.takeIf { fixedExpenseId != null }
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
            val fromId = accountIdByName[key(row.fromAccountName)]
            val toId = accountIdByName[key(row.toAccountName)]
            if (fromId == null || toId == null) {
                skipped++
                return@forEach
            }
            if (existingTransferKeys.consume(listOf(fromId, toId, row.date, row.amountCents, row.note))) {
                duplicatesSkipped++
                return@forEach
            }
            transferDao.insert(
                TransferEntity(
                    fromAccountId = fromId,
                    toAccountId = toId,
                    categoryId = row.categoryName?.let { categoryIdByName[key(it)] },
                    amountCents = row.amountCents,
                    note = row.note,
                    date = row.date
                )
            )
            transfersAdded++
        }

        return ImportResult(
            accountsAdded, categoriesAdded, transactionsAdded, transfersAdded, skipped, attachmentsAdded,
            fixedExpensesAdded, duplicatesSkipped
        )
    }

    /** What's already in the app when a restore starts; empty lists stand for "ignore" (see [commitImport]). */
    private class ExistingData(
        val accounts: List<AccountEntity>,
        val categories: List<CategoryEntity>,
        val fixedExpenses: List<FixedExpenseEntity>,
        val transactions: List<TransactionEntity>,
        val transfers: List<TransferEntity>
    )

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
