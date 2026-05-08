package com.netbug94.reminders

import com.netbug94.core.TimezoneProvider
import com.netbug94.mensajero.MensajeroClient
import com.netbug94.tasks.TaskRepository
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import java.time.LocalTime
import kotlin.time.Duration.Companion.minutes

private val logger = LoggerFactory.getLogger("com.netbug94.reminders.TaskDispatcher")

class TaskDispatcher(
    private val repository: TaskRepository,
    private val mensajeroClient: MensajeroClient,
    private val timezoneProvider: TimezoneProvider,
    private val alertLeadTimeMinutes: Long,
    private val heartbeatTime: LocalTime?,
    dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())
    private var lastHeartbeatDate: java.time.LocalDate? = null

    fun start() {
        logger.info("Dispatcher: Booting background workers...")

        // Worker 1: Periodically refresh tasks from Google
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

        // Worker 2: Check every minute if it's time to trigger an alert
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

        // 1. Check and send Heartbeat
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

        // 2. Process tasks
        repository.getAllCachedTasks().forEach { task ->
            // Skip tasks that are already done, or somehow don't belong to today
            if (task.mensajeroDone || (task.dueDate != null && task.dueDate != userDate)) {
                return@forEach
            }

            val nowMins = userNow.hour * 60 + userNow.minute
            val dueMins = task.dueTime.hour * 60 + task.dueTime.minute
            val triggerMins = dueMins - alertLeadTimeMinutes.toInt()

            var minutesLate = nowMins - triggerMins

            // Bulletproof midnight wrap-around
            if (minutesLate < -720) minutesLate += 1440
            if (minutesLate > 720) minutesLate -= 1440

            // RULE 2 FIXED: If we are at or past the trigger time, fire it. No expiration window.
            val isTimeToTrigger = minutesLate >= 0

            // If it's more than 2 minutes late, we consider it a recovery task and flag it as past due.
            val isSignificantlyLate = minutesLate > 2

            if (isTimeToTrigger) {
                if (task.retryCount >= 5) {
                    if (task.retryCount == 5) {
                        logger.error("❌ Task '${task.title}' reached max retries (5). Giving up.")
                        repository.incrementRetryCount(task.id) // Increment to 6 so we stop logging
                    }
                    return@forEach
                }

                val displayTitle = if (isSignificantlyLate) {
                    "⚠️ PAST DUE\n⏰ Originally scheduled for: ${task.dueTime}\n\n${task.title}"
                } else {
                    task.title
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