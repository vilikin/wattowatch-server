package com.vilikin.services

import com.vilikin.services.sources.TwitchSourceSystem
import com.vilikin.services.sources.YleSourceSystem
import org.joda.time.DateTime
import java.util.EnumSet

enum class SourceSystemId(
    val sourceSystem: SourceSystem,
    val supportedFeatures: EnumSet<SourceSystemFeature>
) {
    TWITCH(
        TwitchSourceSystem,
        EnumSet.of(
            SourceSystemFeature.LIVE_STREAMS
        )
    ),
    YLE(
        YleSourceSystem,
        EnumSet.of(
            SourceSystemFeature.VIDEOS,
            SourceSystemFeature.CHANNEL_SUGGESTIONS
        )
    )
}

enum class SourceSystemFeature {
    VIDEOS,
    LIVE_STREAMS,
    CHANNEL_SUGGESTIONS
}

abstract class SourceSystem {
    abstract suspend fun getChannel(channelIdInSourceSystem: String): Channel?
    abstract suspend fun getNewVideosOfChannel(channel: PersistedChannel, since: DateTime?): List<Video>
    abstract suspend fun getLiveStreams(channels: List<PersistedChannel>): List<LiveStream>
    abstract suspend fun getChannelSuggestions(query: String): List<Channel>
}

class SourceSystemService(
    private val channelService: ChannelService,
    private val liveStreamService: LiveStreamService,
    private val videoService: VideoService
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

    suspend fun addNewVideosFromSourceSystem(sourceSystemId: SourceSystemId) {
        val channels = channelService.getChannelsOfSourceSystem(sourceSystemId)
        channels.forEach { channel ->
            val latestVideoOfChannel = videoService.getLatestVideosFromChannels(listOf(channel), 1)
                .firstOrNull()
            val newVideos = sourceSystemId.sourceSystem.getNewVideosOfChannel(channel, latestVideoOfChannel?.publishedAt)
            newVideos.forEach { videoService.addVideo(it) }
        }
    }
}


