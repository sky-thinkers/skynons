package com.skythinkers.skynons.database

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp

private const val HASH_LENGTH: Int = 32

enum class Constraint {
    UNIQUE_USERS_LOGIN
}

object Users : Table() {
    val uid = long("uid").autoIncrement()
    val login = varchar("login", length = 50).uniqueIndex(Constraint.UNIQUE_USERS_LOGIN.name)
    val salt = binary("salt", length = HASH_LENGTH)
    val passwordHash = binary("passwordHash", length = HASH_LENGTH)

    override val primaryKey = PrimaryKey(uid)
}

object Sessions : Table() {
    val tokenHash = binary("tokenHash", HASH_LENGTH)
    val expirationDate = timestamp("expirationDate")

    override val primaryKey = PrimaryKey(tokenHash)
}
