package org.solodev.fleet.mngt.tracking

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.solodev.fleet.mngt.api.dto.tracking.VehicleStateDelta
import org.solodev.fleet.mngt.auth.TokenProvider

private const val RECONNECT_DELAY_MS = 3_000L
private const val MAX_RECONNECT_ATTEMPTS = 5

class FleetLiveClient(
    private val wsBaseUrl: String,
    private val tokenProvider: TokenProvider,
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val client = HttpClient {
        install(WebSockets)
    }

    private val _deltas = MutableSharedFlow<VehicleStateDelta>(extraBufferCapacity = 128)
    val deltas: SharedFlow<VehicleStateDelta> = _deltas.asSharedFlow()

    private val _connectionState = MutableSharedFlow<ConnectionState>(
        replay = 1,
        extraBufferCapacity = 8,
    )
    val connectionState: SharedFlow<ConnectionState> = _connectionState.asSharedFlow()

    private var connectJob: Job? = null

    fun connect() {
        connectJob?.cancel()
        connectJob = scope.launch { connectWithRetry() }
    }

    fun disconnect() {
        connectJob?.cancel()
        _connectionState.tryEmit(ConnectionState.Disconnected)
    }

    private suspend fun connectWithRetry() {
        var attempts = 0
        while (attempts < MAX_RECONNECT_ATTEMPTS) {
            _connectionState.emit(
                if (attempts == 0) ConnectionState.Connecting else ConnectionState.Reconnecting(attempts)
            )
            try {
                val token = tokenProvider.token ?: run {
                    _connectionState.emit(ConnectionState.Error("No auth token"))
                    return
                }
                client.webSocket(
                    urlString = "$wsBaseUrl/v1/fleet/live?token=$token",
                ) {
                    attempts = 0
                    _connectionState.emit(ConnectionState.Connected)
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            runCatching {
                                json.decodeFromString<VehicleStateDelta>(frame.readText())
                            }.onSuccess { _deltas.emit(it) }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                attempts++
                if (attempts >= MAX_RECONNECT_ATTEMPTS) {
                    _connectionState.emit(ConnectionState.Error(e.message ?: "Connection failed"))
                    return
                }
                delay(RECONNECT_DELAY_MS * attempts)
            }
        }
    }

    fun close() {
        connectJob?.cancel()
        scope.cancel()
        client.close()
    }
}

sealed interface ConnectionState {
    data object Connecting : ConnectionState
    data object Connected : ConnectionState
    data class Reconnecting(val attempt: Int) : ConnectionState
    data object Disconnected : ConnectionState
    data class Error(val message: String) : ConnectionState
}
