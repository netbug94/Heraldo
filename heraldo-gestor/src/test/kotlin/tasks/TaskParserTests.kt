package tasks

import com.netbug94.tasks.parseTaskTitle
import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TaskParserTests {
    @Test
    fun `parses valid time and title correctly`() {
        val (time, title) = parseTaskTitle("[09:30] Morning meeting")
        assertEquals(LocalTime.of(9, 30), time)
        assertEquals("Morning meeting", title)
    }

    @Test
    fun `handles extra whitespace around the input`() {
        val (time, title) = parseTaskTitle("   [14:00]   Buy groceries   ")
        assertEquals(LocalTime.of(14, 0), time)
        assertEquals("Buy groceries", title)
    }

    @Test
    fun `returns null time and raw string if there are no brackets`() {
        val (time, title) = parseTaskTitle("08:15 Workout")
        assertNull(time)
        assertEquals("08:15 Workout", title)
    }

    @Test
    fun `returns null time and raw string if no time is provided`() {
        val (time, title) = parseTaskTitle("Just a regular task")
        assertNull(time)
        assertEquals("Just a regular task", title)
    }

    @Test
    fun `returns null time if time format is missing a digit`() {
        // [9:30] fails because \d{2} expects 09:30
        val (time, title) = parseTaskTitle("[9:30] Morning meeting")
        assertNull(time)
        assertEquals("[9:30] Morning meeting", title)
    }

    @Test
    fun `returns null time for invalid time values that match regex`() {
        val (time, title) = parseTaskTitle("[25:00] Impossible task")
        assertNull(time)
        assertEquals("[25:00] Impossible task", title)
    }

    @Test
    fun `returns null time if the time bracket is not at the beginning`() {
        val (time, title) = parseTaskTitle("Morning meeting [09:30]")
        assertNull(time)
        // Ensure the whole string is preserved as the title
        assertEquals("Morning meeting [09:30]", title)
    }

    @Test
    fun `parses correctly when the title itself contains brackets`() {
        val (time, title) = parseTaskTitle("[15:00] Read [Important] Document")
        assertEquals(LocalTime.of(15, 0), time)
        // Ensure the regex (.+) properly grabs the rest of the string, including other brackets
        assertEquals("Read [Important] Document", title)
    }
}