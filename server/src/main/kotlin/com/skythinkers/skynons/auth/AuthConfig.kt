package com.skythinkers.skynons.auth

import com.skythinkers.skynons.database.CredentialsDatabase
import com.skythinkers.skynons.database.SkynonsDatabase
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours


class AuthConfig(
    val issuer: String,
    val audience: String,
    val secret: ByteArray,
    val pepper: ByteArray,
    val database: SkynonsDatabase,
    val credentialsDatabase: CredentialsDatabase,
    val tokenValidityPeriod: Duration = DEFAULT_TOKEN_VALIDITY_PERIOD,
    val saltSize: Int = DEFAULT_SALT_SIZE,
    val hashingAlgorithmId: String = DEFAULT_HASHING_ALGORITHM_ID,
    val cleanupInterval: Duration = DEFAULT_CLEANUP_INTERVAL,
) {
    companion object {
        private val DEFAULT_TOKEN_VALIDITY_PERIOD: Duration = 30.days
        private const val DEFAULT_SALT_SIZE: Int = 32
        private const val DEFAULT_HASHING_ALGORITHM_ID = "SHA-256"
        private val DEFAULT_CLEANUP_INTERVAL = 1.hours
    }
}
