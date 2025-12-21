package com.skythinkers.skynons.nons

import java.nio.file.Path
import kotlin.time.Instant

sealed interface NonsProcess {
    fun stopIfTooOld(minPermittedLastActivityTimestamp: Instant): Boolean
    fun hibernateIfTooOld(minPermittedLastActivityTimestamp: Instant, dir: Path): SuspendedNonsProcess?
    fun stop()
}
