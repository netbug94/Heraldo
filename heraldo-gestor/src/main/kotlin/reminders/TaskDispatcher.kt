package com.netbug94.reminders

import com.netbug94.core.TimezoneProvider
import com.netbug94.core.logger
import com.netbug94.mensajero.MensajeroClient
import com.netbug94.tasks.TaskRepository
import kotlinx.coroutines.*
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes

class TaskDispatcher(
    private val repository: TaskRepository,
    private val mensajeroClient: MensajeroClient,
    private val timezoneProvider: TimezoneProvider,
    private val alertLeadTimeMinutes: Long,
    private val heartbeatTime: LocalTime?,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val logger by logger()

    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private var lastHeartbeatDate: java.time.LocalDate? = null

    fun start() {
        logger.info("Dispatcher: Booting background workers...")

        scope.launch {
            while (isActive) {
                try {
                    repository.fetchTodayTasks()
                } catch (e: Exception) {
                    logger.error("🚨 Background Worker Error (Fetch Tasks): ${e.message}", e)
                }
                delay(30.minutes)
            }
        }

        scope.launch {
            while (isActive) {
                try {
                    checkAlarms()
                } catch (e: Exception) {
                    logger.error("🚨 Background Worker Error (Check Alarms): ${e.message}", e)
                }
                delay(1.minutes)
            }
        }
    }

    fun stop() {
        logger.info("Dispatcher: Shutting down background workers gracefully...")
        scope.cancel()
    }

    internal suspend fun checkAlarms() {
        if (!mensajeroClient.isHealthy()) {
            logger.warn("🛑 Heraldo Mensajero is offline. Pausing task delivery until it recovers.")
            return
        }

        val userZDT = timezoneProvider.getMyLocalTime()
        val userNow = userZDT.toLocalTime().withSecond(0).withNano(0)
        val userDate = userZDT.toLocalDate()

        if (heartbeatTime != null && userNow == heartbeatTime && lastHeartbeatDate != userDate) {
            logger.info("🫀 Triggering Daily System Heartbeat")
            val success = mensajeroClient.sendMessage(
                "🫀 System Heartbeat",
                "Heraldo is online and monitoring your tasks."
            )
            if (success) {
                lastHeartbeatDate = userDate
            } else {
                logger.warn("⚠️ Failed to send heartbeat, will retry next minute.")
            }
        }

        repository.getAllCachedTasks().forEach { task ->
            if (task.mensajeroDone || (task.dueDate != null && task.dueDate != userDate)) {
                return@forEach
            }

            val userLocalDateTime = userZDT.toLocalDateTime().withSecond(0).withNano(0)
            val targetDate = task.dueDate ?: userDate
            var triggerDateTime = targetDate.atTime(task.dueTime).minusMinutes(alertLeadTimeMinutes)

            if (task.dueDate == null && triggerDateTime.isAfter(userLocalDateTime)) {
                val yesterdayTrigger = triggerDateTime.minusDays(1)

                if (java.time.Duration.between(yesterdayTrigger, userLocalDateTime).toHours() < 4) {
                    triggerDateTime = yesterdayTrigger
                }
            }

            val minutesLate = java.time.Duration.between(triggerDateTime, userLocalDateTime).toMinutes().toInt()

            val isTimeToTrigger = minutesLate >= 0
            val isSignificantlyLate = minutesLate > 2

            if (isTimeToTrigger) {
                if (task.retryCount >= 5) {
                    if (task.retryCount == 5) {
                        logger.error("❌ Task '${task.title}' reached max retries (5). Giving up.")
                        repository.incrementRetryCount(task.id)
                    }
                    return@forEach
                }

                // Prevent the "PAST DUE" text from sticking to the Type 3 Summary task
                val displayTitle = if (isSignificantlyLate && !task.id.startsWith("summary_")) {
                    "📜❗PAST DUE\n⏳ Scheduled: ${task.dueTime}\n\n${task.title}"
                } else {
                    "📜 ${task.title}"
                }

                logger.info("🚀 TRIGGER: Sending '${task.title}' (Attempt ${task.retryCount + 1})")
                val success = mensajeroClient.sendMessage(displayTitle, task.description)

                if (success) {
                    repository.markAsSent(task.id)
                    repository.completeTaskInGoogle(task.taskListId, task.id)
                } else {
                    repository.incrementRetryCount(task.id)
                    logger.warn("⚠️ Mensajero delivery failed. Retrying in 1 minute.")
                }
            }
        }
    }
}