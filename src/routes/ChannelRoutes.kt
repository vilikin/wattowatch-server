package com.vilikin.routes

import com.vilikin.services.ChannelService
import com.vilikin.services.SourceSystemFeature
import com.vilikin.services.SourceSystemId
import com.vilikin.services.SourceSystemService
import com.vilikin.services.UserService
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.route
import org.kodein.di.generic.instance
import org.kodein.di.ktor.kodein

fun Route.channelRoutes() {
    val channelService by kodein().instance<ChannelService>()
    val userService by kodein().instance<UserService>()
    val sourceSystemService by kodein().instance<SourceSystemService>()

    route("/channels") {
        get {
            val userId = Integer.parseInt(call.request.header("x-user-id")!!)
            val channels = channelService.getChannelsUserIsSubscribedTo(userId)
            call.respond(channels)
        }

        get("/suggestions") {
            val query = call.parameters["query"]!!
            val sourceSystemId = SourceSystemId.valueOf(call.parameters["system"]!!)

            if (sourceSystemId.supportedFeatures.contains(SourceSystemFeature.CHANNEL_SUGGESTIONS)) {
                call.respond(sourceSystemId.sourceSystem.getChannelSuggestions(query))
            } else {
                call.respond(HttpStatusCode.BadRequest)
            }
        }

        route("/actions") {
            get("/is-id-valid") {
                val id = call.parameters["channel_id"]!!
                val sourceSystemId = SourceSystemId.valueOf(call.parameters["system"]!!)

                val isValid = sourceSystemService.isChannelValid(sourceSystemId, id)

                call.respond(mapOf(
                    "channelId" to id,
                    "valid" to isValid
                ))
            }

            get("/subscribe") {
                val userId = Integer.parseInt(call.request.header("x-user-id")!!)
                val channelIdInSourceSystem = call.parameters["channel_id"]!!
                val sourceSystemId = SourceSystemId.valueOf(call.parameters["system"]!!)

                val userFromDb = userService.getUser(userId)!!
                val channelFromDb = channelService.getChannel(channelIdInSourceSystem, sourceSystemId)

                if (channelFromDb != null) {
                    channelService.addSubscription(channelFromDb, userFromDb)
                } else {
                    val channel = sourceSystemService.getChannel(sourceSystemId, channelIdInSourceSystem)
                        ?: throw Exception("Channel $channelIdInSourceSystem does not exist in $sourceSystemId")
                    val persistedChannel = channelService.createChannel(channel)
                    channelService.addSubscription(persistedChannel, userFromDb)
                }

                call.respond(mapOf("success" to true))
                return@get
            }

            get("/unsubscribe") {
                val userId = Integer.parseInt(call.request.header("x-user-id")!!)
                val channelIdInSourceSystem = call.parameters["channel_id"]!!
                val sourceSystemId = SourceSystemId.valueOf(call.parameters["system"]!!)

                val userFromDb = userService.getUser(userId)!!
                val channelFromDb = channelService.getChannel(channelIdInSourceSystem, sourceSystemId)!!

                channelService.removeSubscription(channelFromDb, userFromDb)

                call.respond(mapOf("success" to true))
                return@get
            }
        }
    }
}
