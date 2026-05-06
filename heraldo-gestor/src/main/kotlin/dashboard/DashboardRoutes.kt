package com.netbug94.dashboard

import com.netbug94.auth.requireAuth
import com.netbug94.core.TimezoneProvider
import com.netbug94.mensajero.MensajeroClient
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
import java.util.Collections
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.time.format.DateTimeFormatter

private val connections = Collections.synchronizedSet(mutableSetOf<WebSocketSession>())

@Serializable
data class TemplateUpdateRequest(val template: String)

fun Application.dashboardRoutes() {

    val repository by inject<TaskRepository>()
    val timezoneProvider by inject<TimezoneProvider>()
    val mensajeroClient by inject<MensajeroClient>()
    val googleTasksClient by inject<GoogleTasksClient>()

    routing {
        requireAuth {
            get("/") {
                call.response.header(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")

                val tasks = repository.getAllCachedTasks().sortedBy { it.dueTime }
                val userTime = timezoneProvider.getMyLocalTime()
                val currentZone = userTime.zone.id
                val formattedTime = userTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

                val authLink = googleTasksClient.pendingAuthUrl

                val html = DashboardViews.renderIndex(tasks, currentZone, formattedTime, authLink)
                call.respondText(html, ContentType.Text.Html)
            }

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

            post("/sync-env") {
                val request = call.receive<TemplateUpdateRequest>()
                mensajeroClient.updateTemplate(request.template)
                val html = DashboardViews.renderLoadingState("The Ravens have been Dispatched!")
                call.respondText(html, ContentType.Text.Html)
            }

            webSocket("/ws") {
                connections += this
                try {
                    incoming.consumeEach { _ -> /* keep-alive */ }
                } catch (_: ClosedReceiveChannelException) {
                    // Ignore disconnect
                } finally {
                    connections -= this
                }
            }

            post("/webhook/mensajero") {
                connections.forEach {
                    try {
                        it.send("RELOAD")
                    } catch (_: Exception) {
                        // Ignore dead sockets
                    }
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}