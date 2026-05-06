package com.netbug94.dashboard

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
    val googleTasksClient by inject<GoogleTasksClient>() // <--- NEW: Inject Google Client

    routing {
        get("/") {
            call.response.header(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")

            val tasks = repository.getAllCachedTasks().sortedBy { it.dueTime }
            val userTime = timezoneProvider.getMyLocalTime()
            val currentZone = userTime.zone.id
            val formattedTime = userTime.format(DateTimeFormatter.ofPattern("HH:mm:ss"))

            // --- NEW: Grab the pending URL if it exists ---
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
                val html = DashboardViews.renderLoadingState("✅ Authorization successful! You can close this window.")
                call.respondText(html, ContentType.Text.Html)
            } catch (e: Exception) {
                val html = DashboardViews.renderLoadingState("❌ Code exchange failed: ${e.message}")
                call.respondText(html, ContentType.Text.Html)
            }
        }

        get("/sync") {
            repository.fetchTodayTasks()
            val html = DashboardViews.renderLoadingState("Syncing Tasks...")
            call.respondText(html, ContentType.Text.Html)
        }

        get("/sync-zone") {
            timezoneProvider.forceRefresh()
            val html = DashboardViews.renderLoadingState("Updating Timezone...")
            call.respondText(html, ContentType.Text.Html)
        }

        post("/sync-env") {
            val request = call.receive<TemplateUpdateRequest>()
            mensajeroClient.updateTemplate(request.template)
            val html = DashboardViews.renderLoadingState("Template Updated!")
            call.respondText(html, ContentType.Text.Html)
        }

        webSocket("/ws") {
            connections += this
            try {
                for (frame in incoming) {
                    // Ignore incoming messages from client
                }
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