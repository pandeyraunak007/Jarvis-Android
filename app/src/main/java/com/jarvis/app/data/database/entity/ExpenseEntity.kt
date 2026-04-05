package com.jarvis.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.jarvis.app.data.model.ExpenseCategory
import com.jarvis.app.data.model.PaymentMethod
import java.text.DecimalFormat
import java.util.UUID

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val amount: Double = 0.0,
    val merchant: String = "",
    val categoryRaw: String = "Other",
    val paymentMethodRaw: String = "Other",
    val date: Long = System.currentTimeMillis(),
    val note: String? = null,
) {
    val category: ExpenseCategory get() = ExpenseCategory.fromString(categoryRaw)
    val paymentMethod: PaymentMethod get() = PaymentMethod.fromString(paymentMethodRaw)
    val formattedAmount: String get() = "₹${DecimalFormat("#,###").format(amount.toLong())}"
}
