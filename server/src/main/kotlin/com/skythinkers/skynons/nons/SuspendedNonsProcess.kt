package com.skythinkers.skynons.nons

import com.skythinkers.skynons.api.RestoreSimulationRequest
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.deleteIfExists
import kotlin.io.path.getLastModifiedTime
import kotlin.time.Instant

class SuspendedNonsProcess(
    private val configPath: Path,
) : NonsProcess {
    override fun stopIfTooOld(minPermittedLastActivityTimestamp: Instant): Boolean {
        return if (configPath.getLastModifiedTime()
                .toMillis() < minPermittedLastActivityTimestamp.toEpochMilliseconds()
        ) {
            stop()
            true
        } else {
            false
        }
    }

    override fun stop() {
        configPath.deleteIfExists()
    }

    suspend fun resumeInto(process: ActiveNonsProcess) {
        process.message(RestoreSimulationRequest(configPath.absolutePathString()))
        configPath.deleteIfExists()
    }

    override fun hibernateIfTooOld(
        minPermittedLastActivityTimestamp: Instant,
        dir: Path
    ): SuspendedNonsProcess? = null
}
