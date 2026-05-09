package com.netbug94.core

import com.netbug94.mensajero.MensajeroClient
import com.netbug94.reminders.TaskDispatcher
import com.netbug94.tasks.GoogleCalendarClient
import com.netbug94.tasks.GoogleTasksClient
import com.netbug94.tasks.TaskRepository
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import kotlinx.serialization.json.Json
import org.koin.dsl.module
import java.time.LocalTime

private object Defaults {
    val ALL_DAY_TIME: LocalTime = "12:00".parseTime()!!
    val SUMMARY_TASK_TIME: LocalTime = "12:00".parseTime()!!
    const val LEAD_TIME_MINUTES = 0L
    val HEARTBEAT_TIME: LocalTime = "12:00".parseTime()!!
}

val appModule = module {

    single {
        HttpClient(CIO) {
            engine {
                requestTimeout = 15_000
                endpoint { connectTimeout = 15_000 }
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    encodeDefaults = true
                })
            }
        }
    }

    single { SettingsRepository() }

    single {
        val config = get<ApplicationConfig>()
        val settings = get<SettingsRepository>()

        val phone = config.propertyOrNull("app.mensajero.phone")?.getString()
            ?: throw IllegalArgumentException("Missing MENSAJERO_PHONE in config/env")

        val apiKey = config.propertyOrNull("app.mensajero.apiKey")?.getString()
        val baseUrl = config.propertyOrNull("app.mensajero.url")?.getString()?.removeSuffix("/") ?: "http://localhost:3000"

        val defaultTemplate = config.propertyOrNull("app.mensajero.template")?.getString() ?: MensajeroClient.DEFAULT_TEMPLATE

        MensajeroClient(get(), phone, apiKey, baseUrl, settings, defaultTemplate)
    }

    single {
        val config = get<ApplicationConfig>()
        val gistUrl = config.propertyOrNull("app.timezone.gistUrl")?.getString()
            ?: throw IllegalArgumentException("Missing GIST_TIMEZONE_URL in config/env")

        TimezoneProvider(get(), gistUrl)
    }

    single { GoogleTasksClient() }
    single { GoogleCalendarClient(get()) }

    // Build the Repository
    single {
        val config = get<ApplicationConfig>()

        val allDayTime = config.propertyOrNull("app.reminders.allDayMensajeroTime")
            ?.getString()?.parseTime() ?: Defaults.ALL_DAY_TIME

        val summaryTaskTime = config.propertyOrNull("app.reminders.summaryTaskTime")
            ?.getString()?.parseTime() ?: Defaults.SUMMARY_TASK_TIME

        TaskRepository(get(), get<MensajeroClient>(), get(), get(), allDayTime, summaryTaskTime)
    }

    // Build the Dispatcher
    single {
        val config = get<ApplicationConfig>()

        val leadTime = config.propertyOrNull("app.reminders.leadTimeMinutes")
            ?.getString()?.toLongOrNull() ?: Defaults.LEAD_TIME_MINUTES

        val heartbeatTime = config.propertyOrNull("app.reminders.heartbeatTime")
            ?.getString()?.parseTime() ?: Defaults.HEARTBEAT_TIME

        TaskDispatcher(get(), get(), get(), leadTime, heartbeatTime)
    }
}

private fun String.parseTime(): LocalTime? = runCatching { LocalTime.parse(this) }.getOrNull()