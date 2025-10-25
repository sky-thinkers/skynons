package com.skythinkers.skynons.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.skythinkers.skynons.api.LoginRequest
import com.skythinkers.skynons.api.PasswordUpdateRequest
import com.skythinkers.skynons.api.RegistrationRequest
import com.skythinkers.skynons.api.ShortUserInfo
import com.skythinkers.skynons.api.UserId
import com.skythinkers.skynons.database.asResult
import com.skythinkers.skynons.model.user.UserCredentials
import com.skythinkers.skynons.model.user.UserRegistrationInfo
import com.skythinkers.skynons.model.user.UserUpdatePasswordInfo
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.datetime.Clock
import kotlinx.datetime.toJavaInstant
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.coroutines.cancellation.CancellationException

class AccountManagerImpl(
    private val authConfig: AuthConfig,
) : AccountManager {
    private val logger = KtorSimpleLogger("AccountManagerImpl")

    private val digestThreadLocal = ThreadLocal.withInitial {
        MessageDigest.getInstance(authConfig.hashingAlgorithmId)
    }

    private val secureRandomThreadLocal = ThreadLocal.withInitial {
        SecureRandom.getInstanceStrong()
    }

    private val digest: MessageDigest get() = digestThreadLocal.get()

    private val secureRandom get() = secureRandomThreadLocal.get()

    private val signAlgorithm = Algorithm.HMAC256(authConfig.secret)

    override val jwtVerifier: JWTVerifier =
        JWT.require(Algorithm.HMAC256(authConfig.secret)).withAudience(authConfig.audience)
            .withIssuer(authConfig.issuer).build()

    override suspend fun authenticateUser(loginRequest: LoginRequest): Result<Pair<ShortUserInfo, UserSession>> {
        val credentials = authConfig.credentialsDatabase.getUserCredentialsByLogin(loginRequest.login)
            .asResult()
            .getOrElse {
                return Result.failure(it) // TODO(firelion9): map errors
            }

        return if (checkPassword(credentials, loginRequest.password)) {
            logger.debug("Successfully authenticated `${loginRequest.login}`")
            // TODO(firelion9): multiple transactions
            Result.success(
                ShortUserInfo(credentials.uid, credentials.login) to
                        UserSession(
                            createAndRegisterToken(credentials.uid)
                                .getOrElse { return Result.failure(it) })
            ) // TODO(firelion9): map errors
        } else {
            logger.debug("Failed to authenticate `${loginRequest.login}`")
            Result.failure(AuthFailedException("Wrong login or password"))
        }
    }

    override suspend fun registerUser(registrationRequest: RegistrationRequest): Result<Pair<ShortUserInfo, UserSession>> {
        val (salt, passwordHash) = saltAndHash(registrationRequest.password)

        val registrationInfo = UserRegistrationInfo(
            login = registrationRequest.login,
            salt = salt,
            passwordHash = passwordHash,
        )
        val userInfo = authConfig.credentialsDatabase.registerUser(registrationInfo)
            .asResult().getOrElse {
                logger.debug("Failed to register `${registrationRequest.login}`", it)
                return Result.failure(it) // TODO(firelion9): map errors
            }

        logger.info("Successfully registered user `${registrationInfo.login}")
        val token = createAndRegisterToken(userInfo.uid).getOrElse {
            return Result.failure(it) // TODO(firelion9): map errors
        }
        return Result.success(userInfo to UserSession(token))
    }

    override suspend fun updateUserPassword(
        userId: UserId,
        updateRequest: PasswordUpdateRequest,
    ): Result<Unit> {
        val credentials = authConfig.credentialsDatabase.getUserCredentialsByUserId(userId).asResult()
            .getOrElse {
                return Result.failure(it) // TODO(firelion9): map errors
            }

        return if (checkPassword(credentials, updateRequest.oldPassword)) {
            logger.debug("Changing password for `$userId`")
            val (salt, passwordHash) = saltAndHash(updateRequest.newPassword)
            // TODO(firelion9): map errors, multiple transactions
            authConfig.credentialsDatabase.updateUserPassword(
                UserUpdatePasswordInfo(
                    uid = userId,
                    salt = salt,
                    passwordHash = passwordHash,
                )
            ).asResult()
        } else {
            logger.debug("Old password mismatch when trying to change password for `$userId`")
            Result.failure(AuthFailedException("Wrong uid or old password"))
        }
    }

    override suspend fun validateSession(session: UserSession): Result<Boolean> {
        runCatching { jwtVerifier.verify(session.token) }.getOrElse {
            logger.debug("JWT token verification failed", it)
            return Result.success(false)
        }

        val expirationDate = authConfig.credentialsDatabase.getTokenExpirationDate(hashToken(session.token))
            .asResult().getOrElse {
                return Result.failure(it) // TODO(firelion9): map errors
            } ?: return Result.success(false)

        return Result.success(Clock.System.now() <= expirationDate)
    }

    override suspend fun invalidateSession(session: UserSession): Result<Unit> {
        // TODO(firelion9): map errors
        return authConfig.credentialsDatabase.invalidateToken(hashToken(session.token)).asResult()
    }

    override suspend fun getShortUserInfo(session: UserSession): Result<ShortUserInfo> {
        // TODO(firelion9): map errors
        return authConfig.database.getUserInfoByUserId(session.uid).asResult()
    }

    override suspend fun cleanupLoop() {
        while (currentCoroutineContext().isActive) {
            try {
                delay(authConfig.cleanupInterval)
                logger.debug("Starting expired tokens cleanup")
                authConfig.credentialsDatabase.removeExpiredTokens().asResult().getOrThrow()
                logger.debug("Expired tokens cleanup completed")
            } catch (_: CancellationException) {
                break
            } catch (e: Exception) {
                logger.error("Expired tokens cleanup failed", e)
            }
        }
        logger.info("Exiting expired tokens cleanup loop")
    }

    private fun saltAndHash(password: String): Pair<ByteArray, ByteArray> {
        val salt = ByteArray(authConfig.saltSize).apply(secureRandom::nextBytes)
        val hash = hashPassword(salt, password)
        return salt to hash
    }

    private fun checkPassword(credentials: UserCredentials, password: String): Boolean {
        val passwordHash = hashPassword(credentials.salt, password)

        return passwordHash.contentEquals(credentials.passwordHash)
    }

    private fun hashPassword(salt: ByteArray, password: String): ByteArray {
        return with(digest) {
            update(authConfig.pepper)
            update(':'.code.toByte())
            update(salt)
            update(':'.code.toByte())
            update(password.encodeToByteArray())
            digest()
        }
    }

    private suspend fun createAndRegisterToken(uid: UserId): Result<String> {
        val expiresAt = Clock.System.now() + authConfig.tokenValidityPeriod
        return JWT.create().withAudience(authConfig.audience).withIssuer(authConfig.issuer)
            .withClaim(UserSession.USER_ID_CLAIM_NAME, uid)
            .withExpiresAt(expiresAt.toJavaInstant())
            .sign(signAlgorithm)
            .let { token ->
                val hash = hashToken(token)
                authConfig.credentialsDatabase.registerToken(hash, expiresAt).asResult()
                    .map { token }
            }
    }

    private fun hashToken(token: String): ByteArray {
        return with(digest) {
            update(token.encodeToByteArray())
            digest()
        }
    }
}