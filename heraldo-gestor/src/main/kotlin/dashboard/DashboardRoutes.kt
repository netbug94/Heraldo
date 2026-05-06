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
import kotlinx.serialization.Serializable
import org.koin.ktor.ext.inject
import java.time.format.DateTimeFormatter

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
    }
}