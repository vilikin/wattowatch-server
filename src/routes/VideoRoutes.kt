package com.vilikin.routes

import com.vilikin.services.ChannelService
import com.vilikin.services.VideoService
import io.ktor.application.call
import io.ktor.request.header
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import org.kodein.di.generic.instance
import org.kodein.di.ktor.kodein

fun Route.videoRoutes() {
    val channelService by kodein().instance<ChannelService>()
    val videoService by kodein().instance<VideoService>()

    get("/videos") {
        val userId = Integer.parseInt(call.request.header("x-user-id")!!)
        val limit = Integer.parseInt(
            call.parameters["limit"] ?: "20"
        )

        val channels = channelService.getChannelsUserIsSubscribedTo(userId)
        val videos = videoService.getLatestVideosFromChannels(channels, limit)

        call.respond(videos)
    }
}
