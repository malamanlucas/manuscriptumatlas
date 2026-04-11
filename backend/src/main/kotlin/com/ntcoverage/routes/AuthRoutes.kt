package com.ntcoverage.routes

import com.auth0.jwk.JwkProvider
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.ntcoverage.ErrorResponse
import com.ntcoverage.model.*
import com.ntcoverage.repository.UserRepository
import com.ntcoverage.service.UserService
import com.ntcoverage.util.JwtUtil
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("AuthRoutes")

fun Route.authLoginRoute(userRepository: UserRepository, jwkProvider: JwkProvider, googleClientId: String) {
    // DEV-ONLY: bypass login for local testing — generates admin JWT without Google SSO
    post("/auth/dev-login") {
        val isDev = System.getenv("JWT_SECRET")?.contains("dev-secret") == true
            || System.getenv("JWT_SECRET").isNullOrBlank()
        if (!isDev) {
            return@post call.respond(HttpStatusCode.Forbidden, ErrorResponse("Dev login disabled in production"))
        }

        val email = call.request.queryParameters["email"] ?: "dev@manuscriptum.local"
        val user = userRepository.findByEmail(email)
        if (user == null) {
            // Auto-create dev admin user
            userRepository.create(email, "Dev Admin", UserRole.ADMIN)
            log.info("AUTH_DEV: auto-created dev admin user | email=$email")
        }
        val dbUser = userRepository.findByEmail(email)!!
        val token = JwtUtil.generateToken(dbUser.id, dbUser.email, dbUser.role, dbUser.displayName)
        log.info("AUTH_DEV: dev login | email=$email | role=${dbUser.role}")
        call.respond(LoginResponse(token = token, user = dbUser))
    }

    // OAuth2 client_credentials — for service accounts (e.g. /run-llm)
    post("/auth/token") {
        val params = call.receiveParameters()
        val grantType = params["grant_type"]
        if (grantType != "client_credentials") {
            return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Unsupported grant_type. Use 'client_credentials'"))
        }

        val clientId = params["client_id"] ?: ""
        val clientSecret = params["client_secret"] ?: ""
        val expectedId = System.getenv("SERVICE_CLIENT_ID") ?: ""
        val expectedSecret = System.getenv("SERVICE_CLIENT_SECRET") ?: ""

        if (expectedId.isBlank() || expectedSecret.isBlank()) {
            return@post call.respond(HttpStatusCode.ServiceUnavailable, ErrorResponse("Service credentials not configured. Set SERVICE_CLIENT_ID and SERVICE_CLIENT_SECRET env vars."))
        }
        if (clientId != expectedId || clientSecret != expectedSecret) {
            log.warn("AUTH_TOKEN: invalid client credentials | clientId=$clientId")
            return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid client credentials"))
        }

        val token = JwtUtil.generateServiceToken(clientId)
        log.info("AUTH_TOKEN: service token issued | clientId=$clientId")
        call.respond(TokenResponse(accessToken = token, tokenType = "bearer", expiresIn = 86400))
    }

    post("/auth/login") {
        val req = call.receive<LoginRequest>()

        val googleUser = try {
            val decoded = JWT.decode(req.credential)
            val jwk = jwkProvider.get(decoded.keyId)
            val algorithm = Algorithm.RSA256(jwk.publicKey as java.security.interfaces.RSAPublicKey, null)
            val verifier = JWT.require(algorithm)
                .withClaimPresence("email")
                .acceptLeeway(5)
                .build()
            val verified = verifier.verify(req.credential)

            val issuer = verified.issuer
            if (issuer != "https://accounts.google.com" && issuer != "accounts.google.com") {
                log.warn("AUTH: login_rejected_invalid_issuer | issuer=$issuer")
                return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid token issuer"))
            }

            if (googleClientId.isNotBlank()) {
                val audience = verified.audience
                if (audience == null || googleClientId !in audience) {
                    log.warn("AUTH: login_rejected_invalid_audience | aud=$audience")
                    return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid token audience"))
                }
            }

            val emailVerified = verified.getClaim("email_verified")?.asBoolean() ?: false
            if (!emailVerified) {
                log.warn("AUTH: login_rejected_email_not_verified | email=${verified.getClaim("email")?.asString()}")
                return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Email not verified"))
            }

            verified
        } catch (e: Exception) {
            log.warn("AUTH: login_rejected_invalid_token | error=${e.message}")
            return@post call.respond(HttpStatusCode.Unauthorized, ErrorResponse("Invalid Google token"))
        }

        val email = googleUser.getClaim("email").asString()
        val picture = googleUser.getClaim("picture")?.asString()

        val user = userRepository.findByEmail(email)
        if (user == null) {
            log.warn("AUTH: login_rejected_unknown_email | email=$email")
            return@post call.respond(HttpStatusCode.Forbidden, ErrorResponse("User not registered"))
        }

        userRepository.updateLastLoginAndPicture(email, picture)
        log.info("AUTH: login_success | email=$email | role=${user.role}")

        val token = JwtUtil.generateToken(user.id, user.email, user.role, user.displayName)
        call.respond(LoginResponse(token = token, user = user.copy(pictureUrl = picture ?: user.pictureUrl)))
    }
}

fun Route.authRoutes(userRepository: UserRepository, userService: UserService) {
    route("/auth") {

        get("/me") {
            val email = call.principal<JWTPrincipal>()!!
                .payload.getClaim("email").asString()
            val user = userRepository.findByEmail(email)
                ?: return@get call.respond(HttpStatusCode.Forbidden, ErrorResponse("User not registered"))
            call.respond(user)
        }

        route("/users") {

            get {
                val actor = call.requireAdmin(userRepository) ?: return@get
                call.respond(userService.getAll())
            }

            post {
                val actor = call.requireAdmin(userRepository) ?: return@post
                val req = call.receive<CreateUserRequest>()
                val role = try { UserRole.valueOf(req.role) } catch (_: Exception) {
                    return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid role: ${req.role}"))
                }
                val created = userService.create(req.email, req.displayName, role, actor.email)
                call.respond(HttpStatusCode.Created, created)
            }

            patch("/{id}/role") {
                val actor = call.requireAdmin(userRepository) ?: return@patch
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
                val req = call.receive<UpdateUserRoleRequest>()
                val role = try { UserRole.valueOf(req.role) } catch (_: Exception) {
                    return@patch call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid role: ${req.role}"))
                }
                userService.updateRole(id, role, actor.email)
                call.respond(mapOf("ok" to true))
            }

            delete("/{id}") {
                val actor = call.requireAdmin(userRepository) ?: return@delete
                val id = call.parameters["id"]?.toIntOrNull()
                    ?: return@delete call.respond(HttpStatusCode.BadRequest, ErrorResponse("Invalid user ID"))
                userService.delete(id, actor.email)
                call.respond(mapOf("ok" to true))
            }
        }
    }
}

private suspend fun ApplicationCall.requireAdmin(userRepository: UserRepository): UserDTO? {
    val principal = principal<JWTPrincipal>()!!
    val roleClaim = principal.payload.getClaim("role")?.asString()

    if (roleClaim != UserRole.ADMIN.name) {
        respond(HttpStatusCode.Forbidden, ErrorResponse("Admin access required"))
        return null
    }

    val email = principal.payload.getClaim("email").asString()
    val user = userRepository.findByEmail(email)
    if (user == null || user.role != UserRole.ADMIN.name) {
        respond(HttpStatusCode.Forbidden, ErrorResponse("Admin access required"))
        return null
    }
    return user
}
