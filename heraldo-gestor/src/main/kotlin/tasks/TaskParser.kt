package com.netbug94.tasks

import java.time.LocalTime
import java.time.format.DateTimeParseException

// Strict format: [HH:mm] Title
private val TASK_TITLE_REGEX = Regex("""^\[(\d{2}:\d{2})]\s*(.+)""")

fun parseTaskTitle(raw: String): Pair<LocalTime?, String> {
    val match = TASK_TITLE_REGEX.matchEntire(raw.trim()) ?: return Pair(null, raw)

    return try {
        val time = LocalTime.parse(match.groupValues[1])
        val title = match.groupValues[2]
        Pair(time, title)
    } catch (_: DateTimeParseException) {
        Pair(null, raw)
    }
}