package com.netbug94.core

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable
data class AppSettings(
    val lastSync: String? = null,
    val mensajeroTemplate: String? = null
)

class SettingsRepository(
    private val settingsFile: File = File("./config/settings.json")
) {
    private val logger by logger()

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

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

    private fun saveSettings() {
        try {
            settingsFile.parentFile?.mkdirs()
            val jsonStr = json.encodeToString(currentSettings)
            settingsFile.writeText(jsonStr)
        } catch (e: Exception) {
            logger.error("🚨 Failed to save settings: ${e.message}")
        }
    }

    fun getMensajeroTemplate(fallback: String): String {
        return currentSettings.mensajeroTemplate ?: fallback
    }

    fun saveMensajeroTemplate(newTemplate: String) {
        currentSettings = currentSettings.copy(mensajeroTemplate = newTemplate)
        saveSettings()
        logger.info("⚙️ Mensajero template sealed and saved to disk.")
    }
}