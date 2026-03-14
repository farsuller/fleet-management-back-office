# Android Driver App — Sensor Data Pipeline

> **Goal:** Collect GPS + Accelerometer + Gyroscope data, fuse into `SensorPing`, batch, and transmit to `POST /v1/sensors/ping`.

---

## SensorPing Model

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/model/SensorPing.kt
data class SensorPing(
    val vehicleId: UUID,
    val latitude:     Double,
    val longitude:    Double,
    val accuracy:     Float,
    val speed:        Float,
    val heading:      Float,
    val accelX:       Float?,   // TYPE_ACCELEROMETER — m/s²
    val accelY:       Float?,
    val accelZ:       Float?,
    val gyroX:        Float?,   // TYPE_GYROSCOPE — rad/s
    val gyroY:        Float?,
    val gyroZ:        Float?,
    val batteryLevel: Int?,     // BatteryManager.EXTRA_LEVEL (0–100)
    val timestamp:    Instant,
    val routeId:      String? = null,
)
```

**Validation rules:**
- `speed` must be in `0.0..100.0` m/s (or null)
- `heading` must be in `0.0..360.0` (or null)
- `accuracy` must be `≥ 0` (or null)
- `accuracy > 50m` → skip ping entirely (low-quality GPS lock)

---

## SensorEngine

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/service/SensorEngine.kt
class SensorEngine @Inject constructor(
    private val context: Context,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val sensorManager: SensorManager,
) {
    private val _pings = MutableSharedFlow<SensorPing>()
    val pings: SharedFlow<SensorPing> = _pings

    private var lastAccel = FloatArray(3)
    private var lastGyro  = FloatArray(3)

    fun start(vehicleId: UUID) {
        // 1. GPS via FusedLocationProvider (adaptive interval)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000L).build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())

        // 2. Accelerometer — 200ms (SENSOR_DELAY_NORMAL)
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(accelListener, accel, SensorManager.SENSOR_DELAY_NORMAL)

        // 3. Gyroscope — 200ms
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        sensorManager.registerListener(gyroListener, gyro, SensorManager.SENSOR_DELAY_NORMAL)
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val loc = result.lastLocation ?: return
            if ((loc.accuracy ?: Float.MAX_VALUE) > 50f) return  // accuracy gate
            scope.launch {
                _pings.emit(SensorPing(
                    vehicleId    = vehicleId,
                    latitude     = loc.latitude,
                    longitude    = loc.longitude,
                    accuracy     = loc.accuracy,
                    speed        = if (loc.hasSpeed()) loc.speed else 0f,
                    heading      = if (loc.hasBearing()) loc.bearing else 0f,
                    accelX       = lastAccel[0],
                    accelY       = lastAccel[1],
                    accelZ       = lastAccel[2],
                    gyroX        = lastGyro[0],
                    gyroY        = lastGyro[1],
                    gyroZ        = lastGyro[2],
                    batteryLevel = getBatteryLevel(),
                    timestamp    = Instant.now(),
                ))
            }
        }
    }

    private val accelListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) { lastAccel = event.values.clone() }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }

    private val gyroListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) { lastGyro = event.values.clone() }
        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit
    }

    private fun getBatteryLevel(): Int {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        return if (level >= 0 && scale > 0) (level * 100 / scale) else -1
    }

    fun stop() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        sensorManager.unregisterListener(accelListener)
        sensorManager.unregisterListener(gyroListener)
    }

    /** 
     * Item 3.15 — Local detection for UI feedback only.
     * Note: Final detection happens on backend for authority.
     */
    fun detectEvent(ping: SensorPing): DrivingEvent? {
        return when {
            (ping.accelX ?: 0f) < -4.0f -> DrivingEvent.HARSH_BRAKE
            (ping.accelX ?: 0f) > 4.0f  -> DrivingEvent.HARSH_ACCEL
            Math.abs(ping.gyroZ ?: 0f) > 1.5f -> DrivingEvent.SHARP_TURN
            else -> null
        }
    }
}

enum class DrivingEvent { HARSH_BRAKE, HARSH_ACCEL, SHARP_TURN }
```

---

## Adaptive GPS Sampling

```kotlin
class AdaptiveLocationStrategy {
    fun getLocationRequest(vehicleState: VehicleState): LocationRequest {
        return when {
            vehicleState.isStationary -> LocationRequest.Builder(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY, 60_000L  // 1 min — stationary
            ).build()
            vehicleState.isHighSpeed -> LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 5_000L  // 5 sec — high speed
            ).build()
            else -> LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY, 10_000L  // 10 sec — normal
            ).build()
        }
    }
}
```

---

## CoordinateBuffer → Transmission

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/repository/CoordinateRepository.kt
class CoordinateRepository @Inject constructor(
    private val apiClient: FleetApiClient,
    private val roomDb: LocalDatabase,
    private val notificationManager: NotificationManagerCompat,
) {
    private val buffer = mutableListOf<SensorPing>()
    private val BATCH_SIZE = 10

    suspend fun addCoordinate(ping: SensorPing) {
        if (!ping.isValid()) return
        buffer.add(ping)
        if (buffer.size >= BATCH_SIZE) flushBuffer()
    }

    suspend fun flushBuffer() {
        if (buffer.isEmpty()) return
        val batch = buffer.toList()
        buffer.clear()
        try {
            val response = apiClient.sendCoordinates(batch)
            when (response.status.value) {
                202 -> { /* success — discard */ }
                503 -> handleCoordinateReceptionDisabled()
                else -> roomDb.saveCoordinates(batch)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send coordinates", e)
            roomDb.saveCoordinates(batch)
        }
    }

    private fun handleCoordinateReceptionDisabled() {
        notificationManager.notify(DISABLED_NOTIFICATION_ID,
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("GPS Tracking Paused")
                .setContentText("Location tracking disabled by fleet manager")
                .setSmallIcon(R.drawable.ic_pause)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
        scheduleRetry(delayMillis = 300_000L)  // retry after 5 min
    }

    private fun scheduleRetry(delayMillis: Long) {
        WorkManager.getInstance(context).enqueue(
            OneTimeWorkRequestBuilder<CoordinateRetryWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .build()
        )
    }
}
```

---

## API Client

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/api/FleetApiClient.kt
class FleetApiClient @Inject constructor() {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(Logging) { level = LogLevel.INFO }
    }

    suspend fun sendCoordinates(pings: List<SensorPing>): HttpResponse {
        return client.post("$BASE_URL/v1/sensors/ping") {
            contentType(ContentType.Application.Json)
            header("Idempotency-Key", UUID.randomUUID().toString())
            header("Authorization", "Bearer ${tokenProvider.getToken()}")
            setBody(pings)
        }
    }

    companion object {
        private const val BASE_URL = "https://api.fleetmanagement.com"
    }
}
```

---

## Backend Endpoint Contract

```http
POST /v1/sensors/ping
Authorization: Bearer <driver-jwt>
Idempotency-Key: <uuid-v4>
Content-Type: application/json

[{ "vehicleId": "...", "latitude": 14.6935, "longitude": 121.0744,
   "accuracy": 12.5, "speed": 8.3, "heading": 245.0,
   "accelX": -0.45, "accelY": 0.12, "accelZ": 9.82,
   "gyroX": 0.01, "gyroY": -0.02, "gyroZ": 0.08,
   "batteryLevel": 78, "timestamp": "2026-03-13T14:00:00Z" }]
```

| Response | Condition |
|---|---|
| `202 Accepted` | Pings processed |
| `429 Too Many Requests` | > 60 pings/min per vehicle |
| `503 Service Unavailable` | `COORDINATE_RECEPTION_DISABLED` |

> **⚠️ Note:** `POST /v1/sensors/ping` **does not exist yet** in the backend — see backend implementation doc.
