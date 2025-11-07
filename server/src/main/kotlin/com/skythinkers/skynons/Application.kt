package com.skythinkers.skynons

import com.skythinkers.skynons.nons.NonsProcessManager
import com.skythinkers.skynons.nons.NonsProcessManagerImpl
import com.skythinkers.skynons.routing.configureRouting
import io.ktor.server.application.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module(processManager: NonsProcessManager? = null) {
    val nonsPath =
        System.getenv("NONS_PATH")?.takeUnless(String::isEmpty) ?: "../backend/build/nons"
    val processManager = processManager ?: NonsProcessManagerImpl(nonsPath, CoroutineScope(Dispatchers.IO))

    configureHTTP()
    configureRouting(nonsPath, processManager)
}
