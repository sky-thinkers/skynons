package com.skythinkers.skynons.storage

import com.skythinkers.skynons.api.UserId
import com.skythinkers.skynons.database.DatabaseResult
import com.skythinkers.skynons.database.SkynonsDatabase
import com.skythinkers.skynons.database.asResult
import io.ktor.util.logging.KtorSimpleLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.io.path.copyTo
import kotlin.io.path.createParentDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.writeBytes

class BlobStorageImpl(
    private val storePath: Path,
    private val database: SkynonsDatabase,
) : BlobStorage {
    private val logger = KtorSimpleLogger("AccountManagerImpl")

    private val digestThreadLocal = ThreadLocal.withInitial {
        MessageDigest.getInstance(HASHING_ALGORITHM)
    }
    private val digest: MessageDigest get() = digestThreadLocal.get()

    override suspend fun has(
        user: UserId,
        ref: BlobRef,
    ): Result<Boolean> {
        return when (val res = database.ownsBlob(user = user, blob = ref)) {
            is DatabaseResult.Success<Boolean> -> Result.success(res.res)
            !is DatabaseResult.Error -> throw AssertionError("Unreachable code")
            else -> Result.failure(BlobStorageException(cause = res.cause))
        }
    }

    override suspend fun read(
        user: UserId,
        ref: BlobRef,
    ): Result<InputStream> {
        when (val res = database.accessBlob(user = user, blob = ref)) {
            is DatabaseResult.Success<Boolean> -> {
                if (!res.res) {
                    return Result.failure(BlobNotFoundException(ref))
                }
            }

            !is DatabaseResult.Error -> throw AssertionError("Unreachable code")
            else -> return Result.failure(BlobStorageException(cause = res.cause))
        }

        return runCatching { resolveStorage(ref).inputStream() }
            .onFailure {
                val e = BlobNotFoundException(ref)
                logger.warn("Blob $ref not found after successful resolve in db (user $user)", e)
                return Result.failure(e)
            }
    }

    override suspend fun writeFile(
        userId: UserId,
        file: Path,
    ): Result<BlobRef> {
        val hash = runCatching {
            withContext(Dispatchers.IO) {
                with(digest) withDigest@{
                    reset()
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    file.inputStream().buffered().use { inputStream ->
                        while (inputStream.available() >= 0) {
                            val read = inputStream.read(buffer)
                            if (read <= 0) {
                                return@withDigest digest().toHexString()
                            } else {
                                update(buffer, 0, read)
                            }
                        }
                        digest().toHexString()
                    }
                }
            }
        }.getOrElse {
            val e = BlobStorageException("Failed to compute file hash", it)
            logger.error("writeFile failed", e)
            return Result.failure(e)
        }

        val targetPath = resolveStorage(hash)
        runCatching {
            if (!targetPath.exists()) {
                targetPath.createParentDirectories()
                file.copyTo(targetPath)
            }
        }.onFailure {
            if (!targetPath.exists()) {
                val e = BlobStorageException("Failed to store file into blob storage", it)
                logger.error("writeFile failed", e)
                return Result.failure(e)
            } else {
                logger.debug("Store race (writeFile)", it)
            }
        }

        database.allocateBlob(userId, hash).asResult()
            .onFailure {
                val e = BlobStorageException("Failed to allocate blob ref in database", it)
                logger.error("writeFile failed", e)
                return Result.failure(e)
            }

        return Result.success(hash)
    }

    override suspend fun writeData(
        userId: UserId,
        data: ByteArray,
    ): Result<BlobRef> {

        val hash = with(digest) withDigest@{
            reset()
            update(data)
            digest().toHexString()
        }

        val targetPath = resolveStorage(hash)
        runCatching {
            if (!targetPath.exists()) {
                targetPath.createParentDirectories()
                targetPath.writeBytes(data)
            }
        }.onFailure {
            if (!targetPath.exists()) {
                val e = BlobStorageException("Failed to store data into blob storage", it)
                logger.error("writeData failed", e)
                return Result.failure(e)
            } else {
                logger.debug("Store race (writeData)", it)
            }
        }

        database.allocateBlob(userId, hash).asResult()
            .onFailure {
                val e = BlobStorageException("Failed to allocate blob ref in database", it)
                logger.error("writeData failed", e)
                return Result.failure(e)
            }

        return Result.success(hash)
    }

    private fun resolveStorage(hash: String): Path =
        storePath.resolve(hash.windowed(16, 16).joinToString("/"))

    companion object {
        private const val HASHING_ALGORITHM: String = "SHA256"
    }
}
