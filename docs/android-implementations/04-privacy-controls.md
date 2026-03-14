# Android Driver App — Privacy Controls

> **Goal:** Ensure GPS tracking only occurs during agreed work hours and within the fleet's service area. Protect driver privacy outside of work.

---

## WorkHoursManager

Prevents off-duty tracking. Default: Mon–Sat, 06:00–22:00 local time (configurable per driver in Settings).

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/privacy/WorkHoursManager.kt
class WorkHoursManager @Inject constructor(
    private val prefs: SharedPreferences,
) {
    fun isWithinWorkHours(): Boolean {
        val now = LocalDateTime.now()
        val dayOfWeek = now.dayOfWeek

        // Sunday always off (can be overridden in settings)
        if (dayOfWeek == DayOfWeek.SUNDAY) return false

        val startHour = prefs.getInt(PREF_START_HOUR, 6)
        val endHour   = prefs.getInt(PREF_END_HOUR, 22)

        return now.hour in startHour until endHour
    }

    fun setWorkHours(startHour: Int, endHour: Int) {
        prefs.edit()
            .putInt(PREF_START_HOUR, startHour.coerceIn(0, 23))
            .putInt(PREF_END_HOUR,   endHour.coerceIn(1, 24))
            .apply()
    }

    companion object {
        const val PREF_START_HOUR = "work_start_hour"
        const val PREF_END_HOUR   = "work_end_hour"
    }
}
```

### Integration in SensorTrackingService

```kotlin
override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (!workHoursManager.isWithinWorkHours()) {
        Log.d(TAG, "Outside work hours — stopping tracking service")
        stopSelf()
        return START_NOT_STICKY
    }
    // ... proceed with tracking
}
```

---

## GeofenceManager

Stops tracking when driver leaves the 50km service area (Metro Manila default centre).

```kotlin
// app/src/main/kotlin/com/solodev/fleet/driver/privacy/GeofenceManager.kt
class GeofenceManager @Inject constructor(
    private val geofencingClient: GeofencingClient,
    private val context: Context,
) {
    private val SERVICE_AREA_LAT = 14.5995   // Metro Manila centroid
    private val SERVICE_AREA_LNG = 120.9842
    private val SERVICE_AREA_RADIUS_M = 50_000f  // 50 km

    fun registerServiceAreaFence() {
        val geofence = Geofence.Builder()
            .setRequestId("fleet_service_area")
            .setCircularRegion(SERVICE_AREA_LAT, SERVICE_AREA_LNG, SERVICE_AREA_RADIUS_M)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL)
            .addGeofence(geofence)
            .build()

        geofencingClient.addGeofences(request, geofencePendingIntent)
            .addOnFailureListener { e -> Log.e(TAG, "Geofence registration failed", e) }
    }

    private val geofencePendingIntent by lazy {
        PendingIntent.getBroadcast(
            context, 0,
            Intent(context, GeofenceBroadcastReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
    }
}
```

```kotlin
// app/.../GeofenceBroadcastReceiver.kt
@AndroidEntryPoint
class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.w(TAG, "Driver exited service area — pausing tracking")
            SensorTrackingService.stop(context)
            // Notify driver
            NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Tracking Paused")
                .setContentText("You have left the fleet service area")
                .setSmallIcon(R.drawable.ic_warning)
                .build()
                .also { NotificationManagerCompat.from(context).notify(2001, it) }
        }
    }
}
```

---

## Driver Settings Screen

```kotlin
// app/.../ui/SettingsScreen.kt
@Composable
fun DriverSettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val startHour by viewModel.startHour.collectAsState()
    val endHour   by viewModel.endHour.collectAsState()
    val geofenceEnabled by viewModel.geofenceEnabled.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Work Hours", style = MaterialTheme.typography.titleMedium)

        Text("Start: ${startHour}:00")
        Slider(
            value = startHour.toFloat(),
            onValueChange = { viewModel.setStartHour(it.toInt()) },
            valueRange = 0f..12f,
        )

        Text("End: ${endHour}:00")
        Slider(
            value = endHour.toFloat(),
            onValueChange = { viewModel.setEndHour(it.toInt()) },
            valueRange = 13f..24f,
        )

        Spacer(Modifier.height(24.dp))
        Text("Privacy Controls", style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Geofence tracking to service area", Modifier.weight(1f))
            Switch(
                checked = geofenceEnabled,
                onCheckedChange = { viewModel.setGeofenceEnabled(it) },
            )
        }
    }
}
```

---

## Implementation Checklist

- [ ] `WorkHoursManager` with SharedPreferences storage
- [ ] `WorkHoursManager.isWithinWorkHours()` checked in `SensorTrackingService.onStartCommand()`
- [ ] `GeofenceManager.registerServiceAreaFence()` called on tracking start
- [ ] `GeofenceBroadcastReceiver` registered in AndroidManifest
- [ ] `DriverSettingsScreen` with work hours sliders and geofence toggle
- [ ] `SettingsViewModel` persists changes via `WorkHoursManager`

### AndroidManifest Entries

```xml
<uses-permission android:name="android.permission.ACCESS_BACKGROUND_LOCATION" />

<receiver
    android:name=".driver.privacy.GeofenceBroadcastReceiver"
    android:exported="false" />
```
