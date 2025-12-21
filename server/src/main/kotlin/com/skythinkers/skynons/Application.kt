package com.skythinkers.skynons

import com.skythinkers.skynons.auth.AccountManager
import com.skythinkers.skynons.auth.createAuthManager
import com.skythinkers.skynons.database.openDatabase
import com.skythinkers.skynons.history.HistoryManager
import com.skythinkers.skynons.history.HistoryManagerImpl
import com.skythinkers.skynons.nons.NonsProcessManager
import com.skythinkers.skynons.nons.NonsProcessManagerImpl
import com.skythinkers.skynons.routing.ContinuationManager
import com.skythinkers.skynons.routing.configureRouting
import com.skythinkers.skynons.storage.BlobStorage
import com.skythinkers.skynons.storage.BlobStorageImpl
import io.ktor.server.application.Application
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.json.Json
import java.nio.file.Files
import kotlin.io.path.Path

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module(processManager: NonsProcessManager? = null) {
    val nonsPath =
        System.getenv("NONS_PATH")?.takeUnless(String::isEmpty) ?: "../backend/build/nons"
    val blobStoragePath = System.getenv("BLOB_STORAGE_PATH")?.takeUnless(String::isEmpty)
        ?.let { Path(it) } ?: Files.createTempDirectory("blob-storage")
    val continuationConvTimeout = System.getenv("CONTINUATION_CONVERSATION_TIMEOUT")?.toLongOrNull() ?: 1000L

    val processManager = processManager ?: NonsProcessManagerImpl(nonsPath, CoroutineScope(Dispatchers.IO))
    val database = openDatabase()
    val accountManager: AccountManager = createAuthManager(database, database)
    val blobStorage: BlobStorage = BlobStorageImpl(blobStoragePath, database)
    val historyManager: HistoryManager = HistoryManagerImpl(database, blobStorage, Json)
    val continuationManager = ContinuationManager(continuationConvTimeout, CoroutineScope(Dispatchers.IO))

    configureHTTP()
    configureRouting(nonsPath, processManager, accountManager, historyManager, continuationManager)
}
