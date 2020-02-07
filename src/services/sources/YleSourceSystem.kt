package com.vilikin.services.sources

import com.vilikin.Config
import com.vilikin.services.Channel
import com.vilikin.services.LiveStream
import com.vilikin.services.PersistedChannel
import com.vilikin.services.SourceSystem
import com.vilikin.services.SourceSystemId
import com.vilikin.services.Video
import com.vilikin.utils.DateTimeDeserializer
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.joda.time.DateTime

data class YleResponse<T>(
    val data: T
)

data class YleLocalizedString(
    val fi: String
)

data class YleSeries(
    val id: String,
    val description: YleLocalizedString,
    val title: YleLocalizedString
)

data class YleSeason(
    val seasonNumber: Int
)

data class YleService(
    val id: String
)

data class YlePublicationEvent(
    val service: YleService,
    val startTime: DateTime,
    val endTime: DateTime
)

data class YleProgram(
    val id: String,
    val title: YleLocalizedString,
    val description: YleLocalizedString,
    val partOfSeason: YleSeason,
    val episodeNumber: Int,
    val publicationEvent: List<YlePublicationEvent>
) {
    val isAvailableInAreena: Boolean
        get() = publicationEvent.any {
            it.service.id == "yle-areena" && it.startTime.isBeforeNow()
        }

    val availableInAreenaFrom: DateTime?
        get() = publicationEvent.find {
            it.service.id == "yle-areena"
        }?.startTime

    fun toVideo(channel: PersistedChannel): Video {
        return Video(
            id,
            channel,
            title.fi,
            null,
            availableInAreenaFrom!!
        )
    }
}

class YleApiClient {
    private val baseUrl = Config.yle.baseUrl
    private val appId = Config.yle.appId
    private val appKey = Config.yle.appKey

    private val client = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = GsonSerializer {
                registerTypeAdapter(DateTime::class.java, DateTimeDeserializer())
            }
        }

        defaultRequest {
            parameter("app_id", appId)
            parameter("app_key", appKey)
        }
    }

    suspend fun getSeries(idInSourceSystem: String): YleSeries {
        val response = client.get<YleResponse<YleSeries>>("$baseUrl/series/items/$idInSourceSystem.json")
        return response.data
    }

    suspend fun getProgramsOfSeries(idInSourceSystem: String): List<YleProgram> {
        val response = client.get<YleResponse<List<YleProgram>>>("$baseUrl/programs/items.json") {
            parameter("series", idInSourceSystem)
            parameter("type", "program")
            parameter("limit", 100)
        }

        return response.data
    }
}

object YleSourceSystem : SourceSystem() {
    private val yleApiClient = YleApiClient()

    override suspend fun getNewVideosOfChannel(channel: PersistedChannel, since: DateTime?): List<Video> {
        return yleApiClient.getProgramsOfSeries(channel.idInSourceSystem)
            .filter { it.isAvailableInAreena }
            .filter { if (since != null) it.availableInAreenaFrom!!.isAfter(since) else true }
            .map { it.toVideo(channel) }
    }

    override suspend fun getChannel(channelIdInSourceSystem: String): Channel? {
        val yleSeries = yleApiClient.getSeries(channelIdInSourceSystem)
        return Channel(
            SourceSystemId.YLE,
            yleSeries.id,
            yleSeries.title.fi,
            null
        )
    }

    override suspend fun getLiveStreams(channels: List<PersistedChannel>): List<LiveStream> {
        return emptyList()
    }
}
