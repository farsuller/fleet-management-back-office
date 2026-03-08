package org.solodev.fleet.mngt.platform

expect object PlatformConfig {
    val apiBaseUrl: String
    val wsBaseUrl: String
}
