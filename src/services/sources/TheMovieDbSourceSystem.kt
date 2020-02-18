package com.vilikin.services.sources

import com.google.gson.annotations.SerializedName
import com.vilikin.Config
import com.vilikin.services.Channel
import com.vilikin.services.LiveStream
import com.vilikin.services.PersistedChannel
import com.vilikin.services.SourceSystem
import com.vilikin.services.SourceSystemId
import com.vilikin.services.Video
import com.vilikin.utils.LocalDateDeserializer
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.features.defaultRequest
import io.ktor.client.features.json.GsonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import org.joda.time.DateTime
import org.joda.time.LocalDate

data class TheMovieDbSearchResponse<T>(
    val results: List<T>
)

data class TheMovieDbEpisode(
    @SerializedName("air_date") val airDate: LocalDate,
    @SerializedName("episode_number") val episodeNumber: Int,
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("still_path") val stillPath: String?,
    val name: String,
    val id: Int
)

data class TheMovieDbSeason(
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("air_date") val airDate: LocalDate
)

data class TheMovieDbSeasonExtended(
    @SerializedName("season_number") val seasonNumber: Int,
    @SerializedName("air_date") val airDate: LocalDate,
    val episodes: List<TheMovieDbEpisode>
)

data class TheMovieDbTvShow(
    val id: Int,
    val name: String,
    @SerializedName("poster_path") val posterPath: String?
)

data class TheMovieDbTvShowExtended(
    val id: Int,
    val name: String,
    @SerializedName("poster_path") val posterPath: String?,
    val seasons: List<TheMovieDbSeason>
)

fun getUrlForImagePath(path: String): String =
    "https://image.tmdb.org/t/p/w300${path.replace("\\", "")}"

class TheMovieDbApiClient {
    private val baseUrl = Config.theMovieDb.baseUrl
    private val apiKey = Config.theMovieDb.apiKey

    private val client = HttpClient(Apache) {
        install(JsonFeature) {
            serializer = GsonSerializer {
                registerTypeAdapter(LocalDate::class.java, LocalDateDeserializer())
            }
        }

        defaultRequest {
            parameter("api_key", apiKey)
        }
    }

    suspend fun searchTvShows(query: String): List<TheMovieDbTvShow> {
        val response = client.get<TheMovieDbSearchResponse<TheMovieDbTvShow>>("$baseUrl/search/tv") {
            parameter("query", query)
        }

        return response.results
    }

    suspend fun getTvShow(id: Int): TheMovieDbTvShowExtended {
        return client.get("$baseUrl/tv/$id")
    }

    suspend fun getTvShowSeason(tvShowId: Int, seasonNumber: Int): TheMovieDbSeasonExtended {
        return client.get("$baseUrl/tv/$tvShowId/season/$seasonNumber")
    }
}

object TheMovieDbSourceSystem : SourceSystem() {
    private val theMovieDbApiClient = TheMovieDbApiClient()

    override suspend fun getNewVideosOfChannel(channel: PersistedChannel, since: DateTime?): List<Video> {
        val tvShowId = channel.idInSourceSystem.toInt()
        val tvShow = theMovieDbApiClient.getTvShow(tvShowId)

        val seasonsToGoThrough = if (since == null) {
            tvShow.seasons
        } else {
            val selectedSeasons = mutableListOf<TheMovieDbSeason>()
            tvShow.seasons
                .sortedByDescending { it.seasonNumber }
                .forEach { season ->
                    val selectedAlreadyHasOneSeasonBeforeSince = selectedSeasons
                        .any { it.airDate.toDateTimeAtStartOfDay().isBefore(since.withTimeAtStartOfDay()) }
                    if (!selectedAlreadyHasOneSeasonBeforeSince) {
                        selectedSeasons.add(season)
                    }
                }
            selectedSeasons
        }

        val extendedSeasons = seasonsToGoThrough.map { season ->
            theMovieDbApiClient.getTvShowSeason(tvShowId, season.seasonNumber)
        }

        val episodes = extendedSeasons
            .flatMap { it.episodes }

        return episodes.map { episode ->
            Video(
                episode.id.toString(),
                channel,
                episode.name,
                null,
                episode.airDate.toDateTimeAtStartOfDay(),
                episode.stillPath?.let { getUrlForImagePath(it) },
                episode.episodeNumber,
                episode.seasonNumber
            )
        }
    }

    override suspend fun getChannel(channelIdInSourceSystem: String): Channel? {
        return try {
            val tvShow = theMovieDbApiClient.getTvShow(channelIdInSourceSystem.toInt())
            Channel(
                SourceSystemId.THE_MOVIE_DB,
                tvShow.id.toString(),
                tvShow.name,
                null,
                tvShow.posterPath?.let { getUrlForImagePath(it) }
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getLiveStreams(channels: List<PersistedChannel>): List<LiveStream> {
        return emptyList()
    }

    override suspend fun getChannelSuggestions(query: String): List<Channel> {
        return theMovieDbApiClient.searchTvShows(query).map { tvShow ->
            Channel(
                SourceSystemId.THE_MOVIE_DB,
                tvShow.id.toString(),
                tvShow.name,
                null,
                tvShow.posterPath?.let { getUrlForImagePath(it) }
            )
        }
    }
}
