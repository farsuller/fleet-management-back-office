package org.solodev.fleet.mngt.platform

private const val defaultApiBaseUrl = "http://localhost:8080"
private const val defaultWsBaseUrl = "ws://localhost:8080"

// js() in Kotlin/Wasm must be a single expression in a top-level function body.
@Suppress("UnusedParameter")
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun readRuntimeApiBaseUrl(): String? = js("globalThis.__FLEET_CONFIG__?.apiBaseUrl ?? null")

// js() in Kotlin/Wasm must be a single expression in a top-level function body.
@Suppress("UnusedParameter")
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun readRuntimeWsBaseUrl(): String? = js("globalThis.__FLEET_CONFIG__?.wsBaseUrl ?? null")

private fun normalizeBaseUrl(value: String): String = value.trim().removeSuffix("/")

private fun deriveWsBaseUrl(apiBaseUrl: String): String =
    when {
        apiBaseUrl.startsWith("https://") -> "wss://${apiBaseUrl.removePrefix("https://")}"
        apiBaseUrl.startsWith("http://") -> "ws://${apiBaseUrl.removePrefix("http://")}"
        else -> apiBaseUrl
    }.removeSuffix("/")

actual object PlatformConfig {
    actual val apiBaseUrl: String = readRuntimeApiBaseUrl()?.let(::normalizeBaseUrl) ?: defaultApiBaseUrl
    actual val wsBaseUrl: String = readRuntimeWsBaseUrl()?.let(::normalizeBaseUrl) ?: deriveWsBaseUrl(apiBaseUrl).ifBlank { defaultWsBaseUrl }
}
