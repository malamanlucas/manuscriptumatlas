package com.ntcoverage.routes

import com.ntcoverage.ErrorResponse
import com.ntcoverage.model.*
import com.ntcoverage.repository.UserRepository
import com.ntcoverage.service.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

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
    val email = principal<JWTPrincipal>()!!.payload.getClaim("email").asString()
    val user = userRepository.findByEmail(email)
    if (user == null || user.role != UserRole.ADMIN.name) {
        respond(HttpStatusCode.Forbidden, ErrorResponse("Admin access required"))
        return null
    }
    return user
}
