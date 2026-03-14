# Android Driver App — SensorTrackingService (Foreground Service)

> **Goal:** Keep GPS + sensor collection running continuously while the driver's screen is off or the app is backgrounded.

---

## Service Overview

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/service/SensorTrackingService.kt
@AndroidEntryPoint
class SensorTrackingService : Service() {

    @Inject lateinit var sensorEngine: SensorEngine
    @Inject lateinit var coordinateRepository: CoordinateRepository
    @Inject lateinit var workHoursManager: WorkHoursManager

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var vehicleId: UUID? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        vehicleId = intent?.getStringExtra(EXTRA_VEHICLE_ID)?.let { UUID.fromString(it) }
            ?: return START_NOT_STICKY

        if (!workHoursManager.isWithinWorkHours()) {
            stopSelf()
            return START_NOT_STICKY
        }

        scope.launch {
            sensorEngine.start(vehicleId!!)
            sensorEngine.pings.collect { ping ->
                coordinateRepository.addCoordinate(ping)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        sensorEngine.stop()
        scope.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Fleet Tracking Active")
            .setContentText("Your location is being recorded")
            .setSmallIcon(R.drawable.ic_location)
            .setOngoing(true)
            .build()

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 1001
        const val CHANNEL_ID = "fleet_tracking"
        const val EXTRA_VEHICLE_ID = "vehicle_id"

        fun start(context: Context, vehicleId: UUID) {
            val intent = Intent(context, SensorTrackingService::class.java)
                .putExtra(EXTRA_VEHICLE_ID, vehicleId.toString())
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, SensorTrackingService::class.java))
        }
    }
}
```

---

## AndroidManifest Entries

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

<service
    android:name=".driver.service.SensorTrackingService"
    android:foregroundServiceType="location"
    android:exported="false" />
```

---

## Notification Channel Setup

```kotlin
// FleetApplication.kt (or MainActivity.onCreate)
fun createTrackingNotificationChannel(context: Context) {
    val channel = NotificationChannel(
        SensorTrackingService.CHANNEL_ID,
        "Fleet Tracking",
        NotificationManager.IMPORTANCE_LOW,  // silent — no sound while driving
    ).apply {
        description = "Live vehicle tracking for fleet management"
        setShowBadge(false)
    }
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(channel)
}
```

---

## Driver TrackingScreen UI

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/ui/TrackingScreen.kt
@Composable
fun TrackingScreen(viewModel: TrackingViewModel = hiltViewModel()) {
    val trackingState by viewModel.trackingState.collectAsState()
    val lastLocation  by viewModel.lastLocation.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Fleet Tracking") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = when (trackingState) {
                            TrackingState.ACTIVE   -> "Tracking Active"
                            TrackingState.PAUSED   -> "Tracking Paused"
                            TrackingState.DISABLED -> "Disabled by Manager"
                        },
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    lastLocation?.let {
                        Text("Lat: ${it.latitude}")
                        Text("Lng: ${it.longitude}")
                        Text("Accuracy: ${it.accuracy}m")
                        Text("Speed: ${it.speed} m/s")
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.toggleTracking() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (trackingState == TrackingState.ACTIVE) "Stop Tracking" else "Start Tracking")
            }
        }
    }
}
```

---

## AppDependencyDispatcher — Role Gate

```kotlin
// commonMain/auth/AppDependencyDispatcher.kt
object AppDependencyDispatcher {
    fun dispatch(jwt: String): UserFeatureSet {
        val roles = JwtDecoder.decodeRoles(jwt)
        return when {
            "DRIVER" in roles && "CUSTOMER" in roles -> UserFeatureSet.MultiRole
            "DRIVER" in roles                        -> UserFeatureSet.Driver
            "CUSTOMER" in roles                      -> UserFeatureSet.Customer
            else                                     -> UserFeatureSet.Backoffice
        }
    }
}

sealed interface UserFeatureSet {
    data object Driver    : UserFeatureSet
    data object Customer  : UserFeatureSet
    data object Backoffice: UserFeatureSet
    data object MultiRole : UserFeatureSet  // prompt user to choose
}
```

When dispatched as `Driver`, the app mounts the `SensorTrackingService` on login and shows the `TrackingScreen` as home.
