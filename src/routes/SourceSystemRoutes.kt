package com.vilikin.routes

import com.vilikin.services.SourceSystemId
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get

fun Route.sourceSystemRoutes() {
    get("/source-systems") {
        val response = SourceSystemId.values()
            .map {
                mapOf(
                    "system" to it.name,
                    "supportedFeatures" to it.supportedFeatures
                )
            }

        call.respond(response)
    }
}
