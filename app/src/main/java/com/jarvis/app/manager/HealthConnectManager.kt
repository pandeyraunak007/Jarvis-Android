package com.jarvis.app.manager

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.jarvis.app.data.model.DailyHealthEntry
import com.jarvis.app.data.model.HealthData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.time.*
import java.time.format.TextStyle
import java.util.Locale

class HealthConnectManager(private val context: Context) {
    private val _healthData = MutableStateFlow(HealthData())
    val healthData: StateFlow<HealthData> = _healthData

    private val _weeklyEntries = MutableStateFlow<List<DailyHealthEntry>>(emptyList())
    val weeklyEntries: StateFlow<List<DailyHealthEntry>> = _weeklyEntries

    private val _isAvailable = MutableStateFlow(false)
    val isAvailable: StateFlow<Boolean> = _isAvailable

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
    )

    init {
        _isAvailable.value = HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE
    }

    private fun getClient(): HealthConnectClient? {
        return if (_isAvailable.value) HealthConnectClient.getOrCreate(context) else null
    }

    suspend fun fetchAllData() {
        val client = getClient() ?: return
        try {
            val today = LocalDate.now()
            val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val now = Instant.now()

            val steps = fetchSteps(client, startOfDay, now)
            val calories = fetchCalories(client, startOfDay, now)
            val sleepHours = fetchSleep(client, startOfDay, now)
            val workout = fetchWorkout(client, startOfDay, now)

            // Weekly
            val weekAgo = today.minusDays(6)
            val weekStart = weekAgo.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val weeklySteps = fetchSteps(client, weekStart, now)
            val weeklyCalories = fetchCalories(client, weekStart, now)
            val weeklySleep = fetchSleep(client, weekStart, now)
            val weeklyWorkout = fetchWorkout(client, weekStart, now)

            _healthData.value = HealthData(
                steps = steps,
                caloriesBurned = calories,
                sleepHours = sleepHours.first,
                sleepMinutes = sleepHours.second,
                workoutMinutes = workout,
                weeklySteps = weeklySteps / 7.0,
                weeklyCalories = weeklyCalories / 7.0,
                weeklySleepHours = weeklySleep.first / 7.0,
                weeklyWorkoutMinutes = weeklyWorkout / 7.0,
            )

            fetchWeeklyEntries(client)
        } catch (_: Exception) { }
    }

    private suspend fun fetchSteps(client: HealthConnectClient, start: Instant, end: Instant): Double {
        val response = client.readRecords(
            ReadRecordsRequest(StepsRecord::class, timeRangeFilter = TimeRangeFilter.between(start, end))
        )
        return response.records.sumOf { it.count.toDouble() }
    }

    private suspend fun fetchCalories(client: HealthConnectClient, start: Instant, end: Instant): Double {
        val response = client.readRecords(
            ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, timeRangeFilter = TimeRangeFilter.between(start, end))
        )
        return response.records.sumOf { it.energy.inKilocalories }
    }

    private suspend fun fetchSleep(client: HealthConnectClient, start: Instant, end: Instant): Pair<Double, Double> {
        val response = client.readRecords(
            ReadRecordsRequest(SleepSessionRecord::class, timeRangeFilter = TimeRangeFilter.between(start, end))
        )
        val totalMinutes = response.records.sumOf {
            Duration.between(it.startTime, it.endTime).toMinutes().toDouble()
        }
        return Pair(totalMinutes / 60.0, totalMinutes % 60.0)
    }

    private suspend fun fetchWorkout(client: HealthConnectClient, start: Instant, end: Instant): Double {
        val response = client.readRecords(
            ReadRecordsRequest(ExerciseSessionRecord::class, timeRangeFilter = TimeRangeFilter.between(start, end))
        )
        return response.records.sumOf {
            Duration.between(it.startTime, it.endTime).toMinutes().toDouble()
        }
    }

    private suspend fun fetchWeeklyEntries(client: HealthConnectClient) {
        val entries = mutableListOf<DailyHealthEntry>()
        val today = LocalDate.now()
        for (i in 6 downTo 0) {
            val day = today.minusDays(i.toLong())
            val start = day.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val end = day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val steps = fetchSteps(client, start, end)
            val cals = fetchCalories(client, start, end)
            val sleep = fetchSleep(client, start, end)
            val workout = fetchWorkout(client, start, end)
            entries.add(
                DailyHealthEntry(
                    date = start.toEpochMilli(),
                    steps = steps, calories = cals, sleepHours = sleep.first,
                    workoutMinutes = workout,
                    dayLabel = day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                )
            )
        }
        _weeklyEntries.value = entries
    }
}
