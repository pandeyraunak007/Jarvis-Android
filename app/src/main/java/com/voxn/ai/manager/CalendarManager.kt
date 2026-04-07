package com.voxn.ai.manager

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Long,
    val endTime: Long,
    val location: String?,
    val isAllDay: Boolean,
) {
    val startTimeFormatted: String
        get() = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(startTime))

    val endTimeFormatted: String
        get() = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(endTime))

    val timeRange: String
        get() = if (isAllDay) "All day" else "$startTimeFormatted — $endTimeFormatted"

    val isOngoing: Boolean
        get() {
            val now = System.currentTimeMillis()
            return now in startTime..endTime
        }
}

class CalendarManager(private val context: Context) {

    private val _todayEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val todayEvents: StateFlow<List<CalendarEvent>> = _todayEvents

    private val _hasPermission = MutableStateFlow(false)
    val hasPermission: StateFlow<Boolean> = _hasPermission

    init {
        checkPermission()
    }

    fun checkPermission() {
        _hasPermission.value = ContextCompat.checkSelfPermission(
            context, Manifest.permission.READ_CALENDAR
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun fetchTodayEvents() {
        if (!_hasPermission.value) return

        withContext(Dispatchers.IO) {
            val events = mutableListOf<CalendarEvent>()
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + 86400000L

            val projection = arrayOf(
                CalendarContract.Events._ID,
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.EVENT_LOCATION,
                CalendarContract.Events.ALL_DAY,
            )

            val selection = "((${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} < ?) OR " +
                "(${CalendarContract.Events.DTSTART} < ? AND ${CalendarContract.Events.DTEND} > ?))"
            val selectionArgs = arrayOf(
                dayStart.toString(), dayEnd.toString(),
                dayStart.toString(), dayStart.toString(),
            )

            var cursor: Cursor? = null
            try {
                cursor = context.contentResolver.query(
                    CalendarContract.Events.CONTENT_URI,
                    projection, selection, selectionArgs,
                    "${CalendarContract.Events.DTSTART} ASC",
                )
                cursor?.let {
                    while (it.moveToNext()) {
                        val id = it.getLong(0)
                        val title = it.getString(1) ?: "Untitled"
                        val start = it.getLong(2)
                        val end = it.getLong(3).let { e -> if (e == 0L) start + 3600000 else e }
                        val location = it.getString(4)
                        val allDay = it.getInt(5) == 1

                        events.add(CalendarEvent(id, title, start, end, location, allDay))
                    }
                }
            } catch (_: Exception) {
                // Permission denied or content provider error
            } finally {
                cursor?.close()
            }

            _todayEvents.value = events
        }
    }
}
