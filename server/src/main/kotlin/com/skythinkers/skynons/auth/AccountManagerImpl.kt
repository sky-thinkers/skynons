package com.skythinkers.skynons.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.skythinkers.skynons.api.LoginRequest
import com.skythinkers.skynons.api.PasswordUpdateRequest
import com.skythinkers.skynons.api.RegistrationRequest
import com.skythinkers.skynons.api.ShortUserInfo
import com.skythinkers.skynons.api.UserId
import com.skythinkers.skynons.database.DatabaseException
import com.skythinkers.skynons.database.DatabaseResult
import com.skythinkers.skynons.database.asResult
import com.skythinkers.skynons.model.user.GeneratedToken
import com.skythinkers.skynons.model.user.UserCredentials
import com.skythinkers.skynons.model.user.UserCredentialsRef
import com.skythinkers.skynons.model.user.UserRegistrationInfo
import com.skythinkers.skynons.model.user.UserUpdatePasswordInfo
import com.skythinkers.skynons.model.user.toRef
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.toJavaInstant

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
        val credentials =
            when (val cRes = authConfig.credentialsDatabase.getUserCredentialsByLogin(loginRequest.login)) {
                is DatabaseResult.Success -> cRes.res
                !is DatabaseResult.Error -> throw AssertionError("Unreachable code")
                is DatabaseResult.NotFound -> return Result.failure(AuthFailedException("Wrong login or password"))
                else -> return Result.failure(AuthFailedException("Internal error", DatabaseException(cRes)))
            }

        return if (checkPassword(credentials, loginRequest.password)) {
            logger.debug("Successfully authenticated `${loginRequest.login}`")
            createAndRegisterToken(credentials.toRef()).map {
                ShortUserInfo(credentials.uid, credentials.login) to it
            }
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
        val (token, userInfo) = when (val cRes =
            authConfig.credentialsDatabase.registerUser(registrationInfo, this::generateToken)) {
            is DatabaseResult.Success -> {
                logger.info("Successfully registered user `${registrationInfo.login}")
                cRes.res
            }

            !is DatabaseResult.Error -> throw AssertionError("Unreachable code")
            is DatabaseResult.ConstraintViolation -> {
                logger.debug("Failed to register `${registrationRequest.login}`", DatabaseException(cRes))
                return Result.failure(AccountException("Use another username"))
            }

            else -> {
                logger.debug("Failed to register `${registrationRequest.login}`", DatabaseException(cRes))
                return Result.failure(AccountException("Internal error", DatabaseException(cRes)))
            }
        }

        return Result.success(userInfo to token)
    }

    override suspend fun updateUserPassword(
        userId: UserId,
        updateRequest: PasswordUpdateRequest,
    ): Result<Unit> {
        val credentials = authConfig.credentialsDatabase.getUserCredentialsByUserId(userId).asResult()
            .getOrElse {
                logger.debug("Failed to get credentials for uid $userId", it)
                return Result.failure(AccountException("Internal error", it))
            }

        return if (checkPassword(credentials, updateRequest.oldPassword)) {
            logger.debug("Changing password for `$userId`")
            val (salt, passwordHash) = saltAndHash(updateRequest.newPassword)
            when (val cRes = authConfig.credentialsDatabase.updateUserPassword(
                UserUpdatePasswordInfo(
                    uid = userId,
                    salt = salt,
                    passwordHash = passwordHash,
                    previousVersion = credentials.version,
                )
            )) {

                is DatabaseResult.Success -> Result.success(cRes.res)
                !is DatabaseResult.Error -> throw AssertionError("Unreachable code")
                else -> {
                    logger.debug("Failed to update password for uid $userId", DatabaseException(cRes))
                    return Result.failure(AccountException("Internal error", DatabaseException(cRes)))
                }
            }
        } else {
            logger.debug("Old password mismatch when trying to change password for `$userId`")
            Result.failure(AccountException("Wrong uid or old password"))
        }
    }

    override suspend fun validateSession(session: UserSession): Result<Boolean> {
        runCatching { jwtVerifier.verify(session.token) }.getOrElse {
            logger.debug("JWT token verification failed", it)
            return Result.success(false)
        }

        val isValid = authConfig.credentialsDatabase.validateToken(hashToken(session.token))
            .asResult().getOrElse {
                logger.debug("Failed to validate token for uid ${session.uid}", it)
                return Result.failure(AccountException("Internal error", it))
            }

        return Result.success(isValid)
    }

    override suspend fun invalidateSession(session: UserSession): Result<Unit> {
        return authConfig.credentialsDatabase.invalidateToken(hashToken(session.token)).asResult()
            .onFailure {
                logger.debug("Failed to invalidate session for uid ${session.uid}, token ${session.token}", it)
                return Result.failure(AccountException("Internal error", it))
            }
    }

    override suspend fun getShortUserInfo(session: UserSession): Result<ShortUserInfo> {
        return authConfig.database.getUserInfoByUserId(session.uid).asResult()
            .onFailure {
                logger.debug("Failed to get short user info for uid ${session.uid}, token ${session.token}", it)
                return Result.failure(AccountException("Internal error", it))
            }
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

    private suspend fun createAndRegisterToken(forCredentials: UserCredentialsRef): Result<UserSession> {
        val tokenInfo = generateToken(forCredentials.uid)
        return authConfig.credentialsDatabase.registerToken(tokenInfo.hash, tokenInfo.expiresAt, forCredentials)
            .asResult()
            .map { tokenInfo.token }
    }

    private fun generateToken(uid: UserId): GeneratedToken<UserSession> {
        val expiresAt = kotlin.time.Clock.System.now() + authConfig.tokenValidityPeriod
        return JWT.create().withAudience(authConfig.audience).withIssuer(authConfig.issuer)
            .withClaim(UserSession.USER_ID_CLAIM_NAME, uid)
            .withClaim("nonce", secureRandom.nextLong())
            .withExpiresAt(expiresAt.toJavaInstant())
            .sign(signAlgorithm)
            .let { token ->
                val hash = hashToken(token)
                GeneratedToken(UserSession(token), hash, expiresAt)
            }
    }

    private fun hashToken(token: String): ByteArray {
        return with(digest) {
            update(token.encodeToByteArray())
            digest()
        }
    }
}
