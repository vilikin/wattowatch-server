package com.vilikin

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import kotliquery.HikariCP
import org.flywaydb.core.Flyway
import org.kodein.di.generic.bind
import org.kodein.di.generic.instance
import org.kodein.di.generic.singleton
import org.kodein.di.ktor.kodein

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@KtorExperimentalAPI
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(Compression)
    install(CORS)
    install(CallLogging)
    install(ContentNegotiation) { gson() }

    val dbConfig = environment.config.config("db.jdbc")
    val url = dbConfig.property("url").getString()
    val username = dbConfig.property("username").getString()
    val password = dbConfig.property("password").getString()

    val hikari = HikariCP.default(url, username, password)
    Flyway.configure().dataSource(hikari).load().migrate()

    kodein {
        bind<PersonService>() with singleton { PersonService(hikari) }
    }

    routing {
        get("/") {
            val personService by kodein().instance<PersonService>()
            call.respond(personService.getPersons())
        }

        get("/json/gson") {
            call.respond(mapOf("hello" to "world"))
        }
    }
}
