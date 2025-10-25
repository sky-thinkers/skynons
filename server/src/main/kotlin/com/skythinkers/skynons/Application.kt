package com.skythinkers.skynons

import com.skythinkers.skynons.auth.AccountManager
import com.skythinkers.skynons.auth.createAuthManager
import com.skythinkers.skynons.database.openDatabase
import com.skythinkers.skynons.nons.NonsProcessManager
import com.skythinkers.skynons.nons.NonsProcessManagerImpl
import com.skythinkers.skynons.routing.configureRouting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import io.ktor.server.application.Application

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module(processManager: NonsProcessManager? = null) {
    val nonsPath =
        System.getenv("NONS_PATH")?.takeUnless(String::isEmpty) ?: "../backend/build/nons"
    val processManager = processManager ?: NonsProcessManagerImpl(nonsPath, CoroutineScope(Dispatchers.IO))
    val database = openDatabase()
    val accountManager: AccountManager = createAuthManager(database, database)

    configureHTTP()
    configureRouting(nonsPath, processManager, accountManager)
}
