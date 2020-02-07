package com.vilikin.services

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.joda.time.DateTime

class PersistedVideo(
    val id: Long,
    idInSourceSystem: String,
    channel: PersistedChannel,
    title: String,
    url: String?,
    publishedAt: DateTime
) : Video(idInSourceSystem, channel, title, url, publishedAt) {
    companion object {
        fun fromRow(row: Row, videoPrefix: String = "", channelPrefix: String = ""): PersistedVideo {
            return PersistedVideo(
                row.long("${videoPrefix}id"),
                row.string("${videoPrefix}id_in_source_system"),
                PersistedChannel.fromRow(row, channelPrefix),
                row.string("${videoPrefix}title"),
                row.stringOrNull("${videoPrefix}url"),
                row.jodaDateTime("${videoPrefix}published_at")
            )
        }
    }
}

open class Video(
    val idInSourceSystem: String,
    val channel: PersistedChannel,
    val title: String,
    val url: String?,
    val publishedAt: DateTime
)

class VideoService(private val hikariDataSource: HikariDataSource) {
    fun addVideo(video: Video): Unit {
        return using(sessionOf(hikariDataSource)) { session ->
            session.run(
                queryOf(
                    """
                        INSERT INTO videos (id_in_source_system, channel_id, title, url, published_at)
                        VALUES (:id_in_source_system, :channel_id, :title, :url, :published_at)
                    """,
                    mapOf(
                        "id_in_source_system" to video.idInSourceSystem,
                        "channel_id" to video.channel.id,
                        "title" to video.title,
                        "url" to video.url,
                        "published_at" to video.publishedAt
                    )
                ).asExecute
            )
        }
    }

    fun getLatestVideosFromChannels(channels: List<PersistedChannel>, limit: Int): List<PersistedVideo> {
        return using(sessionOf(hikariDataSource)) { session ->
            val channelIds = session.createArrayOf("int4", channels.map { it.id })

            session.run(
                queryOf(
                    """
                        SELECT
                            videos.id as video_id,
                            videos.id_in_source_system as video_id_in_source_system,
                            videos.channel_id as video_channel_id,
                            videos.title as video_title,
                            videos.url as video_url,
                            videos.published_at as video_published_at,
                            channels.id as channel_id,
                            channels.source_system as channel_source_system,
                            channels.id_in_source_system as channel_id_in_source_system,
                            channels.name as channel_name,
                            channels.url as channel_url
                        FROM videos
                        INNER JOIN channels
                        ON videos.channel_id = channels.id
                        WHERE
                            videos.channel_id = ANY (:channel_ids)
                        ORDER BY videos.published_at DESC
                        LIMIT :limit
                    """,
                    mapOf(
                        "channel_ids" to channelIds,
                        "limit" to limit
                    )
                ).map {
                    PersistedVideo.fromRow(it, "video_", "channel_")
                }.asList
            )
        }
    }
}
