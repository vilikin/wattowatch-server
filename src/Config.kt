package com.vilikin

import kotliquery.HikariCP

class MissingRequiredEnvVariable(key: String) : Exception("Missing required environment variable $key")

private inline fun <reified T> requireEnvVar(key: String): T {
    val rawValue = System.getenv(key) ?: throw MissingRequiredEnvVariable(key)
    return when (T::class) {
        String::class -> rawValue as T
        Int::class -> Integer.parseInt(rawValue, 10) as T
        else -> throw Exception("Unsupported env variable type")
    }
}

object Config {
    val port: Int = requireEnvVar("PORT")
    val jdbc = JDBCConfig
    val twitch = TwitchConfig
    val yle = YleConfig
    val theMovieDb = TheMovieDbConfig

    val hikariDataSource = HikariCP.default(jdbc.url, jdbc.username, jdbc.password)
}

object JDBCConfig {
    val url: String = requireEnvVar("JDBC_DATABASE_URL")
    val username: String = requireEnvVar("JDBC_DATABASE_USERNAME")
    val password: String = requireEnvVar("JDBC_DATABASE_PASSWORD")
}

object TwitchConfig {
    val baseUrl: String = requireEnvVar("TWITCH_BASE_URL")
    val clientId: String = requireEnvVar("TWITCH_CLIENT_ID")
}

object YleConfig {
    val baseUrl: String = requireEnvVar("YLE_BASE_URL")
    val appId: String = requireEnvVar("YLE_APP_ID")
    val appKey: String = requireEnvVar("YLE_APP_KEY")
}

object TheMovieDbConfig {
    val baseUrl: String = requireEnvVar("THE_MOVIE_DB_BASE_URL")
    val apiKey: String = requireEnvVar("THE_MOVIE_DB_API_KEY")
}
