package com.netbug94.dashboard

import com.netbug94.auth.requireAuth
import com.netbug94.core.TimezoneProvider
import com.netbug94.tasks.GoogleTasksClient
import com.netbug94.tasks.TaskRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.channels.consumeEach
import org.koin.ktor.ext.inject
import java.time.format.DateTimeFormatter
import java.util.concurrent.CopyOnWriteArraySet

private val connections = CopyOnWriteArraySet<WebSocketSession>()

fun Application.dashboardRoutes() {

    val repository by inject<TaskRepository>()
    val timezoneProvider by inject<TimezoneProvider>()
    val googleTasksClient by inject<GoogleTasksClient>()

    routing {
        get("/Callback") {
            val code = call.request.queryParameters["code"]
            val error = call.request.queryParameters["error"]
            val state = call.request.queryParameters["state"]

            if (error != null) {
                val html = DashboardViews.renderLoadingState("❌ Authorization failed: $error")
                return@get call.respondText(html, ContentType.Text.Html)
            }

            if (code == null || state == null) {
                val html = DashboardViews.renderLoadingState("❌ Authorization failed: Missing code or state.")
                return@get call.respondText(html, ContentType.Text.Html)
            }

            if (state != googleTasksClient.expectedState) {
                val html = DashboardViews.renderLoadingState("❌ Authorization failed: CSRF state mismatch!")
                return@get call.respondText(html, ContentType.Text.Html)
            }

            try {
                googleTasksClient.exchangeCode(code)
                val html = DashboardViews.renderLoadingState("✅ Royal Seal Accepted! You may close this parchment.")
                call.respondText(html, ContentType.Text.Html)
            } catch (e: Exception) {
                val html = DashboardViews.renderLoadingState("❌ The Seal was rejected: ${e.message}")
                call.respondText(html, ContentType.Text.Html)
            }
        }

        requireAuth {
            get("/") {
                call.response.header(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")

                val tasks = repository.getAllCachedTasks().sortedBy { it.dueTime }
                val userTime = timezoneProvider.getMyLocalTime()
                val currentZone = userTime.zone.id
                val formattedTime = userTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

                val authLink = googleTasksClient.pendingAuthUrl

                val lastSync = repository.lastSyncTime

                val html = DashboardViews.renderIndex(tasks, currentZone, formattedTime, authLink, lastSync)
                call.respondText(html, ContentType.Text.Html)
            }

            get("/sync") {
                repository.fetchTodayTasks()
                val html = DashboardViews.renderLoadingState("Summoning Decrees...")
                call.respondText(html, ContentType.Text.Html)
            }

            get("/sync-zone") {
                timezoneProvider.forceRefresh()
                val html = DashboardViews.renderLoadingState("Aligning Astrolabe...")
                call.respondText(html, ContentType.Text.Html)
            }

            webSocket("/ws") {
                connections += this
                try {
                    incoming.consumeEach { _ -> /* keep-alive */ }
                } catch (_: ClosedReceiveChannelException) {
                } finally {
                    connections -= this
                }
            }
        }

        post("/webhook/mensajero") {
            val config = call.application.environment.config
            val expectedKey = config.propertyOrNull("app.mensajero.apiKey")?.getString()

            val clientKey = call.request.header("x-api-key")

            if (expectedKey != null && clientKey != expectedKey) {
                call.respond(HttpStatusCode.Unauthorized, "The seal was rejected.")
                return@post
            }

            connections.forEach { session ->
                try {
                    session.send(Frame.Text("RELOAD"))
                } catch (_: Exception) {
                    // Ignore dead sessions
                }
            }

            call.respond(HttpStatusCode.OK)
        }
    }
}