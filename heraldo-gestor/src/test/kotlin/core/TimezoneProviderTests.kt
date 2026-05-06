package core

import com.netbug94.core.TimezoneProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TimezoneProviderTests {

    private val testGistUrl = "https://gist.githubusercontent.com/test/timezone.txt"

    @Test
    fun `fetches and applies new timezone on first call`() = runTest {
        val mockClient = HttpClient(MockEngine.Companion) {
            engine { addHandler { respond("Europe/London", HttpStatusCode.OK) } }
        }
        val provider = TimezoneProvider(mockClient, testGistUrl)

        val time = provider.getMyLocalTime()
        assertEquals("Europe/London", time.zone.id)
    }

    @Test
    fun `respects cache and does not make network call within 30 minutes`() = runTest {
        var requestCount = 0
        val mockClient = HttpClient(MockEngine.Companion) {
            engine {
                addHandler {
                    requestCount++
                    respond("Europe/London", HttpStatusCode.OK)
                }
            }
        }
        val provider = TimezoneProvider(mockClient, testGistUrl)

        provider.getMyLocalTime()
        assertEquals(1, requestCount)

        provider.getMyLocalTime()
        assertEquals(1, requestCount, "Should not make a second request because of cache")
    }

    @Test
    fun `forceRefresh bypasses cache and makes network call`() = runTest {
        var requestCount = 0
        val mockClient = HttpClient(MockEngine.Companion) {
            engine {
                addHandler {
                    requestCount++
                    respond("Europe/London", HttpStatusCode.OK)
                }
            }
        }
        val provider = TimezoneProvider(mockClient, testGistUrl)

        provider.getMyLocalTime()
        provider.forceRefresh()

        assertEquals(2, requestCount)
    }

    @Test
    fun `retains default timezone if network fails on first call`() = runTest {
        val mockClient = HttpClient(MockEngine.Companion) {
            engine { addHandler { respondError(HttpStatusCode.InternalServerError) } }
        }
        val provider = TimezoneProvider(mockClient, testGistUrl)

        val time = provider.getMyLocalTime()
        assertEquals(TimezoneProvider.DEFAULT_TIMEZONE, time.zone.id)
    }

    @Test
    fun `retains last valid timezone if gist returns invalid string later`() = runTest {
        var callIndex = 0
        val mockClient = HttpClient(MockEngine.Companion) {
            engine {
                addHandler {
                    callIndex++
                    if (callIndex == 1) respond("Asia/Tokyo", HttpStatusCode.OK)
                    else respond("Invalid/Zone", HttpStatusCode.OK)
                }
            }
        }
        val provider = TimezoneProvider(mockClient, testGistUrl)

        // First call: Success
        assertEquals("Asia/Tokyo", provider.getMyLocalTime().zone.id)

        // Force a refresh: Fail
        provider.forceRefresh()

        // Should still be Tokyo because the invalid gist didn't overwrite it
        assertEquals("Asia/Tokyo", provider.getMyLocalTime().zone.id)
    }

    @Test
    fun `appends cache-buster timestamp to URL`() = runTest {
        var capturedUrl = ""
        val mockClient = HttpClient(MockEngine.Companion) {
            engine {
                addHandler { request ->
                    capturedUrl = request.url.toString()
                    respond("UTC", HttpStatusCode.OK)
                }
            }
        }
        val provider = TimezoneProvider(mockClient, "http://example.com/gist")

        provider.getMyLocalTime()
        // Check if the timestamp parameter exists
        assertTrue(
            capturedUrl.contains("?t=") || capturedUrl.contains("&t="),
            "URL should contain a cache-busting timestamp"
        )
    }
}