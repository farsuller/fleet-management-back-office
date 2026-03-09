package org.solodev.fleet.mngt.metrics

/**
 * Frontend performance metrics recorded via the browser's Performance API.
 *
 * All public functions are compiled away in release builds by wrapping calls with [isEnabled].
 * Set `fleet.metrics.enabled = false` in your production build config to disable.
 *
 * Usage:
 *   FrontendMetrics.recordRenderTime("vehicle-1", 12.0)
 *   FrontendMetrics.recordWebSocketLatency(45.0)
 *   FrontendMetrics.recordAnimationFps(60.0)
 */
object FrontendMetrics {

    // Set to false in production via BuildConfig / compile-time constant
    private const val ENABLED = true

    fun recordRenderTime(vehicleId: String, durationMs: Double) {
        if (!ENABLED) return
        recordMeasure("vehicle_render", mapOf("vehicleId" to vehicleId, "durationMs" to durationMs))
    }

    fun recordWebSocketLatency(latencyMs: Double) {
        if (!ENABLED) return
        recordMeasure("ws_latency", mapOf("latencyMs" to latencyMs))
    }

    fun recordAnimationFps(fps: Double) {
        if (!ENABLED) return
        recordMeasure("animation_fps", mapOf("fps" to fps))
    }

    private fun recordMeasure(name: String, detail: Map<String, Any>) {
        // Delegates to the platform-specific Performance API implementation.
        // In WASM/JS this calls window.performance.measure(); the expect/actual
        // binding is resolved in wasmJsMain.
        performanceMeasure(name, detail)
    }
}

/**
 * Platform bridge — implemented via `expect`/`actual` so that WASM/JS can call
 * `window.performance.measure()` while other targets (e.g. Android) are no-ops.
 */
internal expect fun performanceMeasure(name: String, detail: Map<String, Any>)
