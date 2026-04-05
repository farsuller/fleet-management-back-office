package org.solodev.fleet.mngt.platform

// Change 'class' to 'object' to match the expectation
actual object PlatformConfig {
    actual val apiBaseUrl: String = "http://localhost:8080"
    actual val wsBaseUrl: String = "ws://localhost:8080"
}
