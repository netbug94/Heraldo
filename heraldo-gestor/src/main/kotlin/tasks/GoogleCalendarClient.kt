package com.netbug94.tasks

import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.services.calendar.Calendar
import com.google.api.services.calendar.model.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.ZonedDateTime

class GoogleCalendarClient(
    private val tasksClient: GoogleTasksClient
) {
    private var _calendarService: Calendar? = null
    private val calendarService: Calendar
        get() {
            if (_calendarService == null) {
                _calendarService = Calendar.Builder(
                    NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    tasksClient.credential
                ).setApplicationName("HeraldoGestor").build()
            }
            return _calendarService!!
        }

    fun clearService() {
        _calendarService = null
    }

    suspend fun getTodayEvents(startOfDay: ZonedDateTime, endOfDay: ZonedDateTime): List<Event> = withContext(Dispatchers.IO) {
        val minTime = DateTime(startOfDay.toInstant().toEpochMilli())
        val maxTime = DateTime(endOfDay.toInstant().toEpochMilli())

        calendarService.events().list("primary")
            .setTimeMin(minTime)
            .setTimeMax(maxTime)
            .setOrderBy("startTime")
            .setSingleEvents(true)
            .execute()
            .items ?: emptyList()
    }
}