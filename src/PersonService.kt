package com.vilikin

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using

data class Person(
    val id: Int,
    val name: String
) {
    companion object {
        fun fromRow(row: Row): Person {
            return Person(row.int("id"), row.string("name"))
        }
    }
}

class PersonService(private val hikariDataSource: HikariDataSource) {
    fun getPersons(): List<Person> {
        return using(sessionOf(hikariDataSource)) { session ->
            session.run(queryOf("SELECT * FROM person").map { Person.fromRow(it) }.asList)
        }
    }
}
