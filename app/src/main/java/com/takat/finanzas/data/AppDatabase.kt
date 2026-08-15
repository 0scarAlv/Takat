package com.takat.finanzas.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.takat.finanzas.BuildConfig
import com.takat.finanzas.data.dao.AccountDao
import com.takat.finanzas.data.dao.AppSettingsDao
import com.takat.finanzas.data.dao.AttachmentDao
import com.takat.finanzas.data.dao.BudgetSettingsDao
import com.takat.finanzas.data.dao.CategoryDao
import com.takat.finanzas.data.dao.FixedExpenseDao
import com.takat.finanzas.data.dao.FixedExpensePeriodStateDao
import com.takat.finanzas.data.dao.TransactionDao
import com.takat.finanzas.data.dao.TransferDao
import com.takat.finanzas.data.entity.AccountEntity
import com.takat.finanzas.data.entity.AppSettingsEntity
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
        FixedExpensePeriodStateEntity::class,
        AppSettingsEntity::class
    ],
    version = 15,
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
    abstract fun appSettingsDao(): AppSettingsDao

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
            ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13, MIGRATION_13_14, MIGRATION_14_15)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            database.categoryDao().insertAll(defaultCategories())
                            // Fresh install: start "caught up" on the changelog so the "qué hay de nuevo"
                            // dialog only ever appears after a real update, never on first launch.
                            database.appSettingsDao().upsert(AppSettingsEntity(lastSeenVersionCode = BuildConfig.VERSION_CODE))
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

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Replaces the single paidTransactionId flag with an accumulating paidCents counter
                // so partial payments toward the same period add up instead of each being compared
                // to the full amount in isolation. No real transaction data lives in this table.
                db.execSQL("DROP TABLE IF EXISTS `fixed_expense_period_state`")
                db.execSQL(
                    """
                    CREATE TABLE `fixed_expense_period_state` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `fixedExpenseId` INTEGER NOT NULL,
                        `periodKey` TEXT NOT NULL,
                        `active` INTEGER NOT NULL DEFAULT 1,
                        `paidCents` INTEGER NOT NULL DEFAULT 0,
                        `lastPaidTransactionId` INTEGER,
                        `notifiedAt` INTEGER,
                        FOREIGN KEY(`fixedExpenseId`) REFERENCES `fixed_expenses`(`id`) ON DELETE CASCADE,
                        FOREIGN KEY(`lastPaidTransactionId`) REFERENCES `transactions`(`id`) ON DELETE SET NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fixed_expense_period_state_fixedExpenseId` ON `fixed_expense_period_state` (`fixedExpenseId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fixed_expense_period_state_lastPaidTransactionId` ON `fixed_expense_period_state` (`lastPaidTransactionId`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_fixed_expense_period_state_fixedExpenseId_periodKey` " +
                        "ON `fixed_expense_period_state` (`fixedExpenseId`, `periodKey`)"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // How much of a fixed expense's period is paid is now derived by summing the
                // transactions tagged with it (deleting one automatically un-counts it) instead of
                // a separately maintained counter that could drift out of sync.
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `fixedExpenseId` INTEGER")
                db.execSQL("ALTER TABLE `transactions` ADD COLUMN `fixedExpensePeriodKey` TEXT")

                db.execSQL("DROP TABLE IF EXISTS `fixed_expense_period_state`")
                db.execSQL(
                    """
                    CREATE TABLE `fixed_expense_period_state` (
                        `id` INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                        `fixedExpenseId` INTEGER NOT NULL,
                        `periodKey` TEXT NOT NULL,
                        `active` INTEGER NOT NULL DEFAULT 1,
                        `notifiedAt` INTEGER,
                        FOREIGN KEY(`fixedExpenseId`) REFERENCES `fixed_expenses`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_fixed_expense_period_state_fixedExpenseId` ON `fixed_expense_period_state` (`fixedExpenseId`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_fixed_expense_period_state_fixedExpenseId_periodKey` " +
                        "ON `fixed_expense_period_state` (`fixedExpenseId`, `periodKey`)"
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Split the single "was notified" flag into three independent stages (pre-due, due,
                // follow-up) now that MENSUAL expenses get a day-before heads-up and a
                // day-after-due follow-up in addition to the existing due-day notification.
                db.execSQL("ALTER TABLE `fixed_expense_period_state` RENAME COLUMN `notifiedAt` TO `dueNotifiedAt`")
                db.execSQL("ALTER TABLE `fixed_expense_period_state` ADD COLUMN `preNotifiedAt` INTEGER")
                db.execSQL("ALTER TABLE `fixed_expense_period_state` ADD COLUMN `followUpNotifiedAt` INTEGER")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Quincena-gating for MENSUAL expenses is now opt-in per expense instead of
                // always-on: someone paid monthly may want a second-half bill visible for the
                // whole month. Default true preserves the always-on behavior existing rows already had.
                db.execSQL("ALTER TABLE `fixed_expenses` ADD COLUMN `quincenaOnly` INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Daily-budget freeze: track the last-observed live value (+ the day it was observed on) so it
                // can be copied into a static "presupuesto diario" lazily, the next time the app is used after
                // midnight, without needing a background job.
                db.execSQL("ALTER TABLE `budget_settings` ADD COLUMN `lastLiveValueCents` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `budget_settings` ADD COLUMN `lastLiveValueEpochDay` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `budget_settings` ADD COLUMN `frozenBudgetCents` INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE `budget_settings` ADD COLUMN `frozenBudgetEpochDay` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // App-wide preferences (currently just light/dark/system theme), separate from
                // budget_settings which is specific to the daily-budget feature.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `app_settings` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `themeMode` TEXT NOT NULL DEFAULT 'SYSTEM'
                    )
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Categories are moving from emoji to Material icons (see util/CategoryIcons.kt): the
                // `emoji` column itself is unchanged (still a free-form String, still the CSV backup
                // column name) so old backups keep restoring fine — it just now also accepts an icon
                // name as a value. This backfills the 8 seeded default categories only; custom
                // categories keep their emoji until edited from the new "Gestionar categorías" screen.
                val iconByDefaultName = mapOf(
                    "Sueldo" to "Payments",
                    "Otros ingresos" to "TrendingUp",
                    "Comida" to "Restaurant",
                    "Transporte" to "DirectionsBus",
                    "Pago de deuda" to "CreditCard",
                    "Servicios" to "Bolt",
                    "Entretenimiento" to "SportsEsports",
                    "Otros gastos" to "Category"
                )
                iconByDefaultName.forEach { (name, icon) ->
                    db.execSQL(
                        "UPDATE `categories` SET `emoji` = ? WHERE `isDefault` = 1 AND `name` = ?",
                        arrayOf(icon, name)
                    )
                }
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Daily automatic backup: a user-picked SAF tree folder to write a rolling backup.zip
                // into every 24h (see DailyBackupWorker), plus bookkeeping so Settings can show when
                // the last one ran and surface failures (e.g. the folder became inaccessible).
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `backupFolderUri` TEXT")
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `lastBackupEpochMillis` INTEGER")
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `lastBackupError` TEXT")
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Lets the sarcastic asides on "Disponible" / "Disponible hoy" be turned off from
                // Settings. Defaults to on since they already shipped enabled with no toggle.
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `sarcasticMessagesEnabled` INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Tracks the highest changelog version already shown to the user (see util/Changelog.kt),
                // so the "qué hay de nuevo" dialog can appear once after each update. Existing installs
                // default to 0 ("never seen anything") so they get the full reconstructed history the
                // first time they land on this version.
                db.execSQL("ALTER TABLE `app_settings` ADD COLUMN `lastSeenVersionCode` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private fun defaultCategories() = listOf(
            CategoryEntity(name = "Sueldo", emoji = "Payments", kind = CategoryKind.INCOME, isDefault = true),
            CategoryEntity(name = "Otros ingresos", emoji = "TrendingUp", kind = CategoryKind.INCOME, isDefault = true),
            CategoryEntity(name = "Comida", emoji = "Restaurant", kind = CategoryKind.EXPENSE, isDefault = true),
            CategoryEntity(name = "Transporte", emoji = "DirectionsBus", kind = CategoryKind.EXPENSE, isDefault = true),
            CategoryEntity(name = "Pago de deuda", emoji = "CreditCard", kind = CategoryKind.EXPENSE, isDefault = true),
            CategoryEntity(name = "Servicios", emoji = "Bolt", kind = CategoryKind.EXPENSE, isDefault = true),
            CategoryEntity(name = "Entretenimiento", emoji = "SportsEsports", kind = CategoryKind.EXPENSE, isDefault = true),
            CategoryEntity(name = "Otros gastos", emoji = "Category", kind = CategoryKind.BOTH, isDefault = true)
        )
    }
}
