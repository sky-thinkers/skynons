package com.skythinkers.skynons

import com.skythinkers.skynons.api.RegistrationRequest
import com.skythinkers.skynons.api.UserId
import com.skythinkers.skynons.auth.AccountManager
import com.skythinkers.skynons.auth.AccountManagerImpl
import com.skythinkers.skynons.auth.AuthConfig
import com.skythinkers.skynons.database.BlobRefs
import com.skythinkers.skynons.database.DatabaseConnection
import com.skythinkers.skynons.database.Sessions
import com.skythinkers.skynons.database.Users
import com.skythinkers.skynons.storage.BlobStorage
import com.skythinkers.skynons.storage.BlobStorageImpl
import com.skythinkers.skynons.storage.readData
import com.skythinkers.skynons.storage.readText
import com.skythinkers.skynons.storage.writeText
import io.ktor.test.dispatcher.testSuspend
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import java.nio.file.Files
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteIfExists
import kotlin.io.path.deleteRecursively
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.writeBytes
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BlobStorageTest {
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

    @BeforeTest
    @OptIn(ExperimentalPathApi::class)
    fun clearDatabase() {
        transaction<Unit>(db) {
            BlobRefs.deleteAll()
            Sessions.deleteAll()
            Users.deleteAll()
        }
        storePath.listDirectoryEntries().forEach { it.deleteRecursively() }
    }

    @Test
    fun `empty hasn't blob`() = runTest { blobStorage, aliceUid, eveUid ->
        assertFalse(blobStorage.has(aliceUid, "0".repeat(64)).getOrThrow())
    }

    @Test
    fun `has just written`() = runTest { blobStorage, aliceUid, eveUid ->
        val data = "Some text"
        val ref = blobStorage.writeText(aliceUid, data).getOrThrow()
        assertTrue(blobStorage.has(aliceUid, ref).getOrThrow())
    }

    @Test
    fun `can read just written text`() = runTest { blobStorage, aliceUid, eveUid ->
        val data = "Some text"
        val ref = blobStorage.writeText(aliceUid, data).getOrThrow()
        assertEquals(data, blobStorage.readText(aliceUid, ref).getOrThrow())
    }

    @Test
    fun `can read just written binary`() = runTest { blobStorage, aliceUid, eveUid ->
        val data = byteArrayOf(0, 1, 2, 3, 4, 54, 56, 6)
        val ref = blobStorage.writeData(aliceUid, data).getOrThrow()
        assertContentEquals(data, blobStorage.readData(aliceUid, ref).getOrThrow())
    }

    @Test
    fun `can read just written file`() = runTest { blobStorage, aliceUid, eveUid ->
        val data = byteArrayOf(0, 1, 2, 3, 4, 54, 56, 6)
        val f = Files.createTempFile("temp-data", ".bin")
        try {
            f.writeBytes(data)
            val ref = blobStorage.writeFile(aliceUid, f).getOrThrow()
            assertContentEquals(data, blobStorage.read(aliceUid, ref).getOrThrow().buffered().use { it.readAllBytes() })
        } finally {
            f.deleteIfExists()
        }
    }

    @Test
    fun `cannot read from other user`() = runTest { blobStorage, aliceUid, eveUid ->
        val data = "Some text"
        val ref = blobStorage.writeText(aliceUid, data).getOrThrow()
        assertFails {
            blobStorage.readData(eveUid, ref).getOrThrow()
        }
    }

    @Test
    fun `can alias read from other user`() = runTest { blobStorage, aliceUid, eveUid ->
        val data = "Some text"
        val ref1 = blobStorage.writeText(aliceUid, data).getOrThrow()
        val ref2 = blobStorage.writeText(eveUid, data).getOrThrow()
        assertEquals(data, blobStorage.readText(aliceUid, ref1).getOrThrow())
        assertEquals(data, blobStorage.readText(eveUid, ref2).getOrThrow())
    }

    @Test
    fun `can add 2 blobs`() = runTest { blobStorage, aliceUid, eveUid ->
        val data1 = "Some text"
        val data2 = "Some text"
        val ref1 = blobStorage.writeText(aliceUid, data1).getOrThrow()
        val ref2 = blobStorage.writeText(aliceUid, data2).getOrThrow()
        assertEquals(data1, blobStorage.readText(aliceUid, ref1).getOrThrow())
        assertEquals(data2, blobStorage.readText(aliceUid, ref2).getOrThrow())
    }

    private fun runTest(action: suspend CoroutineScope.(blobStorage: BlobStorage, alice: UserId, eve: UserId) -> Unit) {
        testSuspend {
            val aliceUid = manager.registerUser(RegistrationRequest("Alice", "no-way-you-will-ever-guess-such-password"))
                .getOrThrow().first.uid
            val eveUid = manager.registerUser(
                RegistrationRequest(
                    "Eve",
                    "V*BrZ&qaGN7PDmtO9gUGfRh^42y39P58ban8DcorjF2%Ky7@$5A*WvPlEzv9i&M&9tQH%SS8*WVa3u&0T9BXcA9Oz*39f3dqxYKEwRzliKn!ZJpA1hzgbc&wIp@TU&O!Z"
                )
            )
                .getOrThrow().first.uid

            action(blobStorage, aliceUid, eveUid)
        }
    }
}
