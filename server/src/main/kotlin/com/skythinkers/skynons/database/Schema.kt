package com.skythinkers.skynons.database

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.dao.id.CompositeIdTable
import org.jetbrains.exposed.v1.core.dao.id.IdTable
import org.jetbrains.exposed.v1.datetime.timestamp

private const val HASH_LENGTH: Int = 32

enum class Constraint {
    UNIQUE_USERS_LOGIN
}

object Users : Table() {
    val uid = long("uid").autoIncrement()
    val login = varchar("login", length = 50).uniqueIndex(Constraint.UNIQUE_USERS_LOGIN.name)
    val salt = binary("salt", length = HASH_LENGTH)
    val passwordHash = binary("passwordHash", length = HASH_LENGTH)
    val version = long("version").default(0L)

    override val primaryKey = PrimaryKey(uid)
}

object Sessions : Table() {
    val tokenHash = binary("tokenHash", HASH_LENGTH)
    val expirationDate = timestamp("expirationDate")
    val uid = long("uid").references(Users.uid)
    val credentialsVersion = long("credentialsVersion")

    override val primaryKey = PrimaryKey(tokenHash)
}

object Blobs : Table() {
    val blobHash = binary("blobHash", HASH_LENGTH)
    val accessed = timestamp("accessed")
    val state = integer("state").default(0)

    override val primaryKey = PrimaryKey(blobHash)

    const val STATE_ACTIVE: Int = 0
    const val STATE_FOR_REMOVAL: Int = 1
}

object BlobRefs : CompositeIdTable() {
    val blobHash = binary("blobHash", HASH_LENGTH).references(Blobs.blobHash).entityId()
    val owner = long("owner").references(Users.uid).entityId()
    val created = timestamp("created")
    val modified = timestamp("modified")
    val accessed = timestamp("accessed")

    override val primaryKey = PrimaryKey(blobHash, owner)
}

object SimulationHistory : Table() {
    val id = long("id").autoIncrement()
    val owner = long("owner").references(Users.uid)
    val timestamp = timestamp("timestamp")
    val configRef = binary("configRef", HASH_LENGTH)
    val resultsRef = binary("resultsRef", HASH_LENGTH).nullable()

    override val primaryKey = PrimaryKey(id)

    init {
        foreignKey(configRef, owner, target = BlobRefs.primaryKey)
        foreignKey(resultsRef, owner, target = BlobRefs.primaryKey)
    }
}
