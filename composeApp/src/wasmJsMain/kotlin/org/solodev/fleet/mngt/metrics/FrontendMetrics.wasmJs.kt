package org.solodev.fleet.mngt.metrics

// js() in Kotlin/Wasm must be a single expression in a top-level function body.
@Suppress("UnusedParameter")
@OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
private fun consoleDebug(msg: String): Unit = js("console.debug(msg)")

internal actual fun performanceMeasure(
    name: String,
    detail: Map<String, Any>,
) {
    val entries = detail.entries.joinToString(", ") { "\"${it.key}\": ${it.value}" }
    consoleDebug("[Fleet Metrics] $name { $entries }")
}
