package com.vilikin.routes

import com.vilikin.services.User
import com.vilikin.services.UserService
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import org.kodein.di.generic.instance
import org.kodein.di.ktor.kodein

data class CreateUserPayload(
    val name: String
)

fun Route.userRoutes() {
    val userService by kodein().instance<UserService>()

    route("/users") {
        post {
            val (name) = call.receive<CreateUserPayload>()
            val user = userService.createUser(User(name))
            call.respond(user)
        }

        get("/actions/is-name-available") {
            val name = call.parameters["name"]!!
            val isAvailable = userService.isUsernameAvailable(name)
            call.respond(mapOf("name" to name, "available" to isAvailable))
        }

        get("/actions/login") {
            val name = call.parameters["name"]!!
            val user = userService.getUser(name)

            if (user == null) {
                call.respond(HttpStatusCode.Forbidden)
                return@get
            }

            call.respond(user)
        }
    }
}
