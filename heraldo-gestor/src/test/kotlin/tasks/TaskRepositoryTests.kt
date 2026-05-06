package tasks

import com.google.api.client.util.DateTime
import com.google.api.services.calendar.model.Event
import com.google.api.services.calendar.model.EventDateTime
import com.google.api.services.tasks.model.Task
import com.google.api.services.tasks.model.TaskList
import com.netbug94.core.TimezoneProvider
import com.netbug94.tasks.GoogleCalendarClient
import com.netbug94.tasks.GoogleTasksClient
import com.netbug94.tasks.TaskRepository
import com.netbug94.mensajero.MensajeroClient
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.http.HttpHeaders
import com.google.api.client.http.HttpResponseException
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.*

class TaskRepositoryTests {

    private lateinit var repository: TaskRepository
    private lateinit var mockGoogleClient: GoogleTasksClient
    private lateinit var mockCalendarClient: GoogleCalendarClient
    private lateinit var mockMensajeroClient: MensajeroClient
    private lateinit var mockTimezoneProvider: TimezoneProvider
    private val testZoneId = ZoneId.of("America/Mexico_City")
    private lateinit var mockedCurrentTime: ZonedDateTime
    private val testAllDayTime = LocalTime.of(8, 0) // 8:00 AM

    @BeforeTest
    fun setUp() {
        mockGoogleClient = mockk(relaxed = true)
        mockCalendarClient = mockk(relaxed = true)
        mockMensajeroClient = mockk(relaxed = true)
        mockTimezoneProvider = mockk(relaxed = true)

        val today = LocalDate.of(2025, 4, 10)
        mockedCurrentTime = ZonedDateTime.of(today.atTime(12, 0), testZoneId)
        coEvery { mockTimezoneProvider.getMyLocalTime() } answers { mockedCurrentTime }

        // Default empty responses to prevent errors if a test doesn't mock them
        coEvery { mockGoogleClient.getTaskLists() } returns emptyList()
        coEvery { mockCalendarClient.getTodayEvents(any(), any()) } returns emptyList()

        repository = TaskRepository(mockTimezoneProvider, mockMensajeroClient, mockGoogleClient, mockCalendarClient, testAllDayTime, LocalTime.of(9, 0))
    }

    @AfterTest
    fun tearDown() {
        // Clean up the disk cache created by persistState() during tests
        File("./data/cache.json").delete()
        File("./data").deleteRecursively()
    }

    @Test
    fun `when fetchTodayTasks is called, then valid task is added to cache`() = runTest {
        val taskList = TaskList().apply { id = "list-1" }
        val task = Task().apply {
            id = "task-1"
            title = "[14:30] Check emails"
            due = "2025-04-10T00:00:00.000Z"
        }
        coEvery { mockGoogleClient.getTaskLists() } returns listOf(taskList)
        coEvery { mockGoogleClient.getTasks("list-1") } returns listOf(task)

        repository.fetchTodayTasks()

        val cachedTasks = repository.getAllCachedTasks()
        assertEquals(1, cachedTasks.size)
        assertEquals("Check emails", cachedTasks[0].title)
        assertEquals(LocalTime.of(14, 30), cachedTasks[0].dueTime)
    }

    @Test
    fun `fetches calendar events and adds them to cache with correct prefixes`() = runTest {
        // Create a timed event at 2:00 PM
        val eventTimeMs = mockedCurrentTime.withHour(14).withMinute(0).toInstant().toEpochMilli()
        val timedEvent = Event().apply {
            id = "cal-event-1"
            summary = "Dentist Appointment"
            start = EventDateTime().setDateTime(DateTime(eventTimeMs))
        }

        // Create an all-day event
        val allDayEvent = Event().apply {
            id = "cal-event-2"
            summary = "Mom's Birthday"
            start = EventDateTime().setDate(DateTime("2025-04-10"))
        }

        coEvery { mockCalendarClient.getTodayEvents(any(), any()) } returns listOf(timedEvent, allDayEvent)

        repository.fetchTodayTasks()

        val cached = repository.getAllCachedTasks()
        assertEquals(2, cached.size)

        // Verify Timed Event
        val cachedTimed = cached.first { it.id == "cal_cal-event-1" }
        assertEquals("🗓️ Dentist Appointment", cachedTimed.title)
        assertEquals(LocalTime.of(14, 0), cachedTimed.dueTime)

        // Verify All Day Event
        val cachedAllDay = cached.first { it.id == "cal_cal-event-2" }
        assertEquals("🌅 [All Day] Mom's Birthday", cachedAllDay.title)
        assertEquals(testAllDayTime, cachedAllDay.dueTime)
    }

    @Test
    fun `when date rolls over and there are missed tasks, then a summary task is injected`() = runTest {
        val taskList = TaskList().apply { id = "list-1" }
        val missedTask = Task().apply {
            id = "task-1"
            title = "[14:30] Buy groceries"
            due = "2025-04-10T00:00:00.000Z"
        }
        coEvery { mockGoogleClient.getTaskLists() } returns listOf(taskList)
        coEvery { mockGoogleClient.getTasks("list-1") } returns listOf(missedTask)

        repository.fetchTodayTasks()

        // Rollover to next day
        val tomorrow = LocalDate.of(2025, 4, 11)
        mockedCurrentTime = ZonedDateTime.of(tomorrow.atTime(8, 0), testZoneId)
        coEvery { mockTimezoneProvider.getMyLocalTime() } answers { mockedCurrentTime }

        coEvery { mockGoogleClient.getTasks("list-1") } returns emptyList()

        repository.fetchTodayTasks()

        val cachedTasks = repository.getAllCachedTasks()
        val summaryTask = cachedTasks.first { it.id.startsWith("summary_") }
        assertEquals("⚠️ 1 Missed Tasks from Yesterday", summaryTask.title)
        assertEquals(false, summaryTask.mensajeroDone)
    }

    @Test
    fun `when date rolls over but all tasks were sent, then no summary task is created`() = runTest {
        val taskList = TaskList().apply { id = "list-1" }
        val completedTask = Task().apply {
            id = "task-1"
            title = "[14:30] Call Mom"
            due = "2025-04-10T00:00:00.000Z"
        }
        coEvery { mockGoogleClient.getTaskLists() } returns listOf(taskList)
        coEvery { mockGoogleClient.getTasks("list-1") } returns listOf(completedTask)

        repository.fetchTodayTasks()
        repository.markAsSent("task-1") // Mark as sent via Mensajero

        val tomorrow = LocalDate.of(2025, 4, 11)
        mockedCurrentTime = ZonedDateTime.of(tomorrow.atTime(8, 0), testZoneId)
        coEvery { mockTimezoneProvider.getMyLocalTime() } answers { mockedCurrentTime }

        coEvery { mockGoogleClient.getTasks("list-1") } returns emptyList()

        repository.fetchTodayTasks()

        assertTrue(repository.getAllCachedTasks().isEmpty(), "No summary should be created")
    }

    @Test
    fun `completeTaskInGoogle ignores Calendar and Summary events`() = runTest {
        repository.completeTaskInGoogle("CALENDAR", "cal_123")
        repository.completeTaskInGoogle("NONE", "summary_123")

        // Prove that the Google Tasks client was never called
        coVerify(exactly = 0) { mockGoogleClient.completeTask(any(), any()) }
    }

    @Test
    fun `when token expires (401), alerts via Mensajero and clears tokens`() = runTest {
        val httpException = HttpResponseException.Builder(401, "Unauthorized", HttpHeaders()).build()
        val exception = GoogleJsonResponseException(httpException, null)
        
        coEvery { mockGoogleClient.getTaskLists() } throws exception
        coEvery { mockMensajeroClient.sendMessage(any(), any()) } returns true
        
        repository.fetchTodayTasks()
        
        io.mockk.coVerify(exactly = 1) { mockGoogleClient.clearTokens() }
        io.mockk.coVerify(exactly = 1) { mockCalendarClient.clearService() }
        io.mockk.coVerify(exactly = 1) { mockMensajeroClient.sendMessage(any(), any()) }
        
        // Second call should not spam WhatsApp because isTokenRevoked is true
        repository.fetchTodayTasks()
        io.mockk.coVerify(exactly = 1) { mockMensajeroClient.sendMessage(any(), any()) }
    }
}