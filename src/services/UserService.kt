package com.vilikin.services

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using

data class User(
    val id: Int,
    val name: String
) {
    companion object {
        fun fromRow(row: Row): User {
            return User(row.int("id"), row.string("name"))
        }
    }
}

class UserService(private val hikariDataSource: HikariDataSource) {
    fun createUser(name: String): User {
        return using(sessionOf(hikariDataSource)) { session ->
            session.run(queryOf("INSERT INTO users (name) VALUES (:name) RETURNING *", mapOf("name" to name)).map {
                User.fromRow(it)
            }.asSingle)!!
        }
    }

    fun isUsernameAvailable(name: String): Boolean {
        return using(sessionOf(hikariDataSource)) { session ->
            session.run(queryOf("SELECT * FROM users WHERE name = :name", mapOf("name" to name)).map {
                User.fromRow(it)
            }.asList).isEmpty()
        }
    }
}
