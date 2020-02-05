package com.vilikin.services

import com.vilikin.services.sources.TwitchSourceSystem
import org.joda.time.DateTime

enum class SourceSystemId(val sourceSystem: SourceSystem) {
    TWITCH(TwitchSourceSystem)
}

data class Video(
    val id: Long? = null,
    val idInSourceSystem: String,
    val channel: Channel,
    val title: String,
    val url: String?,
    val publishedAt: DateTime,
    val persistedAt: DateTime
)

abstract class SourceSystem {
    abstract suspend fun getChannel(channelIdInSourceSystem: String): Channel?
    abstract suspend fun getNewVideosOfChannel(channel: PersistedChannel, since: DateTime): List<Video>
    abstract suspend fun getLiveStreams(channels: List<PersistedChannel>): List<LiveStream>
}

class SourceSystemService(
    private val channelService: ChannelService,
    private val liveStreamService: LiveStreamService
) {
    suspend fun isChannelValid(sourceSystemId: SourceSystemId, channelIdInSourceSystem: String): Boolean {
        return sourceSystemId.sourceSystem.getChannel(channelIdInSourceSystem) != null
    }

    suspend fun updateLiveStreamsOfSourceSystem(sourceSystemId: SourceSystemId) {
        val channels = channelService.getChannelsOfSourceSystem(sourceSystemId)
        val liveStreams = sourceSystemId.sourceSystem.getLiveStreams(channels)

        liveStreamService.updateLiveStreamsOfSourceSystem(sourceSystemId, liveStreams)
    }

    suspend fun getChannel(sourceSystemId: SourceSystemId, channelIdInSourceSystem: String): Channel? {
        return sourceSystemId.sourceSystem.getChannel(channelIdInSourceSystem)
    }
}


