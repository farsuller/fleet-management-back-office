package org.solodev.fleet.mngt.metrics

// No-op on Android — metrics are browser-only
internal actual fun performanceMeasure(
    name: String,
    detail: Map<String, Any>,
) = Unit
