package com.jarvis.app.data.model

import androidx.compose.ui.graphics.Color
import com.jarvis.app.theme.JarvisColors

enum class ExpenseCategory(val displayName: String, val icon: String, val color: Color) {
    Food("Food", "restaurant", JarvisColors.warningOrange),
    Transport("Transport", "directions_car", JarvisColors.electricBlue),
    Shopping("Shopping", "shopping_bag", JarvisColors.pink),
    Bills("Bills", "description", JarvisColors.yellow),
    Entertainment("Entertainment", "sports_esports", JarvisColors.purple),
    Health("Health", "favorite", JarvisColors.neonGreen),
    Education("Education", "menu_book", JarvisColors.cyan),
    Other("Other", "more_horiz", JarvisColors.gray);

    companion object {
        fun fromString(raw: String): ExpenseCategory =
            entries.find { it.name.equals(raw, ignoreCase = true) } ?: Other
    }
}

enum class PaymentMethod(val displayName: String) {
    CreditCard("Credit Card"),
    DebitCard("Debit Card"),
    UPI("UPI"),
    Cash("Cash"),
    Wallet("Wallet"),
    Other("Other");

    companion object {
        fun fromString(raw: String): PaymentMethod =
            entries.find { it.name.equals(raw, ignoreCase = true) } ?: Other
    }
}
