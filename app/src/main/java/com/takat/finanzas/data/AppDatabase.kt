package com.takat.finanzas.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.takat.finanzas.data.dao.AccountDao
import com.takat.finanzas.data.dao.AttachmentDao
import com.takat.finanzas.data.dao.BudgetSettingsDao
import com.takat.finanzas.data.dao.CategoryDao
import com.takat.finanzas.data.dao.FixedExpenseDao
import com.takat.finanzas.data.dao.FixedExpensePeriodStateDao
import com.takat.finanzas.data.dao.TransactionDao
import com.takat.finanzas.data.dao.TransferDao
import com.takat.finanzas.data.entity.AccountEntity
import com.takat.finanzas.data.entity.AttachmentEntity
import com.takat.finanzas.data.entity.BudgetSettingsEntity
import com.takat.finanzas.data.entity.CategoryEntity
import com.takat.finanzas.data.entity.CategoryKind
import com.takat.finanzas.data.entity.FixedExpenseEntity
import com.takat.finanzas.data.entity.FixedExpensePeriodStateEntity
import com.takat.finanzas.data.entity.TransactionEntity
import com.takat.finanzas.data.entity.TransferEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        TransferEntity::class,
        AttachmentEntity::class,
        BudgetSettingsEntity::class,
        FixedExpenseEntity::class,
        FixedExpensePeriodStateEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun transferDao(): TransferDao
    abstract fun attachmentDao(): AttachmentDao
    abstract fun budgetSettingsDao(): BudgetSettingsDao
    abstract fun fixedExpenseDao(): FixedExpenseDao
    abstract fun fixedExpensePeriodStateDao(): FixedExpensePeriodStateDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }
        }

        private fun build(context: Context): AppDatabase {
            lateinit var database: AppDatabase
            database = Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "takat.db"
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            database.categoryDao().insertAll(defaultCategories())
                        }
                    }
                }).build()
            return database
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE accounts ADD COLUMN isDebt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE accounts ADD COLUMN includeInTotal INTEGER NOT NULL DEFAULT 1")
                db.execSQL("UPDATE accounts SET isDebt = CASE WHEN initialBalanceCents < 0 THEN 1 ELSE 0 END")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `attachments` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `transactionId` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `thumbnailPath` TEXT,
                        `contentHash` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_transactionId` ON `attachments` (`transactionId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_attachments_contentHash` ON `attachments` (`contentHash`)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `budget_settings` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `enabled` INTEGER NOT NULL DEFAULT 0,
                        `periodType` TEXT NOT NULL DEFAULT 'QUINCENA',
                        `dayOfMonth` INTEGER NOT NULL DEFAULT 1,
                        `basis` TEXT NOT NULL DEFAULT 'DISPONIBLE'
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `fixed_expenses` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `name` TEXT NOT NULL,
                        `amountCents` INTEGER NOT NULL,
                        `accountId` INTEGER NOT NULL,
                        `categoryId` INTEGER,
                        `frequency` TEXT NOT NULL,
                        `dayOfMonth` INTEGER NOT NULL,
                        `notifyEnabled` INTEGER NOT NULL DEFAULT 0,
                        `enabled` INTEGER NOT NULL DEFAULT 1,
                        `createdAt` INTEGER NOT NULL,
                        FOREIGN KEY(`accountId`) REFERENCES `accounts`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`categoryId`) REFERENCES `categories`(`id`) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fixed_expenses_accountId` ON `fixed_expenses` (`accountId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fixed_expenses_categoryId` ON `fixed_expenses` (`categoryId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `fixed_expense_period_state` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `fixedExpenseId` INTEGER NOT NULL,
                        `periodKey` TEXT NOT NULL,
                        `active` INTEGER NOT NULL DEFAULT 1,
                        `paidTransactionId` INTEGER,
                        `notifiedAt` INTEGER,
                        FOREIGN KEY(`fixedExpenseId`) REFERENCES `fixed_expenses`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`paidTransactionId`) REFERENCES `transactions`(`id`) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fixed_expense_period_state_fixedExpenseId` ON `fixed_expense_period_state` (`fixedExpenseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fixed_expense_period_state_paidTransactionId` ON `fixed_expense_period_state` (`paidTransactionId`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_fixed_expense_period_state_fixedExpenseId_periodKey` " +
                        "ON `fixed_expense_period_state` (`fixedExpenseId`, `periodKey`)"
                )
            }
        }

        private fun defaultCategories() = listOf(
            CategoryEntity(name = "Sueldo", emoji = "💰", kind = CategoryKind.INCOME, isDefault = true),
            CategoryEntity(name = "Otros ingresos", emoji = "➕", kind = CategoryKind.INCOME, isDefault = true),
            CategoryEntity(name = "Comida", emoji = "🍔", kind = CategoryKind.EXPENSE, isDefault = true),
            CategoryEntity(name = "Transporte", emoji = "🚌", kind = CategoryKind.EXPENSE, isDefault = true),
            CategoryEntity(name = "Pago de deuda", emoji = "💳", kind = CategoryKind.EXPENSE, isDefault = true),
            CategoryEntity(name = "Servicios", emoji = "💡", kind = CategoryKind.EXPENSE, isDefault = true),
            CategoryEntity(name = "Entretenimiento", emoji = "🎮", kind = CategoryKind.EXPENSE, isDefault = true),
            CategoryEntity(name = "Otros gastos", emoji = "📦", kind = CategoryKind.BOTH, isDefault = true)
        )
    }
}
