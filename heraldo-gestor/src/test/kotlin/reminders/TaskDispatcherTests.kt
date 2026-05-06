package reminders

import com.netbug94.core.TimezoneProvider
import com.netbug94.tasks.TaskData
import com.netbug94.tasks.TaskRepository
import com.netbug94.mensajero.MensajeroClient
import com.netbug94.reminders.TaskDispatcher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test

class TaskDispatcherTests {

    private lateinit var dispatcher: TaskDispatcher
    private lateinit var mockRepository: TaskRepository
    private lateinit var mockTimezoneProvider: TimezoneProvider
    private lateinit var mockMensajeroClient: MensajeroClient

    private val testZoneId = ZoneId.of("America/Mexico_City")

    @BeforeTest
    fun setUp() {
        mockRepository = mockk(relaxed = true)
        mockTimezoneProvider = mockk()
        mockMensajeroClient = mockk()

        // Mock current time to 12:00 PM for all tests
        val mockTime = ZonedDateTime.of(LocalDate.now(), LocalTime.of(12, 0), testZoneId)
        coEvery { mockTimezoneProvider.getMyLocalTime() } returns mockTime
        coEvery { mockMensajeroClient.isHealthy() } returns true

        // Setting lead time to 5 minutes
        // (Trigger will happen at 11:55 for a 12:00 task)
        dispatcher = TaskDispatcher(mockRepository, mockMensajeroClient, mockTimezoneProvider, 5L, LocalTime.of(8, 0))
    }

    @Test
    fun `when task is on time, trigger normally`() = runTest {
        val targetTask = TaskData(
            id = "task-on-time",
            taskListId = "list-1",
            title = "Call doctor",
            description = "Ask about results",
            dueTime = LocalTime.of(12, 5) // 12:05 - 5min = 12:00 (Matches current time)
        )

        every { mockRepository.getAllCachedTasks() } returns listOf(targetTask)
        coEvery { mockMensajeroClient.sendMessage(any(), any()) } returns true

        dispatcher.checkAlarms()

        coVerify(exactly = 1) {
            mockMensajeroClient.sendMessage("Call doctor", "Ask about results")
        }
    }

    @Test
    fun `when task is late, trigger with multi-line PAST DUE format`() = runTest {
        val lateTask = TaskData(
            id = "task-late",
            taskListId = "list-1",
            title = "Morning Exercise",
            description = "Don't forget water",
            dueTime = LocalTime.of(10, 0) // Well outside the 10-minute window
        )

        every { mockRepository.getAllCachedTasks() } returns listOf(lateTask)
        coEvery { mockMensajeroClient.sendMessage(any(), any()) } returns true

        dispatcher.checkAlarms()

        // Verify the exact string structure the Relay will receive
        val expectedTitle = "⚠️ PAST DUE\n⏰ Originally scheduled for: 10:00\n\nMorning Exercise"
        val expectedDesc = "Don't forget water"

        coVerify(exactly = 1) {
            mockMensajeroClient.sendMessage(expectedTitle, expectedDesc)
        }
    }

    @Test
    fun `when Relay is offline, skip processing`() = runTest {
        coEvery { mockMensajeroClient.isHealthy() } returns false

        dispatcher.checkAlarms()

        // Should not even attempt to send it if health check fails
        coVerify(exactly = 0) { mockMensajeroClient.sendMessage(any(), any()) }
    }

    @Test
    fun `when delivery succeeds, mark as sent and complete in Google`() = runTest {
        val targetTask = TaskData(
            id = "task-1", taskListId = "list-1", title = "Test", description = "Desc",
            dueTime = LocalTime.of(12, 5) // Triggers at 12:00
        )

        every { mockRepository.getAllCachedTasks() } returns listOf(targetTask)
        coEvery { mockMensajeroClient.sendMessage(any(), any()) } returns true

        dispatcher.checkAlarms()

        coVerify(exactly = 1) { mockRepository.markAsSent("task-1") }
        coVerify(exactly = 1) { mockRepository.completeTaskInGoogle("list-1", "task-1") }
    }

    @Test
    fun `when delivery fails, increment retry count and do not complete`() = runTest {
        val targetTask = TaskData(
            id = "task-fail", taskListId = "list-1", title = "Test", description = "Desc",
            dueTime = LocalTime.of(12, 5)
        )

        every { mockRepository.getAllCachedTasks() } returns listOf(targetTask)
        // Simulate a network failure on the Relay side
        coEvery { mockMensajeroClient.sendMessage(any(), any()) } returns false

        dispatcher.checkAlarms()

        coVerify(exactly = 1) { mockRepository.incrementRetryCount("task-fail") }
        coVerify(exactly = 0) { mockRepository.markAsSent(any()) }
        coVerify(exactly = 0) { mockRepository.completeTaskInGoogle(any(), any()) }
    }

    @Test
    fun `when task is already marked sent, ignore it`() = runTest {
        val sentTask = TaskData(
            id = "task-sent", taskListId = "list-1", title = "Test", description = "Desc",
            dueTime = LocalTime.of(12, 5),
            mensajeroDone = true // Already sent
        )

        every { mockRepository.getAllCachedTasks() } returns listOf(sentTask)

        dispatcher.checkAlarms()

        coVerify(exactly = 0) { mockMensajeroClient.sendMessage(any(), any()) }
    }

    @Test
    fun `when task is far in the future, do not trigger`() = runTest {
        val futureTask = TaskData(
            id = "task-future", taskListId = "list-1", title = "Dinner", description = "Cook",
            dueTime = LocalTime.of(18, 0) // 6:00 PM (way past our 12:00 PM mock time)
        )

        every { mockRepository.getAllCachedTasks() } returns listOf(futureTask)

        dispatcher.checkAlarms()

        coVerify(exactly = 0) { mockMensajeroClient.sendMessage(any(), any()) }
    }
}