package com.skythinkers.skynons

import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap

typealias Port = Int

class NonsProcessManager(val nonsPath: String) : AutoCloseable {
    private val portToNonsProcess = ConcurrentHashMap<Int, Process>()

    fun createSimulation(): Port? {
        val port = findFreePort()
        if (port == null) {
            return null
        }
        val process =
                ProcessBuilder(nonsPath, "-p", port.toString()).redirectErrorStream(true).start()

        portToNonsProcess[port] = process
        return port
    }

    private fun findFreePort(): Port? {
        return try {
            ServerSocket(0).use { socket -> socket.localPort }
        } catch (e: IOException) {
            null
        }
    }

    override fun close() {
        portToNonsProcess.values.forEach { process -> process.destroy() }
        portToNonsProcess.clear()
    }
}
