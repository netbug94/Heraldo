package com.netbug94.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.io.File

private val logger = LoggerFactory.getLogger("com.netbug94.core.SettingsRepository")

@Serializable
data class AppSettings(
    val lastSync: String? = null
)

class SettingsRepository(
    private val settingsFile: File = File("./config/settings.json")
) {
    private var currentSettings: AppSettings = AppSettings()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        try {
            if (settingsFile.exists()) {
                val jsonStr = settingsFile.readText()
                if (jsonStr.isNotBlank()) {
                    currentSettings = json.decodeFromString<AppSettings>(jsonStr)
                    logger.info("⚙️ Settings loaded from ${settingsFile.path}")
                }
            } else {
                settingsFile.parentFile?.mkdirs()
            }
        } catch (e: Exception) {
            logger.error("🚨 Failed to load settings: ${e.message}")
        }
    }

    private val json = Json { 
        prettyPrint = true 
        ignoreUnknownKeys = true
    }

    fun getMensajeroTemplate(fallback: String): String {
        return fallback
    }
}
