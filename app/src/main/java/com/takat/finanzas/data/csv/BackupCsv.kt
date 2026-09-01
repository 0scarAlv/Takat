package com.takat.finanzas.data.csv

import com.takat.finanzas.data.entity.AccountEntity
import com.takat.finanzas.data.entity.CategoryEntity
import com.takat.finanzas.data.entity.CategoryKind
import com.takat.finanzas.data.entity.FixedExpenseEntity
import com.takat.finanzas.data.entity.FixedExpenseFrequency
import com.takat.finanzas.data.entity.FixedExpensePeriodStateEntity
import com.takat.finanzas.data.entity.TransactionEntity
import com.takat.finanzas.data.entity.TransferEntity
import com.takat.finanzas.util.csvAmountToCents
import com.takat.finanzas.util.csvDateToMillis
import com.takat.finanzas.util.encodeCsvField
import com.takat.finanzas.util.parseCsvLine
import com.takat.finanzas.util.toCsvAmount
import com.takat.finanzas.util.toCsvDate

fun Int.toCsvColor(): String = "#%06X".format(this and 0xFFFFFF)

fun String.csvColorToInt(): Int {
    val rgb = trim().removePrefix("#").toLongOrNull(16) ?: 0x10B981L
    return (0xFF000000L or (rgb and 0xFFFFFF)).toInt()
}

object BackupCsv {
    private val HEADER = listOf(
        "type", "name", "emoji", "kind", "initial_balance", "is_debt", "include_in_total",
        "color", "date", "account", "to_account", "category", "amount", "note", "attachments",
        "frequency", "day_of_month", "quincena_only", "notify_enabled", "enabled", "total_debt",
        "installments_count", "period_key", "active"
    )

    /** Backups written before gastos fijos were included in the backup format don't have the trailing columns. */
    private val LEGACY_HEADER_V2 = HEADER.dropLast(9)

    /** Backups written before receipt attachments existed don't have that column either. */
    private val LEGACY_HEADER_V1 = LEGACY_HEADER_V2.dropLast(1)

    fun encode(
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        transfers: List<TransferEntity>,
        attachmentEntriesByTransaction: Map<Long, List<String>> = emptyMap(),
        fixedExpenses: List<FixedExpenseEntity> = emptyList(),
        fixedExpensePeriodStates: List<FixedExpensePeriodStateEntity> = emptyList()
    ): String {
        val accountById = accounts.associateBy { it.id }
        val categoryById = categories.associateBy { it.id }
        val fixedExpenseById = fixedExpenses.associateBy { it.id }
        val lines = mutableListOf(HEADER.joinToString(","))

        accounts.forEach { a ->
            lines += buildRow(
                type = "account",
                name = a.name,
                initialBalance = a.initialBalanceCents.toCsvAmount(),
                isDebt = a.isDebt.toString(),
                includeInTotal = a.includeInTotal.toString(),
                color = a.colorArgb.toCsvColor()
            )
        }
        categories.forEach { c ->
            lines += buildRow(type = "category", name = c.name, emoji = c.emoji, kind = c.kind.name)
        }
        transactions.forEach { t ->
            lines += buildRow(
                type = "transaction",
                date = t.date.toCsvDate(),
                account = accountById[t.accountId]?.name.orEmpty(),
                category = categoryById[t.categoryId]?.name.orEmpty(),
                amount = t.amountCents.toCsvAmount(),
                note = t.note.orEmpty(),
                attachments = attachmentEntriesByTransaction[t.id].orEmpty().joinToString("|")
            )
        }
        transfers.forEach { tr ->
            lines += buildRow(
                type = "transfer",
                date = tr.date.toCsvDate(),
                account = accountById[tr.fromAccountId]?.name.orEmpty(),
                toAccount = accountById[tr.toAccountId]?.name.orEmpty(),
                category = categoryById[tr.categoryId]?.name.orEmpty(),
                amount = tr.amountCents.toCsvAmount(),
                note = tr.note.orEmpty()
            )
        }
        fixedExpenses.forEach { fe ->
            lines += buildRow(
                type = "fixed_expense",
                name = fe.name,
                account = accountById[fe.accountId]?.name.orEmpty(),
                category = categoryById[fe.categoryId]?.name.orEmpty(),
                amount = fe.amountCents.toCsvAmount(),
                frequency = fe.frequency.name,
                dayOfMonth = fe.dayOfMonth.toString(),
                quincenaOnly = fe.quincenaOnly.toString(),
                notifyEnabled = fe.notifyEnabled.toString(),
                enabled = fe.enabled.toString(),
                totalDebt = fe.totalDebtCents?.toCsvAmount().orEmpty(),
                installmentsCount = fe.installmentsCount?.toString().orEmpty()
            )
        }
        fixedExpensePeriodStates.forEach { ps ->
            val fixedExpenseName = fixedExpenseById[ps.fixedExpenseId]?.name ?: return@forEach
            lines += buildRow(
                type = "fixed_expense_period",
                name = fixedExpenseName,
                periodKey = ps.periodKey,
                active = ps.active.toString()
            )
        }
        return lines.joinToString("\n")
    }

    fun decode(csv: String): ParsedBackup {
        val lines = csv.lines().filter { it.isNotBlank() }
        require(lines.isNotEmpty()) { "Archivo vacío" }
        val headerFields = parseCsvLine(lines[0])
        val header = when (headerFields) {
            HEADER -> HEADER
            LEGACY_HEADER_V2 -> LEGACY_HEADER_V2
            LEGACY_HEADER_V1 -> LEGACY_HEADER_V1
            else -> throw IllegalArgumentException("Formato de respaldo no reconocido")
        }

        val accounts = mutableListOf<AccountRow>()
        val categories = mutableListOf<CategoryRow>()
        val transactions = mutableListOf<TransactionRow>()
        val transfers = mutableListOf<TransferRow>()
        val fixedExpenses = mutableListOf<FixedExpenseRow>()
        val fixedExpensePeriodStates = mutableListOf<FixedExpensePeriodStateRow>()

        for (i in 1 until lines.size) {
            val fields = parseCsvLine(lines[i])
            if (fields.size < header.size) continue
            val row = header.zip(fields).toMap()

            when (row["type"]) {
                "account" -> accounts += AccountRow(
                    name = row["name"].orEmpty(),
                    initialBalanceCents = row["initial_balance"].orEmpty().csvAmountToCents() ?: continue,
                    isDebt = row["is_debt"]?.toBooleanStrictOrNull() ?: false,
                    includeInTotal = row["include_in_total"]?.toBooleanStrictOrNull() ?: true,
                    colorArgb = row["color"].orEmpty().csvColorToInt()
                )

                "category" -> categories += CategoryRow(
                    name = row["name"].orEmpty(),
                    emoji = row["emoji"].orEmpty(),
                    kind = runCatching { CategoryKind.valueOf(row["kind"].orEmpty()) }.getOrDefault(CategoryKind.BOTH)
                )

                "transaction" -> transactions += TransactionRow(
                    date = row["date"].orEmpty().csvDateToMillis() ?: continue,
                    accountName = row["account"].orEmpty(),
                    categoryName = row["category"]?.takeIf { it.isNotBlank() },
                    amountCents = row["amount"].orEmpty().csvAmountToCents() ?: continue,
                    note = row["note"]?.takeIf { it.isNotBlank() },
                    attachmentEntries = row["attachments"].orEmpty().split("|").filter { it.isNotBlank() }
                )

                "transfer" -> transfers += TransferRow(
                    date = row["date"].orEmpty().csvDateToMillis() ?: continue,
                    fromAccountName = row["account"].orEmpty(),
                    toAccountName = row["to_account"].orEmpty(),
                    categoryName = row["category"]?.takeIf { it.isNotBlank() },
                    amountCents = row["amount"].orEmpty().csvAmountToCents() ?: continue,
                    note = row["note"]?.takeIf { it.isNotBlank() }
                )

                "fixed_expense" -> fixedExpenses += FixedExpenseRow(
                    name = row["name"].orEmpty(),
                    amountCents = row["amount"].orEmpty().csvAmountToCents() ?: continue,
                    accountName = row["account"].orEmpty(),
                    categoryName = row["category"]?.takeIf { it.isNotBlank() },
                    frequency = runCatching { FixedExpenseFrequency.valueOf(row["frequency"].orEmpty()) }
                        .getOrDefault(FixedExpenseFrequency.MENSUAL),
                    dayOfMonth = row["day_of_month"]?.toIntOrNull() ?: 1,
                    quincenaOnly = row["quincena_only"]?.toBooleanStrictOrNull() ?: true,
                    notifyEnabled = row["notify_enabled"]?.toBooleanStrictOrNull() ?: false,
                    enabled = row["enabled"]?.toBooleanStrictOrNull() ?: true,
                    totalDebtCents = row["total_debt"]?.takeIf { it.isNotBlank() }?.csvAmountToCents(),
                    installmentsCount = row["installments_count"]?.takeIf { it.isNotBlank() }?.toIntOrNull()
                )

                "fixed_expense_period" -> fixedExpensePeriodStates += FixedExpensePeriodStateRow(
                    fixedExpenseName = row["name"].orEmpty(),
                    periodKey = row["period_key"].orEmpty(),
                    active = row["active"]?.toBooleanStrictOrNull() ?: true
                )
            }
        }

        return ParsedBackup(accounts, categories, transactions, transfers, fixedExpenses, fixedExpensePeriodStates)
    }

    private fun buildRow(
        type: String,
        name: String = "",
        emoji: String = "",
        kind: String = "",
        initialBalance: String = "",
        isDebt: String = "",
        includeInTotal: String = "",
        color: String = "",
        date: String = "",
        account: String = "",
        toAccount: String = "",
        category: String = "",
        amount: String = "",
        note: String = "",
        attachments: String = "",
        frequency: String = "",
        dayOfMonth: String = "",
        quincenaOnly: String = "",
        notifyEnabled: String = "",
        enabled: String = "",
        totalDebt: String = "",
        installmentsCount: String = "",
        periodKey: String = "",
        active: String = ""
    ): String = listOf(
        type, name, emoji, kind, initialBalance, isDebt, includeInTotal,
        color, date, account, toAccount, category, amount, note, attachments,
        frequency, dayOfMonth, quincenaOnly, notifyEnabled, enabled, totalDebt,
        installmentsCount, periodKey, active
    ).joinToString(",") { encodeCsvField(it) }
}
