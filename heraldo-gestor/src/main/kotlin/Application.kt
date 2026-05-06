package com.netbug94

import com.netbug94.core.appModule
import com.netbug94.reminders.TaskDispatcher
import com.netbug94.dashboard.dashboardRoutes // Make sure this is imported!
import io.ktor.server.application.*
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.routing
import org.koin.dsl.module
import org.koin.ktor.ext.inject
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger
import org.slf4j.LoggerFactory
import org.slf4j.event.Level

fun main(args: Array<String>) {
    io.ktor.server.cio.EngineMain.main(args)
}

private val logger = LoggerFactory.getLogger("com.netbug94.ApplicationKt")

@Suppress("unused")
fun Application.module() {
    logger.info("🧠 Heraldo Gestor booting...")

    // 1. Create a dynamic Koin module to hold Ktor's config
    val configModule = module {
        single<ApplicationConfig> { environment.config }
    }

    // 2. Load Koin with both the config and your app module
    install(Koin) {
        slf4jLogger()
        modules(configModule, appModule)
    }

    val dispatcher by inject<TaskDispatcher>()
    dispatcher.start()

    monitor.subscribe(ApplicationStopping) {
        dispatcher.stop()
    }

    install(CallLogging) {
        level = Level.INFO
    }

    routing {
        staticResources("/static", "static")
    }

    dashboardRoutes()
}