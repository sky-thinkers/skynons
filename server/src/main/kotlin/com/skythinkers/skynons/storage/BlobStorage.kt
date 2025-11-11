package com.skythinkers.skynons.storage

import com.skythinkers.skynons.api.UserId
import java.io.InputStream
import java.nio.file.Path

typealias BlobRef = String

open class BlobStorageException(message: String? = null, cause: Throwable? = null) : Exception(message, cause)
class BlobNotFoundException(blob: String) : BlobStorageException(blob)

interface BlobStorage {
    suspend fun has(user: UserId, ref: BlobRef): Result<Boolean>
    suspend fun read(user: UserId, ref: BlobRef): Result<InputStream>

    suspend fun writeFile(userId: UserId, file: Path): Result<BlobRef>
    suspend fun writeData(userId: UserId, data: ByteArray): Result<BlobRef>
}

suspend fun BlobStorage.readText(user: UserId, ref: BlobRef): Result<String> =
    read(user, ref).map { stream -> stream.bufferedReader().use { it.readText() } }

suspend fun BlobStorage.readData(user: UserId, ref: BlobRef): Result<ByteArray> =
    read(user, ref).map { stream -> stream.buffered().use { it.readAllBytes()!! } }

suspend fun BlobStorage.writeText(user: UserId, text: String): Result<BlobRef> =
    writeData(user, text.encodeToByteArray())
