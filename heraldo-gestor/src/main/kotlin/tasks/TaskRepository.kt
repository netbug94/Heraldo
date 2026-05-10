package com.netbug94.tasks

import com.netbug94.core.TimezoneProvider
import com.netbug94.core.logger
import com.netbug94.mensajero.MensajeroClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

class TaskRepository(
    private val timezoneProvider: TimezoneProvider,
    private val mensajeroClient: MensajeroClient,
    private val googleTasksClient: GoogleTasksClient,
    private val googleCalendarClient: GoogleCalendarClient,
    private val allDayMensajeroTime: LocalTime,
    private val summaryTaskTime: LocalTime
) {
    private val logger by logger()

    private val dailyCache = ConcurrentHashMap<String, TaskData>()
    private val cacheFile = File("./config/cache.json")

    @Volatile
    private var isTokenRevoked = false
    private val cacheMutex = Mutex()

    @Volatile
    var lastSyncTime: String = "Never"
        private set

    init {
        loadState()
    }

    private fun loadState() {
        try {
            if (cacheFile.exists()) {
                val jsonStr = cacheFile.readText()
                if (jsonStr.isNotBlank()) {
                    val saved = Json.decodeFromString<Map<String, TaskData>>(jsonStr)
                    dailyCache.putAll(saved)
                    logger.info("💾 Loaded ${dailyCache.size} tasks from persisted storage.")
                }
            } else {
                cacheFile.parentFile?.mkdirs()
            }
        } catch (e: Exception) {
            logger.error("🚨 Failed to load cache: ${e.message}")
        }
    }

    private suspend fun persistState() {
        try {
            val jsonStr = cacheMutex.withLock { Json.encodeToString(dailyCache.toMap()) }
            withContext(Dispatchers.IO) {
                cacheFile.writeText(jsonStr)
            }
        } catch (e: Exception) {
            logger.error("🚨 Failed to persist cache: ${e.message}")
        }
    }

    suspend fun fetchTodayTasks() {
        logger.info("Repository: Summoning decrees from Google...")
        try {
            val myRealNow = timezoneProvider.getMyLocalTime()
            val myRealToday = myRealNow.toLocalDate()

            val allMissedTitles = mutableSetOf<String>()

            // Cache missed tasks (Type 3 Logic Part 1)
            cacheMutex.withLock {
                val cachedMissed = dailyCache.values.filter {
                    !it.mensajeroDone && !it.id.startsWith("summary_") && it.dueDate != null && it.dueDate < myRealToday
                }
                cachedMissed.forEach { allMissedTitles.add(it.title) }

                // Clean up old individual items so they don't linger
                val staleIds = cachedMissed.map { it.id }
                staleIds.forEach { dailyCache.remove(it) }
            }

            val taskLists = googleTasksClient.getTaskLists()
            val validTaskIdsForToday = mutableSetOf<String>()

            // Google tasks
            for (taskList in taskLists) {
                googleTasksClient.getTasks(taskList.id).forEach { task ->
                    val rawTitle = task.title ?: return@forEach
                    val dueString = task.due ?: return@forEach

                    val dueDateLocal = runCatching { LocalDate.parse(dueString.take(10)) }.getOrNull()
                    val (parsedTime, cleanTitle) = parseTaskTitle(rawTitle)

                    if (dueDateLocal != null && dueDateLocal < myRealToday) {
                        if (parsedTime != null) {
                            allMissedTitles.add(cleanTitle)
                        }
                        return@forEach
                    }

                    if (dueDateLocal != myRealToday) return@forEach
                    if (parsedTime == null) {
                        logger.warn("⚠️ DROPPED (Regex Failed): '$rawTitle' is set for today, but missing [HH:mm]")
                        return@forEach
                    }

                    validTaskIdsForToday.add(task.id)
                    syncTaskToCache(task.id, taskList.id, cleanTitle, task.notes, parsedTime, myRealToday)
                }
            }

            // Google calendar
            try {
                val startOfDay = myRealToday.atStartOfDay(myRealNow.zone)
                val calendarEvents = googleCalendarClient.getTodayEvents(startOfDay, startOfDay.plusDays(1).minusNanos(1))

                for (event in calendarEvents) {
                    val eventLocalTime: LocalTime
                    val titlePrefix: String

                    if (event.start?.dateTime != null) {
                        eventLocalTime = java.time.Instant.ofEpochMilli(event.start.dateTime.value)
                            .atZone(myRealNow.zone).toLocalTime()
                        titlePrefix = "🗓️"
                    } else {
                        eventLocalTime = allDayMensajeroTime
                        titlePrefix = "🌅 [All Day]"
                    }

                    val eventId = "cal_${event.id}"
                    validTaskIdsForToday.add(eventId)
                    syncTaskToCache(eventId, "CALENDAR", "$titlePrefix ${event.summary ?: "Busy"}", event.description, eventLocalTime, myRealToday)
                }
            } catch (e: Exception) { logger.error("🚨 Calendar Sync Error: ${e.message}") }

            // Build type 3 summary
            if (allMissedTitles.isNotEmpty()) {
                val summaryId = "summary_$myRealToday"
                val listStr = allMissedTitles.joinToString("\n") { "• $it" }

                cacheMutex.withLock {
                    val existingSummary = dailyCache[summaryId]
                    dailyCache[summaryId] = TaskData(
                        id = summaryId,
                        taskListId = "NONE",
                        title = "📜‼️: ${allMissedTitles.size} Yesterday Tasks",
                        description = "The server has returned. These tasks were left in the fog:\n$listStr",
                        dueTime = summaryTaskTime,
                        mensajeroDone = existingSummary?.mensajeroDone ?: false,
                        dueDate = myRealToday
                    )
                }
            }

            // Cache cleanup
            cacheMutex.withLock {
                val deletedIds = dailyCache.keys - validTaskIdsForToday
                deletedIds.forEach { id ->
                    if (id == "summary_$myRealToday") return@forEach
                    if (id.startsWith("summary_")) {
                        dailyCache.remove(id)
                        return@forEach
                    }
                    if (dailyCache[id]?.mensajeroDone != true) dailyCache.remove(id)
                }
            }

            persistState()
            isTokenRevoked = false

            // Record the exact time it finished
            lastSyncTime = myRealNow.format(java.time.format.DateTimeFormatter.ofPattern("hh:mm:ss a"))

        } catch (e: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
            if (e.statusCode == 401 && !isTokenRevoked) {
                isTokenRevoked = true
                googleTasksClient.clearTokens()
                googleCalendarClient.clearService()

                mensajeroClient.sendMessage("🚨 ACTION REQUIRED: Google Token Expired", "Please open the Dashboard to renew the oath.")

                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    googleTasksClient.triggerReauth()
                }
            }
        } catch (e: Exception) { logger.error("🚨 Repository Error: ${e.message}") }
    }

    private suspend fun syncTaskToCache(id: String, listId: String, title: String, notes: String?, time: LocalTime, date: LocalDate) {
        cacheMutex.withLock {
            val existing = dailyCache[id]
            if (existing == null) {
                dailyCache[id] = TaskData(id, listId, title, notes, time, dueDate = date)
            } else if (existing.dueTime != time || existing.title != title || existing.description != notes || existing.dueDate != date) {
                dailyCache[id] = existing.copy(
                    title = title, description = notes, dueTime = time, dueDate = date,
                    mensajeroDone = if (existing.dueTime != time || existing.dueDate != date) false else existing.mensajeroDone
                )
            }
        }
    }

    fun getAllCachedTasks(): List<TaskData> = dailyCache.values.toList()

    suspend fun markAsSent(taskId: String) {
        cacheMutex.withLock {
            dailyCache.computeIfPresent(taskId) { _, existing -> existing.copy(mensajeroDone = true) }
        }
        persistState()
    }

    suspend fun completeTaskInGoogle(taskListId: String, taskId: String) {
        if (taskListId == "NONE" || taskListId == "CALENDAR") return
        try {
            googleTasksClient.completeTask(taskListId, taskId)
        } catch (t: Throwable) { logger.error("❌ Google Tasks Completion Error: ${t.message}") }
    }

    suspend fun incrementRetryCount(taskId: String): Int {
        var newCount = 0
        cacheMutex.withLock {
            dailyCache.computeIfPresent(taskId) { _, existing ->
                newCount = existing.retryCount + 1
                existing.copy(retryCount = newCount)
            }
        }
        if (newCount > 0) persistState()
        return newCount
    }
}