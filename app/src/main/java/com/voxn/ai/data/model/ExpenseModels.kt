package com.voxn.ai.data.model

import androidx.compose.ui.graphics.Color
import com.voxn.ai.theme.VoxnColors

enum class ExpenseCategory(val displayName: String, val icon: String, val color: Color) {
    Food("Food", "restaurant", VoxnColors.warningOrange),
    Transport("Transport", "directions_car", VoxnColors.electricBlue),
    Shopping("Shopping", "shopping_bag", VoxnColors.pink),
    Bills("Bills", "description", VoxnColors.yellow),
    Entertainment("Entertainment", "sports_esports", VoxnColors.purple),
    Health("Health", "favorite", VoxnColors.neonGreen),
    Education("Education", "menu_book", VoxnColors.cyan),
    Other("Other", "more_horiz", VoxnColors.gray);

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
