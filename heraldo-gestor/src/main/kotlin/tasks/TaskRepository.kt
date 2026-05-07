package com.netbug94.tasks

import com.netbug94.core.TimezoneProvider
import com.netbug94.mensajero.MensajeroClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.ConcurrentHashMap

private val logger = LoggerFactory.getLogger("com.netbug94.tasks.TaskRepository")

class TaskRepository(
    private val timezoneProvider: TimezoneProvider,
    private val mensajeroClient: MensajeroClient,
    private val googleTasksClient: GoogleTasksClient,
    private val googleCalendarClient: GoogleCalendarClient,
    private val allDayMensajeroTime: LocalTime,
    private val summaryTaskTime: LocalTime
) {

    private val dailyCache = ConcurrentHashMap<String, TaskData>()
    private val cacheFile = File("./data/cache.json")

    @Volatile
    private var isTokenRevoked = false

    // Kotlin standard for coroutine concurrency
    private val cacheMutex = Mutex()

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
                    logger.info("💾 Loaded ${dailyCache.size} tasks from local disk storage.")
                }
            } else {
                cacheFile.parentFile?.mkdirs()
            }
        } catch (e: Exception) {
            logger.error("🚨 Failed to load cache from disk: ${e.message}", e)
        }
    }

    private suspend fun persistState() {
        try {
            // Lock the state to serialize it safely
            val jsonStr = cacheMutex.withLock { Json.encodeToString(dailyCache.toMap()) }
            // Write to disk on the IO thread so Ktor isn't frozen
            withContext(Dispatchers.IO) {
                cacheFile.writeText(jsonStr)
            }
        } catch (e: Exception) {
            logger.error("🚨 Failed to persist cache to disk: ${e.message}", e)
        }
    }

    suspend fun fetchTodayTasks() {
        logger.info("Repository: Fetching tasks from Google Tasks and Calendar...")
        try {
            val myRealNow = timezoneProvider.getMyLocalTime()
            val myRealToday = myRealNow.toLocalDate()

            // === 0. MISSED TASKS LOGIC (From previous days) ===
            cacheMutex.withLock {
                val missedTasks = dailyCache.values.filter { 
                    !it.mensajeroDone && !it.id.startsWith("summary_") && it.dueDate != null && it.dueDate < myRealToday 
                }

                if (missedTasks.isNotEmpty()) {
                    val count = missedTasks.size
                    val listStr = missedTasks.joinToString("\n") { "• ${it.title}" }
                    val summaryId = "summary_$myRealToday"

                    dailyCache[summaryId] = TaskData(
                        id = summaryId,
                        taskListId = "NONE",
                        title = "⚠️ $count Missed Tasks from Previous Days",
                        description = "Server was down! Check Tasks site to clear them:\n$listStr",
                        dueTime = summaryTaskTime,
                        mensajeroDone = false,
                        dueDate = myRealToday
                    )
                    logger.info("📝 Created daily summary task for $count missed tasks, scheduled for $summaryTaskTime.")
                }
                
                // Cleanup any stale tasks from previous days (missed ones were just summarized, others are done)
                val staleIds = dailyCache.values.filter { 
                    it.dueDate != null && it.dueDate < myRealToday && !it.id.startsWith("summary_")
                }.map { it.id }
                
                staleIds.forEach { dailyCache.remove(it) }
            }

            val taskLists = googleTasksClient.getTaskLists()
            val validTaskIdsForToday = mutableSetOf<String>()

            // === 1. GOOGLE TASKS LOGIC ===
            for (taskList in taskLists) {
                val tasks = googleTasksClient.getTasks(taskList.id)

                tasks.forEach { task ->
                    val rawTitle = task.title ?: return@forEach
                    val dueString = task.due ?: return@forEach

                    val dueDateLocal = runCatching {
                        LocalDate.parse(dueString.take(10))
                    }.getOrNull()

                    if (dueDateLocal == null || dueDateLocal != myRealToday) return@forEach

                    val (parsedTime, cleanTitle) = parseTaskTitle(rawTitle)
                    if (parsedTime == null) return@forEach

                    validTaskIdsForToday.add(task.id)
                    syncTaskToCache(task.id, taskList.id, cleanTitle, task.notes, parsedTime, myRealToday)
                }
            }

            // === 2. GOOGLE CALENDAR LOGIC ===
            try {
                val startOfDay = myRealToday.atStartOfDay(myRealNow.zone)
                val endOfDay = startOfDay.plusDays(1).minusNanos(1)

                val calendarEvents = googleCalendarClient.getTodayEvents(startOfDay, endOfDay)

                for (event in calendarEvents) {
                    val eventLocalTime: LocalTime
                    val titlePrefix: String

                    if (event.start?.dateTime != null) {
                        val eventTimeMs = event.start.dateTime.value
                        eventLocalTime = java.time.Instant.ofEpochMilli(eventTimeMs)
                            .atZone(myRealNow.zone)
                            .toLocalTime()
                        titlePrefix = "🗓️"
                    } else if (event.start?.date != null) {
                        eventLocalTime = allDayMensajeroTime
                        titlePrefix = "🌅 [All Day]"
                    } else {
                        continue
                    }

                    val cleanTitle = "$titlePrefix ${event.summary ?: "Busy"}"
                    val eventId = "cal_${event.id}"

                    validTaskIdsForToday.add(eventId)
                    syncTaskToCache(eventId, "CALENDAR", cleanTitle, event.description, eventLocalTime, myRealToday)
                }
            } catch (e: Exception) {
                logger.error("🚨 Calendar Sync Error: ${e.message}", e)
            }

            // === 3. CLEANUP LOGIC ===
            cacheMutex.withLock {
                val deletedIds = dailyCache.keys - validTaskIdsForToday
                deletedIds.forEach { id ->
                    if (id.startsWith("summary_")) return@forEach

                    val task = dailyCache[id]
                    if (task?.mensajeroDone != true) {
                        dailyCache.remove(id)
                        logger.debug("DEBUG: Removed -> ${task?.title}")
                    }
                }
            }

            logger.info("Repository: Tracking ${dailyCache.size} active tasks/events.")
            persistState()
            
            // If we successfully fetched, the token is good
            isTokenRevoked = false

        } catch (e: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
            if (e.statusCode == 401 && !isTokenRevoked) {
                logger.error("🚨 GOOGLE TOKEN EXPIRED OR REVOKED!")
                isTokenRevoked = true
                
                // Clear the cached services
                googleTasksClient.clearTokens()
                googleCalendarClient.clearService()

                // Send the WhatsApp Alert
                mensajeroClient.sendMessage(
                    "🚨 ACTION REQUIRED: Google Token Expired",
                    "Your Google API Token has been revoked or expired.\n\nPlease open your Heraldo Ktor Dashboard immediately to re-authenticate and resume automation."
                )

                // Trigger the re-auth flow in the background so the dashboard URL generates
                kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
                    googleTasksClient.triggerReauth()
                }
            } else if (e.statusCode != 401) {
                logger.error("🚨 Google API Error: ${e.message}")
            }
        } catch (e: Exception) {
            logger.error("🚨 Repository Error: ${e.message}", e)
        }
    }

    private suspend fun syncTaskToCache(id: String, listId: String, title: String, notes: String?, time: LocalTime, date: LocalDate) {
        cacheMutex.withLock {
            val existing = dailyCache[id]
            if (existing == null) {
                dailyCache[id] = TaskData(id, listId, title, notes, time, dueDate = date)
                logger.debug("DEBUG: Added -> [{}] {}", time, title)
            } else if (existing.dueTime != time || existing.title != title || existing.description != notes || existing.dueDate != date) {
                dailyCache[id] = existing.copy(
                    title = title,
                    description = notes,
                    dueTime = time,
                    dueDate = date,
                    mensajeroDone = if (existing.dueTime != time || existing.dueDate != date) false else existing.mensajeroDone
                )
                logger.debug("DEBUG: Updated -> [{}] {}", time, title)
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
            logger.info("⏳ Completing task '$taskId' in Google...")
            googleTasksClient.completeTask(taskListId, taskId)
            logger.info("✅ Google Tasks: Marked '$taskId' as completed.")
        } catch (t: Throwable) {
            logger.error("❌ FATAL Google Tasks Error: ${t.javaClass.simpleName} - ${t.message}", t)
        }
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