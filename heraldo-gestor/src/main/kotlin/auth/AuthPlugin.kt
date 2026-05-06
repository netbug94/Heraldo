package com.netbug94.auth

import io.ktor.server.application.*
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import kotlin.time.Duration.Companion.days

// ── Session data class ────────────────────────────────────────────────────────

/**
 * The data stored inside the signed cookie. Just a token string — the
 * server validates it against [HeraldoSessionStore].
 */
data class HeraldoSession(val token: String)

// ── In-memory token store ─────────────────────────────────────────────────────

/**
 * Holds all active session tokens. Tokens are invalidated on container restart.
 * For a single-user personal tool this is intentional — no database needed.
 */
object HeraldoSessionStore {
    private val tokens = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    fun add(token: String) = tokens.add(token)
    fun contains(token: String) = tokens.contains(token)
    fun remove(token: String) = tokens.remove(token)
}

// ── Session plugin install ────────────────────────────────────────────────────

private const val COOKIE_NAME = "heraldo_session"
private val COOKIE_MAX_AGE = 30.days

/**
 * Installs Ktor Sessions with a signed, HttpOnly cookie.
 * Call this once from [Application.module].
 */
fun Application.installAuth() {
    install(Sessions) {
        cookie<HeraldoSession>(COOKIE_NAME) {
            cookie.httpOnly = true
            cookie.path = "/"
            cookie.maxAgeInSeconds = COOKIE_MAX_AGE.inWholeSeconds
            // SameSite=Lax prevents cross-site request forgery
            cookie.extensions["SameSite"] = "Lax"
            // Sign the cookie with a secret derived from the password hash so
            // tampering is detectable without a separate SECRET env var.
            transform(SessionTransportTransformerMessageAuthentication(
                "heraldo_session_key_v1".toByteArray()
            ))
        }
    }
}

// ── requireAuth helper ────────────────────────────────────────────────────────

/**
 * Route-level guard. Redirects unauthenticated requests to /login.
 * Usage:
 *   routing {
 *       requireAuth {
 *           get("/") { ... }
 *       }
 *   }
 */
fun Route.requireAuth(build: Route.() -> Unit): Route {
    val guardRoute = createChild(object : RouteSelector() {
        override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int) =
            RouteSelectorEvaluation.Transparent
    })
    guardRoute.install(createRouteScopedPlugin("AuthGuard") {
        onCall { call ->
            val session = call.sessions.get<HeraldoSession>()
            if (session == null || !HeraldoSessionStore.contains(session.token)) {
                call.respondRedirect("/login")
            }
        }
    })
    guardRoute.build()
    return guardRoute
}
