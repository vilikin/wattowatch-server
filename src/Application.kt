package com.vilikin

import com.vilikin.routes.channelRoutes
import com.vilikin.routes.liveStreamRoutes
import com.vilikin.routes.userRoutes
import com.vilikin.services.Channel
import com.vilikin.services.ChannelService
import com.vilikin.services.LiveStream
import com.vilikin.services.LiveStreamService
import com.vilikin.services.PersistedChannel
import com.vilikin.services.SourceSystemId
import com.vilikin.services.SourceSystemService
import com.vilikin.services.UserService
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
import org.joda.time.DateTime
import org.kodein.di.generic.bind
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

    Flyway.configure().dataSource(Config.hikariDataSource).load().migrate()

    kodein {
        val userService = UserService(Config.hikariDataSource)
        val channelService = ChannelService(Config.hikariDataSource)
        val liveStreamService = LiveStreamService(Config.hikariDataSource)
        val sourceSystemService = SourceSystemService(channelService, liveStreamService)

        bind<UserService>() with singleton { userService }
        bind<ChannelService>() with singleton { channelService }
        bind<LiveStreamService>() with singleton { liveStreamService }
        bind<SourceSystemService>() with singleton { sourceSystemService }
    }

    routing {
        userRoutes()
        channelRoutes()
        liveStreamRoutes()
    }
}
