# Android Driver App — GPS Tracking & Coordinate Transmission

## Status
- Overall: **Ready for Implementation**
- Refined Date: 2026-02-26
- **Verification Responsibility**:
    - **Lead Developer (USER)**: Android instrumentation tests, GPS accuracy tests
    - **Architect (Antigravity)**: Battery optimization, background service reliability

---

## 👥 Persona Comparison (Unified App Experience)

Instead of separate binaries, the Fleet Management system uses a **Unified Mobile App**. The application dynamically switches its interface and capabilities based on the logged-in user's roles.

| Feature/Usage | **Driver Persona** | **Customer Persona** |
|:---|:---|:---|
| **Primary User** | Professional Fleet Drivers | End-customers / Renters |
| **Core Goal** | Telemetry, safety monitoring, route compliance | Booking, payments, vehicle unlocking |
| **Data Flow** | **High Write / Continuous**: GPS pings, sensor data (gyro, accel) every few seconds. | **High Read / Discrete**: Browsing catalog, checking rental status, making payments. |
| **Connectivity** | Must handle long periods of offline/tunnel travel. | Usually high-quality network (LTE/Wi-Fi) during booking. |
| **Hardware Use** | Heavy use of GPS, Accelerometer, Gyroscope. | Camera (license scans), NFC/Bluetooth (unlocking). |
| **UI Focus** | Low-interaction, high-visibility status. | Rich browsing, interactive maps, payment forms. |
| **Activation** | Requires `DRIVER` role. Started manually or by shift. | Default for all registered `CUSTOMER` users. |

---

## 🛠️ Unified Architecture: The Role Dispatcher

The application uses a **Dispatcher Pattern** to handle RBAC-based feature toggling. This ensures that sensitive driver telemetry code is only active when a qualified driver is logged in.

### **1. Identity Flow**
1. **Login**: User provides credentials.
2. **Token Receipt**: Server returns a JWT containing `roles: ["CUSTOMER", "DRIVER"]`.
3. **Dispatch**: The app decrypts the JWT and routes the user:
    - If `roles` contains `DRIVER` → Show **Driver Dashboard** (or prompt to enter Driver Mode).
    - If `roles` only contains `CUSTOMER` → Show standard **Rental Catalog**.

### **2. Feature Toggling Logic**
```kotlin
// Simplified Dispatcher logic
class AppDependencyDispatcher(private val userSession: UserSession) {
    
    fun getInitialDestination(): Screen {
        return when {
            userSession.hasRole(UserRole.DRIVER) -> Screen.DriverDashboard
            else -> Screen.CustomerHome
        }
    }

    fun shouldStartTelemetry(): Boolean {
        // Only start background services if the user is a driver AND is 'On Clock'
        return userSession.hasRole(UserRole.DRIVER) && userSession.isShiftActive
    }
}
```

### **3. Permission Management**
To maintain a clean user experience, permissions are requested **Contextually**:
- **Customer**: Requires Camera (for license) and minimal GPS (to find nearby cars).
- **Driver**: Transitions to a "Driver Mode" setup phase where High-Accuracy GPS and Background Location permissions are requested only once the driver attempts to start their shift.

---

## 🏎️ Driver App: Feature Laydown

The Driver App is built for reliability and "set-and-forget" operation. It is the primary sensor for the Fleet Management system.

### **1. Advanced Telemetry Engine**
- **Foreground Tracking**: Continuous tracking via Android Foreground Service with persistent notification.
- **Smart Sampling**: Adaptive collection rates (10s moving / 60s idle) to save battery.
- **Sensor Fusion**: Combined data from GPS, Accelerometer, and Gyroscope for 3D vehicle state telemetry.

### **2. Operational Resilience**
- **Offline Buffer**: High-speed Room DB stores coordinate batches when network is lost.
- **WorkManager Sync**: Automatic, battery-aware background synchronization with exponential backoff.
- **Work Hours Geofencing**: Automatic start/stop of tracking based on configured schedules and map boundaries.

### **3. Safety & Compliance**
- **Harsh Event Detection**: Local analysis of sensor data to flag harsh braking, swerving, or rapid acceleration.
- **Shift Management**: Integration with driver schedules to prevent accidental off-duty tracking.
- **Interactive Console**: Simple dashboard showing trip duration, current state, and connectivity status.

---

## Scope Note

This document is now **driver-focused only**.
Customer-mobile scope is documented in:
- `docs/frontend-implementations/android-implementations/android-customer-app.md`

---

## Purpose
Implement the Android driver application that captures GPS coordinates from the device and transmits them to the backend via HTTP API. This app runs as a foreground service to ensure continuous location tracking even when the app is in the background.

---

## Technical Strategy

### 1. Technology Stack
- **Kotlin**: Modern Android development
- **Jetpack Compose**: Declarative UI for driver interface
- **Ktor Client**: HTTP communication with backend
- **Fused Location Provider**: Google Play Services for accurate GPS
- **WorkManager**: Reliable background coordinate transmission
- **Foreground Service**: Continuous tracking with notification

### 2. Architecture Principles
- **Clean Architecture**: Domain, Use Case, Infrastructure layers
- **MVVM Pattern**: ViewModel + StateFlow for reactive UI
- **Repository Pattern**: Abstract location and network data sources
- **Dependency Injection**: Hilt for testable components

### 2.1 Applied Modular Architecture (Driver App)

Driver app module layout (inside this KMP repository):

```text
:app:driver
    - bootstrap, DI graph, root navigation

:feature:driver-tracking
    - presentation: tracking screens, state, actions
    - domain: tracking use cases, repository contracts
    - data: tracking api adapters, mappers

:feature:driver-safety
    - harsh event detection, alert policies

:feature:driver-settings
    - work hours, geofence, privacy toggles

:feature:driver-history
    - timeline/history UI and paging state

:shared:tracking-contracts (KMP)
    - ApiResponse envelope, DTOs, request/response models

:shared:auth-session (KMP)
    - token/session model and role dispatch contracts

:core:network
    - Ktor client setup, auth interceptors, error mapping

:core:database
    - Room database, DAOs, local queue persistence

:core:navigation
    - app-level routes and entry contracts

:core:ui
    - design system primitives and reusable components
```

Dependency direction:

```text
presentation -> domain -> data -> core
```

Rules applied:
- Presentation never talks directly to storage/network.
- Domain owns repository interfaces and policies.
- Data implements domain contracts and maps DTO/entity/domain.
- Android platform features (Foreground Service, WorkManager, sensors, geofencing) remain in Android-specific modules.

### 3. Battery Optimization & Smart Tracking
- **Adaptive Sampling**: Reduce GPS frequency when stationary
- **Batching**: Send multiple coordinates in single request
- **Doze Mode Handling**: Use WorkManager for guaranteed delivery
- **Geofencing**: Reduce tracking when outside service area
- **Work Hours Detection**: Auto-pause tracking outside configured hours
- **Privacy Protection**: Prevent data collection when driver off-duty

### 4. Privacy & Storage Protection

**Problem**: Driver forgets to turn off GPS after work → Local database fills with unnecessary data

**Solution**: Automatic tracking controls

| Protection | Implementation | Benefit |
|------------|----------------|---------|
| **Work Hours** | Auto-pause outside 6 AM - 10 PM | No off-hours data collection |
| **Geofencing** | Stop tracking outside service area | Privacy + battery savings |
| **Shift Detection** | Backend shift schedule integration | Only track during assigned shifts |
| **Manual Override** | Driver can force-stop anytime | User control |
| **Storage Limits** | Max 1000 pings, 7-day retention | Prevents disk exhaustion |

---

## Data Flow Architecture

### GPS Coordinate Transmission Pipeline

```
┌──────────────────────────────────────────────────────────┐
│ 1. Sensor Data Collection                               │
├──────────────────────────────────────────────────────────┤
│ A. GPS Location Provider                                │
│    ├─ Fused Location Provider (Google Play Services)    │
│    ├─ Priority: PRIORITY_HIGH_ACCURACY                  │
│    ├─ Interval: 10 seconds (moving) / 60 seconds (idle) │
│    └─ Data: lat, lng, accuracy, speed, heading          │
│                                                          │
│ B. Accelerometer Sensor                                 │
│    ├─ Sensor Type: TYPE_ACCELEROMETER                   │
│    ├─ Sample Rate: SENSOR_DELAY_NORMAL (200ms)          │
│    └─ Data: accelX, accelY, accelZ (m/s²)              │
│    └─ Use: Detect harsh braking, acceleration           │
│                                                          │
│ C. Gyroscope Sensor                                     │
│    ├─ Sensor Type: TYPE_GYROSCOPE                       │
│    ├─ Sample Rate: SENSOR_DELAY_NORMAL (200ms)          │
│    └─ Data: gyroX, gyroY, gyroZ (rad/s)                │
│    └─ Use: Detect sharp turns, swerving                 │
├──────────────────────────────────────────────────────────┤
│ 2. Sensor Data Fusion                                   │
│ ├─ Combine GPS + Accel + Gyro into SensorPing           │
│ ├─ Timestamp synchronization                            │
│ └─ Apply low-pass filter to reduce noise                │
├──────────────────────────────────────────────────────────┤
│ 3. Location Validation                                  │
│ ├─ Check accuracy (< 50 meters)                         │
│ ├─ Check timestamp (not stale)                          │
│ ├─ Check speed (realistic for vehicle)                  │
│ └─ Validate sensor data ranges                          │
├──────────────────────────────────────────────────────────┤
│ 4. Coordinate Batching                                  │
│ ├─ Buffer up to 10 sensor pings                         │
│ ├─ Flush on: 60 seconds OR 10 pings OR app pause        │
│ └─ Persist to local DB if network unavailable           │
├──────────────────────────────────────────────────────────┤
│ 5. HTTP Transmission                                    │
│ POST /v1/tracking/vehicles/{id}/location                │
│ ├─ Success (200) → Clear buffer                         │
│ ├─ Failure (429) → Respect retry-after/backoff          │
│ ├─ Failure (503) → Coordinate reception disabled        │
│ │   └─ Show notification, pause tracking, retry later   │
│ └─ Network error → Persist, retry with backoff          │
└──────────────────────────────────────────────────────────┘
```

### Integration with Coordinate Toggle Feature

The app gracefully handles coordinate rejection when the [Coordinate Reception Toggle](docs/implementations/feature-coordinate-reception-toggle.md) is disabled:

**Server Response: 503 Service Unavailable**
```json
{
    "success": false,
    "error": {
        "code": "COORDINATE_RECEPTION_DISABLED",
        "message": "Location tracking is currently disabled. Please try again later."
    },
    "requestId": "req_..."
}
```

**App Behavior**:
1. Show persistent notification: "GPS tracking paused by fleet manager"
2. Stop requesting GPS updates (save battery)
3. **Stop sensor listeners** (accelerometer, gyroscope)
4. Schedule retry after 5 minutes
5. Log event for driver visibility

## Backend-Aligned Driver Feature Breakdown

### Authentication and Driver Session
- Login via `POST /v1/users/login` and store JWT securely.
- Decode role claims and activate driver feature graph only when role includes `DRIVER`.
- Use `Authorization: Bearer <token>` for protected tracking calls.

### Driver Tracking Core
- Route catalog: `GET /v1/tracking/routes/active`.
- Live telemetry write: `POST /v1/tracking/vehicles/{id}/location`.
- Latest state read: `GET /v1/tracking/vehicles/{vehicleId}/state`.
- History read: `GET /v1/tracking/vehicles/{vehicleId}/history?limit=&offset=`.
- Live updates: `WS /v1/fleet/live` (Ping/Pong + reconnect).

### Driver Reliability and Safety
- Offline queue in Room with replay order guarantees.
- Retry with WorkManager and exponential backoff.
- `Idempotency-Key` for retry-safe POST location updates.
- Handle `429`, `503`, and network failure as first-class states.
- Show status banner: `TRACKING`, `PAUSED`, `OFFLINE`, `RATE_LIMITED`.

### Driver Compliance and Privacy
- Work-hour enforcement for off-duty auto-pause.
- Geofence auto-pause/resume for service-area boundaries.
- Daily off-hours notification throttling.
- Storage caps and retention cleanup.

### Driver Feature Acceptance Checklist
- [ ] Driver can start/stop foreground tracking service.
- [ ] GPS + accelerometer + gyroscope pings are captured.
- [ ] Route selection is loaded from backend route catalog.
- [ ] Location updates post to `/v1/tracking/vehicles/{id}/location`.
- [ ] Live state renders from `/v1/tracking/vehicles/{vehicleId}/state`.
- [ ] Tracking history renders with pagination.
- [ ] WebSocket reconnect restores live updates after drops.
- [ ] 429/503/network errors are visible and retriable.
- [ ] Off-duty and out-of-geofence auto-pause works correctly.

## Routing Integration Checklist (Migrated)

### Implementation-Accurate Android API Contract
- Route catalog: `GET /v1/tracking/routes/active`.
- Driver location updates: `POST /v1/tracking/vehicles/{id}/location`.
- Vehicle state: `GET /v1/tracking/vehicles/{vehicleId}/state`.
- Tracking history: `GET /v1/tracking/vehicles/{vehicleId}/history?limit=100&offset=0`.
- Live stream: `WS /v1/fleet/live`.

### Android Build Checklist
- Use Ktor client with JWT auth interceptor.
- Add retry policy for `429`, network failure, and `503`.
- Persist outbound pings in Room when offline.
- WorkManager task flushes queued pings with idempotency keys.
- Add connection status UI: `TRACKING`, `PAUSED`, `OFFLINE`, `RATE_LIMITED`.
- Add route selector based on `GET /v1/tracking/routes/active`.

### Android Backend Checklist (Ready to Implement)

#### 1) Base Networking
- [ ] Base URL points to fleet backend API host.
- [ ] JSON serialization configured for `ApiResponse<T>` envelope.
- [ ] Global request timeout configured (connect + socket).
- [ ] Logging interceptor enabled for non-production builds only.

#### 2) Required Headers
- [ ] `Authorization: Bearer <jwt>` for protected endpoints.
- [ ] `Content-Type: application/json` for POST requests.
- [ ] `Accept: application/json` for all HTTP requests.
- [ ] `Idempotency-Key: <uuid>` for `POST /v1/tracking/vehicles/{id}/location` retries.

#### 3) Endpoint Wiring
- [ ] `GET /v1/tracking/routes/active` for selectable route catalog.
- [ ] `POST /v1/tracking/vehicles/{id}/location` for telemetry updates.
- [ ] `GET /v1/tracking/vehicles/{vehicleId}/state` for current status panel.
- [ ] `GET /v1/tracking/vehicles/{vehicleId}/history?limit=&offset=` for trip timeline.
- [ ] `WS /v1/fleet/live` for live updates.

#### 4) Success and Error Handling
- [ ] Parse `ApiResponse.success` + `data` before updating UI state.
- [ ] Handle `429` with retry delay/backoff.
- [ ] Handle `401/403` with token refresh or forced logout.
- [ ] Handle `503` and network failures by storing unsent pings locally.
- [ ] Surface backend `requestId` in logs for support/debug tracing.

#### 5) Offline and Delivery Guarantees
- [ ] Room table for outbound location queue.
- [ ] WorkManager periodic flush with exponential backoff.
- [ ] Preserve send order per vehicle when replaying queued pings.
- [ ] De-duplicate retries with stable idempotency keys.

#### 6) Sample Envelope Parsing

Success shape:
```json
{
    "success": true,
    "data": {
        "vehicleId": "0a8e652a-604d-4cfb-81c6-e14eccc80ac8",
        "routeId": "68a1a7f1-76dd-4ec9-ad63-fefc22acf428",
        "progress": 0.42,
        "status": "IN_TRANSIT"
    },
    "requestId": "req_abc123"
}
```

Error shape:
```json
{
    "success": false,
    "error": {
        "code": "RATE_LIMIT_EXCEEDED",
        "message": "Too many location updates. Wait 12s before retrying."
    },
    "requestId": "req_def456"
}
```

#### 7) Ktor Client Interface (Template)

```kotlin
interface TrackingApi {
        suspend fun getActiveRoutes(): ApiResponse<List<RouteDto>>

        suspend fun postVehicleLocation(
                vehicleId: String,
                idempotencyKey: String,
                body: LocationUpdateRequest
        ): ApiResponse<LocationUpdateAckDto>

        suspend fun getVehicleState(vehicleId: String): ApiResponse<VehicleStateDto>

        suspend fun getVehicleHistory(
                vehicleId: String,
                limit: Int = 100,
                offset: Int = 0
        ): ApiResponse<TrackingHistoryDto>
}

@kotlinx.serialization.Serializable
data class LocationUpdateRequest(
        val latitude: Double,
        val longitude: Double,
        val speed: Double,
        val heading: Double,
        val accuracy: Double,
        val routeId: String? = null
)

@kotlinx.serialization.Serializable
data class ApiResponse<T>(
        val success: Boolean,
        val data: T? = null,
        val error: ApiError? = null,
        val requestId: String? = null
)

@kotlinx.serialization.Serializable
data class ApiError(
        val code: String,
        val message: String
)
```

#### 8) WebSocket Client Checklist
- [ ] Connect to `WS /v1/fleet/live` with JWT-authenticated session.
- [ ] Implement Ping/Pong keepalive.
- [ ] Reconnect with capped exponential backoff.
- [ ] Merge deltas into in-memory `VehicleRouteState` store.
- [ ] Trigger full snapshot refresh on desync/reconnect.

---

## Sensor Data Collection

### Three Primary Sensors

| Sensor | Purpose | Sample Rate | Data Points |
|--------|---------|-------------|-------------|
| **GPS** | Vehicle location, speed, heading | 10 seconds | lat, lng, accuracy, speed, heading |
| **Accelerometer** | Harsh braking, rapid acceleration | 200ms | accelX, accelY, accelZ (m/s²) |
| **Gyroscope** | Sharp turns, swerving, cornering | 200ms | gyroX, gyroY, gyroZ (rad/s) |

### Why All Three Sensors?

1. **GPS alone** cannot detect:
   - Harsh braking (sudden deceleration)
   - Sharp turns (rapid direction change)
   - Driving quality metrics

2. **Accelerometer** detects:
   - Forward/backward acceleration (harsh braking/acceleration)
   - Lateral acceleration (turning forces)
   - Vertical bumps (road quality)

3. **Gyroscope** detects:
   - Rotation rate around axes
   - Sharp turns and swerving
   - Vehicle stability

### Driving Event Detection Examples

**Harsh Braking**: `accelX < -4.0 m/s²` (sudden deceleration)  
**Harsh Acceleration**: `accelX > 4.0 m/s²` (rapid speed increase)  
**Sharp Turn**: `gyroZ > 1.5 rad/s` (rapid rotation)  
**Swerving**: Rapid `gyroZ` oscillation

---

## Offline-First Strategy

### Overview

The Android driver app implements a **robust offline-first architecture** to handle network connectivity issues, especially when vehicles enter areas with no signal (tunnels, remote areas, underground parking).

### Strategy Components

```
┌──────────────────────────────────────────────────────────┐
│ Offline-First Data Flow                                 │
├──────────────────────────────────────────────────────────┤
│ 1. Sensor data collected (GPS + Accel + Gyro)           │
│    ↓                                                     │
│ 2. Add to in-memory buffer (10 pings)                   │
│    ↓                                                     │
│ 3. Attempt network transmission                         │
│    ├─ SUCCESS → Clear buffer                            │
│    ├─ NETWORK ERROR → Save to Room database             │
│    └─ 503 DISABLED → Save to Room + Show notification   │
│    ↓                                                     │
│ 4. Background WorkManager retries pending data          │
│    ├─ Exponential backoff (1min, 5min, 15min, 1hr)     │
│    └─ Automatic retry when network restored             │
│    ↓                                                     │
│ 5. Once sent successfully → Delete from local DB        │
└──────────────────────────────────────────────────────────┘
```

### Key Features

| Feature | Implementation | Benefit |
|---------|----------------|---------|
| **Local Persistence** | Room database with encrypted storage | No data loss in no-signal zones |
| **Automatic Retry** | WorkManager with exponential backoff | Reliable delivery without manual intervention |
| **Network Awareness** | ConnectivityManager callbacks | Smart retry only when network available |
| **Storage Limits** | Max 1000 pings (configurable) | Prevents disk space exhaustion |
| **Data Encryption** | SQLCipher for Room database | Protects sensitive location data |

### Room Database Schema

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/data/local/CoordinateDatabase.kt
@Database(entities = [SensorPingEntity::class], version = 1)
abstract class CoordinateDatabase : RoomDatabase() {
    abstract fun sensorPingDao(): SensorPingDao
}

@Entity(tableName = "sensor_pings")
data class SensorPingEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val vehicleId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float,
    val heading: Float,
    val accelX: Float?,
    val accelY: Float?,
    val accelZ: Float?,
    val gyroX: Float?,
    val gyroY: Float?,
    val gyroZ: Float?,
    val timestamp: Long,
    val batteryLevel: Int?,
    val retryCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface SensorPingDao {
    @Insert
    suspend fun insertAll(pings: List<SensorPingEntity>)
    
    @Query("SELECT * FROM sensor_pings ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getPendingPings(limit: Int = 50): List<SensorPingEntity>
    
    @Query("DELETE FROM sensor_pings WHERE id IN (:ids)")
    suspend fun deletePings(ids: List<Long>)
    
    @Query("SELECT COUNT(*) FROM sensor_pings")
    suspend fun getPendingCount(): Int
    
    @Query("DELETE FROM sensor_pings WHERE createdAt < :timestamp")
    suspend fun deleteOldPings(timestamp: Long)
}
```

### Retry Worker (WorkManager)

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/workers/CoordinateRetryWorker.kt
@HiltWorker
class CoordinateRetryWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val coordinateRepository: CoordinateRepository
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        return try {
            val success = coordinateRepository.retryPendingCoordinates()
            
            if (success) {
                Result.success()
            } else {
                // Retry with exponential backoff
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Retry worker failed", e)
            Result.retry()
        }
    }
    
    companion object {
        private const val TAG = "CoordinateRetryWorker"
        
        fun scheduleRetry(context: Context, delayMillis: Long = 60_000L) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            
            val retryWork = OneTimeWorkRequestBuilder<CoordinateRetryWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    1, TimeUnit.MINUTES
                )
                .build()
            
            WorkManager.getInstance(context).enqueueUniqueWork(
                "coordinate_retry",
                ExistingWorkPolicy.REPLACE,
                retryWork
            )
        }
    }
}
```

### Network Connectivity Monitoring

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/network/NetworkMonitor.kt
class NetworkMonitor @Inject constructor(
    private val context: Context
) {
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // Network restored - trigger retry
            CoordinateRetryWorker.scheduleRetry(context, delayMillis = 0L)
        }
        
        override fun onLost(network: Network) {
            Log.i(TAG, "Network lost - data will be queued locally")
        }
    }
    
    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, networkCallback)
    }
    
    fun stopMonitoring() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
    
    companion object {
        private const val TAG = "NetworkMonitor"
    }
}
```

### Storage Management

```kotlin
// Prevent disk space exhaustion
class StorageManager {
    suspend fun enforceStorageLimits(dao: SensorPingDao) {
        val count = dao.getPendingCount()
        
        if (count > MAX_PENDING_PINGS) {
            // Delete oldest pings beyond limit
            val cutoffTime = System.currentTimeMillis() - MAX_AGE_MILLIS
            dao.deleteOldPings(cutoffTime)
            
            Log.w(TAG, "Storage limit exceeded, deleted old pings")
        }
    }
    
    companion object {
        private const val MAX_PENDING_PINGS = 1000
        private const val MAX_AGE_MILLIS = 7 * 24 * 60 * 60 * 1000L // 7 days
        private const val TAG = "StorageManager"
    }
}
```

### Work Hours and Geofencing Governance

| Scenario | Without Protection | With Protection |
|----------|-------------------|-----------------|
| Driver forgets to stop tracking | Off-duty pings accumulate | Auto-paused at end of work hours |
| Weekend/off-day operation | Unnecessary battery and data usage | Auto-paused on non-work days |
| Vehicle leaves service area | Irrelevant coordinates still uploaded | Geofence transition pauses tracking |
| Personal errands | Privacy exposure | Tracking limited to duty windows |
| Local queue pressure | DB growth risk | Retention + max pending limits |

Default configuration:
- Work hours: 6 AM to 10 PM
- Work days: Monday to Saturday
- Geofence radius: 50km from depot
- Storage cap: 1000 queued pings, 7-day retention

Compliance posture:
- Data minimization via off-duty pause
- Clear driver-visible pause/resume notifications
- Manual override path available
- Audit-friendly behavior with request IDs and logs

### Work Hours and Geofencing Implementation Components

#### 1) Work Hours Manager

```kotlin
class WorkHoursManager @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    fun isWithinWorkHours(): Boolean {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val startHour = sharedPreferences.getInt(KEY_START_HOUR, DEFAULT_START_HOUR)
        val endHour = sharedPreferences.getInt(KEY_END_HOUR, DEFAULT_END_HOUR)
        return currentHour in startHour until endHour
    }

    fun isWorkDay(): Boolean {
        val dayOfWeek = Calendar.getInstance().get(Calendar.DAY_OF_WEEK)
        return dayOfWeek in Calendar.MONDAY..Calendar.SATURDAY
    }

    fun shouldTrack(): Boolean = isWorkDay() && isWithinWorkHours()

    companion object {
        private const val KEY_START_HOUR = "work_start_hour"
        private const val KEY_END_HOUR = "work_end_hour"
        private const val DEFAULT_START_HOUR = 6
        private const val DEFAULT_END_HOUR = 22
    }
}
```

#### 2) Geofencing Manager

```kotlin
class GeofenceManager @Inject constructor(
    private val context: Context,
    private val geofencingClient: GeofencingClient
) {
    fun setupServiceAreaGeofence(
        latitude: Double,
        longitude: Double,
        radiusMeters: Float = 50_000f
    ) {
        val geofence = Geofence.Builder()
            .setRequestId("service_area")
            .setCircularRegion(latitude, longitude, radiusMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(
                Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT
            )
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val geofencePendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)
        }
    }
}

class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) return

        when (geofencingEvent.geofenceTransition) {
            Geofence.GEOFENCE_TRANSITION_EXIT -> pauseTracking(context)
            Geofence.GEOFENCE_TRANSITION_ENTER -> resumeTracking(context)
        }
    }
}
```

#### 3) Enhanced SensorTrackingService Guard

```kotlin
private fun handleNewLocation(location: Location) {
    if (!workHoursManager.shouldTrack()) {
        if (shouldShowOffHoursNotification()) {
            showOffHoursNotification()
        }
        return
    }

    if (location.accuracy > 50f) return

    val ping = SensorPing(
        vehicleId = getVehicleId(),
        latitude = location.latitude,
        longitude = location.longitude,
        // ... sensor and metadata fields
    )

    lifecycleScope.launch {
        coordinateRepository.addCoordinate(ping)
    }
}
```

#### 4) Shift Schedule Integration (Backend)

```kotlin
class ShiftScheduleRepository @Inject constructor(
    private val apiClient: FleetApiClient
) {
    suspend fun isOnDuty(): Boolean {
        val shift = getCurrentShift() ?: return false
        val now = Instant.now()
        return now in shift.startTime..shift.endTime
    }
}
```

#### 5) Tracking Settings UI

```kotlin
@Composable
fun TrackingSettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    // work-hours toggle
    // start/end hour controls
    // geofence toggle
    // privacy information card
}
```

### Additional Benefits and Privacy Notes

| Scenario | Without Protection | With Protection |
|----------|-------------------|-----------------|
| Forgot to turn off GPS | Off-duty pings continue | Auto-paused at configured end time |
| Weekend tracking | Unnecessary battery and data | Auto-paused on non-work days |
| Outside service area | Irrelevant location uploads | Geofence pauses tracking |
| Personal errands | Off-duty location exposure | Shift-window-only tracking |

Privacy compliance goals:
- Data minimization
- Right-to-disconnect behavior
- Transparent pause/resume notifications
- Configurable policy with sane defaults

---

## Dependencies & Setup

### build.gradle.kts (app module)
```kotlin
dependencies {
    // --- Jetpack Compose ---
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    
    // --- Location Services ---
    implementation(libs.play.services.location) // Fused Location Provider
    
    // --- Networking ---
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    
    // --- Background Work ---
    implementation(libs.androidx.work.runtime.ktx)
    
    // --- Dependency Injection ---
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.androidx.hilt.work)
    
    // --- Local Storage ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    
    // --- Lifecycle ---
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.service)
}
```

### AndroidManifest.xml
```xml
<manifest>
    <!-- Permissions -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    
    <!-- Sensor Features (optional, but recommended) -->
    <uses-feature android:name="android.hardware.sensor.accelerometer" android:required="false" />
    <uses-feature android:name="android.hardware.sensor.gyroscope" android:required="false" />
    
    <application>
        <!-- Foreground Service -->
        <service
            android:name=".tracking.LocationTrackingService"
            android:foregroundServiceType="location"
            android:exported="false" />
        
        <!-- WorkManager Worker -->
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup" />
        </provider>
    </application>
</manifest>
```

## KMP Dependency Compatibility (No Blockers)

You can keep Android in the same KMP repository without dependency blockers if dependencies are placed in the correct source sets.

| Dependency | Allowed in KMP repo | Recommended source set |
|------------|----------------------|------------------------|
| Ktor Client | Yes | `commonMain` contracts + platform engines |
| Kotlinx Serialization | Yes | `commonMain` |
| Koin | Yes | `commonMain` + platform module wiring |
| Hilt / Dagger | Yes | `androidMain` only |
| Room | Yes | `androidMain` only |
| Coil | Yes | `androidMain` (or Coil3 multiplatform if intentionally adopted) |
| Unit Tests | Yes | `commonTest` + `androidUnitTest` |
| Instrumentation Tests | Yes | `androidInstrumentedTest` |

Rule of thumb:
- Put domain models, API contracts, validation, and shared use cases in `commonMain`.
- Put Android platform capabilities (Foreground Service, WorkManager, sensors, Room, notifications, permissions) in `androidMain`.

This setup has no architectural blocker for your plan.

---

## Code Implementation

### 1. Domain Model

```kotlin
// shared/domain/model/SensorPing.kt
@Serializable
data class SensorPing(
    val vehicleId: @Serializable(with = UUIDSerializer::class) UUID,
    
    // GPS Data
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float,
    val heading: Float,
    
    // Accelerometer Data (m/s²)
    val accelX: Float? = null,
    val accelY: Float? = null,
    val accelZ: Float? = null,
    
    // Gyroscope Data (rad/s)
    val gyroX: Float? = null,
    val gyroY: Float? = null,
    val gyroZ: Float? = null,
    
    // Metadata
    val timestamp: Instant,
    val batteryLevel: Int? = null
)
```

### 2. Sensor Tracking Service (GPS + Accelerometer + Gyroscope)

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/tracking/SensorTrackingService.kt
@AndroidEntryPoint
class SensorTrackingService : LifecycleService(), SensorEventListener {
    
    @Inject lateinit var locationProvider: FusedLocationProviderClient
    @Inject lateinit var coordinateRepository: CoordinateRepository
    
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null
    private var gyroscope: Sensor? = null
    
    // Latest sensor readings
    private var lastAccelData: FloatArray? = null
    private var lastGyroData: FloatArray? = null
    
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        10_000L // 10 seconds
    ).apply {
        setMinUpdateIntervalMillis(5_000L)
        setMinUpdateDistanceMeters(10f)
    }.build()
    
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                handleNewLocation(location)
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize sensor manager
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        
        // Show foreground notification
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
        
        // Start location updates
        startLocationUpdates()
        
        // Start sensor listeners
        startSensorListeners()
        
        return START_STICKY
    }
    
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        locationProvider.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }
    
    private fun startSensorListeners() {
        // Register accelerometer (200ms sample rate)
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL // ~200ms
            )
        }
        
        // Register gyroscope (200ms sample rate)
        gyroscope?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_NORMAL // ~200ms
            )
        }
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                // Apply low-pass filter to reduce noise
                lastAccelData = applyLowPassFilter(event.values.clone(), lastAccelData)
            }
            Sensor.TYPE_GYROSCOPE -> {
                lastGyroData = event.values.clone()
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Log accuracy changes if needed
    }
    
    private fun applyLowPassFilter(input: FloatArray, output: FloatArray?): FloatArray {
        if (output == null) return input
        
        val alpha = 0.8f // Smoothing factor
        for (i in input.indices) {
            output[i] = alpha * output[i] + (1 - alpha) * input[i]
        }
        return output
    }
    
    private fun handleNewLocation(location: Location) {
        // Validate location
        if (location.accuracy > 50f) {
            Log.w(TAG, "Location accuracy too low: ${location.accuracy}m")
            return
        }
        
        // Create sensor ping with GPS + Accel + Gyro data
        val ping = SensorPing(
            vehicleId = getVehicleId(),
            // GPS Data
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = location.accuracy,
            speed = location.speed,
            heading = location.bearing,
            // Accelerometer Data
            accelX = lastAccelData?.get(0),
            accelY = lastAccelData?.get(1),
            accelZ = lastAccelData?.get(2),
            // Gyroscope Data
            gyroX = lastGyroData?.get(0),
            gyroY = lastGyroData?.get(1),
            gyroZ = lastGyroData?.get(2),
            // Metadata
            timestamp = Instant.ofEpochMilli(location.time),
            batteryLevel = getBatteryLevel()
        )
        
        // Send to repository (will batch and transmit)
        lifecycleScope.launch {
            coordinateRepository.addCoordinate(ping)
        }
    }
    
    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "GPS Tracking",
            NotificationManager.IMPORTANCE_LOW
        )
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fleet Tracking Active")
            .setContentText("Sending location + sensor updates")
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .build()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        locationProvider.removeLocationUpdates(locationCallback)
        sensorManager.unregisterListener(this)
    }
    
    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "location_tracking"
        private const val TAG = "SensorTrackingService"
    }
}
```

### 3. Coordinate Repository

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/data/CoordinateRepository.kt
class CoordinateRepository @Inject constructor(
    private val apiClient: FleetApiClient,
    private val localDatabase: CoordinateDatabase,
    private val notificationManager: NotificationManager
) {
    private val coordinateBuffer = mutableListOf<SensorPing>()
    private val bufferLock = Mutex()
    
    suspend fun addCoordinate(ping: SensorPing) {
        bufferLock.withLock {
            coordinateBuffer.add(ping)
            
            // Flush if buffer is full or 60 seconds elapsed
            if (coordinateBuffer.size >= 10 || shouldFlush()) {
                flushBuffer()
            }
        }
    }
    
    private suspend fun flushBuffer() {
        if (coordinateBuffer.isEmpty()) return
        
        val batch = coordinateBuffer.toList()
        coordinateBuffer.clear()
        
        try {
            val response = apiClient.sendCoordinateBatch(batch)
            
            when (response.status) {
                HttpStatusCode.OK -> {
                    Log.i(TAG, "Coordinates sent successfully: ${batch.size}")
                }
                HttpStatusCode.TooManyRequests -> {
                    // Respect server-side rate limits and retry with backoff
                    localDatabase.saveCoordinates(batch)
                    scheduleRetry(delayMillis = 60_000L)
                }
                HttpStatusCode.ServiceUnavailable -> {
                    handleCoordinateReceptionDisabled()
                }
                else -> {
                    // Save to local DB for retry
                    localDatabase.saveCoordinates(batch)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send coordinates", e)
            localDatabase.saveCoordinates(batch)
        }
    }
    
    private fun handleCoordinateReceptionDisabled() {
        // Show notification
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("GPS Tracking Paused")
            .setContentText("Location tracking disabled by fleet manager")
            .setSmallIcon(R.drawable.ic_pause)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        
        notificationManager.notify(DISABLED_NOTIFICATION_ID, notification)
        
        // Schedule retry after 5 minutes
        scheduleRetry(delayMillis = 300_000L)
    }
    
    private fun scheduleRetry(delayMillis: Long) {
        val retryWork = OneTimeWorkRequestBuilder<CoordinateRetryWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .build()
        
        WorkManager.getInstance(context).enqueue(retryWork)
    }
    
    companion object {
        private const val TAG = "CoordinateRepository"
        private const val DISABLED_NOTIFICATION_ID = 1002
    }
}
```

### 4. API Client

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/api/FleetApiClient.kt
class FleetApiClient @Inject constructor() {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
            })
        }
        install(Logging) {
            level = LogLevel.INFO
        }
    }
    
    // Current backend contract accepts one location update per request.
    // Batch behavior is handled client-side by iterating over queued pings.
    suspend fun sendCoordinateBatch(pings: List<SensorPing>): HttpResponse {
        require(pings.isNotEmpty()) { "At least one ping is required" }

        var lastResponse: HttpResponse? = null
        pings.forEach { ping ->
            lastResponse = client.post("$BASE_URL/v1/tracking/vehicles/${ping.vehicleId}/location") {
                contentType(ContentType.Application.Json)
                header("Idempotency-Key", java.util.UUID.randomUUID().toString())
                setBody(
                    mapOf(
                        "latitude" to ping.latitude,
                        "longitude" to ping.longitude,
                        "speed" to ping.speed,
                        "heading" to ping.heading,
                        "accuracy" to ping.accuracy
                    )
                )
            }
        }

        return checkNotNull(lastResponse)
    }
    
    companion object {
        private const val BASE_URL = "https://api.fleetmanagement.com"
    }
}
```

### 5. Driver UI (Jetpack Compose)

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/ui/TrackingScreen.kt
@Composable
fun TrackingScreen(viewModel: TrackingViewModel = hiltViewModel()) {
    val trackingState by viewModel.trackingState.collectAsState()
    val lastLocation by viewModel.lastLocation.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Fleet Tracking") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Card
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (trackingState) {
                            TrackingState.ACTIVE -> "Tracking Active"
                            TrackingState.PAUSED -> "Tracking Paused"
                            TrackingState.DISABLED -> "Disabled by Manager"
                        },
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    lastLocation?.let { location ->
                        Text("Lat: ${location.latitude}")
                        Text("Lng: ${location.longitude}")
                        Text("Accuracy: ${location.accuracy}m")
                        Text("Speed: ${location.speed} m/s")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Start/Stop Button
            Button(
                onClick = { viewModel.toggleTracking() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (trackingState == TrackingState.ACTIVE) 
                        "Stop Tracking" 
                    else 
                        "Start Tracking"
                )
            }
        }
    }
}
```

---

## Battery Optimization Strategy

### Adaptive GPS Sampling

```kotlin
class AdaptiveLocationStrategy {
    fun getLocationRequest(vehicleState: VehicleState): LocationRequest {
        return when {
            vehicleState.isStationary -> {
                // Reduce frequency when not moving
                LocationRequest.Builder(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    60_000L // 1 minute
                ).build()
            }
            vehicleState.isHighSpeed -> {
                // Increase frequency at high speed
                LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    5_000L // 5 seconds
                ).build()
            }
            else -> {
                // Normal tracking
                LocationRequest.Builder(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    10_000L // 10 seconds
                ).build()
            }
        }
    }
}
```

---

## Testing Strategy

### Unit Tests
```kotlin
class CoordinateRepositoryTest {
    @Test
    fun `should batch coordinates before sending`() = runTest {
        val repository = CoordinateRepository(mockApiClient, mockDb, mockNotificationManager)
        
        repeat(5) { repository.addCoordinate(createMockPing()) }
        
        verify(mockApiClient, never()).sendCoordinateBatch(any())
        
        repeat(5) { repository.addCoordinate(createMockPing()) }
        
        verify(mockApiClient, times(1)).sendCoordinateBatch(argThat { size == 10 })
    }
    
    @Test
    fun `should handle 503 response gracefully`() = runTest {
        whenever(mockApiClient.sendCoordinateBatch(any()))
            .thenReturn(HttpResponse(HttpStatusCode.ServiceUnavailable))
        
        repository.addCoordinate(createMockPing())
        repository.flushBuffer()
        
        verify(mockNotificationManager).notify(eq(DISABLED_NOTIFICATION_ID), any())
        verify(mockWorkManager).enqueue(any<OneTimeWorkRequest>())
    }
}
```

### Instrumentation Tests
```kotlin
@RunWith(AndroidJUnit4::class)
class LocationTrackingServiceTest {
    @Test
    fun testLocationUpdatesStarted() {
        val scenario = ServiceScenario.launch(LocationTrackingService::class.java)
        
        scenario.onService { service ->
            // Verify foreground notification shown
            assertNotNull(service.getForegroundNotification())
            
            // Verify location updates requested
            verify(mockLocationProvider).requestLocationUpdates(
                any(),
                any<LocationCallback>(),
                any()
            )
        }
    }
}
```

---

## Observability

### Metrics
- **GPS Accuracy**: Average accuracy of location fixes
- **Transmission Success Rate**: % of successful coordinate uploads
- **Battery Drain**: mAh consumed per hour of tracking
- **Network Efficiency**: Bytes sent per coordinate

### Logging
```kotlin
Log.i(TAG, "Location update", mapOf(
    "lat" to location.latitude,
    "lng" to location.longitude,
    "accuracy" to location.accuracy,
    "speed" to location.speed,
    "batteryLevel" to batteryLevel
))
```

---

## Security Considerations

1. **Authentication**: JWT/session secrets are encrypted with Android Keystore-backed crypto and stored via DataStore.
2. **HTTPS Only**: All API communication over TLS
3. **Location Privacy**: Coordinates encrypted in local database
4. **Permission Handling**: Runtime permission requests with rationale

---

## Definition of Done

- [ ] Foreground service runs continuously
- [ ] GPS + Accelerometer + Gyroscope data collected
- [ ] Coordinates batched and transmitted efficiently
- [ ] 503 Service Unavailable handled gracefully
- [ ] **Offline-first: Data persisted locally when no network**
- [ ] **Work hours detection: Auto-pause outside 6 AM - 10 PM**
- [ ] **Geofencing: Auto-pause outside service area**
- [ ] **Storage limits enforced: Max 1000 pings, 7-day retention**
- [ ] Battery drain < 5% per hour
- [ ] All unit and instrumentation tests pass
- [ ] Notification shown when tracking active/paused

## Implementation Bill of Materials (Driver)

### Core Dependencies
- Ktor Client + `kotlinx.serialization` for API transport and envelope parsing.
- Koin DI for module composition and feature-level wiring.
- Room for offline telemetry queue and replay persistence.
- Android Keystore (`KeyStore`/`KeyGenerator`/`Cipher`) + DataStore for secure token/session persistence.
- WorkManager for retry/backoff delivery guarantees.
- Android location/sensor stack (Fused Location, accelerometer, gyroscope).

### Optional/UX Dependencies
- Coil for image/icon-heavy screens when needed.
- Vector icon set (Material Symbols Extended or app-owned vectors) via `core:ui`.

### Module Ownership
- `commonMain`: DTOs, `ApiResponse<T>`, validation, domain contracts.
- `androidMain`: Room/WorkManager/Foreground Service/geofence/sensor integrations.
- `feature:*`: tracking, settings, safety, and history feature logic.
- `core:*`: network, database, navigation, and UI primitives.

### Test Stack
- `commonTest`: shared use cases and validation.
- `androidUnitTest`: batching, retry, repository, and mapper behavior.
- `androidInstrumentedTest`: service lifecycle, permission flow, background execution.

### Caching/Queue Policy
- Driver caching is write-heavy queueing, not read cache first.
- Preserve send order during replay.
- Enforce retention and max queue limits (7 days / 1000 pings baseline).

### Efficient WorkManager Usage (Driver App)
- Use WorkManager only for deferrable and guaranteed delivery tasks (queued telemetry replay, deferred sync cleanup, recovery retries).
- Do not route live tracking capture through WorkManager; keep immediate sensor ingestion in foreground service and enqueue only failed/offline deliveries.
- Apply constraints (`NetworkType.CONNECTED`) so retry jobs run only when they can succeed.
- Use unique work names per queue type to avoid duplicate workers and redundant uploads.
- Use exponential backoff with capped retries to reduce battery impact.
- Keep jobs idempotent by reusing stable `Idempotency-Key` values for replayed requests.
- Cancel stale jobs when tracking stops, vehicle changes, or driver logs out.

---

## References

- [Backend Phase 6 - PostGIS](docs/implementations/phase-6-postgis-spatial-extensions.md)
- [Backend Phase 7 - WebSocket](docs/implementations/phase-7-schematic-visualization-engine.md)
- [Coordinate Reception Toggle](docs/implementations/feature-coordinate-reception-toggle.md)
- [Web Frontend - Schematic Visualization](docs/frontend-implementations/web-schematic-visualization.md)
- [Android Customer App](docs/frontend-implementations/android-implementations/android-customer-app.md)

---

## 📱 Mobile / Client Integration (Hardening Patterns)

When integrating the Fleet Management API into mobile apps, use these patterns to ensure reliability and data integrity.

### **1. Generating the Idempotency-Key (Android/Kotlin)**
Always generate a fresh UUID for every new "Action" (like a button tap).
```kotlin
// Inside your ViewModel or Repository
val idempotencyKey = java.util.UUID.randomUUID().toString()

// Add to your Retrofit/Ktor-Client headers:
// .header("Idempotency-Key", idempotencyKey)
```

### **2. Handling Retries on Mobile**
If a mobile request fails due to a `SocketTimeoutException` or `NoRouteToHostException`:
1.  **Do NOT** generate a new UUID.
2.  **REUSE** the same UUID from the first attempt.
3.  This ensures that if the first request actually reached the server but the response was dropped, the second attempt will safely return the cached success.

### **3. Ktor Client Example (Android/Mobile)**
If you are using the Ktor Client on Android, you can either pass it manually or use a simple `DefaultRequest` plugin.

**Manual usage per call:**
```kotlin
suspend fun submitPayment(invoiceId: String, amount: Long) {
    // 1. Generate the key ONCE for this transaction
    val transactionId = java.util.UUID.randomUUID().toString()

    client.post("https://api.v1/accounting/invoices/$invoiceId/pay") {
        contentType(ContentType.Application.Json)
        // 2. Attach the key to the header
        header("Idempotency-Key", transactionId)
        setBody(PaymentRequest(amount = amount))
    }
}
```

**Why generating it "just once" matters:**
If the request times out, you call `submitPayment` again. By keeping the **same** `transactionId`, the server knows it's the same attempt. If you generated a *new* UUID on every retry, you'd risk paying twice if the first request actually reached the server but the response was blocked by a weak mobile signal!

### **4. Full Android Clean Architecture Example (Koin + Ktor + MVVM)**
If you are using **MVVM**, **Clean Architecture**, and **Koin**, follow this flow for features requiring idempotency:

#### **A. The ViewModel (Presentation)**
The ViewModel is the best place to generate the key because it outlives simple screen rotations.
```kotlin
class InvoiceViewModel(private val payInvoiceUseCase: PayInvoiceUseCase) : ViewModel() {
    
    fun processPayment(invoiceId: String, amount: Long) {
        // 1. Generate the key ONCE at the start of the user action
        val idempotencyKey = UUID.randomUUID().toString()
        
        viewModelScope.launch {
            val result = payInvoiceUseCase.execute(idempotencyKey, invoiceId, amount)
            // handle success/failure
        }
    }
}
```

#### **B. The Use Case (Domain)**
```kotlin
class PayInvoiceUseCase(private val repository: InvoiceRepository) {
    suspend fun execute(key: String, id: String, amount: Long) = 
        repository.pay(key, id, amount)
}
```

#### **C. The Repository Implementation (Data/Infrastructure)**
```kotlin
class InvoiceRepositoryImpl(private val client: HttpClient) : InvoiceRepository {
    override suspend fun pay(key: String, id: String, amount: Long) {
        client.post("https://api.v1/accounting/invoices/$id/pay") {
            header("Idempotency-Key", key) // Pass the key generated in the VM
            setBody(PaymentRequest(amount))
        }
    }
}
```
