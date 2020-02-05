package com.vilikin.routes

import com.vilikin.services.ChannelService
import com.vilikin.services.LiveStreamService
import com.vilikin.services.SourceSystemId
import com.vilikin.services.SourceSystemService
import com.vilikin.services.User
import com.vilikin.services.UserService
import io.ktor.application.call
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import org.kodein.di.generic.instance
import org.kodein.di.ktor.kodein

fun Route.liveStreamRoutes() {
    val channelService by kodein().instance<ChannelService>()
    val liveStreamService by kodein().instance<LiveStreamService>()

    get("/live-streams") {
        val userId = Integer.parseInt(call.request.header("x-user-id")!!)

        val channels = channelService.getChannelsUserIsSubscribedTo(userId)
        val liveStreams = liveStreamService.getLiveStreamsOfChannels(channels)

        call.respond(liveStreams)
    }
}
