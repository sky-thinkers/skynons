package com.skythinkers.skynons

import com.skythinkers.skynons.api.LoginRequest
import com.skythinkers.skynons.api.PasswordUpdateRequest
import com.skythinkers.skynons.api.RegistrationRequest
import com.skythinkers.skynons.auth.AccountManager
import com.skythinkers.skynons.auth.AccountManagerImpl
import com.skythinkers.skynons.auth.AuthConfig
import com.skythinkers.skynons.database.DatabaseConnection
import com.skythinkers.skynons.database.Sessions
import com.skythinkers.skynons.database.Users
import io.ktor.test.dispatcher.testSuspend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AccountManagerTest {
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

    @BeforeTest
    fun clearDatabase() {
        transaction(db) {
            Users.deleteAll()
            Sessions.deleteAll()
        }
    }

    @Test
    fun `can register`() = runTest { manager ->
        manager.registerUser(
            RegistrationRequest(
                "root",
                "root-of-all-evils",
            )
        ).getOrThrow()
    }

    @Test
    fun `registration yields valid token`() = runTest { manager ->
        val (_, session) = manager.registerUser(
            RegistrationRequest(
                "root",
                "root-of-all-evils",
            )
        ).getOrThrow()

        assertTrue(manager.validateSession(session).getOrThrow())
    }

    @Test
    fun `can change password`() = runTest { manager ->
        val (info, _) = manager.registerUser(
            RegistrationRequest(
                "root",
                "root-of-all-evils",
            )
        ).getOrThrow()

        manager.updateUserPassword(info.uid, PasswordUpdateRequest("root-of-all-evils", "pretty-girl"))
            .getOrThrow()
    }

    @Test
    fun `can auth`() = runTest { manager ->
        val (info, _) = manager.registerUser(
            RegistrationRequest(
                "root",
                "root-of-all-evils",
            )
        ).getOrThrow()

        val (newInfo, _) = manager.authenticateUser(LoginRequest("root", "root-of-all-evils"))
            .getOrThrow()

        assertEquals(info, newInfo)
    }

    @Test
    fun `can not auth with wrong password`() = runTest { manager ->
        manager.registerUser(
            RegistrationRequest(
                "root",
                "root-of-all-evils",
            )
        ).getOrThrow()

        assertFails {
            manager.authenticateUser(LoginRequest("root", "1234")).getOrThrow()
        }
    }

    @Test
    fun `can change password and auth with the new one`() = runTest { manager ->
        val (info, _) = manager.registerUser(
            RegistrationRequest(
                "root",
                "root-of-all-evils",
            )
        ).getOrThrow()

        manager.updateUserPassword(info.uid, PasswordUpdateRequest("root-of-all-evils", "pretty-girl"))
            .getOrThrow()

        val (newInfo, _) = manager.authenticateUser(LoginRequest("root", "pretty-girl"))
            .getOrThrow()

        assertEquals(info, newInfo)
    }

    @Test
    fun `can not change password and auth with the old one`() = runTest { manager ->
        val (info, _) = manager.registerUser(
            RegistrationRequest(
                "root",
                "root-of-all-evils",
            )
        ).getOrThrow()

        manager.updateUserPassword(info.uid, PasswordUpdateRequest("root-of-all-evils", "pretty-girl"))
            .getOrThrow()

        assertFails {
            manager.authenticateUser(LoginRequest("root", "root-of-all-evils")).getOrThrow()
        }
    }

    @Test
    fun `can register null`() = runTest { manager ->
        manager.registerUser(
            RegistrationRequest(
                "null",
                "null",
            )
        ).getOrThrow()
    }

    @Test
    fun `concurrent registration`() = runTest { manager ->
        val userRequests = listOf(
            RegistrationRequest(
                "root",
                "root-of-all-evils",
            ),
            RegistrationRequest(
                "null",
                "null",
            ),
            RegistrationRequest(
                "empty",
                "ha-ha",
            ),
            RegistrationRequest(
                "never",
                "1234",
            ),
        )

        val differentUserCount = userRequests.map {
            async {
                manager.registerUser(it).getOrThrow()
            }
        }.awaitAll().map { it.first.uid }.distinct().count()

        assertEquals(userRequests.size, differentUserCount)
    }

    @Test
    fun `can invalidate session`() = runTest { manager ->
        val (_, session) = manager.registerUser(
            RegistrationRequest(
                "null",
                "null",
            )
        ).getOrThrow()

        manager.invalidateSession(session).getOrThrow()

        assertFalse(manager.validateSession(session).getOrThrow(), "Session is still valid")
    }

    @Test
    fun `can get short user info`() = runTest { manager ->
        val (info, session) = manager.registerUser(
            RegistrationRequest(
                "null",
                "null",
            )
        ).getOrThrow()

        assertEquals(info, manager.getShortUserInfo(session).getOrThrow())
    }

    private fun runTest(action: suspend CoroutineScope.(manager: AccountManager) -> Unit) {
        testSuspend {
            action(manager)
        }
    }
}
