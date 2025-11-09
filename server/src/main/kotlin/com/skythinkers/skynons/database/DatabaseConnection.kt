package com.skythinkers.skynons.database

import com.skythinkers.skynons.api.ShortUserInfo
import com.skythinkers.skynons.api.UserId
import com.skythinkers.skynons.model.user.GeneratedToken
import com.skythinkers.skynons.model.user.UserCredentials
import com.skythinkers.skynons.model.user.UserCredentialsRef
import com.skythinkers.skynons.model.user.UserRegistrationInfo
import com.skythinkers.skynons.model.user.UserUpdatePasswordInfo
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.Instant
import org.jetbrains.exposed.exceptions.ExposedSQLException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.postgresql.util.PSQLException

class DatabaseConnection(private val database: Database) : SkynonsDatabase, CredentialsDatabase {
    init {
        transaction(database) {
            SchemaUtils.create(Users, Sessions)
        }
    }

    private val logger = KtorSimpleLogger("DatabaseConnection")

    override suspend fun getUserInfoByUserId(uid: UserId): DatabaseResult<ShortUserInfo> = transaction {
        val user = Users
            .select(Users.uid, Users.login)
            .where { (Users.uid eq uid) }
            .map { ShortUserInfo(uid = it[Users.uid], login = it[Users.login]) }
            .singleOrNull()

        when (user) {
            null -> DatabaseResult.NotFound("No such user $uid")
            else -> DatabaseResult.Success(user)
        }
    }

    override suspend fun getUserInfoByLogin(login: String): DatabaseResult<ShortUserInfo> = transaction {
        val user = Users
            .select(Users.uid, Users.login)
            .where { (Users.login eq login) }
            .map { ShortUserInfo(uid = it[Users.uid], login = it[Users.login]) }
            .singleOrNull()

        when (user) {
            null -> DatabaseResult.NotFound("No such user $login")
            else -> DatabaseResult.Success(user)
        }
    }

    override suspend fun <T> registerUser(
        info: UserRegistrationInfo,
        tokenGenerator: (uid: UserId) -> GeneratedToken<T>,
    ): DatabaseResult<Pair<T, ShortUserInfo>> = transaction {
        val uid = Users.insert {
            it[login] = info.login
            it[salt] = info.salt
            it[passwordHash] = info.passwordHash
        }[Users.uid]

        val token = tokenGenerator(uid)

        Sessions.insert {
            it[Sessions.tokenHash] = token.hash
            it[Sessions.expirationDate] = token.expiresAt
            it[Sessions.uid] = uid
            it[Sessions.credentialsVersion] = 0L
        }

        DatabaseResult.Success(token.token to ShortUserInfo(uid, info.login))
    }

    override suspend fun getUserCredentialsByLogin(login: String): DatabaseResult<UserCredentials> = transaction {
        val info = Users
            .select(Users.uid, Users.login, Users.salt, Users.passwordHash, Users.version)
            .where { (Users.login eq login) }
            .map {
                UserCredentials(
                    uid = it[Users.uid],
                    login = it[Users.login],
                    salt = it[Users.salt],
                    passwordHash = it[Users.passwordHash],
                    version = it[Users.version],
                )
            }
            .singleOrNull()

        when (info) {
            null -> DatabaseResult.NotFound("No such user $login")
            else -> DatabaseResult.Success(info)
        }
    }

    override suspend fun getUserCredentialsByUserId(uid: UserId): DatabaseResult<UserCredentials> = transaction {
        val info = Users
            .select(Users.uid, Users.login, Users.salt, Users.passwordHash, Users.version)
            .where { (Users.uid eq uid) }
            .map {
                UserCredentials(
                    uid = it[Users.uid],
                    login = it[Users.login],
                    salt = it[Users.salt],
                    passwordHash = it[Users.passwordHash],
                    version = it[Users.version],
                )
            }
            .singleOrNull()

        when (info) {
            null -> DatabaseResult.NotFound("No such user $uid")
            else -> DatabaseResult.Success(info)
        }
    }

    override suspend fun updateUserPassword(updateInfo: UserUpdatePasswordInfo): DatabaseResult<Unit> = transaction {
        val updatedCount = Users
            .update(
                where = {
                    (Users.uid eq updateInfo.uid) and (Users.version eq updateInfo.previousVersion)
                }
            ) {

                it[Users.salt] = updateInfo.salt
                it[Users.passwordHash] = updateInfo.passwordHash
                it[Users.version] = updateInfo.previousVersion + 1
            }

        when (updatedCount) {
            0 -> DatabaseResult.NotFound("Version mismatch or no such user uid ${updateInfo.uid}")
            else -> DatabaseResult.Success(Unit)
        }
    }

    override suspend fun registerToken(
        tokenHash: ByteArray,
        expirationDate: Instant,
        forCredentials: UserCredentialsRef,
    ): DatabaseResult<Unit> = transaction {
        val versionMatches = Users.select(Users.uid, Users.version)
            .where {
                (Users.uid eq forCredentials.uid) and (Users.version eq forCredentials.version)
            }.count() == 1L

        if (versionMatches) {
            Sessions.insert {
                it[Sessions.tokenHash] = tokenHash
                it[Sessions.expirationDate] = expirationDate
                it[Sessions.uid] = forCredentials.uid
                it[Sessions.credentialsVersion] = forCredentials.version
            }

            DatabaseResult.Success(Unit)
        } else {
            DatabaseResult.NotFound("Version or uid mismatch")
        }
    }

    override suspend fun invalidateToken(tokenHash: ByteArray): DatabaseResult<Unit> = transaction {
        Sessions
            .deleteWhere {
                Sessions.tokenHash eq tokenHash
            }

        DatabaseResult.Success(Unit)
    }

    override suspend fun validateToken(tokenHash: ByteArray, now: Instant): DatabaseResult<Boolean> = transaction {
        val activeTokens = Sessions
            .selectAll()
            .where {
                (Sessions.tokenHash eq tokenHash) and (Sessions.expirationDate greaterEq now)
            }
            .count()

        DatabaseResult.Success(activeTokens == 1L)
    }

    override suspend fun removeExpiredTokens(byDate: Instant): DatabaseResult<Unit> = transaction {
        val removedTokens = Sessions
            .deleteWhere {
                Sessions.expirationDate less byDate
            }

        logger.debug("Removed {} tokens expired by {}", removedTokens, byDate)

        DatabaseResult.Success(Unit)
    }

    private suspend fun <T> transaction(block: suspend () -> DatabaseResult<T>): DatabaseResult<T> =
        runCatching { newSuspendedTransaction(Dispatchers.IO, database) { block() } }
            .getOrElse { e ->
                return when {
                    e.isSerializationFailure() -> DatabaseResult.GeneralError("Serialization failure", e)
                    e !is ExposedSQLException -> DatabaseResult.GeneralError("Non-SQL exception", e)
                    e.isConnectionException() -> DatabaseResult.ConnectionError("Non-SQL exception", e)
                    e.isConstraintViolation() -> mapConstraintViolationException(e)
                    else -> DatabaseResult.GeneralError("Unknown error", e)
                }
            }

    companion object {

        private fun mapConstraintViolationException(e: ExposedSQLException): DatabaseResult<Nothing> {
            val cause = (e.cause as? PSQLException)
                ?: return DatabaseResult.GeneralError("Unknown constraint violation", e)

            val constraintStr = cause.serverErrorMessage?.constraint
                ?: return DatabaseResult.GeneralError("Unknown constraint violation", e)

            val constraint = runCatching { Constraint.valueOf(constraintStr) }.getOrNull()
                ?: return DatabaseResult.GeneralError("Unknown constraint violation", e)

            return DatabaseResult.ConstraintViolation(constraint, e)
        }

        /**
         * For PostgreSQL only
         *
         * @see <a href="https://www.postgresql.org/docs/current/errcodes-appendix.html">PostgreSQL error codes</a>
         */
        private fun Throwable.isSerializationFailure(): Boolean =
            this is PSQLException && sqlState == "40001" || this is ExposedSQLException && sqlState == "40001"

        /**
         * For PostgreSQL only
         *
         * @see <a href="https://www.postgresql.org/docs/current/errcodes-appendix.html">PostgreSQL error codes</a>
         */
        private fun ExposedSQLException.isConnectionException() = sqlState.startsWith("08")

        /**
         * For PostgreSQL only
         *
         * @see <a href="https://www.postgresql.org/docs/current/errcodes-appendix.html">PostgreSQL error codes</a>
         */
        private fun ExposedSQLException.isConstraintViolation() = sqlState.startsWith("23")
    }
}
