package com.skythinkers.skynons

import com.skythinkers.skynons.api.HistoryEntry
import com.skythinkers.skynons.api.RegistrationRequest
import com.skythinkers.skynons.api.SimpleSimulationResult
import com.skythinkers.skynons.api.UserId
import com.skythinkers.skynons.auth.AccountManager
import com.skythinkers.skynons.auth.AccountManagerImpl
import com.skythinkers.skynons.auth.AuthConfig
import com.skythinkers.skynons.database.BlobRefs
import com.skythinkers.skynons.database.Blobs
import com.skythinkers.skynons.database.DatabaseConnection
import com.skythinkers.skynons.database.Sessions
import com.skythinkers.skynons.database.SimulationHistory
import com.skythinkers.skynons.database.Users
import com.skythinkers.skynons.history.HistoryManager
import com.skythinkers.skynons.history.HistoryManagerImpl
import com.skythinkers.skynons.storage.BlobStorageImpl
import io.ktor.test.dispatcher.testSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.listDirectoryEntries
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class HistoryManagerTest {
    private val db = Database.connect(
        url = "jdbc:h2:mem:test;DB_CLOSE_DELAY=-1",
        user = "root",
        driver = "org.h2.Driver",
        password = "",
    )
    private val connection = DatabaseConnection(db)
    private val manager: AccountManager = AccountManagerImpl(
        AuthConfig(
            issuer = "test",
            audience = "test",
            secret = ByteArray(32) { it.toByte() },
            pepper = ByteArray(32) { it.toByte() },
            database = connection,
            credentialsDatabase = connection,
        )
    )
    private val storePath = Files.createTempDirectory("blobs")
    private val blobStorage = BlobStorageImpl(storePath, connection)
    private val historyManager = HistoryManagerImpl(connection, blobStorage, Json)

    @BeforeTest
    @OptIn(ExperimentalPathApi::class)
    fun clearDatabase() {
        transaction<Unit>(db) {
            SimulationHistory.deleteAll()
            BlobRefs.deleteAll()
            Blobs.deleteAll()
            Sessions.deleteAll()
            Users.deleteAll()
        }
        storePath.listDirectoryEntries().forEach { it.deleteRecursively() }
    }

    @Test
    fun `can store and read no-results entry`() = runTest { historyManager, aliceUid, eveUid ->
        val id = historyManager.storeEntry(aliceUid, CONFIG1, null).getOrThrow()
        val extracted = historyManager.getEntryById(aliceUid, id).getOrThrow()
        val expected = HistoryEntry(id = id, timestamp = extracted.timestamp, CONFIG1, null)
        assertEquals(expected, extracted)
    }

    @Test
    fun `can store and read full entry`() = runTest { historyManager, aliceUid, eveUid ->
        val id = historyManager.storeEntry(aliceUid, CONFIG1, RESULTS1).getOrThrow()
        val extracted = historyManager.getEntryById(aliceUid, id).getOrThrow()
        val expected = HistoryEntry(id = id, timestamp = extracted.timestamp, CONFIG1, RESULTS1)
        assertEquals(expected, extracted)
    }

    @Test
    fun `can store and read 2 entries`() = runTest { historyManager, aliceUid, eveUid ->
        val id1 = historyManager.storeEntry(aliceUid, CONFIG1, RESULTS1).getOrThrow()
        val id2 = historyManager.storeEntry(aliceUid, CONFIG2, RESULTS2).getOrThrow()

        val extracted1 = historyManager.getEntryById(aliceUid, id1).getOrThrow()
        val expected1 = HistoryEntry(id = id1, timestamp = extracted1.timestamp, CONFIG1, RESULTS1)

        val extracted2 = historyManager.getEntryById(aliceUid, id2).getOrThrow()
        val expected2 = HistoryEntry(id = id2, timestamp = extracted2.timestamp, CONFIG2, RESULTS2)

        assertEquals(expected1, extracted1)
        assertEquals(expected2, extracted2)
    }

    @Test
    fun `can store and read 2 entries from different users`() = runTest { historyManager, aliceUid, eveUid ->
        val id1 = historyManager.storeEntry(aliceUid, CONFIG1, RESULTS1).getOrThrow()
        val id2 = historyManager.storeEntry(eveUid, CONFIG2, RESULTS2).getOrThrow()

        val extracted1 = historyManager.getEntryById(aliceUid, id1).getOrThrow()
        val expected1 = HistoryEntry(id = id1, timestamp = extracted1.timestamp, CONFIG1, RESULTS1)

        val extracted2 = historyManager.getEntryById(eveUid, id2).getOrThrow()
        val expected2 = HistoryEntry(id = id2, timestamp = extracted2.timestamp, CONFIG2, RESULTS2)

        assertEquals(expected1, extracted1)
        assertEquals(expected2, extracted2)
    }

    @Test
    fun `can not read other user's entry`() = runTest { historyManager, aliceUid, eveUid ->
        val id = historyManager.storeEntry(aliceUid, CONFIG1, RESULTS1).getOrThrow()
        assertFails {
            historyManager.getEntryById(eveUid, id).getOrThrow()
        }
    }

    @Test
    fun `can list`() = runTest { historyManager, aliceUid, eveUid ->
        val id1 = historyManager.storeEntry(aliceUid, CONFIG1, RESULTS1).getOrThrow()
        val id2 = historyManager.storeEntry(aliceUid, CONFIG2, RESULTS2).getOrThrow()

        val extracted1 = historyManager.getEntryById(aliceUid, id1).getOrThrow()
        val extracted2 = historyManager.getEntryById(aliceUid, id2).getOrThrow()

        val extractedList = historyManager.listLastEntries(aliceUid, limit = 2,).getOrThrow()
        assertEquals(listOf(extracted2, extracted1), extractedList)
    }

    @Test
    fun `can list before`() = runTest { historyManager, aliceUid, eveUid ->
        val id1 = historyManager.storeEntry(aliceUid, CONFIG1, RESULTS1).getOrThrow()
        val id2 = historyManager.storeEntry(aliceUid, CONFIG2, RESULTS2).getOrThrow()
        val id3 = historyManager.storeEntry(aliceUid, CONFIG1, RESULTS1).getOrThrow()
        val id4 = historyManager.storeEntry(eveUid, CONFIG1, RESULTS1).getOrThrow()

        val extracted1 = historyManager.getEntryById(aliceUid, id1).getOrThrow()
        val extracted2 = historyManager.getEntryById(aliceUid, id2).getOrThrow()
        val extracted3 = historyManager.getEntryById(aliceUid, id3).getOrThrow()

        val extractedList = historyManager.listEntriesBefore(aliceUid, id3, limit = 2,).getOrThrow()
        assertEquals(listOf(extracted2, extracted1), extractedList)
    }

    @Test
    fun `list ignores other user's entry`() = runTest { historyManager, aliceUid, eveUid ->
        val id1 = historyManager.storeEntry(aliceUid, CONFIG1, RESULTS1).getOrThrow()
        historyManager.storeEntry(eveUid, CONFIG1, RESULTS1).getOrThrow()
        val id2 = historyManager.storeEntry(aliceUid, CONFIG2, RESULTS2).getOrThrow()
        historyManager.storeEntry(eveUid, CONFIG1, RESULTS1).getOrThrow()
        val id3 = historyManager.storeEntry(aliceUid, CONFIG1, RESULTS1).getOrThrow()

        val extracted1 = historyManager.getEntryById(aliceUid, id1).getOrThrow()
        val extracted2 = historyManager.getEntryById(aliceUid, id2).getOrThrow()
        val extracted3 = historyManager.getEntryById(aliceUid, id3).getOrThrow()

        val extractedList = historyManager.listLastEntries(aliceUid, limit = 3,).getOrThrow()
        assertEquals(listOf(extracted3, extracted2, extracted1), extractedList)
    }

    @Test
    fun `list before ignores other user's entry`() = runTest { historyManager, aliceUid, eveUid ->
        val id1 = historyManager.storeEntry(aliceUid, CONFIG1, RESULTS1).getOrThrow()
        historyManager.storeEntry(eveUid, CONFIG1, RESULTS1).getOrThrow()
        val id2 = historyManager.storeEntry(aliceUid, CONFIG2, RESULTS2).getOrThrow()
        historyManager.storeEntry(eveUid, CONFIG1, RESULTS1).getOrThrow()
        val id3 = historyManager.storeEntry(aliceUid, CONFIG1, RESULTS1).getOrThrow()

        val extracted1 = historyManager.getEntryById(aliceUid, id1).getOrThrow()
        val extracted2 = historyManager.getEntryById(aliceUid, id2).getOrThrow()
        val extracted3 = historyManager.getEntryById(aliceUid, id3).getOrThrow()

        val extractedList = historyManager.listEntriesBefore(aliceUid, id3, limit = 2,).getOrThrow()
        assertEquals(listOf(extracted2, extracted1), extractedList)
    }

    @Test
    fun `list uses limit`() = runTest { historyManager, aliceUid, eveUid ->
        val id1 = historyManager.storeEntry(aliceUid, CONFIG1, RESULTS1).getOrThrow()
        val id2 = historyManager.storeEntry(aliceUid, CONFIG2, RESULTS2).getOrThrow()
        val id3 = historyManager.storeEntry(aliceUid, CONFIG1, RESULTS1).getOrThrow()

        val extracted1 = historyManager.getEntryById(aliceUid, id1).getOrThrow()
        val extracted2 = historyManager.getEntryById(aliceUid, id2).getOrThrow()
        val extracted3 = historyManager.getEntryById(aliceUid, id3).getOrThrow()

        val extractedList = historyManager.listLastEntries(aliceUid, limit = 1,).getOrThrow()
        assertEquals(listOf(extracted3), extractedList)
    }

    @Test
    fun `list before uses limit`() = runTest { historyManager, aliceUid, eveUid ->
        val id1 = historyManager.storeEntry(aliceUid, CONFIG1, RESULTS1).getOrThrow()
        val id2 = historyManager.storeEntry(aliceUid, CONFIG2, RESULTS2).getOrThrow()
        val id3 = historyManager.storeEntry(aliceUid, CONFIG1, RESULTS1).getOrThrow()

        val extracted1 = historyManager.getEntryById(aliceUid, id1).getOrThrow()
        val extracted2 = historyManager.getEntryById(aliceUid, id2).getOrThrow()
        val extracted3 = historyManager.getEntryById(aliceUid, id3).getOrThrow()

        val extractedList = historyManager.listEntriesBefore(aliceUid, id3, limit = 1,).getOrThrow()
        assertEquals(listOf(extracted2), extractedList)
    }

    private fun runTest(action: suspend CoroutineScope.(historyManager: HistoryManager, alice: UserId, eve: UserId) -> Unit) {
        testSuspend {
            val aliceUid =
                manager.registerUser(RegistrationRequest("Alice", "no-way-you-will-ever-guess-such-password"))
                    .getOrThrow().first.uid
            val eveUid = manager.registerUser(
                RegistrationRequest(
                    "Eve",
                    "V*BrZ&qaGN7PDmtO9gUGfRh^42y39P58ban8DcorjF2%Ky7@$5A*WvPlEzv9i&M&9tQH%SS8*WVa3u&0T9BXcA9Oz*39f3dqxYKEwRzliKn!ZJpA1hzgbc&wIp@TU&O!Z"
                )
            )
                .getOrThrow().first.uid

            action(historyManager, aliceUid, eveUid)
        }
    }

    companion object {
        private const val CONFIG1 = "some config #1"
        private const val CONFIG2 = "some config #2"
        private val RESULTS1 = SimpleSimulationResult(
            "svg1",
            "svg2",
            "svg3",
            "svg4",
        )
        private val RESULTS2 = SimpleSimulationResult(
            "svg5",
            "svg6",
            "svg7",
            "svg8",
        )
    }
}
