package com.vilikin

import com.vilikin.routes.channelRoutes
import com.vilikin.routes.liveStreamRoutes
import com.vilikin.routes.sourceSystemRoutes
import com.vilikin.routes.userRoutes
import com.vilikin.routes.videoRoutes
import com.vilikin.services.ChannelService
import com.vilikin.services.LiveStreamService
import com.vilikin.services.SourceSystemService
import com.vilikin.services.UserService
import com.vilikin.services.VideoService
import com.vilikin.utils.DateTimeDeserializer
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
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
    install(CORS) {
        anyHost()
        HttpMethod.DefaultMethods.forEach { method(it) }
        header(HttpHeaders.ContentType)
        header("x-user-id")
    }
    install(CallLogging)
    install(ContentNegotiation) {
        gson {
            registerTypeAdapter(DateTime::class.java, DateTimeDeserializer())
        }
    }

    Flyway.configure().dataSource(Config.hikariDataSource).load().migrate()

    kodein {
        val userService = UserService(Config.hikariDataSource)
        val channelService = ChannelService(Config.hikariDataSource)
        val liveStreamService = LiveStreamService(Config.hikariDataSource)
        val videoService = VideoService(Config.hikariDataSource)
        val sourceSystemService = SourceSystemService(channelService, liveStreamService, videoService)

        bind<UserService>() with singleton { userService }
        bind<ChannelService>() with singleton { channelService }
        bind<LiveStreamService>() with singleton { liveStreamService }
        bind<VideoService>() with singleton { videoService }
        bind<SourceSystemService>() with singleton { sourceSystemService }
    }

    routing {
        userRoutes()
        channelRoutes()
        liveStreamRoutes()
        videoRoutes()
        sourceSystemRoutes()
    }
}
