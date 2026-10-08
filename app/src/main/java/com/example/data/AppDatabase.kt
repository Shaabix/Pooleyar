package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.*
import com.example.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        AccountEntity::class,
        BankCardEntity::class,
        CategoryEntity::class,
        TransactionEntity::class,
        BudgetEntity::class,
        DebtEntity::class,
        CheckEntity::class,
        RecurringTransactionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun debtDao(): DebtDao
    abstract fun checkDao(): CheckDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pooleyar_finance.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed default Iranian categories & sample initial account
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            seedInitialData(database)
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun seedInitialData(database: AppDatabase) {
            val defaultCategories = listOf(
                // Expenses
                CategoryEntity(name = "خوراک و سوپرمارکت", type = "EXPENSE", iconName = "shopping_cart", colorHex = 0xFFFF7043, isDefault = true),
                CategoryEntity(name = "رستوران و کافه", type = "EXPENSE", iconName = "restaurant", colorHex = 0xFFFF5722, isDefault = true),
                CategoryEntity(name = "حمل و نقل و بنزین", type = "EXPENSE", iconName = "directions_car", colorHex = 0xFF42A5F5, isDefault = true),
                CategoryEntity(name = "مسکن، اجاره و قبوض", type = "EXPENSE", iconName = "home", colorHex = 0xFFAB47BC, isDefault = true),
                CategoryEntity(name = "پزشکی و درمان", type = "EXPENSE", iconName = "local_hospital", colorHex = 0xFFE91E63, isDefault = true),
                CategoryEntity(name = "پوشاک و خرید شخصی", type = "EXPENSE", iconName = "checkroom", colorHex = 0xFF26A69A, isDefault = true),
                CategoryEntity(name = "تفریح، سفر و سرگرمی", type = "EXPENSE", iconName = "flight", colorHex = 0xFFFFA726, isDefault = true),
                CategoryEntity(name = "آموزش و کتاب", type = "EXPENSE", iconName = "school", colorHex = 0xFF5C6BC0, isDefault = true),
                CategoryEntity(name = "اقساط، وام و بیمه", type = "EXPENSE", iconName = "receipt_long", colorHex = 0xFF78909C, isDefault = true),
                CategoryEntity(name = "سایر هزینه‌ها", type = "EXPENSE", iconName = "more_horiz", colorHex = 0xFF8D6E63, isDefault = true),

                // Incomes
                CategoryEntity(name = "حقوق و دستمزد", type = "INCOME", iconName = "payments", colorHex = 0xFF4CAF50, isDefault = true),
                CategoryEntity(name = "پروژه و فریلنسری", type = "INCOME", iconName = "laptop", colorHex = 0xFF2E7D32, isDefault = true),
                CategoryEntity(name = "سود بانکی و سرمایه‌گذاری", type = "INCOME", iconName = "trending_up", colorHex = 0xFF00897B, isDefault = true),
                CategoryEntity(name = "پاداش و عیدی", type = "INCOME", iconName = "card_giftcard", colorHex = 0xFF8BC34A, isDefault = true),
                CategoryEntity(name = "فروش کالا یا دارایی", type = "INCOME", iconName = "storefront", colorHex = 0xFF00ACC1, isDefault = true),
                CategoryEntity(name = "سایر درآمدها", type = "INCOME", iconName = "account_balance_wallet", colorHex = 0xFF66BB6A, isDefault = true)
            )
            database.categoryDao().insertAll(defaultCategories)

            // Seed initial default accounts
            val initialAccountMellat = AccountEntity(
                name = "حساب جاری ملت",
                type = "BANK",
                bankId = "mellat",
                accountNumber = "1234567890",
                shebaNumber = "IR120120000000001234567890",
                initialBalance = 15000000L, // 15M Toman
                colorHex = 0xFFC2185B,
                notes = "حساب اصلی واریز حقوق"
            )
            val accId1 = database.accountDao().insertAccount(initialAccountMellat)
            database.accountDao().insertCard(
                BankCardEntity(
                    accountId = accId1,
                    cardHolderName = "کاربر پولیار",
                    cardNumber = "6104337890123456",
                    bankId = "mellat",
                    expiryMonth = "08",
                    expiryYear = "08"
                )
            )

            val initialAccountCash = AccountEntity(
                name = "کیف پول نقدی",
                type = "CASH",
                bankId = null,
                initialBalance = 2500000L, // 2.5M Toman
                colorHex = 0xFF00897B,
                notes = "موجودی اسکناس روزمره"
            )
            database.accountDao().insertAccount(initialAccountCash)
        }
    }
}
