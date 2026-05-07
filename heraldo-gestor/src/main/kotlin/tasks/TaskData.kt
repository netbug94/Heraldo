package com.netbug94.tasks

import com.netbug94.core.LocalTimeSerializer
import com.netbug94.core.LocalDateSerializer
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalTime

@Serializable
data class TaskData(
    val id: String,
    val taskListId: String,
    val title: String,
    val description: String?,
    @Serializable(with = LocalTimeSerializer::class)
    val dueTime: LocalTime,
    val mensajeroDone: Boolean = false,
    val retryCount: Int = 0,
    @Serializable(with = LocalDateSerializer::class)
    val dueDate: LocalDate? = null
)