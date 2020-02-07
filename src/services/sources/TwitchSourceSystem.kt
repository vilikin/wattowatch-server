package com.vilikin.services.sources

import com.google.gson.annotations.SerializedName
import com.vilikin.Config
import com.vilikin.services.Channel
import com.vilikin.services.LiveStream
import com.vilikin.services.PersistedChannel
import com.vilikin.services.SourceSystem
import com.vilikin.services.SourceSystemId
import com.vilikin.services.Video
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import org.joda.time.DateTime

data class TwitchUser(
    val id: String,
    val login: String,
    @SerializedName("display_name") val displayName: String
)

data class TwitchResponse<T>(
    val data: List<T>
)

data class TwitchStream(
    val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("user_name") val userName: String,
    val type: String,
    val title: String,
    @SerializedName("viewer_count") val viewerCount: Int,
    @SerializedName("started_at") val startedAt: String
)

class TwitchApiClient {
    private val clientId = Config.twitch.clientId
    private val baseUrl = Config.twitch.baseUrl

    private val client = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = GsonSerializer()
        }

        defaultRequest {
            header("Client-ID", clientId)
        }
    }

    suspend fun getUsers(logins: List<String>): List<TwitchUser> {
        val response = client.get<TwitchResponse<TwitchUser>>("$baseUrl/users") {
            logins.forEach { parameter("login", it) }
        }

        return response.data
    }

    suspend fun getLiveStreams(logins: List<String>): List<TwitchStream> {
        return logins.chunked(100)
            .map { loginsInChunk ->
                val response = client.get<TwitchResponse<TwitchStream>>("$baseUrl/streams") {
                    loginsInChunk.forEach { parameter("user_login", it) }
                }

                response.data
            }
            .flatten()
    }
}

object TwitchSourceSystem : SourceSystem() {
    private val twitchClient = TwitchApiClient()

    override suspend fun getNewVideosOfChannel(channel: PersistedChannel, since: DateTime?): List<Video> {
        return emptyList()
    }

    override suspend fun getChannel(channelIdInSourceSystem: String): Channel? {
        return twitchClient.getUsers(listOf(channelIdInSourceSystem)).map {
            Channel(SourceSystemId.TWITCH, it.login, it.displayName, null)
        }.firstOrNull()
    }

    override suspend fun getLiveStreams(channels: List<PersistedChannel>): List<LiveStream> {
        return twitchClient.getLiveStreams(channels.map { it.idInSourceSystem }).map { twitchStream ->
            val channel = channels.find { it.idInSourceSystem.equals(twitchStream.userName, ignoreCase = true) }!!
            LiveStream(
                channel = channel,
                title = twitchStream.title,
                live = twitchStream.type == "live",
                liveSince = DateTime.parse(twitchStream.startedAt)
            )
        }
    }
}
