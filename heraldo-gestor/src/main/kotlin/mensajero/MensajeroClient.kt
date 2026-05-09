package com.netbug94.mensajero

import com.netbug94.core.SettingsRepository
import com.netbug94.core.logger
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

class MensajeroClient(
    private val client: HttpClient,
    phoneNumber: String,
    private val apiKey: String?,
    private val baseUrl: String,
    private val settings: SettingsRepository,
    private val defaultTemplate: String
) {
    private val logger by logger()

    private val cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")

    suspend fun isHealthy(): Boolean {
        return try {
            val response: HttpResponse = client.get("${baseUrl}/health") {
                apiKey?.let { header("x-api-key", it) }
            }

            if (!response.status.isSuccess()) {
                logger.warn("⚠️ Heraldo Mensajero responded with error status: ${response.status}")
                return false
            }

            // Safe Serialization
            val statusMap = response.body<Map<String, JsonElement>>()

            // Safe Extraction
            val status = statusMap["status"]?.jsonPrimitive?.content

            status == "CONNECTED"

        } catch (e: Exception) {
            logger.error("🛑 Mensajero Healthcheck failed at: $baseUrl/health")
            logger.error("🛑 Error: ${e.message}")
            false
        }
    }

    suspend fun sendMessage(title: String, description: String?): Boolean {
        // Fetch fresh template every time
        val currentTemplate = settings.getMensajeroTemplate(defaultTemplate)

        val header = currentTemplate
            .replace("{title}", title.trim())
            .replace("%s", title.trim())
        val body = if (!description.isNullOrBlank()) "\n${description.trim()}" else ""
        val fullMessage = "$header$body"

        return try {
            val response: HttpResponse = client.post("$baseUrl/send") {
                contentType(ContentType.Application.Json)
                apiKey?.let { header("x-api-key", it) }

                setBody(MensajeroRequest(
                    phone = cleanPhone,
                    message = fullMessage
                ))
            }

            if (!response.status.isSuccess()) {
                logger.error("❌ Mensajero Error: ${response.status} - ${response.bodyAsText()}")
                false
            } else {
                logger.info("✅ Mensajero: Message delivered for '$title'")
                true
            }
        } catch (e: Exception) {
            logger.error("🚨 MensajeroClient Connection Error: ${e.message}", e)
            false
        }
    }

    companion object {
        const val DEFAULT_TEMPLATE = "🔔 *TASK: {title}*"
    }
}