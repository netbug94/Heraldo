package com.netbug94.tasks

import com.google.api.client.auth.oauth2.Credential
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

private const val TOKENS_DIRECTORY_PATH = "tokens"
private const val APPLICATION_NAME = "HeraldoGestor"

private val logger = LoggerFactory.getLogger("com.netbug94.tasks.GoogleTasksClient")

class GoogleTasksClient {

    private val jsonFactory = GsonFactory.getDefaultInstance()
    private val httpTransport = NetHttpTransport()

    // --- NEW: Expose the auth URL to the dashboard ---
    @Volatile var pendingAuthUrl: String? = null
        private set

    @Volatile var expectedState: String? = null
        private set

    private var _credential: Credential? = null
    val credential: Credential
        get() {
            if (_credential == null) {
                _credential = buildCredential()
            }
            return _credential!!
        }

    private val flow: GoogleAuthorizationCodeFlow by lazy {
        val jsonStr = System.getenv("GOOGLE_CREDENTIALS_JSON")
        if (jsonStr.isNullOrBlank()) {
            throw IllegalStateException("🚨 Missing GOOGLE_CREDENTIALS_JSON environment variable. Please set it in your .env file.")
        }
        val clientSecrets = GoogleClientSecrets.load(jsonFactory, java.io.StringReader(jsonStr))

        GoogleAuthorizationCodeFlow.Builder(
            httpTransport, jsonFactory, clientSecrets,
            listOf(TasksScopes.TASKS, CalendarScopes.CALENDAR_READONLY)
        )
            .setDataStoreFactory(FileDataStoreFactory(File(TOKENS_DIRECTORY_PATH)))
            .setAccessType("offline")
            .build()
    }

    private fun buildCredential(): Credential {
        val cred = flow.loadCredential("user")
        if (cred != null && (cred.refreshToken != null || cred.expiresInSeconds == null || cred.expiresInSeconds > 60)) {
            pendingAuthUrl = null
            return cred
        }

        // We need auth! Generate CSRF state and Auth URL.
        val state = java.util.UUID.randomUUID().toString()
        expectedState = state

        val externalPort = System.getenv("EXTERNAL_PORT") ?: "8080"
        val url = flow.newAuthorizationUrl()
            .setRedirectUri("http://localhost:$externalPort/Callback")
            .setState(state)
            .build()
            
        pendingAuthUrl = url
        
        logger.warn("==================================================")
        logger.warn("🚨 GOOGLE AUTH REQUIRED 🚨")
        logger.warn("Please open your Dashboard or copy this URL:")
        logger.warn(url)
        logger.warn("==================================================")
        
        throw IllegalStateException("Google Auth Required. Check the Dashboard.")
    }

    fun exchangeCode(code: String) {
        val externalPort = System.getenv("EXTERNAL_PORT") ?: "8080"
        val response = flow.newTokenRequest(code)
            .setRedirectUri("http://localhost:$externalPort/Callback")
            .execute()
        _credential = flow.createAndStoreCredential(response, "user")
        pendingAuthUrl = null
        expectedState = null
        logger.info("✅ Google Auth Successful!")
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