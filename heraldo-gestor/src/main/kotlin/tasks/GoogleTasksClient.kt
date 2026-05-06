package com.netbug94.tasks

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.DateTime
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.calendar.CalendarScopes
import com.google.api.services.tasks.Tasks
import com.google.api.services.tasks.TasksScopes
import com.google.api.services.tasks.model.Task
import com.google.api.services.tasks.model.TaskList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.io.InputStreamReader

private const val CONFIG_DIR = "config"
private const val CREDENTIALS_FILE_NAME = "credentials.json"
private const val TOKENS_DIRECTORY_PATH = "tokens"
private const val AUTH_RECEIVER_PORT = 8888
private const val APPLICATION_NAME = "HeraldoGestor"

private val logger = LoggerFactory.getLogger("com.netbug94.tasks.GoogleTasksClient")

class GoogleTasksClient {

    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = NetHttpTransport()

    // --- NEW: Expose the auth URL to the dashboard ---
    @Volatile var pendingAuthUrl: String? = null
        private set

    private var _credential: Credential? = null
    val credential: Credential
        get() {
            if (_credential == null) {
                _credential = buildCredential()
            }
            return _credential!!
        }

    private fun buildCredential(): Credential {
        val configFile = File(CONFIG_DIR, CREDENTIALS_FILE_NAME)

        if (!configFile.exists()) {
            throw kotlinx.io.files.FileNotFoundException(
                "🚨 CONFIG ERROR: '${configFile.absolutePath}' not found!"
            )
        }

        val inputStream = configFile.inputStream()
        val clientSecrets = GoogleClientSecrets.load(jsonFactory, InputStreamReader(inputStream))

        val flow = GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jsonFactory, clientSecrets,
            listOf(TasksScopes.TASKS, CalendarScopes.CALENDAR_READONLY)
        )
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()

        val receiver = LocalServerReceiver.Builder()
            .setHost("0.0.0.0")
            .setPort(AUTH_RECEIVER_PORT)
            .build()

        // --- NEW: Intercept the URL so Docker doesn't crash trying to open a browser ---
        val app = object : AuthorizationCodeInstalledApp(flow, receiver) {
            override fun onAuthorization(authorizationUrl: AuthorizationCodeRequestUrl) {
                val url = authorizationUrl.build()
                pendingAuthUrl = url // Save it for the dashboard to read

                logger.warn("==================================================")
                logger.warn("🚨 GOOGLE AUTH REQUIRED 🚨")
                logger.warn("Please open your Ktor Dashboard or copy this URL:")
                logger.warn(url)
                logger.warn("==================================================")
            }
        }

        val authResult = app.authorize("user")
        pendingAuthUrl = null // Clear the link once successful!
        return authResult
    }

    private var _googleTasksService: Tasks? = null
    private val googleTasksService: Tasks
        get() {
            if (_googleTasksService == null) {
                _googleTasksService = Tasks.Builder(httpTransport, jsonFactory, credential)
                    .setApplicationName(APPLICATION_NAME)
                    .build()
            }
            return _googleTasksService!!
        }

    fun clearTokens() {
        logger.warn("🧹 Clearing expired Google tokens...")
        val tokenDir = File(TOKENS_DIRECTORY_PATH)
        if (tokenDir.exists()) {
            tokenDir.deleteRecursively()
        }
        _credential = null
        _googleTasksService = null
        pendingAuthUrl = null
    }

    suspend fun triggerReauth() = withContext(Dispatchers.IO) {
        try {
            credential // This getter blocks and starts the LocalServerReceiver
        } catch (e: Exception) {
            logger.error("Auth flow interrupted: ${e.message}")
        }
    }

    suspend fun getTaskLists(): List<TaskList> = withContext(Dispatchers.IO) {
        googleTasksService.tasklists().list().execute().items ?: emptyList()
    }

    suspend fun getTasks(taskListId: String): List<Task> = withContext(Dispatchers.IO) {
        googleTasksService.tasks().list(taskListId)
            .setShowCompleted(false)
            .setShowHidden(false)
            .execute()
            .items ?: emptyList()
    }

    suspend fun completeTask(taskListId: String, taskId: String): Unit = withContext(Dispatchers.IO) {
        val task = googleTasksService.tasks().get(taskListId, taskId).execute()
        task.status = "completed"
        task.completed = DateTime(System.currentTimeMillis()).toStringRfc3339()

        googleTasksService.tasks().update(taskListId, taskId, task).execute()
    }
}