package com.skythinkers.skynons.database

import com.skythinkers.skynons.api.HistoryEntryId
import com.skythinkers.skynons.api.ShortUserInfo
import com.skythinkers.skynons.api.UserId
import com.skythinkers.skynons.model.history.ExtractedHistoryEntryData
import com.skythinkers.skynons.model.history.HistoryEntryData
import com.skythinkers.skynons.model.user.GeneratedToken
import com.skythinkers.skynons.model.user.UserCredentials
import com.skythinkers.skynons.model.user.UserCredentialsRef
import com.skythinkers.skynons.model.user.UserRegistrationInfo
import com.skythinkers.skynons.model.user.UserUpdatePasswordInfo
import com.skythinkers.skynons.storage.BlobRef
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.SortOrder
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.dao.id.CompositeID
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.greaterEq
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.SchemaUtils
import org.jetbrains.exposed.v1.jdbc.deleteWhere
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.select
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.suspendTransaction
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update
import org.postgresql.util.PSQLException
import kotlin.time.Instant

class DatabaseConnection(private val database: Database) : SkynonsDatabase, CredentialsDatabase {
    init {
        transaction(database) {
            SchemaUtils.create(Users, Sessions, Blobs, BlobRefs, SimulationHistory)
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

    override suspend fun touchBlob(blob: BlobRef, now: Instant): DatabaseResult<Unit> = transaction {
        val decodedHash = blob.hexToByteArray()

        val updated =
            Blobs.update(where = { (Blobs.blobHash eq decodedHash) and (Blobs.state eq Blobs.STATE_ACTIVE) }) {
                it[Blobs.accessed] = now
            }
        if (updated == 0) {
            Blobs.insert {
                it[Blobs.blobHash] = decodedHash
                it[Blobs.accessed] = now
            }
        }

        DatabaseResult.Success(Unit)
    }

    override suspend fun ownsBlob(
        user: UserId,
        blob: BlobRef,
    ): DatabaseResult<Boolean> = transaction {
        val decodedHash = blob.hexToByteArray()
        val records = BlobRefs
            .select(BlobRefs.blobHash, BlobRefs.owner)
            .where {
                (BlobRefs.blobHash eq decodedHash) and (BlobRefs.owner eq user)
            }
            .count()

        DatabaseResult.Success(records == 1L)
    }

    override suspend fun accessBlob(
        user: UserId,
        blob: BlobRef,
        now: Instant,
    ): DatabaseResult<Boolean> = transaction {
        val decodedHash = blob.hexToByteArray()

        val updates = BlobRefs
            .update(where = {
                (BlobRefs.blobHash eq decodedHash) and (BlobRefs.owner eq user)
            }) {
                it[BlobRefs.accessed] = now
            }

        DatabaseResult.Success(updates == 1)
    }

    override suspend fun allocateBlob(
        user: UserId,
        blob: BlobRef,
        now: Instant,
    ): DatabaseResult<Unit> = transaction {
        val decodedHash = blob.hexToByteArray()

        val updatedRecords = BlobRefs.update(
            where = {
                (BlobRefs.blobHash eq decodedHash) and (BlobRefs.owner eq user)
            }
        ) {
            it[BlobRefs.modified] = now
            it[BlobRefs.accessed] = now
        }
        if (updatedRecords == 0) {
            BlobRefs.insert {
                it[BlobRefs.blobHash] = decodedHash
                it[BlobRefs.owner] = user
                it[BlobRefs.created] = now
                it[BlobRefs.modified] = now
                it[BlobRefs.accessed] = now
            }
        }

        DatabaseResult.Success(Unit)
    }

    override suspend fun storeHistoryEntry(data: HistoryEntryData): DatabaseResult<HistoryEntryId> = transaction {
        val id = SimulationHistory.insert {
            it[SimulationHistory.owner] = data.owner
            it[SimulationHistory.timestamp] = data.timestamp
            it[SimulationHistory.configRef] = data.configRef.hexToByteArray()
            it[SimulationHistory.resultsRef] = data.resultRef?.hexToByteArray()
        }[SimulationHistory.id]

        DatabaseResult.Success(id)
    }

    override suspend fun listHistoryLastEntries(
        owner: UserId,
        limit: Int,
    ): DatabaseResult<List<ExtractedHistoryEntryData>> = transaction {
        SimulationHistory
            .selectAll()
            .where { (SimulationHistory.owner eq owner) }
            .orderBy(SimulationHistory.id, SortOrder.DESC)
            .limit(limit)
            .map(this::extractHistoryEntry)
            .let { DatabaseResult.Success(it) }
    }

    override suspend fun listHistoryBeforeEntryId(
        owner: UserId,
        entryId: HistoryEntryId,
        limit: Int,
    ): DatabaseResult<List<ExtractedHistoryEntryData>> = transaction {
        val toId = SimulationHistory
            .select(SimulationHistory.id, SimulationHistory.owner)
            .where { (SimulationHistory.id eq entryId) and (SimulationHistory.owner eq owner) }
            .map { it[SimulationHistory.id] }
            .singleOrNull()

        if (toId == null) {
            return@transaction DatabaseResult.NotFound("No such entry $entryId for user $owner")
        }

        SimulationHistory
            .selectAll()
            .where { (SimulationHistory.id less toId) and (SimulationHistory.owner eq owner) }
            .orderBy(SimulationHistory.id, SortOrder.DESC)
            .limit(limit)
            .map(this::extractHistoryEntry)
            .let { DatabaseResult.Success(it) }
    }

    override suspend fun getHistoryEntryById(
        owner: UserId,
        entryId: HistoryEntryId,
    ): DatabaseResult<ExtractedHistoryEntryData> = transaction {
        SimulationHistory
            .selectAll()
            .where { (SimulationHistory.id eq entryId) and (SimulationHistory.owner eq owner) }
            .orderBy(SimulationHistory.id, SortOrder.DESC)
            .map(this::extractHistoryEntry)
            .singleOrNull()
            ?.let { DatabaseResult.Success(it) }
            ?: DatabaseResult.NotFound("No such entry $entryId for user $owner")
    }

    override suspend fun removeExpiredTokens(byDate: Instant): DatabaseResult<Unit> = transaction {
        val removedTokens = Sessions
            .deleteWhere {
                Sessions.expirationDate less byDate
            }

        logger.debug("Removed {} tokens expired by {}", removedTokens, byDate)

        DatabaseResult.Success(Unit)
    }

    private fun extractHistoryEntry(row: ResultRow): ExtractedHistoryEntryData = ExtractedHistoryEntryData(
        id = row[SimulationHistory.id],
        timestamp = row[SimulationHistory.timestamp],
        configRef = row[SimulationHistory.configRef].toHexString(),
        resultRef = row[SimulationHistory.resultsRef]?.toHexString(),
    )

    private suspend fun <T> transaction(block: suspend () -> DatabaseResult<T>): DatabaseResult<T> =
        runCatching { withContext(Dispatchers.IO) { suspendTransaction(database) { block() } } }
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
