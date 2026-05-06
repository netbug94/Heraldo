package com.netbug94.mensajero

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.netbug94.mensajero.MensajeroClient")

class MensajeroClient(
    private val client: HttpClient,
    phoneNumber: String,
    private val apiKey: String?,
    private val baseUrl: String,
    @Volatile private var template: String // <--- Added @Volatile for thread-safe updates
) {
    private val cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")

    fun updateTemplate(newTemplate: String) {
        this.template = newTemplate
        logger.info("🔄 Message Template updated: $template")
    }

    suspend fun isHealthy(): Boolean {
        return try {
            val response: HttpResponse = client.get("$baseUrl/health") {
                apiKey?.let { header("x-api-key", it) }
            }

            if (!response.status.isSuccess()) {
                logger.warn("⚠️ Heraldo Mensajero responded with error status: ${response.status}")
                return false
            }

            val statusMap = response.body<Map<String, String>>()
            statusMap["status"] == "CONNECTED" || statusMap["status"] == "UP"
        } catch (e: Exception) {
            logger.error("🛑 Mensajero Healthcheck failed at: $baseUrl/health")
            logger.error("🛑 Error: ${e.message}")
            false
        }
    }

    suspend fun sendMessage(title: String, description: String?): Boolean {
        val header = template.replace("{title}", title.trim())
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
