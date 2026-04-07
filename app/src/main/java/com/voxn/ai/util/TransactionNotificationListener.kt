package com.voxn.ai.util

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.voxn.ai.R
import com.voxn.ai.manager.ExpenseParser
import com.voxn.ai.manager.ParsedExpense
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for bank SMS/notification and auto-detects transactions.
 * Shows a notification prompting the user to log it.
 *
 * User must grant Notification Access in Settings > Apps > Special Access > Notification Access.
 */
class TransactionNotificationListener : NotificationListenerService() {

    // Bank package names to watch
    private val bankPackages = setOf(
        "com.google.android.apps.messaging", // Google Messages
        "com.samsung.android.messaging",     // Samsung Messages
        "com.android.mms",                   // Default SMS
    )

    // Keywords that indicate a transaction notification
    private val transactionKeywords = listOf(
        "debited", "credited", "spent", "paid", "received",
        "withdrawn", "deposited", "transferred", "transaction",
        "rs.", "rs ", "inr", "₹",
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val notification = sbn?.notification ?: return
        val packageName = sbn.packageName ?: return

        // Only process SMS/banking apps
        if (packageName !in bankPackages) return

        val extras = notification.extras
        val text = extras.getCharSequence("android.text")?.toString() ?: return
        val title = extras.getCharSequence("android.title")?.toString() ?: ""

        val fullText = "$title $text"

        // Check if this looks like a transaction
        if (transactionKeywords.none { it in fullText.lowercase() }) return

        // Try to parse
        val parser = ExpenseParser(applicationContext)
        val parsed = parser.parseNotification(fullText) ?: return

        // Show a notification to confirm logging
        showLogPrompt(parsed, fullText)
    }

    private fun showLogPrompt(parsed: ParsedExpense, rawText: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Auto-log action
        val logIntent = Intent(this, AutoLogReceiver::class.java).apply {
            putExtra("amount", parsed.amount)
            putExtra("merchant", parsed.merchant)
            putExtra("category", parsed.category.name)
            putExtra("payment_method", parsed.paymentMethod.name)
            putExtra("notification_id", AUTO_LOG_NOTIFICATION_ID)
        }
        val logPending = PendingIntent.getBroadcast(
            this, AUTO_LOG_NOTIFICATION_ID * 100 + 1, logIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Dismiss action
        val dismissIntent = Intent(this, AutoLogReceiver::class.java).apply {
            action = "DISMISS"
            putExtra("notification_id", AUTO_LOG_NOTIFICATION_ID)
        }
        val dismissPending = PendingIntent.getBroadcast(
            this, AUTO_LOG_NOTIFICATION_ID * 100 + 2, dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, "jarvis_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF00D4FF.toInt())
            .setContentTitle("Transaction detected")
            .setContentText("₹${String.format("%,.0f", parsed.amount)} at ${parsed.merchant}")
            .setSubText(parsed.category.name)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(0, "Log It", logPending)
            .addAction(0, "Dismiss", dismissPending)
            .build()

        manager.notify(AUTO_LOG_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val AUTO_LOG_NOTIFICATION_ID = 9999
    }
}
