package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.entity.AccountEntity
import com.example.data.entity.BankCardEntity
import com.example.data.entity.BudgetEntity
import com.example.data.entity.DebtEntity
import com.example.model.DebtDirection
import com.example.model.DebtStatus
import com.example.model.IranianBank
import com.example.model.IranianBanks
import com.example.repository.FinanceRepository
import com.example.util.Currency
import com.example.util.CurrencyFormatter
import com.example.util.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Phase1SafeguardsAndRegressionTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: FinanceRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = FinanceRepository(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // -------------------------------------------------------------
    // CURRENCY REGRESSION TESTS
    // -------------------------------------------------------------

    @Test
    fun testCurrencyRatioTenToOne() {
        val toman = 100_000L
        val formattedToman = CurrencyFormatter.formatAmount(toman, Currency.TOMAN, persianDigits = false)
        val formattedRial = CurrencyFormatter.formatAmount(toman, Currency.RIAL, persianDigits = false)

        assertEquals("100,000 تومان", formattedToman)
        assertEquals("1,000,000 ریال", formattedRial)

        // 10 Rial = 1 Toman
        val parsedFromRial = CurrencyFormatter.parseAmount("1000000", Currency.RIAL)
        assertEquals(100_000L, parsedFromRial)

        val parsedFromToman = CurrencyFormatter.parseAmount("100000", Currency.TOMAN)
        assertEquals(100_000L, parsedFromToman)
    }

    @Test
    fun testZeroAndEmptyAmounts() {
        assertEquals(0L, CurrencyFormatter.parseAmount("", Currency.TOMAN))
        assertEquals(0L, CurrencyFormatter.parseAmount("", Currency.RIAL))
        assertEquals(0L, CurrencyFormatter.parseAmount("0", Currency.TOMAN))
        assertEquals(0L, CurrencyFormatter.parseAmount("0", Currency.RIAL))
        assertEquals(0L, CurrencyFormatter.parseAmount("۰", Currency.TOMAN))
        assertEquals(0L, CurrencyFormatter.parseAmount("۰", Currency.RIAL))
        assertEquals(0L, CurrencyFormatter.parseAmount("   ", Currency.TOMAN))
    }

    @Test
    fun testSmallRialAmountsNeverDropToZero() {
        // Positive Rial amounts between 1 and 9 must not truncate to 0
        assertEquals(1L, CurrencyFormatter.parseAmount("1", Currency.RIAL))
        assertEquals(1L, CurrencyFormatter.parseAmount("5", Currency.RIAL))
        assertEquals(1L, CurrencyFormatter.parseAmount("9", Currency.RIAL))
        assertEquals(1L, CurrencyFormatter.parseAmount("10", Currency.RIAL))
        assertEquals(1L, CurrencyFormatter.parseAmount("14", Currency.RIAL))
        assertEquals(2L, CurrencyFormatter.parseAmount("15", Currency.RIAL))
        assertEquals(2L, CurrencyFormatter.parseAmount("20", Currency.RIAL))
    }

    @Test
    fun testPersianNumeralsAndSeparators() {
        // Persian digits: ۲,۵۰۰,۰۰۰ Toman
        val parsedToman = CurrencyFormatter.parseAmount("۲,۵۰۰,۰۰۰", Currency.TOMAN)
        assertEquals(2_500_000L, parsedToman)

        // Persian digits: ۲۵,۰۰۰,۰۰۰ Rial = 2,500,000 Toman
        val parsedRial = CurrencyFormatter.parseAmount("۲۵,۰۰۰,۰۰۰", Currency.RIAL)
        assertEquals(2_500_000L, parsedRial)
    }

    @Test
    fun testRealisticLargeAmounts() {
        val largeToman = 150_000_000L // 150 Million Toman
        val formattedRial = CurrencyFormatter.formatAmount(largeToman, Currency.RIAL, persianDigits = false)
        assertEquals("1,500,000,000 ریال", formattedRial)

        val parsedBack = CurrencyFormatter.parseAmount("1,500,000,000", Currency.RIAL)
        assertEquals(largeToman, parsedBack)
    }

    @Test
    fun testAmountForInputPrefill() {
        val storedToman = 50_000L

        // In TOMAN mode, input field gets raw Toman string
        assertEquals("50000", CurrencyFormatter.amountForInput(storedToman, Currency.TOMAN))

        // In RIAL mode, input field gets 10x amount string
        assertEquals("500000", CurrencyFormatter.amountForInput(storedToman, Currency.RIAL))

        // Round-trip check: user opens dialog with RIAL preference, saves unchanged
        val inputPrefill = CurrencyFormatter.amountForInput(storedToman, Currency.RIAL)
        val parsedBack = CurrencyFormatter.parseAmount(inputPrefill, Currency.RIAL)
        assertEquals(storedToman, parsedBack)
    }

    // -------------------------------------------------------------
    // ROOM PERSISTENCE & EDIT TESTS
    // -------------------------------------------------------------

    @Test
    fun testFreshDatabaseHasZeroFictitiousAccounts() = runBlocking {
        val accounts = db.accountDao().getAllActiveAccounts().first()
        val cards = db.accountDao().getAllCards().first()
        val transactions = db.transactionDao().getAllTransactions().first()

        assertEquals(0, accounts.size)
        assertEquals(0, cards.size)
        assertEquals(0, transactions.size)
    }

    @Test
    fun testAccountEditPreservesIdAndPreventsDuplicateCards() = runBlocking {
        // 1. Create initial account with card
        val initialAcc = AccountEntity(
            name = "بانک سامان من",
            type = "BANK",
            bankId = "saman",
            initialBalance = 1_000_000L
        )
        val accId = repository.saveAccountWithCard(initialAcc, "6219861234567890")
        assertTrue(accId > 0)

        val accountsBefore = db.accountDao().getAllActiveAccounts().first()
        val cardsBefore = db.accountDao().getAllCards().first()
        assertEquals(1, accountsBefore.size)
        assertEquals(1, cardsBefore.size)
        val originalCardId = cardsBefore.first().id

        // 2. Edit the existing account (name, initialBalance, and update card number)
        val editedAcc = accountsBefore.first().copy(
            name = "بانک سامان اصلی",
            initialBalance = 2_000_000L
        )
        val editedAccId = repository.saveAccountWithCard(editedAcc, "6219869999999999", existingCardId = originalCardId)
        assertEquals(accId, editedAccId)

        // 3. Verify in Room: still exactly 1 account and 1 card, with same IDs and updated values
        val accountsAfter = db.accountDao().getAllActiveAccounts().first()
        val cardsAfter = db.accountDao().getAllCards().first()

        assertEquals(1, accountsAfter.size)
        assertEquals("بانک سامان اصلی", accountsAfter.first().name)
        assertEquals(2_000_000L, accountsAfter.first().initialBalance)
        assertEquals(accId, accountsAfter.first().id)

        assertEquals(1, cardsAfter.size)
        assertEquals(originalCardId, cardsAfter.first().id)
        assertEquals("6219869999999999", cardsAfter.first().cardNumber)
    }

    @Test
    fun testBudgetEditPreservesIdAndPreventsDuplicates() = runBlocking {
        // 1. Create a budget
        val budget = BudgetEntity(
            categoryId = 1L,
            monthlyLimit = 5_000_000L,
            jalaliYear = 1405,
            jalaliMonth = 7
        )
        val budgetId = repository.saveBudget(budget)
        assertTrue(budgetId > 0)

        val budgetsBefore = db.budgetDao().getAllBudgets().first()
        assertEquals(1, budgetsBefore.size)

        // 2. Edit existing budget
        val editedBudget = budgetsBefore.first().copy(monthlyLimit = 7_500_000L)
        val returnedId = repository.saveBudget(editedBudget)
        assertEquals(budgetId, returnedId)

        // 3. Verify in Room: still exactly 1 budget record with updated limit
        val budgetsAfter = db.budgetDao().getAllBudgets().first()
        assertEquals(1, budgetsAfter.size)
        assertEquals(budgetId, budgetsAfter.first().id)
        assertEquals(7_500_000L, budgetsAfter.first().monthlyLimit)
    }

    @Test
    fun testDebtEditPreservesIdAndState() = runBlocking {
        val debt = DebtEntity(
            personName = "علی رضایی",
            direction = DebtDirection.I_OWE.name,
            totalAmount = 10_000_000L,
            paidAmount = 3_000_000L,
            dateEpochMillis = 1000L,
            jalaliYear = 1405,
            jalaliMonth = 7,
            jalaliDay = 1,
            status = DebtStatus.PARTIALLY_PAID.name
        )
        val debtId = repository.saveDebt(debt)
        assertTrue(debtId > 0)

        // Edit description
        val editedDebt = debt.copy(id = debtId, totalAmount = 12_000_000L, description = "توضیح اضافه شد")
        repository.saveDebt(editedDebt)

        val debts = db.debtDao().getAllDebts().first()
        assertEquals(1, debts.size)
        assertEquals(debtId, debts.first().id)
        assertEquals(12_000_000L, debts.first().totalAmount)
        assertEquals(3_000_000L, debts.first().paidAmount)
        assertEquals(DebtStatus.PARTIALLY_PAID.name, debts.first().status)
    }

    @Test
    fun testCustomBankPersistence() {
        val prefs = PreferencesManager(context)
        val customBank = IranianBank(
            id = "custom_test_123",
            nameFa = "بانک مهرگان",
            nameEn = "Mehregan Bank",
            cardPrefix = "505050",
            primaryColorHex = 0xFF123456L,
            isCustom = true
        )

        prefs.saveCustomBank(customBank)
        val loaded = prefs.getCustomBanks()
        assertEquals(1, loaded.size)
        assertEquals("custom_test_123", loaded.first().id)
        assertEquals("بانک مهرگان", loaded.first().nameFa)
        assertEquals("Mehregan Bank", loaded.first().nameEn)
        assertEquals("505050", loaded.first().cardPrefix)
        assertEquals(0xFF123456L, loaded.first().primaryColorHex)

        // IranianBanks sync
        IranianBanks.setCustomBanks(loaded)
        val found = IranianBanks.findById("custom_test_123")
        assertNotNull(found)
        assertEquals("بانک مهرگان", found?.nameFa)
    }

    @Test
    fun testCardEditPreservesExpiryAndNotesAndValidatesAccountOwnership() = runBlocking {
        // Create Account 1
        val acc1 = AccountEntity(name = "حساب اول", type = "BANK", bankId = "mellat", initialBalance = 1_000_000L)
        val acc1Id = repository.saveAccount(acc1)

        // Insert card directly with expiryMonth, expiryYear, notes
        val card1 = BankCardEntity(
            accountId = acc1Id,
            cardHolderName = "کاربر اول",
            cardNumber = "6104331111111111",
            bankId = "mellat",
            expiryMonth = "08",
            expiryYear = "06",
            notes = "یادداشت محرمانه کارت"
        )
        val card1Id = db.accountDao().insertCard(card1)

        // Create Account 2
        val acc2 = AccountEntity(name = "حساب دوم", type = "BANK", bankId = "melli", initialBalance = 500_000L)
        val acc2Id = repository.saveAccount(acc2)

        // 1. Edit Account 1 and its card: cardNumber changes, but expiryMonth/expiryYear/notes must be preserved
        val updatedAcc1 = acc1.copy(id = acc1Id, name = "حساب اول ویرایش شده")
        repository.saveAccountWithCard(updatedAcc1, cardNumber = "6104332222222222", existingCardId = card1Id)

        val retrievedCard1 = db.accountDao().getCardById(card1Id)
        assertNotNull(retrievedCard1)
        assertEquals("6104332222222222", retrievedCard1?.cardNumber)
        assertEquals("08", retrievedCard1?.expiryMonth)
        assertEquals("06", retrievedCard1?.expiryYear)
        assertEquals("یادداشت محرمانه کارت", retrievedCard1?.notes)
        assertEquals(acc1Id, retrievedCard1?.accountId)

        // 2. An explicit card ID belonging to another account must be rejected.
        val updatedAcc2 = acc2.copy(id = acc2Id)
        var rejected = false
        try {
            repository.saveAccountWithCard(
                updatedAcc2,
                cardNumber = "6037993333333333",
                existingCardId = card1Id
            )
        } catch (_: IllegalArgumentException) {
            rejected = true
        }
        assertTrue("Cross-account card ID should be rejected", rejected)

        // The failed transaction must leave both accounts' card data unchanged.
        val recheckedCard1 = db.accountDao().getCardById(card1Id)
        assertEquals(acc1Id, recheckedCard1?.accountId)
        assertEquals("6104332222222222", recheckedCard1?.cardNumber)
        assertTrue(db.accountDao().getCardsForAccountSync(acc2Id).isEmpty())
    }

    @Test
    fun testCorruptedCustomBankJsonHandling() {
        val prefs = PreferencesManager(context)
        val sharedPrefs = context.getSharedPreferences("pooleyar_prefs", Context.MODE_PRIVATE)

        // Write malformed JSON
        sharedPrefs.edit().putString("custom_banks_json", "{ invalid json").apply()
        val banksFromCorrupted = prefs.getCustomBanks()
        assertTrue(banksFromCorrupted.isEmpty())

        // Saving a valid bank after corrupted state safely resets and preserves valid data
        val validBank = IranianBank(id = "safe_bank", nameFa = "بانک امن")
        prefs.saveCustomBank(validBank)

        val recovered = prefs.getCustomBanks()
        assertEquals(1, recovered.size)
        assertEquals("safe_bank", recovered.first().id)
        assertEquals("بانک امن", recovered.first().nameFa)
    }
}
