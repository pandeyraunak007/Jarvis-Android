package com.voxn.ai.util

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.voxn.ai.data.database.VoxnDatabase
import com.voxn.ai.data.database.entity.ExpenseEntity
import com.voxn.ai.data.model.ExpenseCategory
import com.voxn.ai.data.model.PaymentMethod
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AutoLogReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra("notification_id", 0)

        // Dismiss the notification
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(notificationId)

        if (intent.action == "DISMISS") return

        // Log the expense
        val amount = intent.getDoubleExtra("amount", 0.0)
        val merchant = intent.getStringExtra("merchant") ?: "Unknown"
        val categoryRaw = intent.getStringExtra("category") ?: "Other"
        val paymentMethodRaw = intent.getStringExtra("payment_method") ?: "Other"

        if (amount <= 0) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = VoxnDatabase.getInstance(context).expenseDao()
                dao.insert(
                    ExpenseEntity(
                        amount = amount,
                        merchant = merchant,
                        categoryRaw = categoryRaw,
                        paymentMethodRaw = paymentMethodRaw,
                    )
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
