package com.netbug94.auth

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.days

// ── Session data class ────────────────────────────────────────────────────────
// We store the username and an expiration timestamp directly in the cookie.
@Serializable
data class HeraldoSession(val username: String, val expiresAt: Long)

private const val COOKIE_NAME = "heraldo_gestor_session"
private val COOKIE_MAX_AGE = 30.days

/**
 * Installs Ktor Sessions with a signed, HttpOnly cookie.
 */
fun Application.installAuth(secretKey: String) {
    // Pad or trim the secret key so Ktor's cryptography is happy
    val signKey = secretKey.padEnd(32, '0').substring(0, 32).toByteArray()

    install(Sessions) {
        cookie<HeraldoSession>(COOKIE_NAME) {
            cookie.httpOnly = true
            cookie.path = "/"
            cookie.maxAgeInSeconds = COOKIE_MAX_AGE.inWholeSeconds
            cookie.extensions["SameSite"] = "Lax"

            // This cryptographically signs the cookie.
            // If anyone tampers with it, Ktor will reject it.
            transform(SessionTransportTransformerMessageAuthentication(signKey))
        }
    }
}

// ── requireAuth helper ────────────────────────────────────────────────────────
fun Route.requireAuth(build: Route.() -> Unit): Route {
    val guardRoute = createChild(object : RouteSelector() {
        override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int) =
            RouteSelectorEvaluation.Transparent
    })
    guardRoute.install(createRouteScopedPlugin("AuthGuard") {
        onCall { call ->
            val session = call.sessions.get<HeraldoSession>()
            val now = System.currentTimeMillis()

            // Check if session is missing OR expired
            if (session == null || now > session.expiresAt) {
                call.sessions.clear<HeraldoSession>() // Clean up if expired
                call.respondRedirect("/login")
            }
        }
    })
    guardRoute.build()
    return guardRoute
}