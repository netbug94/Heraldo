package com.netbug94.core

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private val logger = LoggerFactory.getLogger("com.netbug94.core.TimezoneProvider")

class TimezoneProvider(private val client: HttpClient, private val gistUrl: String) {

    companion object {
        const val DEFAULT_TIMEZONE = "America/Mexico_City"
        private val CACHE_EXPIRATION = 30.minutes
    }

    private var currentZone = ZoneId.of(DEFAULT_TIMEZONE)
    private val mutex = Mutex()

    // Use Monotonic time for bulletproof duration measuring, initialized to the past so it fetches immediately
    private var lastChecked = TimeSource.Monotonic.markNow() - CACHE_EXPIRATION

    suspend fun getMyLocalTime(): ZonedDateTime {
        // Quick check (no lock) so fast-paths aren't blocked
        if (lastChecked.elapsedNow() > CACHE_EXPIRATION) {

            // Lock to prevent multiple network calls at the exact same time
            mutex.withLock {
                // Double-check inside the lock in case another thread just updated it
                if (lastChecked.elapsedNow() > CACHE_EXPIRATION) {
                    try {
                        val maxAttempts = 3
                        for (attempt in 1..maxAttempts) {
                            try {
                                val nowMs = System.currentTimeMillis()
                                val finalUrl = if (gistUrl.contains("?")) "$gistUrl&t=$nowMs" else "$gistUrl?t=$nowMs"
                                val response = client.get(finalUrl).bodyAsText().trim()
                                currentZone = ZoneId.of(response)
                                logger.info("🌍 Timezone Sync: Updated to $currentZone")
                                break // Success! Break out of the for-loop
                            } catch (e: Exception) {
                                if (attempt >= maxAttempts) throw e // If it's our last try, bubble the error up

                                logger.warn("🌍 Timezone Sync Attempt $attempt failed: ${e.message}. Retrying in 5s...")

                                // Fix 2: Using explicit Duration instead of legacy Long
                                delay(5.seconds)
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("🌍 Timezone Sync Failed after 3 attempts: ${e.message}. Retaining $currentZone")
                    } finally {
                        lastChecked = TimeSource.Monotonic.markNow()
                    }
                }
            }
        }
        return ZonedDateTime.now(currentZone)
    }

    suspend fun forceRefresh() {
        mutex.withLock {
            lastChecked = TimeSource.Monotonic.markNow() - CACHE_EXPIRATION
        }
        getMyLocalTime()
    }
}