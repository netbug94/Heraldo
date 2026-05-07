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

val appModule = module {

    single {
        HttpClient(CIO) {
            engine {
                requestTimeout = 15_000
                endpoint {
                    connectTimeout = 15_000
                }
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

    // Builds the Mensajero client using ApplicationConfig
    single {
        val config = get<ApplicationConfig>()
        val settings = get<SettingsRepository>()

        val phone = config.propertyOrNull("app.mensajero.phone")?.getString()
            ?: throw IllegalArgumentException("Missing MENSAJERO_PHONE in config/env")

        val apiKey = config.propertyOrNull("app.mensajero.apiKey")?.getString()
        val baseUrl = config.propertyOrNull("app.mensajero.url")?.getString()?.removeSuffix("/") ?: "http://localhost:3000"
        
        val defaultTemplate = config.propertyOrNull("app.mensajero.template")?.getString() ?: MensajeroClient.DEFAULT_TEMPLATE
        val template = settings.getMensajeroTemplate(defaultTemplate)

        MensajeroClient(get(), phone, apiKey, baseUrl, template, settings)
    }

    single {
        val config = get<ApplicationConfig>()
        val gistUrl = config.propertyOrNull("app.timezone.gistUrl")?.getString()
            ?: throw IllegalArgumentException("Missing GIST_TIMEZONE_URL in config/env")

        TimezoneProvider(get(), gistUrl)
    }

    // Build the APIs
    single { GoogleTasksClient() }
    single { GoogleCalendarClient(get()) }

    // Build the Repository
    single {
        val config = get<ApplicationConfig>()
        val allDayTimeStr = config.propertyOrNull("app.reminders.allDayMensajeroTime")?.getString()
        val allDayTime = allDayTimeStr?.let {
            runCatching { LocalTime.parse(it) }.getOrNull()
        } ?: LocalTime.of(7, 0)

        val summaryTimeStr = config.propertyOrNull("app.reminders.summaryTaskTime")?.getString()
        val summaryTaskTime = summaryTimeStr?.let {
            runCatching { LocalTime.parse(it) }.getOrNull()
        } ?: LocalTime.of(9, 0)

        TaskRepository(get(), get<MensajeroClient>(), get(), get(), allDayTime, summaryTaskTime)
    }

    single {
        val config = get<ApplicationConfig>()
        val leadTimeStr = config.propertyOrNull("app.reminders.leadTimeMinutes")?.getString()
        val leadTime = leadTimeStr?.toLongOrNull() ?: 0L

        val heartbeatTimeStr = config.propertyOrNull("app.reminders.heartbeatTime")?.getString()
        val heartbeatTime = heartbeatTimeStr?.let {
            runCatching { LocalTime.parse(it) }.getOrNull()
        }

        TaskDispatcher(get(), get(), get(), leadTime, heartbeatTime)
    }
}