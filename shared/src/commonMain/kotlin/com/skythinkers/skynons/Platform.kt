package com.skythinkers.skynons

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform