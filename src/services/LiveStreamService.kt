package com.vilikin.services

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import org.joda.time.DateTime

class PersistedLiveStream(
    val id: Int,
    channel: PersistedChannel,
    title: String,
    live: Boolean,
    liveSince: DateTime,
    imageUrl: String?
) : LiveStream(channel, title, live, liveSince, imageUrl) {
    companion object {
        fun fromRow(row: Row, liveStreamPrefix: String = "", channelPrefix: String = ""): PersistedLiveStream {
            return PersistedLiveStream(
                row.int("${liveStreamPrefix}id"),
                PersistedChannel.fromRow(row, channelPrefix),
                row.string("${liveStreamPrefix}title"),
                row.boolean("${liveStreamPrefix}live"),
                row.jodaDateTime("${liveStreamPrefix}live_since"),
                row.stringOrNull("${liveStreamPrefix}image_url")
            )
        }
    }
}

open class LiveStream(
    val channel: PersistedChannel,
    val title: String,
    val live: Boolean,
    val liveSince: DateTime,
    val imageUrl: String?
)

class LiveStreamService(private val hikariDataSource: HikariDataSource) {
    fun updateLiveStreamsOfSourceSystem(sourceSystemId: SourceSystemId, liveStreams: List<LiveStream>): Unit {
        return using(sessionOf(hikariDataSource)) { session ->
            session.transaction { trx ->
                trx.run(
                    queryOf(
                        """
                            DELETE FROM live_streams
                            USING channels
                            WHERE live_streams.channel_id = channels.id AND channels.source_system = :source_system
                        """,
                        mapOf("source_system" to sourceSystemId.name)
                    ).asExecute
                )

                liveStreams.forEach {
                    trx.run(
                        queryOf(
                            """
                                INSERT INTO live_streams (channel_id, title, live, live_since, image_url)
                                VALUES (:channel_id, :title, :live, :live_since, :image_url)
                            """,
                            mapOf(
                                "channel_id" to it.channel.id,
                                "title" to it.title,
                                "live" to it.live,
                                "live_since" to it.liveSince,
                                "image_url" to it.imageUrl
                            )
                        ).asExecute
                    )
                }
            }
        }
    }

    fun getLiveStreamsOfChannels(channels: List<PersistedChannel>): List<PersistedLiveStream> {
        return using(sessionOf(hikariDataSource)) { session ->
            val channelIds = session.createArrayOf("int4", channels.map { it.id })

            session.run(
                queryOf(
                    """
                        SELECT
                            live_streams.id as live_stream_id,
                            live_streams.channel_id as live_stream_channel_id,
                            live_streams.title as live_stream_title,
                            live_streams.live as live_stream_live,
                            live_streams.live_since as live_stream_live_since,
                            live_streams.image_url as live_stream_image_url,
                            channels.id as channel_id,
                            channels.source_system as channel_source_system,
                            channels.id_in_source_system as channel_id_in_source_system,
                            channels.name as channel_name,
                            channels.url as channel_url,
                            channels.image_url as channel_image_url
                        FROM live_streams
                        INNER JOIN channels
                        ON live_streams.channel_id = channels.id
                        WHERE live_streams.channel_id = ANY (:channel_ids)
                    """,
                    mapOf(
                        "channel_ids" to channelIds
                    )
                ).map {
                    PersistedLiveStream.fromRow(it, "live_stream_", "channel_")
                }.asList
            )
        }
    }
}
