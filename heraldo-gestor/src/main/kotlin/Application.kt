package com.netbug94

import com.netbug94.auth.authRoutes
import com.netbug94.auth.installAuth
import com.netbug94.core.appModule
import com.netbug94.reminders.TaskDispatcher
import com.netbug94.dashboard.dashboardRoutes
import io.ktor.server.application.*
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.calllogging.*
import io.ktor.server.routing.routing
import io.ktor.server.websocket.*
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

    install(WebSockets) {
        pingPeriod = kotlin.time.Duration.parse("15s")
        timeout = kotlin.time.Duration.parse("15s")
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }

    // ── CRITICAL FIX: The Stateless Cryptographic Seal ────────────
    // Ktor grabs your HERALDO_INTERNAL_TOKEN from the Docker .env to lock the session
    val secretKey = System.getenv("HERALDO_INTERNAL_TOKEN") ?: "fallback_super_secret_change_me_in_prod"

    // Auth: install Sessions plugin with our secret key
    installAuth(secretKey)
    // ──────────────────────────────────────────────────────────────

    authRoutes()

    routing {
        staticResources("/static", "static")
    }

    dashboardRoutes()
}