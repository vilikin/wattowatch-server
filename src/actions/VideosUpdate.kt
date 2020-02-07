package com.vilikin.actions

import com.vilikin.Config
import com.vilikin.services.ChannelService
import com.vilikin.services.LiveStreamService
import com.vilikin.services.SourceSystemId
import com.vilikin.services.SourceSystemService
import com.vilikin.services.VideoService
import kotlinx.coroutines.runBlocking
import org.flywaydb.core.Flyway

// Action that fetches all new videos from all source systems
fun main(args: Array<String>) {
    Flyway.configure().dataSource(Config.hikariDataSource).load().migrate()

    val channelService = ChannelService(Config.hikariDataSource)
    val liveStreamService = LiveStreamService(Config.hikariDataSource)
    val videoService = VideoService(Config.hikariDataSource)

    val sourceSystemService = SourceSystemService(
        channelService,
        liveStreamService,
        videoService
    )

    runBlocking {
        sourceSystemService.addNewVideosFromSourceSystem(SourceSystemId.YLE)
    }
}
