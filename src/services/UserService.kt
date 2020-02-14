package com.vilikin.services

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using

class PersistedUser(
    val id: Int,
    name: String
) : User(name) {
    companion object {
        fun fromRow(row: Row): PersistedUser {
            return PersistedUser(row.int("id"), row.string("name"))
        }
    }
}

open class User(
    val name: String
)

class UserService(private val hikariDataSource: HikariDataSource) {
    fun createUser(user: User): PersistedUser {
        return using(sessionOf(hikariDataSource)) { session ->
            session.run(queryOf("INSERT INTO users (name) VALUES (:name) RETURNING *", mapOf("name" to user.name)).map {
                PersistedUser.fromRow(it)
            }.asSingle)!!
        }
    }

    fun isUsernameAvailable(name: String): Boolean {
        return using(sessionOf(hikariDataSource)) { session ->
            session.run(queryOf("SELECT * FROM users WHERE name = :name", mapOf("name" to name)).map {
                PersistedUser.fromRow(it)
            }.asList).isEmpty()
        }
    }

    fun getUser(id: Int): PersistedUser? {
        return using(sessionOf(hikariDataSource)) { session ->
            session.run(queryOf("SELECT * FROM users WHERE id = :id", mapOf("id" to id)).map {
                PersistedUser.fromRow(it)
            }.asSingle)
        }
    }

    fun getUser(name: String): PersistedUser? {
        return using(sessionOf(hikariDataSource)) { session ->
            session.run(queryOf("SELECT * FROM users WHERE name = :name", mapOf("name" to name)).map {
                PersistedUser.fromRow(it)
            }.asSingle)
        }
    }
}
