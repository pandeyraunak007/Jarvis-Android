package com.voxn.ai.data.model

import java.text.DecimalFormat
import java.util.UUID

data class HealthData(
    val steps: Double = 0.0,
    val caloriesBurned: Double = 0.0,
    val sleepHours: Double = 0.0,
    val sleepMinutes: Double = 0.0,
    val workoutMinutes: Double = 0.0,
    val weeklySteps: Double = 0.0,
    val weeklyCalories: Double = 0.0,
    val weeklySleepHours: Double = 0.0,
    val weeklyWorkoutMinutes: Double = 0.0,
) {
    val stepsProgress get() = (steps / 10000.0).coerceIn(0.0, 1.0)
    val caloriesProgress get() = (caloriesBurned / 500.0).coerceIn(0.0, 1.0)
    val sleepProgress get() = (sleepHours / 8.0).coerceIn(0.0, 1.0)
    val workoutProgress get() = (workoutMinutes / 60.0).coerceIn(0.0, 1.0)

    val stepsFormatted: String get() = DecimalFormat("#,###").format(steps.toLong())
    val caloriesFormatted: String get() = "${DecimalFormat("#,###").format(caloriesBurned.toLong())} kcal"
    val sleepFormatted: String get() {
        val h = sleepHours.toInt()
        val m = sleepMinutes.toInt()
        return "${h}h ${m}m"
    }
    val workoutFormatted: String get() = "${workoutMinutes.toInt()} min"
}

data class DailyHealthEntry(
    val id: String = UUID.randomUUID().toString(),
    val date: Long = System.currentTimeMillis(),
    val steps: Double = 0.0,
    val calories: Double = 0.0,
    val sleepHours: Double = 0.0,
    val workoutMinutes: Double = 0.0,
    val dayLabel: String = ""
)
