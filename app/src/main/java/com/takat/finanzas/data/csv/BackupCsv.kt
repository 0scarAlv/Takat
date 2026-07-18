package com.takat.finanzas.data.csv

import com.takat.finanzas.data.entity.AccountEntity
import com.takat.finanzas.data.entity.CategoryEntity
import com.takat.finanzas.data.entity.CategoryKind
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
        "color", "date", "account", "to_account", "category", "amount", "note"
    )

    fun encode(
        accounts: List<AccountEntity>,
        categories: List<CategoryEntity>,
        transactions: List<TransactionEntity>,
        transfers: List<TransferEntity>
    ): String {
        val accountById = accounts.associateBy { it.id }
        val categoryById = categories.associateBy { it.id }
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
                note = t.note.orEmpty()
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
        return lines.joinToString("\n")
    }

    fun decode(csv: String): ParsedBackup {
        val lines = csv.lines().filter { it.isNotBlank() }
        require(lines.isNotEmpty()) { "Archivo vacío" }
        require(parseCsvLine(lines[0]) == HEADER) { "Formato de CSV no reconocido" }

        val accounts = mutableListOf<AccountRow>()
        val categories = mutableListOf<CategoryRow>()
        val transactions = mutableListOf<TransactionRow>()
        val transfers = mutableListOf<TransferRow>()

        for (i in 1 until lines.size) {
            val fields = parseCsvLine(lines[i])
            if (fields.size < HEADER.size) continue
            val row = HEADER.zip(fields).toMap()

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
                    note = row["note"]?.takeIf { it.isNotBlank() }
                )

                "transfer" -> transfers += TransferRow(
                    date = row["date"].orEmpty().csvDateToMillis() ?: continue,
                    fromAccountName = row["account"].orEmpty(),
                    toAccountName = row["to_account"].orEmpty(),
                    categoryName = row["category"]?.takeIf { it.isNotBlank() },
                    amountCents = row["amount"].orEmpty().csvAmountToCents() ?: continue,
                    note = row["note"]?.takeIf { it.isNotBlank() }
                )
            }
        }

        return ParsedBackup(accounts, categories, transactions, transfers)
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
        note: String = ""
    ): String = listOf(
        type, name, emoji, kind, initialBalance, isDebt, includeInTotal,
        color, date, account, toAccount, category, amount, note
    ).joinToString(",") { encodeCsvField(it) }
}
