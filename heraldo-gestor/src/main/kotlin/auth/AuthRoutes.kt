package com.netbug94.auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.sessions.*
import org.koin.ktor.ext.inject
import io.ktor.server.config.ApplicationConfig
import java.security.MessageDigest
import kotlin.time.Duration.Companion.days

fun Application.authRoutes() {

    val config by inject<ApplicationConfig>()

    val expectedUser = config.propertyOrNull("app.dashboard.user")?.getString()
        ?: error("DASHBOARD_USER is not set in env — cannot start safely.")
    val expectedPassword = config.propertyOrNull("app.dashboard.password")?.getString()
        ?: error("DASHBOARD_PASSWORD is not set in env — cannot start safely.")

    routing {

        // ── GET /login ─────────────────────────────────────────────────────
        get("/login") {
            val session = call.sessions.get<HeraldoSession>()
            val now = System.currentTimeMillis()

            if (session != null && now <= session.expiresAt) {
                call.respondRedirect("/")
                return@get
            }
            call.respondText(LoginView.renderLogin(), ContentType.Text.Html)
        }

        // ── POST /login ────────────────────────────────────────────────────
        post("/login") {
            val params = call.receiveParameters()
            val username = params["username"]?.trim() ?: ""
            val password = params["password"] ?: ""

            val userMatch = constantTimeEquals(username, expectedUser)
            val passMatch = constantTimeEquals(password, expectedPassword)

            if (!userMatch || !passMatch) {
                call.respondText(
                    LoginView.renderLogin("The seal was rejected. Check thy credentials."),
                    ContentType.Text.Html,
                    HttpStatusCode.Unauthorized
                )
                return@post
            }

            // Issue a stateless session valid for 30 days
            val expirationTime = System.currentTimeMillis() + 30.days.inWholeMilliseconds
            call.sessions.set(HeraldoSession(username, expirationTime))

            call.respondRedirect("/")
        }

        // ── GET /logout ────────────────────────────────────────────────────
        get("/logout") {
            call.sessions.clear<HeraldoSession>()
            call.respondRedirect("/login")
        }
    }
}

private fun constantTimeEquals(a: String, b: String): Boolean {
    val aBytes = a.toByteArray(Charsets.UTF_8)
    val bBytes = b.toByteArray(Charsets.UTF_8)
    return MessageDigest.isEqual(aBytes, bBytes)
}