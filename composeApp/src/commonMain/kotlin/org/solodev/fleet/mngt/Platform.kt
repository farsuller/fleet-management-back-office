package org.solodev.fleet.mngt

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform