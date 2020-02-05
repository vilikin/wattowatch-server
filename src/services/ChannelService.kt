package com.vilikin.services

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using

class PersistedChannel(
    val id: Int,
    sourceSystem: SourceSystemId,
    idInSourceSystem: String,
    name: String,
    url: String?
) : Channel(sourceSystem, idInSourceSystem, name, url) {
    companion object {
        fun fromRow(row: Row, channelPrefix: String = ""): PersistedChannel {
            return PersistedChannel(
                row.int("${channelPrefix}id"),
                SourceSystemId.valueOf(row.string("${channelPrefix}source_system")),
                row.string("${channelPrefix}id_in_source_system"),
                row.string("${channelPrefix}name"),
                row.stringOrNull("${channelPrefix}url")
            )
        }
    }
}

open class Channel(
    val sourceSystem: SourceSystemId,
    val idInSourceSystem: String,
    val name: String,
    val url: String?
)

class ChannelService(private val hikariDataSource: HikariDataSource) {
    fun createChannel(channel: Channel): PersistedChannel {
        return using(sessionOf(hikariDataSource)) { session ->
            session.run(
                queryOf(
                    """
                        INSERT INTO channels (source_system, id_in_source_system, name, url)
                        VALUES (:source_system, :id_in_source_system, :name, :url)
                        RETURNING *
                    """,
                    mapOf(
                        "source_system" to channel.sourceSystem.name,
                        "id_in_source_system" to channel.idInSourceSystem,
                        "name" to channel.name,
                        "url" to channel.url
                    )
                ).map {
                    PersistedChannel.fromRow(it)
                }.asSingle
            )!!
        }
    }

    fun getChannel(idInSourceSystem: String, sourceSystemId: SourceSystemId): PersistedChannel? {
        return using(sessionOf(hikariDataSource)) { session ->
            session.run(
                queryOf(
                    """
                        SELECT * FROM channels
                        WHERE id_in_source_system = :id_in_source_system AND source_system = :source_system
                    """,
                    mapOf(
                        "source_system" to sourceSystemId.name,
                        "id_in_source_system" to idInSourceSystem
                    )
                ).map {
                    PersistedChannel.fromRow(it)
                }.asSingle
            )
        }
    }

    fun getChannelsOfSourceSystem(sourceSystem: SourceSystemId): List<PersistedChannel> {
        return using(sessionOf(hikariDataSource)) { session ->
            session.run(
                queryOf(
                    "SELECT * FROM channels WHERE source_system = :source_system",
                    mapOf(
                        "source_system" to sourceSystem.name
                    )
                ).map {
                    PersistedChannel.fromRow(it)
                }.asList
            )
        }
    }

    fun addSubscription(channel: PersistedChannel, user: PersistedUser) {
        return using(sessionOf(hikariDataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO user_is_subscribed_to_channel (user_id, channel_id) VALUES (:user_id, :channel_id)",
                    mapOf(
                        "user_id" to user.id,
                        "channel_id" to channel.id
                    )
                ).asExecute
            )
        }
    }

    fun getChannelsUserIsSubscribedTo(userId: Int): List<PersistedChannel> {
        return using(sessionOf(hikariDataSource)) { session ->
            session.run(
                queryOf(
                    """
                        SELECT id, source_system, id_in_source_system, name, url FROM user_is_subscribed_to_channel
                        INNER JOIN channels ON user_is_subscribed_to_channel.channel_id = channels.id
                        WHERE user_is_subscribed_to_channel.user_id = :user_id
                    """,
                    mapOf(
                        "user_id" to userId
                    )
                ).map {
                    PersistedChannel.fromRow(it)
                }.asList
            )
        }
    }
}
