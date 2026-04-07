package com.voxn.ai

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager

class VoxnApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            "jarvis_channel",
            "Voxn AI Notifications",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Habit reminders and note notifications"
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }
}
