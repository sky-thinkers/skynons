package com.skythinkers.skynons.database

import io.ktor.server.application.Application
import io.ktor.server.application.log
import org.jetbrains.exposed.v1.jdbc.Database

fun Application.openDatabase(): DatabaseConnection {
    val embedded = environment.config.property("db.embedded").getString().toBooleanStrictOrNull() ?: true
    return DatabaseConnection(connectToPostgres(embedded))
}

private fun Application.connectToPostgres(embedded: Boolean): Database {
    Class.forName("org.postgresql.Driver")
    if (embedded) {
        log.warn("DEV_ENV: Using embedded H2 database")
        return Database.connect(
            url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
            user = "root",
            driver = "org.h2.Driver",
            password = "",
        )
    } else {
        val url = environment.config.property("db.url").getString()
        val user = environment.config.property("db.user").getString()
        val password = environment.config.property("db.password").getString()
        log.info("Connecting to postgres database at $url")

        return Database.connect(
            url = url,
            user = user,
            password = password,
            driver = "org.h2.Driver",
        )
    }
}
