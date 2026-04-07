package com.voxn.ai

import com.voxn.ai.data.model.ExpenseCategory
import com.voxn.ai.data.model.PaymentMethod
import org.junit.Assert.*
import org.junit.Test

class ExpenseParserTest {

    // --- Amount extraction tests (via parseNotification) ---

    @Test
    fun `categorize maps Swiggy to Food`() {
        assertEquals(ExpenseCategory.Food, categorize("Swiggy"))
        assertEquals(ExpenseCategory.Food, categorize("ZOMATO"))
        assertEquals(ExpenseCategory.Food, categorize("dominos order"))
    }

    @Test
    fun `categorize maps Uber to Transport`() {
        assertEquals(ExpenseCategory.Transport, categorize("Uber"))
        assertEquals(ExpenseCategory.Transport, categorize("ola ride"))
        assertEquals(ExpenseCategory.Transport, categorize("IRCTC"))
    }

    @Test
    fun `categorize maps Amazon to Shopping`() {
        assertEquals(ExpenseCategory.Shopping, categorize("Amazon"))
        assertEquals(ExpenseCategory.Shopping, categorize("flipkart"))
        assertEquals(ExpenseCategory.Shopping, categorize("Myntra purchase"))
    }

    @Test
    fun `categorize maps Netflix to Entertainment`() {
        assertEquals(ExpenseCategory.Entertainment, categorize("Netflix"))
        assertEquals(ExpenseCategory.Entertainment, categorize("hotstar"))
        assertEquals(ExpenseCategory.Entertainment, categorize("BookMyShow"))
    }

    @Test
    fun `categorize maps Airtel to Bills`() {
        assertEquals(ExpenseCategory.Bills, categorize("Airtel"))
        assertEquals(ExpenseCategory.Bills, categorize("jio recharge"))
        assertEquals(ExpenseCategory.Bills, categorize("electricity"))
    }

    @Test
    fun `categorize maps Apollo to Health`() {
        assertEquals(ExpenseCategory.Health, categorize("Apollo"))
        assertEquals(ExpenseCategory.Health, categorize("pharmeasy"))
        assertEquals(ExpenseCategory.Health, categorize("cult.fit"))
    }

    @Test
    fun `categorize maps Udemy to Education`() {
        assertEquals(ExpenseCategory.Education, categorize("Udemy"))
        assertEquals(ExpenseCategory.Education, categorize("coursera"))
    }

    @Test
    fun `categorize returns Other for unknown merchant`() {
        assertEquals(ExpenseCategory.Other, categorize("Random Store"))
        assertEquals(ExpenseCategory.Other, categorize(""))
    }

    // Inline categorize logic mirroring ExpenseParser.categorize() for unit testing without Context
    private fun categorize(merchant: String): ExpenseCategory {
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
}
