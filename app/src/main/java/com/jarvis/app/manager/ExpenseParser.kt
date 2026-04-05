package com.jarvis.app.manager

import android.content.Context
import com.jarvis.app.data.database.JarvisDatabase
import com.jarvis.app.data.database.entity.ExpenseEntity
import com.jarvis.app.data.model.ExpenseCategory
import com.jarvis.app.data.model.PaymentMethod
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

data class ParsedExpense(
    val amount: Double,
    val merchant: String,
    val category: ExpenseCategory,
    val paymentMethod: PaymentMethod,
)

class ExpenseParser(private val context: Context) {
    private val dao = JarvisDatabase.getInstance(context).expenseDao()

    val expensesFlow: Flow<List<ExpenseEntity>> = dao.getAll()

    fun parseNotification(text: String): ParsedExpense? {
        val amount = extractAmount(text) ?: return null
        val merchant = extractMerchant(text)
        val category = categorize(merchant)
        val payment = extractPaymentMethod(text)
        return ParsedExpense(amount, merchant, category, payment)
    }

    private fun extractAmount(text: String): Double? {
        val patterns = listOf(
            Regex("(?:rs\\.?|₹|inr)\\s*([\\d,]+\\.?\\d*)", RegexOption.IGNORE_CASE),
            Regex("(?:debited|paid|spent|charged)\\s*(?:rs\\.?|₹|inr)?\\s*([\\d,]+\\.?\\d*)", RegexOption.IGNORE_CASE),
        )
        for (p in patterns) {
            val match = p.find(text)
            if (match != null) {
                return match.groupValues[1].replace(",", "").toDoubleOrNull()
            }
        }
        return Regex("([\\d,]+\\.?\\d*)").find(text)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull()
    }

    private fun extractMerchant(text: String): String {
        val patterns = listOf(
            Regex("(?:at|to|paid to|towards)\\s+([a-zA-Z0-9\\s.]+?)(?:\\s+on|\\s+ref|\\s*$)", RegexOption.IGNORE_CASE),
        )
        for (p in patterns) {
            val match = p.find(text)
            if (match != null) return match.groupValues[1].trim()
        }
        return "Unknown"
    }

    private fun extractPaymentMethod(text: String): PaymentMethod {
        val lower = text.lowercase()
        return when {
            "credit card" in lower -> PaymentMethod.CreditCard
            "debit card" in lower -> PaymentMethod.DebitCard
            "upi" in lower || "gpay" in lower || "phonepe" in lower -> PaymentMethod.UPI
            "wallet" in lower || "paytm" in lower -> PaymentMethod.Wallet
            "cash" in lower -> PaymentMethod.Cash
            else -> PaymentMethod.Other
        }
    }

    fun categorize(merchant: String): ExpenseCategory {
        val lower = merchant.lowercase()
        return when {
            listOf("swiggy", "zomato", "dominos", "mcdonalds", "starbucks", "dunkin", "burger king", "pizza hut", "kfc", "subway", "barbeque nation").any { it in lower } -> ExpenseCategory.Food
            listOf("uber", "ola", "rapido", "metro", "irctc", "redbus", "blablacar").any { it in lower } -> ExpenseCategory.Transport
            listOf("amazon", "flipkart", "myntra", "ajio", "nykaa", "meesho", "tata cliq").any { it in lower } -> ExpenseCategory.Shopping
            listOf("airtel", "jio", "vi", "bsnl", "electricity", "water", "gas", "broadband").any { it in lower } -> ExpenseCategory.Bills
            listOf("netflix", "hotstar", "prime video", "spotify", "bookmyshow", "pvr").any { it in lower } -> ExpenseCategory.Entertainment
            listOf("apollo", "pharmeasy", "netmeds", "1mg", "practo", "cult.fit").any { it in lower } -> ExpenseCategory.Health
            listOf("udemy", "coursera", "unacademy", "byju").any { it in lower } -> ExpenseCategory.Education
            else -> ExpenseCategory.Other
        }
    }

    suspend fun addFromNotification(text: String): Boolean {
        val parsed = parseNotification(text) ?: return false
        dao.insert(
            ExpenseEntity(
                amount = parsed.amount,
                merchant = parsed.merchant,
                categoryRaw = parsed.category.name,
                paymentMethodRaw = parsed.paymentMethod.name,
            )
        )
        return true
    }

    suspend fun addExpense(
        amount: Double, merchant: String, category: ExpenseCategory,
        paymentMethod: PaymentMethod, date: Long = System.currentTimeMillis(), note: String? = null
    ) {
        dao.insert(
            ExpenseEntity(
                amount = amount, merchant = merchant, categoryRaw = category.name,
                paymentMethodRaw = paymentMethod.name, date = date, note = note,
            )
        )
    }

    suspend fun deleteExpense(expense: ExpenseEntity) = dao.delete(expense)

    suspend fun updateCategory(expense: ExpenseEntity, category: ExpenseCategory) {
        dao.update(expense.copy(categoryRaw = category.name))
    }

    companion object {
        fun todayStart(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
        fun weekStart(): Long {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, -7)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
        fun monthStart(): Long {
            val cal = Calendar.getInstance()
            cal.set(Calendar.DAY_OF_MONTH, 1)
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
            return cal.timeInMillis
        }
    }
}
